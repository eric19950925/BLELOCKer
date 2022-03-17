package com.example.blelocker

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blelocker.Entity.HomeLocks
import com.example.blelocker.Entity.LockConnectionInformation
import com.example.blelocker.Entity.LockSetting
import com.example.blelocker.Model.LockConnInfoRepository
import com.example.blelocker.View.LockSettingUseCase
import com.polidea.rxandroidble2.RxBleConnection
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.launch

class HomeViewModel(
//    private val repository: LockConnInfoRepository,
    private val lockSettingUseCase: LockSettingUseCase
): ViewModel() {

    var mLockSetting = MutableLiveData<LockSetting>()

    var mHomeLocksData = MutableLiveData<List<HomeLocks>>()

    private var connectionDisposable: CompositeDisposable?= null

    fun controlLockStatus(isLockLocked: Int, mLock: LockConnectionInformation, rxBleConnection: RxBleConnection){
        val disposable = lockSettingUseCase
            .rxBleControlLockStatus(isLockLocked, mLock, rxBleConnection)
            .doOnNext { lockSetting ->
                viewModelScope.launch {
                    mHomeLocksData.value?.find { it.macAddress == mLock.macAddress }?.lockStatus = lockSetting.status
                }
            }.subscribe({},{
                Log.d("TAG",it.toString())
            })
        connectionDisposable?.add(disposable)
    }


    fun getLockSetting(mLock: LockConnectionInformation, rxBleConnection: RxBleConnection){
        val disposable = lockSettingUseCase
            .rxBleGetLockSetting(mLock, rxBleConnection)
            .subscribe({ lockSetting ->
                viewModelScope.launch {
                    mHomeLocksData.value?.find { it.macAddress == mLock.macAddress }?.lockStatus = lockSetting.status
                }
            },{
                Log.d("TAG",it.toString())
            })
        connectionDisposable?.add(disposable)
    }

    fun disposeConnection(){
        connectionDisposable?.dispose()
    }

    fun clearConnection(){
        connectionDisposable?.clear()
    }
}