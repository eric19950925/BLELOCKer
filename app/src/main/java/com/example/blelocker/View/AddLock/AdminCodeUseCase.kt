package com.example.blelocker.View.AddLock

import android.util.Base64
import android.util.Log
import com.example.blelocker.BluetoothUtils.BleCmdRepository
import com.example.blelocker.Entity.LockConnectionInformation
import com.example.blelocker.MainActivity
import com.example.blelocker.unSignedInt
import com.polidea.rxandroidble2.NotificationSetupMode
import com.polidea.rxandroidble2.RxBleConnection
import io.reactivex.Observable
import io.reactivex.functions.BiFunction

class AdminCodeUseCase (private val mBleCmdRepository: BleCmdRepository){
    fun rxSendC7(
        mLock: LockConnectionInformation,
        code: String,
        rxBleConnection: RxBleConnection
    ): Observable<Boolean> {
        //if not catch will crash!!
        var adminCode = byteArrayOf()
        try{
            adminCode = mBleCmdRepository.stringCodeToHex(code)
        }catch (e: Exception){
            Log.d("TAG",e.toString())
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
                    decrypted.component3().unSignedInt() == 0xC7
                } ?: false
            },
            rxBleConnection.writeCharacteristic(
                MainActivity.NOTIFICATION_CHARACTERISTIC,
                mBleCmdRepository.createCommand(
                    0xC7,
                    Base64.decode(mLock.keyTwo, Base64.DEFAULT),
                    adminCode
                )
            ).toObservable(),
            BiFunction { notification: ByteArray, written: ByteArray ->
                val result = mBleCmdRepository.resolveC7(
                    Base64.decode(mLock.keyTwo, Base64.DEFAULT),
                    notification
                )
                result
            }
//            .flatMap { isAddAdminCodeSuccessful ->
//                if (isAddAdminCodeSuccessful) {
//                    Single
//                        .timer(400, TimeUnit.MILLISECONDS)
//                        .flatMap { updateOwnerTokenName(mLock, input2) }
//                } else {
//                    throw UserCodeException.AddAdminCodeException()
//                }
//            }
//            .doOnSuccess { _ ->
//                lockInformationRepository.save(
//                    lockConnection.copy(
//                        isOwnerToken = true,
////                        tokenName = input2,
//                        permission = mLock.permission
//                    )
//                )
//            },
        )
    }
}