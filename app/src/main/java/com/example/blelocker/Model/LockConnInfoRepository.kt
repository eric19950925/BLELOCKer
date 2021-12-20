package com.example.blelocker.Model

import androidx.annotation.WorkerThread
import com.example.blelocker.entity.LockConnectionInformation
import kotlinx.coroutines.flow.Flow

class LockConnInfoRepository(private val lockConnInfoDAO: LockConnInfoDAO) {

    val allLockInfo: Flow<List<LockConnectionInformation>> = lockConnInfoDAO.getAlphabetizedLocks()

    @Suppress
    @WorkerThread
    suspend fun LockInsert(lockConnectionInformation: LockConnectionInformation){
        lockConnInfoDAO.insert(lockConnectionInformation)
    }
}