package com.example.blelocker.View

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.blelocker.R
import com.example.blelocker.entity.LockConnectionInformation
import kotlinx.android.synthetic.main.alllocks_recyclerview_item.view.*

class AllLocksAdapter() : ListAdapter<LockConnectionInformation, AllLocksAdapter.AllLocksHolder>(LockComparator()) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AllLocksHolder {
        val inflatedView = parent.inflate(R.layout.alllocks_recyclerview_item, false)
        return AllLocksHolder(inflatedView)
    }

    override fun onBindViewHolder(holder: AllLocksHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    class AllLocksHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(lockConnectionInformation: LockConnectionInformation){
            itemView.tv_my_lock_mac.text = lockConnectionInformation.macAddress
            itemView.tv_my_lock_tk.text = lockConnectionInformation.permanentToken
        }
    }
    class LockComparator : DiffUtil.ItemCallback<LockConnectionInformation>() {
        override fun areItemsTheSame(oldItem: LockConnectionInformation, newItem: LockConnectionInformation): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: LockConnectionInformation, newItem: LockConnectionInformation): Boolean {
            return oldItem.macAddress == newItem.macAddress
        }
    }

}

fun ViewGroup.inflate(@LayoutRes layoutRes: Int, attachToRoot: Boolean = false): View {
    return LayoutInflater.from(context).inflate(layoutRes, this, attachToRoot)
}
