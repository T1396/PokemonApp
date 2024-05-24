package com.example.pokinfo.data.util

import android.annotation.SuppressLint
import androidx.recyclerview.widget.DiffUtil
import com.example.pokinfo.adapter.abilities.AbilityListAdapter
import com.example.pokinfo.data.models.PublicProfile
import com.example.pokinfo.data.models.database.pokemon.PokemonForList
import com.example.pokinfo.data.models.firebase.AttacksData
import com.example.pokinfo.data.models.firebase.PokemonTeam

abstract class BaseDiffCallback<T : Any>: DiffUtil.ItemCallback<T>() {
    override fun areItemsTheSame(oldItem: T, newItem: T): Boolean {
        return oldItem == newItem
    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: T, newItem: T): Boolean {
        return oldItem == newItem
    }
}

class AttacksDiffCallback: BaseDiffCallback<AttacksData>() {
    override fun areContentsTheSame(oldItem: AttacksData, newItem: AttacksData): Boolean {
        return oldItem.name == newItem.name && oldItem.levelLearned == newItem.levelLearned
    }
}

class PokemonDiffCallback: BaseDiffCallback<PokemonForList>() {

    override fun areItemsTheSame(oldItem: PokemonForList, newItem: PokemonForList): Boolean {
        return oldItem == newItem
    }
    override fun areContentsTheSame(oldItem: PokemonForList, newItem: PokemonForList): Boolean {
        return oldItem.id == newItem.id
    }
}

class AbilityDiffCallback: BaseDiffCallback<AbilityListAdapter.AbilityInfo>() {
    override fun areContentsTheSame(
        oldItem: AbilityListAdapter.AbilityInfo,
        newItem: AbilityListAdapter.AbilityInfo
    ): Boolean {
        return oldItem.abilityId == newItem.abilityId
    }
}

class ImagesDiffCallback: BaseDiffCallback<Pair<String, String>>() {
    override fun areContentsTheSame(
        oldItem: Pair<String, String>,
        newItem: Pair<String, String>
    ): Boolean {
        return oldItem.first == newItem.first && oldItem.second == newItem.second
    }
}

class TeamDiffCallback: BaseDiffCallback<PokemonTeam>() {
    override fun areContentsTheSame(oldItem: PokemonTeam, newItem: PokemonTeam): Boolean {
        return oldItem.id == newItem.id
    }
}

class UserDiffCallback: BaseDiffCallback<PublicProfile>() {
    override fun areContentsTheSame(oldItem: PublicProfile, newItem: PublicProfile): Boolean {
        return oldItem.username == newItem.username
    }
}