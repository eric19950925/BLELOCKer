package com.example.blelocker.Entity

data class GitHubUser (
    val id: Long,
    val login: String,
    val avatar_url: String
)

data class GitHubCommits (
    val sha: String,
    val commit: Commit
)

data class Commit (
    val author: Author,
    val message: String
)


data class Author (
    val name: String,
    val date: String
)


data class LoadingState private constructor(val status: Status, val msg: String? = null) {
    companion object {
        val LOADED = LoadingState(Status.SUCCESS)
        val LOADING = LoadingState(Status.RUNNING)
        fun error(msg: String?) = LoadingState(Status.FAILED, msg)
    }

    enum class Status {
        RUNNING,
        SUCCESS,
        FAILED
    }
}