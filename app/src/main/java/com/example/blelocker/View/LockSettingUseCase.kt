package com.example.blelocker.View

import android.util.Base64
import android.util.Log
import com.example.blelocker.BluetoothUtils.BleCmdRepository
import com.example.blelocker.BluetoothUtils.NotConnectedException
import com.example.blelocker.Entity.LockConnectionInformation
import com.example.blelocker.Entity.LockSetting
import com.example.blelocker.Entity.LockStatus
import com.example.blelocker.Exception.LockStatusException
import com.example.blelocker.MainActivity
import com.example.blelocker.unSignedInt
import com.polidea.rxandroidble2.NotificationSetupMode
import com.polidea.rxandroidble2.RxBleConnection
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.BiFunction

class LockSettingUseCase(
    private val mBleCmdRepository: BleCmdRepository
) {
    //todo 沒有反應~
    fun rxBleGetLockSetting(mLock: LockConnectionInformation,
                       rxBleConnection: RxBleConnection
    ): Observable<LockSetting> {
        Log.d("TAG","Do D6")
        return Observable.zip(
            rxBleConnection
                .setupNotification(
                    MainActivity.NOTIFICATION_CHARACTERISTIC,
                    NotificationSetupMode.DEFAULT
                )
                .flatMap { it }
                .filter { notification ->
                    mBleCmdRepository.decrypt(
                        Base64.decode(mLock.keyTwo, Base64.DEFAULT), notification
                    )?.let { decrypted ->
                        if (decrypted.component3().unSignedInt() == 0xEF) {
                            throw LockStatusException.AdminCodeNotSetException()
                        } else decrypted.component3().unSignedInt() == 0xD6
                    } ?: false
                },
            rxBleConnection.writeCharacteristic(
                MainActivity.NOTIFICATION_CHARACTERISTIC,
                mBleCmdRepository.createCommand(
                    0xD6,
                    Base64.decode(mLock.keyTwo, Base64.DEFAULT)
                )
            ).toObservable(),
            BiFunction { notification: ByteArray, written: ByteArray ->
                Log.d("TAG","d6 notify")
                mBleCmdRepository.resolveD6(
                    Base64.decode(mLock.keyTwo, Base64.DEFAULT),
                    notification
                )
            })
    }

    fun rxBleControlLockStatus(isLockLocked: Int, mLock: LockConnectionInformation, rxBleConnection: RxBleConnection): Observable<LockSetting> {
        val keyOne = Base64.decode(mLock.keyOne, Base64.DEFAULT)
        val token = if (mLock.permanentToken.isBlank()) {
            Base64.decode(mLock.oneTimeToken, Base64.DEFAULT)
        } else {
            Base64.decode(mLock.permanentToken, Base64.DEFAULT)
        }
        return Observable.zip(
            rxBleConnection
                .setupNotification(
                    MainActivity.NOTIFICATION_CHARACTERISTIC,
                    NotificationSetupMode.DEFAULT
                )
                .flatMap { it }
                .filter { notification ->
                    mBleCmdRepository.decrypt(
                        Base64.decode(mLock.keyTwo, Base64.DEFAULT), notification
                    )?.let { decrypted ->
                        if (decrypted.component3().unSignedInt() == 0xEF) {
                            throw LockStatusException.AdminCodeNotSetException()
                        } else decrypted.component3().unSignedInt() == 0xD6
                    } ?: false
                },
            rxBleConnection.writeCharacteristic(
                MainActivity.NOTIFICATION_CHARACTERISTIC,
                mBleCmdRepository.createCommand(
                    0xD7,
                    Base64.decode(mLock.keyTwo, Base64.DEFAULT),
                    if(isLockLocked == LockStatus.LOCKED)byteArrayOf(0x00) else byteArrayOf(0x01)
                )
            ).toObservable(),
            BiFunction { notification: ByteArray, written: ByteArray ->
                mBleCmdRepository.resolveD6(
                    Base64.decode(mLock.keyTwo, Base64.DEFAULT),
                    notification
                )
            }
        )
    }

}