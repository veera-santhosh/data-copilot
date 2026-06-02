package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.viewmodel.AppState
import com.example.viewmodel.DataCopilotViewModel
import com.example.viewmodel.DataQualityReport

import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontFamily
import android.content.Intent
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.IconButton
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap

import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Divider
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.core.entry.entryModelOf

@Composable
fun FilePrintLayoutView(datasetContext: com.example.viewmodel.DatasetContext, onUpdate: (String) -> Unit, onReAnalyze: () -> Unit) {
    if (datasetContext.inlineData != null) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            val imageBytes = android.util.Base64.decode(datasetContext.inlineData.data, android.util.Base64.DEFAULT)
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            if (bitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Preview Image",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text("Failed to load image preview.", color = MaterialTheme.colorScheme.error)
            }
        }
        return
    }

    var content by remember(datasetContext.textContent) { mutableStateOf(datasetContext.textContent ?: "") }

    if (datasetContext.textContent == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No text content available for preview.", color = MaterialTheme.colorScheme.onBackground)
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.TopCenter) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(0.dp) // Paper sharp edges
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(vertical = 32.dp, horizontal = 24.dp)) {
                Text(
                    text = "DOCUMENT PREVIEW (${datasetContext.fileMimeType})",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Divider(modifier = Modifier.padding(vertical = 12.dp))
                OutlinedTextField(
                    value = content,
                    onValueChange = { 
                        content = it
                        onUpdate(it)
                    },
                    modifier = Modifier.fillMaxSize(),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color.Black
                    ),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        cursorColor = Color.Black
                    )
                )
            }
        }
        
        FloatingActionButton(
            onClick = onReAnalyze,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 88.dp, end = 16.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ) {
            Icon(Icons.Default.Sync, contentDescription = "Re-analyze data", tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataQualityScreen(viewModel: DataCopilotViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val reportHistory by viewModel.reportHistory.collectAsState(initial = emptyList())
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.analyzeFile(context, it) }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.systemBars
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp)
                    .align(Alignment.Center)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 24.dp).clickable { viewModel.reset() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "App Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                    Text(
                        text = "Data Copilot",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                }

                // Main Content Area
                when (val state = uiState) {
                    is AppState.Idle, is AppState.Error -> {
                        UploadPrompt(
                            modifier = Modifier.weight(1f),
                            onUploadClick = { launcher.launch("*/*") },
                            errorMessage = if (state is AppState.Error) state.message else null,
                            history = reportHistory,
                            onHistoryClick = { viewModel.loadReport(it) },
                            onHistoryDelete = { viewModel.deleteReport(it) }
                        )
                    }
                    is AppState.Loading -> {
                        LoadingView(modifier = Modifier.weight(1f))
                    }
                    is AppState.Success -> {
                        var selectedTabIndex by remember { mutableStateOf(0) }
                        val tabs = listOf("Report", "Dashboard", "File", "Code", "Chat")
                        
                        Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
                            TabRow(selectedTabIndex = selectedTabIndex) {
                                tabs.forEachIndexed { index, title ->
                                    val icon = when(index) {
                                        0 -> Icons.Default.Assessment
                                        1 -> Icons.Default.Dashboard
                                        2 -> Icons.Default.Description
                                        3 -> Icons.Default.Code
                                        4 -> Icons.Default.Chat
                                        else -> Icons.Default.Assessment
                                    }
                                    Tab(
                                        selected = selectedTabIndex == index,
                                        onClick = { selectedTabIndex = index },
                                        text = { Text(title) },
                                        icon = { Icon(icon, contentDescription = null) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                                when (selectedTabIndex) {
                                    0 -> ReportView(report = state.report, onUploadNew = { launcher.launch("*/*") })
                                    1 -> DashboardView(report = state.report)
                                    2 -> FilePrintLayoutView(
                                        datasetContext = state.datasetContext,
                                        onUpdate = { newContent -> viewModel.updateDatasetContent(newContent) },
                                        onReAnalyze = { viewModel.reAnalyzeFile(context) }
                                    )
                                    3 -> CodeView(report = state.report)
                                    4 -> ChatView(
                                        messages = state.chatMessages,
                                        isLoading = state.isChatLoading,
                                        onSendMessage = { text -> viewModel.sendMessage(context, text) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UploadPrompt(
    modifier: Modifier = Modifier, 
    onUploadClick: () -> Unit, 
    errorMessage: String?,
    history: List<com.example.model.ReportHistoryEntity> = emptyList(),
    onHistoryClick: (com.example.model.ReportHistoryEntity) -> Unit = {},
    onHistoryDelete: (com.example.model.ReportHistoryEntity) -> Unit = {}
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onUploadClick() }
                .testTag("upload_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Upload,
                    contentDescription = "Upload Icon",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Tap to upload file",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Upload a dataset to generate a neat data quality report.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Error",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        if (history.isNotEmpty()) {
            Spacer(modifier = Modifier.height(32.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.History, contentDescription = "History", tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.widthIn(8.dp))
                Text("Recent Reports", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(history) { entity ->
                    val format = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onHistoryClick(entity) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(entity.title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                Text(format.format(Date(entity.timestamp)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f))
                            }
                            IconButton(onClick = { onHistoryDelete(entity) }) {
                                Icon(androidx.compose.material.icons.Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Analyzing Dataset...",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Finding missing values and anomalies.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun ReportView(modifier: Modifier = Modifier, report: DataQualityReport, onUploadNew: () -> Unit) {
    val context = LocalContext.current
    Box(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 72.dp)) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow))
            ) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        ReportSectionHeader("Summary", Icons.Default.Assessment)
                        Text(
                            text = report.summary,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            lineHeight = 24.sp
                        )
                    }

                    if (report.missingValues.isNotEmpty()) {
                        item { ReportSectionHeader("Missing Values", Icons.Default.Warning) }
                        items(report.missingValues) { issue ->
                            IssueCard(issue)
                        }
                    }

                    if (report.anomalies.isNotEmpty()) {
                        item { ReportSectionHeader("Anomalies", Icons.Default.Warning) }
                        items(report.anomalies) { issue ->
                            IssueCard(issue)
                        }
                    }

                    if (report.labelProblems.isNotEmpty()) {
                        item { ReportSectionHeader("Label Problems", Icons.Default.Warning) }
                        items(report.labelProblems) { issue ->
                            IssueCard(issue)
                        }
                    }

                    item {
                        ReportSectionHeader("Conclusion", Icons.Default.Assessment)
                        Text(
                            text = report.conclusion,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            lineHeight = 24.sp
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }

            Button(
                onClick = onUploadNew,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .height(56.dp)
                    .testTag("upload_new_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Upload, contentDescription = null)
                Spacer(modifier = Modifier.widthIn(8.dp))
                Text("Upload New Dataset", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        FloatingActionButton(
            onClick = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Data Quality Report")
                    putExtra(Intent.EXTRA_TEXT, "Data Quality Analytics:\n\n${report.summary}\n\nConclusion:\n${report.conclusion}")
                }
                context.startActivity(Intent.createChooser(intent, "Share Report"))
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 88.dp, end = 16.dp),
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ) {
            Icon(Icons.Default.Share, contentDescription = "Share Report", tint = MaterialTheme.colorScheme.onTertiaryContainer)
        }
    }
}

@Composable
fun ReportSectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.widthIn(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun IssueCard(issue: DataQualityReport.Issue) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = issue.itemInfo,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = issue.details,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun DashboardView(report: DataQualityReport) {
    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(start = 8.dp, end = 8.dp, bottom = 72.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Summary Statistics", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp, top = 16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatCard("Est. Rows", report.summaryStats.estimatedRows.toString())
                    StatCard("Columns", report.summaryStats.columns.size.toString())
                }
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Columns Detected", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(report.summaryStats.columns.joinToString(", "), style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Data Types", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(report.summaryStats.dataTypes, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            if (report.chartData.isNotEmpty()) {
                item {
                    Text("Key Data Points", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 16.dp))
                    BarChartCanvas(data = report.chartData)
                }
            }
        }
        
        FloatingActionButton(
            onClick = {
                val csvContent = buildString {
                    appendLine("label,value")
                    report.chartData.forEach { appendLine("${it.label},${it.value}") }
                }
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_SUBJECT, "Data Points Export")
                    putExtra(Intent.EXTRA_TEXT, csvContent)
                }
                context.startActivity(Intent.createChooser(intent, "Export Data as CSV"))
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp),
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ) {
            Icon(Icons.Default.Download, contentDescription = "Export CSV", tint = MaterialTheme.colorScheme.onTertiaryContainer)
        }
    }
}

@Composable
fun StatCard(title: String, value: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer), modifier = Modifier.padding(4.dp)) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

@Composable
fun BarChartCanvas(data: List<DataQualityReport.ChartPoint>) {
    if (data.isNotEmpty()) {
        val entries = data.mapIndexed { index, point -> 
            com.patrykandpatrick.vico.core.entry.FloatEntry(x = index.toFloat(), y = point.value)
        }
        
        val chartEntryModel = entryModelOf(entries)

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().height(250.dp).padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                Chart(
                    chart = columnChart(),
                    model = chartEntryModel,
                    startAxis = rememberStartAxis(),
                    bottomAxis = rememberBottomAxis(
                        valueFormatter = { value, _ ->
                            data.getOrNull(value.toInt())?.label?.take(8) ?: ""
                        }
                    ),
                )
            }
        }
    }
}

@Composable
fun CodeView(report: DataQualityReport) {
    val context = LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        Column(modifier = Modifier.fillMaxSize().padding(bottom = 72.dp)) {
            Text("Data Cleaning Code", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 16.dp))
            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(8.dp)
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    item {
                        Text(
                            text = report.cleaningCode,
                            color = Color(0xFFD4D4D4),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
        
        Row(modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            FloatingActionButton(
                onClick = {
                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(report.cleaningCode))
                    android.widget.Toast.makeText(context, "Code copied to clipboard!", android.widget.Toast.LENGTH_SHORT).show()
                },
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Icon(androidx.compose.material.icons.Icons.Default.ContentCopy, contentDescription = "Copy Code")
            }
            
            FloatingActionButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Data Cleaning Code")
                        putExtra(Intent.EXTRA_TEXT, report.cleaningCode)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share Code"))
                },
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            ) {
                Icon(Icons.Default.Share, contentDescription = "Share Code", tint = MaterialTheme.colorScheme.onTertiaryContainer)
            }
        }
    }
}

@Composable
fun ChatView(messages: List<com.example.viewmodel.ChatMessage>, isLoading: Boolean, onSendMessage: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth().weight(1f).padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                val isUser = msg.role == com.example.viewmodel.ChatRole.USER
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(
                            topStart = 16.dp, topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 0.dp,
                            bottomEnd = if (isUser) 0.dp else 16.dp
                        ),
                        modifier = Modifier.widthIn(max = 280.dp)
                    ) {
                        Text(
                            text = msg.text,
                            modifier = Modifier.padding(12.dp),
                            color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (isLoading) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(8.dp))
                    }
                }
            }
        }
        
        val quickPrompts = listOf("What are the main anomalies?", "How do I fix missing values?", "Generate a summary report.")
        androidx.compose.foundation.lazy.LazyRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickPrompts) { prompt ->
                androidx.compose.material3.AssistChip(
                    onClick = { onSendMessage(prompt) },
                    label = { Text(prompt) }
                )
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask about your data...") },
                singleLine = true,
                maxLines = 1,
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (text.isNotBlank() && !isLoading) {
                                onSendMessage(text)
                                text = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send")
                    }
                }
            )
        }
    }
}
