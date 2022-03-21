package com.example.blelocker

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.blelocker.GithubUtils.GithubRepository
import com.example.blelocker.Entity.GitHubCommits
import com.example.blelocker.Entity.LoadingState
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AccountViewModel(private val repo: GithubRepository) : ViewModel(), Callback<List<GitHubCommits>> {

    private val _loadingState = MutableLiveData<LoadingState>()
    var mCommits = MutableLiveData<List<GitHubCommits>>()
    val loadingState: LiveData<LoadingState>
        get() = _loadingState

    private val _data = MutableLiveData<List<GitHubCommits>>()
    val data: LiveData<List<GitHubCommits>>
        get() = _data

//    init {
//        fetchData()
//    }

    fun fetchData() {
        _loadingState.postValue(LoadingState.LOADING)
//        repo.getAllUsers().enqueue(this)
        repo.getCommits().enqueue(this)
    }

    override fun onFailure(call: Call<List<GitHubCommits>>, t: Throwable) {
        _loadingState.postValue(LoadingState.error(t.message))
    }

    override fun onResponse(call: Call<List<GitHubCommits>>, response: Response<List<GitHubCommits>>) {
        if (response.isSuccessful) {
            _data.postValue(response.body())
            mCommits.value = response.body()
            _loadingState.postValue(LoadingState.LOADED)
        } else {
            _loadingState.postValue(LoadingState.error(response.errorBody().toString()))
        }
    }
}