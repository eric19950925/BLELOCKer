package com.example.blelocker.BluetoothUtils

import android.util.Log
import com.example.blelocker.MainActivity.Companion.NOTIFICATION_CHARACTERISTIC
import com.example.blelocker.UseCase
import com.example.blelocker.toHex
import com.example.blelocker.unSignedInt
import com.polidea.rxandroidble2.*
import com.polidea.rxandroidble2.scan.ScanFilter
import com.polidea.rxandroidble2.scan.ScanResult
import com.polidea.rxandroidble2.scan.ScanSettings
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.subjects.PublishSubject
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.concurrent.TimeUnit

/**
 * This is a class which will scan ble device and return result, using rxAndroidBle2 in reactivex java ver.2.
 * Won't use android.bluetooth.le.ScanResult
 */
class BleScanUseCase (
    private val rxBleClient: RxBleClient,
    private val mBleCmdRepository: BleCmdRepository
):UseCase.Execute<String?, Observable<ScanResult>> {
    private val disconnectTriggerSubject = PublishSubject.create<Unit>()
    private lateinit var connectionObservable: Observable<RxBleConnection>
    override fun invoke(input: String?): Observable<ScanResult> {
        return rxBleClient
            .scanBleDevices(
                ScanSettings.Builder().build(),
                ScanFilter.Builder().setDeviceAddress(input).build()
            )
    }
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


    fun setupNotify(
        rxConnection: Observable<RxBleConnection>,
        keyOne: ByteArray
    ): Observable<ByteArray> {
        return rxConnection
            .flatMap {
                it.setupNotification(NOTIFICATION_CHARACTERISTIC)
                    .flatMap {
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
            .doOnNext { Log.d("TAG","setup notify Success") }
            // we have to flatmap in order to get the actual notification observable
            // out of the enclosing observable, which only performed notification setup
//            .flatMap { it }
//            .observeOn(AndroidSchedulers.mainThread())
//            .subscribe({ Log.d("TAG","setup notify Success") }, { Log.d("TAG","setup notify Failure") })
    }
    fun setupC0Notify(
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


    fun sendC00(
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


    fun sendC0(
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


    fun rxSendC1(
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
}