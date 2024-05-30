package com.example.pokinfo.adapter.teamAndTeambuilder

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.dispose
import com.example.pokinfo.R
import com.example.pokinfo.data.models.firebase.PokemonTeam
import com.example.pokinfo.data.models.firebase.TeamPokemon
import com.example.pokinfo.data.util.ImageAltLoader.loadAnyImage
import com.example.pokinfo.data.util.TeamDiffCallback
import com.example.pokinfo.data.util.toGermanDateString
import com.example.pokinfo.databinding.ItemListPokemonteamBinding
import com.example.pokinfo.ui.teams.TeamType


class TeamAdapterLarge(
    private val teamType: TeamType,
    private val onItemLongClicked: (PokemonTeam, TeamType) -> Unit,
    private val onCreatorClicked: ((String) -> Unit)? = null,
    private val onLiked: ((String, String) -> Unit)? = null,
    private val onLikeRemove: ((String, String) -> Unit)? = null
) :
    ListAdapter<PokemonTeam, TeamAdapterLarge.ItemViewHolder>(TeamDiffCallback()) {

    private var currentLikedTeams: MutableList<String> = mutableListOf()
    private var isInitialized = false


    fun setLikedTeams(teams: List<String>) {
        if (!isInitialized) {
            currentLikedTeams = teams.toMutableList()
            currentLikedTeams.forEach {  id ->
                val index = currentList.indexOfFirst { it.id == id }
                if (index != -1) notifyItemChanged(index)
            }
            isInitialized = true
        }
    }

    inner class ItemViewHolder(val binding: ItemListPokemonteamBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemListPokemonteamBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val team = currentList[position]
        val pokemonList = team.pokemons

        holder.binding.tvTeamname.text = team.name
        holder.binding.tvCreationDate.text = team.timestamp.toGermanDateString()
        if (teamType == TeamType.SHARED_TEAMS) {
            showCreatorAndSetClickListener(holder, team)
        }
        if (teamType == TeamType.PUBLIC_TEAMS) {
            holder.binding.likeLayout.visibility = View.VISIBLE
            holder.binding.likeButton.isSelected = team.id in currentLikedTeams
            holder.binding.tvLikeCount.text = team.likeCount.toString()
            showCreatorAndSetClickListener(holder, team)

            holder.binding.likeButton.setOnClickListener {
                it.isSelected = !it.isSelected
                toggleLikeButton(it, team, holder)
            }
        }

        updatePokemonViews(pokemonList, holder)

        holder.binding.cv.setOnLongClickListener {
            onItemLongClicked(team, teamType)
            true
        }
    }

    override fun getItemCount(): Int {
        return currentList.size
    }
    private fun toggleLikeButton(
        it: View,
        team: PokemonTeam,
        holder: ItemViewHolder
    ) {
        if (it.isSelected) {
            animateLikeButton(it)
            onLiked?.invoke(team.ownerId, team.id)
            val actualTextAsInt =
                holder.binding.tvLikeCount.text.toString().toIntOrNull() ?: 0
            holder.binding.tvLikeCount.text =
                holder.itemView.context.getString(R.string.like_count, actualTextAsInt + 1)
            currentLikedTeams.add(team.id)
        } else {
            onLikeRemove?.invoke(team.ownerId, team.id)
            val actualTextAsInt =
                holder.binding.tvLikeCount.text.toString().toIntOrNull() ?: 0
            holder.binding.tvLikeCount.text =
                holder.itemView.context.getString(R.string.like_count, actualTextAsInt - 1)
            currentLikedTeams.remove(team.id)
        }
    }

    private fun showCreatorAndSetClickListener(
        holder: ItemViewHolder,
        team: PokemonTeam
    ) {
        holder.binding.tvCreator.visibility = View.VISIBLE
        holder.binding.tvCreator.text = team.creator

        holder.binding.tvCreator.setOnClickListener {
            onCreatorClicked?.invoke(team.ownerId)
        }
    }

    private fun updatePokemonViews(
        pokemonList: List<TeamPokemon?>,
        holder: ItemViewHolder,
    ) {
        val list = getImageTextViewPairList(holder.binding)
        pokemonList.forEachIndexed { index, teamPokemon ->


            val imageView = list[index].first
            imageView.dispose()
            val textView = list[index].second
            textView.text = ""

            if (teamPokemon != null) {
                loadAnyImage(
                    imageView,
                    teamPokemon.pokemonInfos.imageUrl,
                    teamPokemon.pokemonInfos.altImageUrl,
                    teamPokemon.pokemonInfos.officialImageUrl
                )
                textView.text = teamPokemon.pokemonInfos.name
            } else {
                val text = holder.itemView.context.getString(R.string.slot_placeholder, index + 1)
                textView.text = text
                imageView.setImageResource(R.drawable.pokeball)
            }
        }
    }

    private fun animateLikeButton(it: View?) {
        val scaleUp = ObjectAnimator.ofPropertyValuesHolder(
            it,
            PropertyValuesHolder.ofFloat("scaleX", 1.0f, 1.2f),
            PropertyValuesHolder.ofFloat("scaleY", 1.0f, 1.2f)
        )
        scaleUp.duration = 150

        val scaleDown = ObjectAnimator.ofPropertyValuesHolder(
            it,
            PropertyValuesHolder.ofFloat("scaleX", 1.2f, 1.0f),
            PropertyValuesHolder.ofFloat("scaleY", 1.2f, 1.0f)
        )
        scaleDown.duration = 150

        val pulse = AnimatorSet()
        pulse.play(scaleUp).before(scaleDown)
        pulse.start()
    }


    private fun getImageTextViewPairList(holder: ItemListPokemonteamBinding): List<Pair<ImageView, TextView>> {
        return listOf(
            holder.ivPokemon1 to holder.tvPokemon1,
            holder.ivPokemon2 to holder.tvPokemon2,
            holder.ivPokemon3 to holder.tvPokemon3,
            holder.ivPokemon4 to holder.tvPokemon4,
            holder.ivPokemon5 to holder.tvPokemon5,
            holder.ivPokemon6 to holder.tvPokemon6,
        )
    }
}