package com.example.blelocker

class GithubRepository (private val api: GithubApi) {
    fun getAllUsers() = api.getUsers()
    fun getCommits() = api.getCommits()
}