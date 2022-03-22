package com.sunionrd.blelocker.GithubUtils

import com.sunionrd.blelocker.Entity.GitHubCommits
import com.sunionrd.blelocker.Entity.GitHubUser
import retrofit2.Call
import retrofit2.http.GET

interface GithubApi {

    @GET("users")
    fun getUsers(): Call<List<GitHubUser>>

    @GET("repos/eric19950925/BLELOCKer/commits")
    fun getCommits(): Call<List<GitHubCommits>>
}