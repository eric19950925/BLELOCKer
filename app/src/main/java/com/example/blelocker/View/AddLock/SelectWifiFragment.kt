package com.example.blelocker.View.AddLock

import android.os.Bundle
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.blelocker.BaseFragment
import com.example.blelocker.R
import com.example.blelocker.createSimpleAdapter
import kotlinx.android.synthetic.main.fragment_select_wifi.*
import kotlinx.android.synthetic.main.wifi_list_item.view.*
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

    override fun onViewHasCreated() {
        rv_wifi_list.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
//        rv_wifi_list.adapter = createSimpleAdapter<Triple<Int, Int, Int>>(WIFI_ITEM_MODEL, R.layout.wifi_list_item) {
        rv_wifi_list.adapter = createSimpleAdapter<Triple<String, Int, Int>>(WIFI_ITEM_MODEL, R.layout.wifi_list_item) {
                item: Triple<String, Int, Int>, holder: RecyclerView.ViewHolder, _: Int ->
            with(holder.itemView) {
                this.tv_wifi_name.text = item.first
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