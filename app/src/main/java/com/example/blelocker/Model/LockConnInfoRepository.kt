package com.example.blelocker.Model

import androidx.annotation.WorkerThread
import com.example.blelocker.entity.LockConnectionInformation
import kotlinx.coroutines.flow.Flow

class LockConnInfoRepository(private val lockConnInfoDAO: LockConnInfoDAO) {

    val allLockInfo: Flow<List<LockConnectionInformation>> = lockConnInfoDAO.getAlphabetizedLocks()

    //todo
    @Suppress
    @WorkerThread
    suspend fun LockInsert(lockConnectionInformation: LockConnectionInformation){
        lockConnInfoDAO.insert(lockConnectionInformation)
    }

    suspend fun deleteAllLocks(){
        lockConnInfoDAO.deleteAllLocks()
    }
    suspend fun getLockConnectInformation(macAddress: String):LockConnectionInformation{
        return lockConnInfoDAO.getLockInfo(macAddress)
    }
    suspend fun LockUpdate(lockConnectionInformation: LockConnectionInformation){
        lockConnInfoDAO.insert(lockConnectionInformation)
    }
    suspend fun deleteAllLocks(lockConnectionInformation: LockConnectionInformation){
        lockConnInfoDAO.delete(lockConnectionInformation)
    }
}