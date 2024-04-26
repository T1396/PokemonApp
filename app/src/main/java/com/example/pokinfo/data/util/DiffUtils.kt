package com.example.pokinfo.data.util

import androidx.recyclerview.widget.DiffUtil
import com.example.pokinfo.data.models.firebase.AttacksData

class DiffUtil: DiffUtil.ItemCallback<AttacksData>() {

    override fun areItemsTheSame(oldItem: AttacksData, newItem: AttacksData): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: AttacksData, newItem: AttacksData): Boolean {
        return oldItem.name == newItem.name && oldItem.levelLearned == newItem.levelLearned
    }

}