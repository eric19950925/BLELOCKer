package com.example.blelocker.Model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.blelocker.entity.LockConnectionInformation
import kotlinx.coroutines.flow.Flow

@Dao
interface LockConnInfoDAO {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(information: LockConnectionInformation)

    @Query("SELECT * FROM lock_connection_information ORDER BY display_name ASC")
    fun getAlphabetizedLocks(): Flow<List<LockConnectionInformation>>

}