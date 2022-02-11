package com.example.blelocker.Module

import com.example.blelocker.TFHApiViewModel
import com.example.blelocker.mTFHApiUtils.TFHApiRepository
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val TFHApiModule = module {

    single { TFHApiRepository() }

    viewModel { TFHApiViewModel(get()) }
}