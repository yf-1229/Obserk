package com.obserk.data

import kotlinx.coroutines.flow.Flow

class StudyLogRepository(private val studyLogDao: StudyLogDao) {
    val allLogs: Flow<List<StudyLogEntity>> = studyLogDao.getAllLogs()

    suspend fun insert(log: StudyLogEntity) {
        studyLogDao.insert(log)
    }
}
