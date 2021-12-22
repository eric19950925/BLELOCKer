package com.example.blelocker.Model

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.blelocker.entity.LockConnectionInformation

@Database(entities = arrayOf(LockConnectionInformation::class), version = 1, exportSchema = false)
public abstract class LockConnInfoDatabase : RoomDatabase(){
    abstract fun lockConnInfoDao(): LockConnInfoDAO

}