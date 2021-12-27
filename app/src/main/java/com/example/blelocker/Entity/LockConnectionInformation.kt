package com.example.blelocker.Entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant


@Entity(tableName = "lock_connection_information")
data class LockConnectionInformation(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "macAddress")
    val macAddress: String,

    @ColumnInfo(name = "display_name")
    val displayName: String = "",

    @ColumnInfo(name = "device_name", defaultValue = "")
    val deviceName: String = "",

    @ColumnInfo(name = "key_one")
    val keyOne: String,

    @ColumnInfo(name = "key_two")
    val keyTwo: String,

    @ColumnInfo(name = "one_off_token")
    val oneTimeToken: String,

    @ColumnInfo(name = "token")
    val permanentToken: String,

    @ColumnInfo(name = "is_owner_token")
    val isOwnerToken: Boolean,

    @ColumnInfo(name = "token_name")
    val tokenName: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = Instant.now().epochSecond * 1000L,

    @ColumnInfo(name = "update_at")
    val updateAt: Long = Instant.now().epochSecond * 1000L,

    val adminCode: String? = null,

    val permission: String? = null,

    @ColumnInfo(name = "is_auto_unlock_on")
    val isAutoUnlockOn: Boolean = false,

    @ColumnInfo(name = "is_enter_notify", defaultValue = "false")
    val isEnterNotify: Boolean = false,

    @ColumnInfo(name = "is_exit_notify", defaultValue = "false")
    val isExitNotify: Boolean = false,

    @ColumnInfo(name = "latitude")
    val latitude: Double? = null,

    @ColumnInfo(name = "longitude")
    val longitude: Double? = null,

    @ColumnInfo(name = "shared_from_user")
    val sharedFrom: String? = null,

    @ColumnInfo(name = "display_index")
    val index: Int
)