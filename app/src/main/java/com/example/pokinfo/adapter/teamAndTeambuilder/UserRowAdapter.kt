package com.example.pokinfo.adapter.teamAndTeambuilder

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.example.pokinfo.R
import com.example.pokinfo.data.models.firebase.PublicProfile
import com.example.pokinfo.data.util.UserDiffCallback
import com.example.pokinfo.databinding.ItemListUserBinding

class UserRowAdapter(
    alreadySharedWithIds: List<String>,
    private val onUserClicked: (List<String>) -> Unit
): ListAdapter<PublicProfile, UserRowAdapter.ItemViewHolder>(UserDiffCallback()) {

    inner class ItemViewHolder(val binding: ItemListUserBinding) :
        RecyclerView.ViewHolder(binding.root)

    private val selectedUserIds: MutableList<String> = alreadySharedWithIds.toMutableList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserRowAdapter.ItemViewHolder {
        val binding = ItemListUserBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserRowAdapter.ItemViewHolder, position: Int) {
        val user = currentList[position]
        holder.binding.tvInitials.visibility = View.GONE
        holder.binding.ivUserPic.load(user.profilePicture) {
            transformations(CircleCropTransformation())
            listener(
                onError = { _, _ ->
                    val drawable = AppCompatResources.getDrawable(holder.itemView.context, R.drawable.circle_background)
                    drawable?.setTint(Color.BLUE)
                    holder.binding.ivUserPic.background = drawable
                    holder.binding.ivUserPic.setImageDrawable(null)
                    holder.binding.tvInitials.text = user.username.first().uppercase()
                    holder.binding.tvInitials.visibility = View.VISIBLE
                }
            )
        }
        val isSelected = selectedUserIds.contains(user.userId)
        holder.binding.ivChecked.visibility = if (isSelected) View.VISIBLE else View.GONE
        holder.binding.tvUserName.text = user.username
        holder.binding.item.setOnClickListener {
            if (isSelected) {
                selectedUserIds.remove(user.userId)
            } else {
                selectedUserIds.add(user.userId)
            }
            onUserClicked(selectedUserIds)
            notifyItemChanged(position)
        }
    }

    override fun getItemCount(): Int {
        return currentList.size
    }
}