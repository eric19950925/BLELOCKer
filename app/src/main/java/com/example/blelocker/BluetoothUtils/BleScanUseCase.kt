package com.example.blelocker.BluetoothUtils

import com.example.blelocker.UseCase
import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.RxBleDevice
import com.polidea.rxandroidble2.RxBleScanResult
import com.polidea.rxandroidble2.scan.ScanFilter
import com.polidea.rxandroidble2.scan.ScanResult
import com.polidea.rxandroidble2.scan.ScanSettings
import io.reactivex.Observable

/**
 * This is a class which will scan ble device and return result, using rxAndroidBle2 in reactivex java ver.2.
 * Won't use android.bluetooth.le.ScanResult
 */
class BleScanUseCase (private val rxBleClient: RxBleClient):UseCase.Execute<String?, Observable<ScanResult>> {

    override fun invoke(input: String?): Observable<ScanResult> {
        return rxBleClient
            .scanBleDevices(ScanSettings.Builder().build(), ScanFilter.Builder().setDeviceAddress(input).build())
    }
    fun device(input: String): RxBleDevice {
        return rxBleClient.getBleDevice(input)
    }
}