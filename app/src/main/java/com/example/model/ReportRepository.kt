package com.example.model

import kotlinx.coroutines.flow.Flow

class ReportRepository(private val reportDao: ReportDao) {
    val allReports: Flow<List<ReportHistoryEntity>> = reportDao.getAllReports()

    suspend fun insert(report: ReportHistoryEntity) = reportDao.insertReport(report)

    suspend fun deleteById(id: Int) = reportDao.deleteReportById(id)
}
