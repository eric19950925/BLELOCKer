package com.example.blelocker.BluetoothUtils

import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.RxBleDevice
import io.reactivex.Observable
import io.reactivex.disposables.Disposable

interface StatefulConnection {
    fun establishConnection(macAddress: String, isSilentlyFail: Boolean): Disposable
    fun runConnectionSequence(
        rxBleConnection: Observable<RxBleConnection>,
        device: RxBleDevice,
        isSilentlyFail: Boolean
    ): Disposable
}