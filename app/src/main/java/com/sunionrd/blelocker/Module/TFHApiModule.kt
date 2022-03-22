package com.sunionrd.blelocker.Module

import com.sunionrd.blelocker.TFHApiViewModel
import com.sunionrd.blelocker.mTFHApiUtils.TFHApiRepository
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val TFHApiModule = module {

    single { TFHApiRepository() }

    viewModel { TFHApiViewModel(get()) }
}