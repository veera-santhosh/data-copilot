package com.example.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.api.Content
import com.example.api.GenerateContentRequest
import com.example.api.GenerationConfig
import com.example.api.Part
import com.example.api.ResponseFormat
import com.example.api.ResponseFormatText
import com.example.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.io.BufferedReader
import java.io.InputStreamReader

import androidx.lifecycle.AndroidViewModel
import android.app.Application
import androidx.room.Room
import com.example.model.AppDatabase
import com.example.model.ReportRepository
import com.example.model.ReportHistoryEntity
import kotlinx.serialization.encodeToString

@kotlinx.serialization.Serializable
data class DataQualityReport(
    val summary: String,
    val missingValues: List<Issue>,
    val anomalies: List<Issue>,
    val labelProblems: List<Issue>,
    val conclusion: String,
    val cleaningCode: String,
    val summaryStats: SummaryStats,
    val chartData: List<ChartPoint>
) {
    @kotlinx.serialization.Serializable
    data class Issue(
        val itemInfo: String,
        val details: String
    )
    @kotlinx.serialization.Serializable
    data class SummaryStats(
        val columns: List<String>,
        val estimatedRows: Int,
        val dataTypes: String
    )
    @kotlinx.serialization.Serializable
    data class ChartPoint(
        val label: String,
        val value: Float
    )
}

enum class ChatRole { USER, MODEL }
data class ChatMessage(val role: ChatRole, val text: String)

data class DatasetContext(
    val fileMimeType: String,
    val textContent: String? = null,
    val inlineData: com.example.api.InlineData? = null
)

sealed class AppState {
    object Idle : AppState()
    object Loading : AppState()
    data class Success(
        val report: DataQualityReport,
        val datasetContext: DatasetContext,
        val chatMessages: List<ChatMessage> = emptyList(),
        val isChatLoading: Boolean = false
    ) : AppState()
    data class Error(val message: String) : AppState()
}

