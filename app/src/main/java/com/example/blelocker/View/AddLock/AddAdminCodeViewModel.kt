package com.example.blelocker.View.AddLock

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blelocker.Entity.LockConnectionInformation
import com.example.blelocker.Model.LockConnInfoRepository
import com.polidea.rxandroidble2.RxBleConnection
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.launch

class AddAdminCodeViewModel(
    private val repository: LockConnInfoRepository,
    private val adminCodeUseCase: AdminCodeUseCase
): ViewModel(){
    var adminCodeSet = MutableLiveData<Boolean>()
    private var connectionDisposable: CompositeDisposable ?= null
    var mLock: LockConnectionInformation ?= null

    fun getLockInfo(macAddress: String) = viewModelScope.launch {
        mLock = repository.getLockConnectInformation(macAddress)
    }

    fun addAdminCode(code: String, rxBleConnection: RxBleConnection){
        val disposable = adminCodeUseCase
            .rxSendC7(mLock?:return, code, rxBleConnection)
            .doOnNext {
                viewModelScope.launch { adminCodeSet.value = it }
            }.subscribe({

            },{
                Log.d("TAG",it.toString())
            })
        connectionDisposable?.add(disposable)
    }

    fun updateLockAdminCode(code: String) = viewModelScope.launch {
        val newLockInfo = mLock?.let {
            LockConnectionInformation(
                macAddress = it.macAddress,
                displayName = it.displayName,
                keyOne = it.keyOne,
                keyTwo = it.keyTwo,
                oneTimeToken = it.oneTimeToken,
                permanentToken = it.permanentToken,
                isOwnerToken = it.isOwnerToken,
                tokenName = "T",
                sharedFrom = it.sharedFrom,
                index = 0,
                adminCode = code
            )
        }
        repository.LockInsert(newLockInfo?:return@launch)
        mLock = newLockInfo
    }

    fun disposeConnection(){
        connectionDisposable?.dispose()
    }

    fun clearConnection(){
        connectionDisposable?.clear()
    }
}