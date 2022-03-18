package com.example.blelocker.View.Github

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.blelocker.Entity.GitHubCommits
import com.example.blelocker.databinding.CommitsRecyclerviewItemBinding


class CommitsAdapter(private val onClickListener: OnClickListener) : ListAdapter<GitHubCommits, CommitsAdapter.CommitsHolder>(
    LockComparator()
) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommitsHolder {
        val binding = CommitsRecyclerviewItemBinding.inflate(LayoutInflater.from(parent.context))
        return CommitsHolder(binding)
    }

    override fun onBindViewHolder(holder: CommitsHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
        holder.itemView.setOnClickListener {
//            onClickListener.onClick(item.macAddress)
        }
    }

    class CommitsHolder(binding: CommitsRecyclerviewItemBinding) : RecyclerView.ViewHolder(binding.root) {
        private val tvCommitDate: TextView = binding.tvCommitDate
        private val tvCommitMsg: TextView = binding.tvCommitMsg

        fun bind(gitHubCommits: GitHubCommits){
            tvCommitDate.text = gitHubCommits.commit.author.date
            tvCommitMsg.text = gitHubCommits.commit.message
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