class DataCopilotViewModel(application: Application) : AndroidViewModel(application) {
    private val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java, "data-copilot-db"
    ).build()
    private val repository = ReportRepository(db.reportDao())

    val reportHistory = repository.allReports

    private val _uiState = MutableStateFlow<AppState>(AppState.Idle)
    val uiState = _uiState.asStateFlow()


    fun reset() {
        _uiState.value = AppState.Idle
    }

    fun updateDatasetContent(newContent: String) {
        val currentState = _uiState.value
        if (currentState is AppState.Success) {
            val updatedDatasetContext = currentState.datasetContext.copy(textContent = newContent)
            _uiState.value = currentState.copy(datasetContext = updatedDatasetContext)
        }
    }

    fun reAnalyzeFile(context: Context) {
        val currentState = _uiState.value
        if (currentState is AppState.Success) {
            val datasetContext = currentState.datasetContext
            if (datasetContext.textContent == null && datasetContext.inlineData == null) return
            
            viewModelScope.launch {
                _uiState.value = AppState.Loading
                try {
                    val report = fetchReportFromGemini(datasetContext)
                    if (report != null) {
                        val reportJson = Json.encodeToString(report)
                        repository.insert(ReportHistoryEntity(title = "Re-analyzed Report ${System.currentTimeMillis()}", reportJson = reportJson, isImage = datasetContext.inlineData != null))

                        _uiState.value = AppState.Success(
                            report = report,
                            datasetContext = datasetContext,
                            chatMessages = listOf(ChatMessage(ChatRole.MODEL, "Hi! I've re-analyzed your edited dataset. Ask me anything about it."))
                        )
                    } else {
                        _uiState.value = AppState.Error("Failed to parse report from AI.")
                    }
                } catch (e: Exception) {
                    _uiState.value = AppState.Error(e.message ?: "Unknown error occurred")
                }
            }
        }
    }

    private fun extractTextFromDocx(inputStream: java.io.InputStream): String {
        val stringBuilder = StringBuilder()
        try {
            val zipInputStream = java.util.zip.ZipInputStream(inputStream)
            var entry = zipInputStream.nextEntry
            while (entry != null) {
                if (entry.name == "word/document.xml") {
                    val scanner = java.util.Scanner(zipInputStream).useDelimiter("\\A")
                    val xmlContent = if (scanner.hasNext()) scanner.next() else ""
                    
                    val pMatcher = java.util.regex.Pattern.compile("<w:p(?:[^>]*)>(.*?)</w:p>").matcher(xmlContent)
                    while (pMatcher.find()) {
                        val pContent = pMatcher.group(1) ?: ""
                        val tMatcher = java.util.regex.Pattern.compile("<w:t(?:[^>]*)>([^<]*)</w:t>").matcher(pContent)
                        val paraText = StringBuilder()
                        while (tMatcher.find()) {
                            paraText.append(tMatcher.group(1))
                        }
                        if (paraText.isNotEmpty()) {
                            stringBuilder.append(paraText.toString()).append("\n")
                        }
                    }
                    break
                }
                zipInputStream.closeEntry()
                entry = zipInputStream.nextEntry
            }
            zipInputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return stringBuilder.toString().trim()
    }

    fun analyzeFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.value = AppState.Loading
            try {
                val mimeType = context.contentResolver.getType(uri) ?: "text/plain"
                var fileContent: String? = null
                var inlineData: com.example.api.InlineData? = null
                
                val isImage = mimeType.startsWith("image/")
                val isPdf = mimeType == "application/pdf"
                val isDocx = mimeType == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" || 
                             mimeType == "application/msword" || mimeType.contains("wordprocessing")
                
                if (isImage || isPdf) {
                    val base64Data = withContext(Dispatchers.IO) {
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val bytes = inputStream?.readBytes()
                        inputStream?.close()
                        if (bytes != null) android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP) else null
                    }
                    if (base64Data != null) {
                        val type = if (isPdf) "application/pdf" else mimeType
                        inlineData = com.example.api.InlineData(type, base64Data)
                    } else {
                        _uiState.value = AppState.Error("Failed to read file")
                        return@launch
                    }
                } else if (isDocx) {
                    fileContent = withContext(Dispatchers.IO) {
                        val inputStream = context.contentResolver.openInputStream(uri)
                        if (inputStream != null) {
                            val extracted = extractTextFromDocx(inputStream)
                            inputStream.close()
                            extracted
                        } else null
                    }
                    if (fileContent.isNullOrEmpty()) {
                        _uiState.value = AppState.Error("Empty or unsupported document format")
                        return@launch
                    }
                    fileContent = fileContent?.take(10000)
                } else {
                    fileContent = withContext(Dispatchers.IO) {
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val reader = java.io.BufferedReader(java.io.InputStreamReader(inputStream))
                        val stringBuilder = StringBuilder()
                        var line: String?
                        var count = 0
                        while (reader.readLine().also { line = it } != null && count < 1000 && stringBuilder.length < 10000) {
                            stringBuilder.append(line).append("\n")
                            count++
                        }
                        reader.close()
                        stringBuilder.toString()
                    }
                    if (fileContent.isNullOrEmpty()) {
                        _uiState.value = AppState.Error("Empty file")
                        return@launch
                    }
                }

                val datasetContext = DatasetContext(mimeType, fileContent, inlineData)
                val report = fetchReportFromGemini(datasetContext)
                if (report != null) {
                    // Save to Room DB
                    val reportJson = Json.encodeToString(report)
                    repository.insert(ReportHistoryEntity(title = "Report ${System.currentTimeMillis()}", reportJson = reportJson, isImage = inlineData != null))

                    _uiState.value = AppState.Success(
                        report = report,
                        datasetContext = datasetContext,
                        chatMessages = listOf(ChatMessage(ChatRole.MODEL, "Hi! I've analyzed your dataset. Ask me anything about it."))
                    )
                } else {
                    _uiState.value = AppState.Error("Failed to parse report from AI.")
                }

            } catch (e: Exception) {
                _uiState.value = AppState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }

    fun loadReport(entity: ReportHistoryEntity) {
        try {
            val report = Json.decodeFromString<DataQualityReport>(entity.reportJson)
            val datasetContext = DatasetContext(
                fileMimeType = if (entity.isImage) "image/unknown" else "text/plain",
                textContent = if (entity.isImage) null else "Restored from history"
            )
            _uiState.value = AppState.Success(
                report = report,
                datasetContext = datasetContext,
                chatMessages = listOf(ChatMessage(ChatRole.MODEL, "Hi! I restored this report. Ask me anything about it."))
            )
        } catch (e: Exception) {
            _uiState.value = AppState.Error("Failed to load historical report.")
        }
    }

    fun deleteReport(entity: ReportHistoryEntity) {
        viewModelScope.launch {
            repository.deleteById(entity.id)
        }
    }

    fun sendMessage(context: Context, text: String) {
        val currentState = _uiState.value
        if (currentState is AppState.Success) {
            val userMsg = ChatMessage(ChatRole.USER, text)
            val updatedMessages = currentState.chatMessages + userMsg
            _uiState.value = currentState.copy(chatMessages = updatedMessages, isChatLoading = true)
            
            viewModelScope.launch {
                try {
                    val promptText = "You are a helpful Data Quality Copilot analyzing a user's dataset. " +
                        "Here is the dataset summary we generated: ${currentState.report.summary}\n" +
                        "The user has a follow-up question. Question: $text\n" +
                        "If relevant, answer directly using the dataset summary or your general knowledge about data quality. Keep it concise."
                        
                    val partInfos = mutableListOf<Part>()
                    partInfos.add(Part(text = promptText))
                    if (currentState.datasetContext.inlineData != null) {
                        partInfos.add(Part(inlineData = currentState.datasetContext.inlineData))
                    } else if (currentState.datasetContext.textContent != null) {
                        partInfos.add(Part(text = "Dataset content extract:\n${currentState.datasetContext.textContent}"))
                    }

                    val request = GenerateContentRequest(
                        contents = listOf(Content(parts = partInfos)),
                        generationConfig = GenerationConfig(temperature = 0.4f)
                    )

                    val response = com.example.api.RetrofitClient.service.generateContent(com.example.BuildConfig.GEMINI_API_KEY, request)
                    val replyText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "I am not sure."
                    
                    val modelMsg = ChatMessage(ChatRole.MODEL, replyText)
                    val finalState = _uiState.value
                    if (finalState is AppState.Success) {
                        _uiState.value = finalState.copy(chatMessages = finalState.chatMessages + modelMsg, isChatLoading = false)
                    }
                } catch (e: Exception) {
                    val errorMsg = ChatMessage(ChatRole.MODEL, "Error: ${e.message}")
                    val finalState = _uiState.value
                    if (finalState is AppState.Success) {
                        _uiState.value = finalState.copy(chatMessages = finalState.chatMessages + errorMsg, isChatLoading = false)
                    }
                }
            }
        }
    }

    private suspend fun fetchReportFromGemini(datasetContext: DatasetContext): DataQualityReport? = withContext(Dispatchers.IO) {
        val promptText = """
            Analyze the following dataset sample or image. It may be in CSV, JSON, XML, or an image.
            Identify any data quality issues including missing values, label problems, and anomalies.
            Generate a JSON report according to the provided schema with a summary, missing values, anomalies, label problems, conclusion, cleaningCode, summaryStats, and chartData.
            If it's an image of a diagram, dataset or table, extract the tabular relationships into the same schema.
            ${if (datasetContext.textContent != null) "\nDataset Sample:\n${datasetContext.textContent}" else ""}
        """.trimIndent()

        val partInfos = mutableListOf<Part>()
        partInfos.add(Part(text = promptText))
        if (datasetContext.inlineData != null) {
            partInfos.add(Part(inlineData = datasetContext.inlineData))
        }

        val jsonSchema = buildJsonObject {
            put("type", "OBJECT")
            putJsonObject("properties") {
                putJsonObject("summary") { put("type", "STRING"); put("description", "A brief summary of the dataset characteristics.") }
                putJsonObject("missingValues") {
                    put("type", "ARRAY")
                    putJsonObject("items") {
                        put("type", "OBJECT")
                        putJsonObject("properties") {
                            putJsonObject("itemInfo") { put("type", "STRING"); put("description", "The column or row with missing values.") }
                            putJsonObject("details") { put("type", "STRING"); put("description", "Details about the missing values and suggestions.") }
                        }
                    }
                }
                putJsonObject("anomalies") {
                    put("type", "ARRAY")
                    putJsonObject("items") {
                        put("type", "OBJECT")
                        putJsonObject("properties") {
                            putJsonObject("itemInfo") { put("type", "STRING"); put("description", "The name of the item or row and column that is anomalous") }
                            putJsonObject("details") { put("type", "STRING"); put("description", "Description of the anomaly and severity") }
                        }
                    }
                }
                putJsonObject("labelProblems") {
                    put("type", "ARRAY")
                    putJsonObject("items") {
                        put("type", "OBJECT")
                        putJsonObject("properties") {
                            putJsonObject("itemInfo") { put("type", "STRING"); put("description", "Categorical label or target columns with issues.") }
                            putJsonObject("details") { put("type", "STRING"); put("description", "Details on the label issue e.g., typos, imbalance.") }
                        }
                    }
                }
                putJsonObject("conclusion") { put("type", "STRING"); put("description", "Overall recommendation based on the data quality.") }
                putJsonObject("cleaningCode") { put("type", "STRING"); put("description", "Python Pandas code to clean the identified issues.") }
                putJsonObject("summaryStats") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("columns") { put("type", "ARRAY"); putJsonObject("items") { put("type", "STRING") } }
                        putJsonObject("estimatedRows") { put("type", "INTEGER") }
                        putJsonObject("dataTypes") { put("type", "STRING") }
                    }
                }
                putJsonObject("chartData") {
                    put("type", "ARRAY")
                    put("description", "5 to 10 interesting aggregate data points to visualize as a bar chart")
                    putJsonObject("items") {
                        put("type", "OBJECT")
                        putJsonObject("properties") {
                            putJsonObject("label") { put("type", "STRING") }
                            putJsonObject("value") { put("type", "NUMBER") }
                        }
                    }
                }
            }
            put("required", kotlinx.serialization.json.JsonArray(listOf(
                kotlinx.serialization.json.JsonPrimitive("summary"),
                kotlinx.serialization.json.JsonPrimitive("missingValues"),
                kotlinx.serialization.json.JsonPrimitive("anomalies"),
                kotlinx.serialization.json.JsonPrimitive("labelProblems"),
                kotlinx.serialization.json.JsonPrimitive("conclusion"),
                kotlinx.serialization.json.JsonPrimitive("cleaningCode"),
                kotlinx.serialization.json.JsonPrimitive("summaryStats"),
                kotlinx.serialization.json.JsonPrimitive("chartData")
            )))
        }

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = partInfos)),
            generationConfig = GenerationConfig(
                responseFormat = ResponseFormat(
                    text = ResponseFormatText(
                        mimeType = "application/json",
                        schema = jsonSchema
                    )
                ),
                temperature = 0.2f
            )
        )

        try {
            val response = RetrofitClient.service.generateContent(BuildConfig.GEMINI_API_KEY, request)
            val jsonText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (jsonText != null) {
                val json = Json { ignoreUnknownKeys = true }
                @kotlinx.serialization.Serializable
                data class TempIssue(val itemInfo: String? = null, val details: String? = null)
                @kotlinx.serialization.Serializable
                data class TempSummaryStats(val columns: List<String>? = null, val estimatedRows: Int? = null, val dataTypes: String? = null)
                @kotlinx.serialization.Serializable
                data class TempChartPoint(val label: String? = null, val value: Float? = null)
                @kotlinx.serialization.Serializable
                data class TempReport(
                    val summary: String? = null,
                    val missingValues: List<TempIssue>? = null,
                    val anomalies: List<TempIssue>? = null,
                    val labelProblems: List<TempIssue>? = null,
                    val conclusion: String? = null,
                    val cleaningCode: String? = null,
                    val summaryStats: TempSummaryStats? = null,
                    val chartData: List<TempChartPoint>? = null
                )

                try {
                    val parsed = json.decodeFromString<TempReport>(jsonText)
                    return@withContext DataQualityReport(
                        summary = parsed.summary ?: "No summary provided.",
                        missingValues = parsed.missingValues?.map { DataQualityReport.Issue(it.itemInfo ?: "Unknown", it.details ?: "") } ?: emptyList(),
                        anomalies = parsed.anomalies?.map { DataQualityReport.Issue(it.itemInfo ?: "Unknown", it.details ?: "") } ?: emptyList(),
                        labelProblems = parsed.labelProblems?.map { DataQualityReport.Issue(it.itemInfo ?: "Unknown", it.details ?: "") } ?: emptyList(),
                        conclusion = parsed.conclusion ?: "No conclusion.",
                        cleaningCode = parsed.cleaningCode ?: "# No cleaning code generated",
                        summaryStats = DataQualityReport.SummaryStats(
                            columns = parsed.summaryStats?.columns ?: emptyList(),
                            estimatedRows = parsed.summaryStats?.estimatedRows ?: 0,
                            dataTypes = parsed.summaryStats?.dataTypes ?: "Unknown"
                        ),
                        chartData = parsed.chartData?.map { DataQualityReport.ChartPoint(it.label ?: "Unknown", it.value ?: 0f) } ?: emptyList()
                    )
                } catch (e: Exception) {
                    throw Exception("Failed to decode AI response. Raw json: ${jsonText.take(100)}...")
                }
            } else {
                throw Exception("AI returned empty response")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
}
