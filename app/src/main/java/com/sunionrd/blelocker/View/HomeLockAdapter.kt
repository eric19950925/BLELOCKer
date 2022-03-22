package com.sunionrd.blelocker.View

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sunionrd.blelocker.Entity.HomeLocks
import com.sunionrd.blelocker.Entity.LockConnectionInformation
import com.sunionrd.blelocker.Entity.LockStatus
import com.sunionrd.blelocker.R
import com.sunionrd.blelocker.databinding.HomeLockRecyclerviewItemBinding

class HomeLockAdapter(
    private val onClickListener: OnClickListener,
    private val onLockStatusClickListener: OnClickListener,
    private val onSettingClickListener: OnClickListener,
    ) : ListAdapter<HomeLocks, HomeLockAdapter.HomeLockHolder>(LockComparator()) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeLockHolder {
        val inflatedView = HomeLockRecyclerviewItemBinding.inflate(LayoutInflater.from(parent.context))
        return HomeLockHolder(inflatedView)
    }

    override fun onBindViewHolder(holder: HomeLockHolder, position: Int) {
        val item = getItem(position)
        holder.lockName.text = item.deviceName

        holder.lockStatus.setOnClickListener {
            onLockStatusClickListener.onLockStatusClickListener(item.macAddress)
        }
        holder.setting.setOnClickListener {
            onSettingClickListener.onSettingClickListener(item.macAddress)
        }
        holder.lockStatus.setBackgroundResource(
            when(item.lockStatus){
                LockStatus.UNLOCKED -> {
                    R.drawable.ic_auto_unlock
                }
                LockStatus.LOCKED -> {
                    R.drawable.ic_lock_main
                }
                else -> {R.drawable.ic_baseline_bluetooth_searching_24}
            }
        )

    }

    class HomeLockHolder(binding: HomeLockRecyclerviewItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val lockName: TextView = binding.tvLockName
        val lockStatus: Button = binding.ivMyLockBleStatus
        val setting: ImageView = binding.ivSetting
    }
    class LockComparator : DiffUtil.ItemCallback<HomeLocks>() {
        override fun areItemsTheSame(oldItem: HomeLocks, newItem: HomeLocks): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: HomeLocks, newItem: HomeLocks): Boolean {
            return oldItem.macAddress == newItem.macAddress && oldItem.lockStatus == newItem.lockStatus
        }
    }
    class OnClickListener(val clickListener: (macAddress: String) -> Unit) {
        fun onClick(macAddress: String) = clickListener(macAddress)
        fun onLockStatusClickListener(macAddress: String) = clickListener(macAddress)
        fun onSettingClickListener(macAddress: String) = clickListener(macAddress)
    }

}
