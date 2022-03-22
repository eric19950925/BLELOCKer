package com.sunionrd.blelocker.View.AddLock

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.sunionrd.blelocker.BaseFragment
import com.sunionrd.blelocker.R
import com.sunionrd.blelocker.createSimpleAdapter
import com.sunionrd.blelocker.databinding.FragmentSelectWifiBinding
import org.jetbrains.anko.sdk27.coroutines.onClick

class SelectWifiFragment: BaseFragment(){
    companion object {
        private const val MODEL_KD0 = 3331
        private const val MODEL_KL0 = 3332
        private const val MODEL_TD0 = 3333
        private const val MODEL_TL0 = 3334

        val WIFI_ITEM_MODEL = listOf(
            Triple("Aaaaaa", R.drawable.ic_github, MODEL_KD0),
            Triple("Bbbbbb", R.drawable.ic_github, MODEL_KL0),
            Triple("Cccccc", R.drawable.ic_github, MODEL_TD0),
            Triple("Dddddd", R.drawable.ic_github, MODEL_TL0)
        )
    }
    override fun getLayoutRes(): Int = R.layout.fragment_select_wifi
    private lateinit var currentBinding: FragmentSelectWifiBinding
    override fun getLayoutBinding(inflater: LayoutInflater, container: ViewGroup?): ViewBinding? {
        currentBinding = FragmentSelectWifiBinding.inflate(inflater, container, false)
        return currentBinding
    }
    override fun onViewHasCreated() {
        currentBinding.rvWifiList.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
//        rv_wifi_list.adapter = createSimpleAdapter<Triple<Int, Int, Int>>(WIFI_ITEM_MODEL, R.layout.wifi_list_item) {
        currentBinding.rvWifiList.adapter = createSimpleAdapter<Triple<String, Int, Int>>(WIFI_ITEM_MODEL, R.layout.wifi_list_item) {
                item: Triple<String, Int, Int>, holder: RecyclerView.ViewHolder, _: Int ->
            with(holder.itemView) {
                val wifiName = findViewById<TextView>(R.id.tvWifiName)
                wifiName.text = item.first
                this.onClick {
                    try {
                        val bundle = Bundle()
                        bundle.putString("WIFI_NAME", item.first)
                        Navigation.findNavController(requireView()).navigate(R.id.action_selectWifiFragment_to_enterWifiPWFragment,bundle)
                    } catch (error: Throwable) {
//                        Timber.e(error)
                    }
                }
            }
        }
    }

    override fun onBackPressed() {
    }

}