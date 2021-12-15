package com.example.blelocker

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

abstract class BaseFragment: Fragment() {
    protected abstract fun getLayoutRes(): Int
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
//        Log.d("TAG","onCreateView")
        val v = inflater.inflate(getLayoutRes(), container, false)
        return v
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onViewHasCreated()
//        Log.d("TAG","onViewCreated")
    }
    abstract fun onViewHasCreated()
}