package com.example.blelocker.GithubUtils

import com.example.blelocker.Entity.GitHubCommits
import com.example.blelocker.Entity.GitHubUser
import retrofit2.Call
import retrofit2.http.GET

interface GithubApi {

    @GET("users")
    fun getUsers(): Call<List<GitHubUser>>

    @GET("repos/eric19950925/BLELOCKer/commits")
    fun getCommits(): Call<List<GitHubCommits>>
}