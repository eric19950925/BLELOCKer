package com.example.blelocker.Model

import androidx.room.*
import com.example.blelocker.entity.LockConnectionInformation
import kotlinx.coroutines.flow.Flow

@Dao
interface LockConnInfoDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(information: LockConnectionInformation)

    @Query("SELECT * FROM lock_connection_information ORDER BY display_name ASC")
    fun getAlphabetizedLocks(): Flow<List<LockConnectionInformation>>
    //return flow can be unsuspend function

    @Query("SELECT * FROM lock_connection_information WHERE macAddress = :macAddress")
    suspend fun getLockInfo(macAddress: String):LockConnectionInformation

//    @Update
//    suspend fun upadate(information: LockConnectionInformation)
// use insert not OnConflictStrategy.IGNORE but replace

    @Query("DELETE FROM lock_connection_information")
    suspend fun deleteAllLocks()

    @Delete
    suspend fun delete(information: LockConnectionInformation)

}