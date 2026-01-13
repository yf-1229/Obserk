package com.obserk.data

import kotlinx.coroutines.flow.Flow

class StudyLogRepository(private val studyLogDao: StudyLogDao) {
    val allLogs: Flow<List<StudyLogEntity>> = studyLogDao.getAllLogs()
    val allLabels: Flow<List<StudyLabelEntity>> = studyLogDao.getAllLabels()

    suspend fun insert(log: StudyLogEntity) = studyLogDao.insert(log)
    suspend fun update(log: StudyLogEntity) = studyLogDao.update(log)
    suspend fun insertLabel(label: StudyLabelEntity) = studyLogDao.insertLabel(label)

    suspend fun getLatestLog(): StudyLogEntity? = studyLogDao.getLatestLog()
    suspend fun getLogById(id: Int): StudyLogEntity? = studyLogDao.getLogById(id)
}
