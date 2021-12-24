package com.example.blelocker.View

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.blelocker.R
import com.example.blelocker.entity.Commit
import com.example.blelocker.entity.GitHubCommits
import com.example.blelocker.entity.LockConnectionInformation
import kotlinx.android.synthetic.main.alllocks_recyclerview_item.view.*
import kotlinx.android.synthetic.main.alllocks_recyclerview_item.view.tv_my_lock_mac
import kotlinx.android.synthetic.main.commits_recyclerview_item.view.*

class CommitsAdapter(private val onClickListener: OnClickListener) : ListAdapter<GitHubCommits, CommitsAdapter.CommitsHolder>(LockComparator()) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommitsHolder {
        val inflatedView = parent.inflate(R.layout.commits_recyclerview_item, false)
        return CommitsHolder(inflatedView)
    }

    override fun onBindViewHolder(holder: CommitsHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
        holder.itemView.setOnClickListener {
//            onClickListener.onClick(item.macAddress)
        }
    }

    class CommitsHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(gitHubCommits: GitHubCommits){
            itemView.tv_commit_date.text = gitHubCommits.commit.author.date
            itemView.tv_commit_msg.text = gitHubCommits.commit.message
        }
    }
    class LockComparator : DiffUtil.ItemCallback<GitHubCommits>() {
        override fun areItemsTheSame(oldItem: GitHubCommits, newItem: GitHubCommits): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: GitHubCommits, newItem: GitHubCommits): Boolean {
            return oldItem.sha == newItem.sha
        }
    }
    class OnClickListener(val clickListener: (macAddress: String) -> Unit) {
        fun onClick(macAddress: String) = clickListener(macAddress)
    }

}
