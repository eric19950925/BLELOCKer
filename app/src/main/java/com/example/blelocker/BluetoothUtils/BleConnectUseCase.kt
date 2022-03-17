package com.example.blelocker.BluetoothUtils

import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blelocker.Entity.DeviceToken
import com.example.blelocker.Entity.LockConnectionInformation
import com.example.blelocker.Entity.LockSetting
import com.example.blelocker.Entity.LockStatus
import com.example.blelocker.Entity.LockStatus.LOCKED
import com.example.blelocker.Exception.LockStatusException
import com.example.blelocker.MainActivity
import com.example.blelocker.MainActivity.Companion.NOTIFICATION_CHARACTERISTIC
import com.example.blelocker.Model.LockConnInfoRepository
import com.example.blelocker.toHex
import com.example.blelocker.unSignedInt
import com.polidea.rxandroidble2.*
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.launch
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.concurrent.TimeUnit

/**
 * This is a class which will scan ble device and return result, using rxAndroidBle2 in reactivex java ver.2.
 * Won't use android.bluetooth.le.ScanResult
 */
class BleConnectUseCase (
    private val rxBleClient: RxBleClient,
    private val mBleCmdRepository: BleCmdRepository,
    private val repository: LockConnInfoRepository
): ViewModel() {
    private val disconnectTriggerSubject = PublishSubject.create<Unit>()
    private lateinit var connectionObservable: Observable<RxBleConnection>

    fun device(input: String): RxBleDevice {
        return rxBleClient.getBleDevice(input)
    }

    fun connectDevice(input: String): Observable<RxBleConnection> {
        val bluetoothGattRefreshCustomOp = refreshAndroidStackCacheCustomOperation()
        val discoverServicesCustomOp = customDiscoverServicesOperation()
        connectionObservable =
//            Observable.timer(500, TimeUnit.MILLISECONDS)
//                .flatMap {
                    rxBleClient.getBleDevice(input)
                    .establishConnection(false)
//                    .takeUntil(disconnectTriggerSubject)
//                    .compose(ReplayingShare.instance())
//                        .flatMap { connection ->
//                            connection
//                                .requestMtu(RxBleConnection.GATT_MTU_MAXIMUM)
//                                .ignoreElement()
//                                .andThen(Observable.just(connection))
//                        }
//                        .flatMap { connection ->
//                            connection
//                                .queue(bluetoothGattRefreshCustomOp)
//                                .ignoreElements()
//                                .andThen(connection.queue(discoverServicesCustomOp))
//                                .ignoreElements()
//                                .andThen(Observable.just(connection))
//                        }
//                }
        return connectionObservable
    }

    private fun refreshAndroidStackCacheCustomOperation() =
        RxBleCustomOperation { bluetoothGatt, _, _ ->
            try {
                val bluetoothGattRefreshFunction: Method =
                    bluetoothGatt.javaClass.getMethod("refresh")
                val success = bluetoothGattRefreshFunction.invoke(bluetoothGatt) as Boolean
                if (!success) {
                    Observable.error(RuntimeException("BluetoothGatt.refresh() returned false"))
                } else {
                    Observable.empty<Void>().delay(200, TimeUnit.MILLISECONDS)
                }
            } catch (e: NoSuchMethodException) {
                Observable.error<Void>(e)
            } catch (e: IllegalAccessException) {
                Observable.error<Void>(e)
            } catch (e: InvocationTargetException) {
                Observable.error<Void>(e)
            }
        }

    private fun customDiscoverServicesOperation() =
        RxBleCustomOperation { bluetoothGatt, rxBleGattCallback, _ ->
            val success: Boolean = bluetoothGatt.discoverServices()
            if (!success) {
                Observable.error(RuntimeException("BluetoothGatt.discoverServices() returned false"))
            } else {
                rxBleGattCallback.onServicesDiscovered
                    .take(1) // so this RxBleCustomOperation will complete after the first result from BluetoothGattCallback.onServicesDiscovered()
                    .map(RxBleDeviceServices::getBluetoothGattServices)
            }
        }

    fun checkService(rxConnection: RxBleConnection): Disposable{
        return rxConnection
            .discoverServices()
            .toObservable()
            .subscribe {
                for ( service in it.bluetoothGattServices ){
                    Log.d("TAG",service.toString())
                    for ( chara in service.characteristics ){
                        Log.d("TAG",chara.uuid.toString())
                    }
                }
            }
    }

    fun connectWithToken(mLock: LockConnectionInformation, rxBleConnection: RxBleConnection): Observable<String> {
        val keyOne = Base64.decode(mLock.keyOne, Base64.DEFAULT)
        val token = if (mLock.permanentToken.isBlank()) {
            Base64.decode(mLock.oneTimeToken, Base64.DEFAULT)
        } else {
            Base64.decode(mLock.permanentToken, Base64.DEFAULT)
        }
        val isLockFromSharing =
            mLock.sharedFrom != null && mLock.sharedFrom.isNotBlank()

        return if (mLock.permanentToken.isBlank()) {
            connectWithOneTimeToken(mLock, rxBleConnection, keyOne, token, isLockFromSharing)
        } else {
            connectWithPermanentToken(keyOne, token, mLock, rxBleConnection, isLockFromSharing)
        }
    }


    private fun connectWithPermanentToken(
        keyOne: ByteArray,
        token: ByteArray,
        mLock: LockConnectionInformation,
        rxBleConnection: RxBleConnection,
        isLockFromSharing: Boolean)
            : Observable<String>{
        return  sendC0(rxBleConnection, keyOne, token)
            .flatMap { keyTwo ->
                Log.d("TAG","get k2")
                sendC1(rxBleConnection, keyTwo, token, isLockFromSharing)
                    .filter { it.first == DeviceToken.VALID_TOKEN }
                    .flatMap { stateAndPermission ->
                        rxBleConnection
                            .setupNotification(
                                NOTIFICATION_CHARACTERISTIC,
                                NotificationSetupMode.DEFAULT
                            ) // receive [C1], [D6] only
                            .flatMap { it }
                            .map { keyTwo to it }
                            .take(1)
                            .doOnNext { pair ->
//                                Timber.d("received receive [C1], [D6] in exchange permanent token")
//                                viewModelScope.launch { mCharacteristicValue.value = "PermanentToken" to pair }
                                viewModelScope.launch {repository.LockUpdate(mLock.copy(
                                    keyTwo = Base64.encodeToString(
                                        keyTwo,
                                        Base64.DEFAULT
                                    ),
                                    deviceName = mLock.deviceName ,
                                    permission = stateAndPermission.second
                                ))}
                            }
                            .flatMap { Observable.just(stateAndPermission.second) }
                    }
            }

    }

    private fun connectWithOneTimeToken(
        mLock: LockConnectionInformation,
        rxBleConnection: RxBleConnection,
        keyOne: ByteArray,
        oneTimeToken: ByteArray,
        isLockFromSharing: Boolean
    ): Observable<String>{
        return sendC0(rxBleConnection, keyOne, oneTimeToken)
            .flatMap { keyTwo ->
                Log.d("TAG","get k2")
                sendC1(rxBleConnection, keyTwo, oneTimeToken, isLockFromSharing)
                    .take(1)
                    .filter { it.first == DeviceToken.ONE_TIME_TOKEN || it.first == DeviceToken.VALID_TOKEN }
                    .flatMap { stateAndPermission ->
                        rxBleConnection
                            .setupNotification(
                                NOTIFICATION_CHARACTERISTIC,
                                NotificationSetupMode.DEFAULT
                            )
                            .flatMap { it }
                            .filter { notification -> // [E5] will sent from device
                                mBleCmdRepository.decrypt(keyTwo, notification)?.let { bytes ->
                                    bytes.component3().unSignedInt() == 0xE5
                                } ?: false
                            }
                            .distinct { notification ->
                                mBleCmdRepository.decrypt(keyTwo, notification)?.component3()
                                    ?.unSignedInt() ?: 0xE5
                            }
                            .map { notification ->
                                val token =
                                    mBleCmdRepository.decrypt(keyTwo, notification)?.let { bytes ->
                                        val permanentToken = mBleCmdRepository.extractToken(mBleCmdRepository.resolveE5(bytes))
                                        permanentToken
                                    } ?: throw NotConnectedException()
                                keyTwo to token
                            }
                            .doOnNext { pair -> //todo
//                                Timber.d("received receive [C1], [E5], [D6] in exchange one time token")
                                updateLockInfo(mLock, pair.first, pair.second as DeviceToken.PermanentToken)
                            }
                            .flatMap { pair ->
                                val token = pair.second
                                if (token is DeviceToken.PermanentToken) Observable.just(token.permission) else Observable.never()
                            }
                    }
            }
    }

    private fun setupC0Notify(
        rxConnection: RxBleConnection,
        keyOne: ByteArray
    ): Observable<ByteArray> {
        return rxConnection.setupNotification(
                NOTIFICATION_CHARACTERISTIC
            ).flatMap {
                    notification -> notification
            }.filter { notification ->
                val decrypted = mBleCmdRepository.decrypt(
                    keyOne,
                    notification
                )
                println("filter [C0] decrypted: ${decrypted?.toHex()}")
                decrypted?.component3()?.unSignedInt() == 0xC0
            }
    }


    private fun sendC00(
        rxConnection: RxBleConnection,
        keyOne: ByteArray,
        token: ByteArray
    ): Observable<ByteArray>  {
//    ): Disposable  {
        val writeC0 = mBleCmdRepository.createCommand(0xC0, keyOne, token)
        Log.d("TAG", "writeC0 ${writeC0.toHex()}")
        return rxConnection.writeCharacteristic(NOTIFICATION_CHARACTERISTIC, writeC0)
            .toObservable()
//            .subscribe{
//                Log.d("TAG", "written C0 ${it.toHex()}")
//            }
    }


    private fun sendC0(
        rxConnection: RxBleConnection,
        keyOne: ByteArray,
        token: ByteArray
    ): Observable<ByteArray> {
        return Observable.zip(
            setupC0Notify(rxConnection, keyOne),
            sendC00(rxConnection, keyOne, token),
            BiFunction { notification: ByteArray, written: ByteArray ->
                val randomNumberOne = mBleCmdRepository.resolveC0(keyOne, written)
                Log.d("TAG", "[C0] has written: ${written.toHex()}")
                Log.d("TAG", "[C0] has notified: ${notification.toHex()}")
                val randomNumberTwo = mBleCmdRepository.resolveC0(keyOne, notification)
                Log.d("TAG", "randomNumberTwo: ${randomNumberTwo.toHex()}")
                val keyTwo = mBleCmdRepository.generateKeyTwo(
                    randomNumberOne = randomNumberOne,
                    randomNumberTwo = randomNumberTwo
                )
                Log.d("TAG", "keyTwo: ${keyTwo.toHex()}")
                keyTwo
            })
    }
    private fun writeAndReadOnNotification(
        rxBleConnection: RxBleConnection,
        keyOne: ByteArray,
        token: ByteArray
    ): Observable<ByteArray?> {
        return setupC0Notify(rxBleConnection, keyOne).let { notificationObservable ->
            Observable.combineLatest(
                sendC00(rxBleConnection, keyOne, token),
                notificationObservable.take(1),
                { writtenBytes: ByteArray, responseBytes: ByteArray ->
                    responseBytes
                }
            )
        }
    }

    private fun sendC1(
        rxConnection: RxBleConnection,
        keyTwo: ByteArray,
        token: ByteArray,
        isLockFromSharing: Boolean
    ): Observable<Pair<Int, String>> {
        return Observable.zip(
            rxConnection.setupNotification(
                NOTIFICATION_CHARACTERISTIC,
                NotificationSetupMode.DEFAULT
            )
                .flatMap { notification -> notification }
                .filter { notification ->
                    mBleCmdRepository.decrypt(keyTwo, notification)?.component3()
                        ?.unSignedInt() == 0xC1
                },
            rxConnection.writeCharacteristic(
                NOTIFICATION_CHARACTERISTIC,
                mBleCmdRepository.createCommand(0xC1, keyTwo, token)
            ).toObservable(),
            BiFunction { notification: ByteArray, written: ByteArray ->
                //                Timber.d("[C1] has written: ${written.toHex()}")
                //                Timber.d("[C1] has notified: ${notification.toHex()}")
                val tokenStateFromDevice = mBleCmdRepository.resolveC1(keyTwo, notification)
                //                Timber.d("token state from device : ${tokenStateFromDevice.toHex()}")
                val deviceToken = mBleCmdRepository.determineTokenState(tokenStateFromDevice, isLockFromSharing)
                //                Timber.d("token state: ${token.toHex()}")
                val permission = mBleCmdRepository.determineTokenPermission(tokenStateFromDevice)
                //                Timber.d("token permission: $permission")
                deviceToken to permission
            }
        )
    }
    private fun updateLockInfo(mLock: LockConnectionInformation, mKeyTwo: ByteArray, token: DeviceToken.PermanentToken) = viewModelScope.launch {
        repository.LockUpdate(mLock.copy(
            keyTwo = Base64.encodeToString(mKeyTwo, Base64.DEFAULT),
            permission = token.permission,
            permanentToken = token.token
        ))
    }



}