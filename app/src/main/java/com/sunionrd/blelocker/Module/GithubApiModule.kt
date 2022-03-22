package com.sunionrd.blelocker

import com.sunionrd.blelocker.GithubUtils.GithubApi
import com.sunionrd.blelocker.GithubUtils.GithubRepository
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

val GithubModule = module {

    fun provideGson(): Gson {
        return GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.IDENTITY).create()
    }

    fun provideHttpClient(): OkHttpClient {
        val okHttpClientBuilder = OkHttpClient.Builder()

        return okHttpClientBuilder.build()
    }

    fun provideRetrofit(factory: Gson, client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(GsonConverterFactory.create(factory))
            .client(client)
            .build()
    }

    single { provideGson() }
    single { provideHttpClient() }
    single { provideRetrofit(get(), get()) }

    fun provideUseApi(retrofit: Retrofit): GithubApi {
        return retrofit.create(GithubApi::class.java)
    }
    single { provideUseApi(get()) }

    single { GithubRepository(get()) }

    viewModel { com.sunionrd.blelocker.AccountViewModel(get()) }
}