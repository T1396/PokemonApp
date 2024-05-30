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
import com.example.pokinfo.R
import com.example.pokinfo.data.models.firebase.PokemonTeam
import com.example.pokinfo.data.util.ImageAltLoader
import com.example.pokinfo.data.util.TeamDiffCallback
import com.example.pokinfo.data.util.toGermanDateString
import com.example.pokinfo.databinding.ItemListPokemonteamSmallBinding
import com.example.pokinfo.ui.teams.TeamType


class TeamAdapterSmall(
    private val teamType: TeamType = TeamType.PUBLIC_TEAMS,
    private val showCreator: Boolean = false,
    private val onItemLongClicked: (PokemonTeam) -> Unit,
    private val onCreatorClicked: ((String) -> Unit)? = null,
    private val onLiked: ((String, String) -> Unit)? = null,
    private val onLikeRemove: ((String, String) -> Unit)? = null
) :
    ListAdapter<PokemonTeam, TeamAdapterSmall.ItemViewHolder>(TeamDiffCallback()) {

    private var currentLikedTeams: List<String> = emptyList()
    private var isInitialized = false


    fun setLikedTeams(teams: List<String>) {
        if (!isInitialized) {
            currentLikedTeams = teams
            currentLikedTeams.forEach {  id ->
                val index = currentList.indexOfFirst { it.id == id }
                if (index != -1) notifyItemChanged(index)
            }
            isInitialized = true
        }
    }



    inner class ItemViewHolder(val binding: ItemListPokemonteamSmallBinding) :
        RecyclerView.ViewHolder(binding.root)



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemListPokemonteamSmallBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return currentList.size
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val team = currentList[position]
        val pokemonSlotViewList = getImageTextViewPairList(holder.binding)
        val pokemonList = team.pokemons

        holder.binding.tvTeamname.text = team.name
        holder.binding.tvCreationDate.text = team.timestamp.toGermanDateString()

        if (teamType == TeamType.PUBLIC_TEAMS) {
            holder.binding.likeLayout.visibility = View.VISIBLE
            holder.binding.likeButton.isSelected = team.id in currentLikedTeams
            holder.binding.tvLikeCount.text = team.likeCount.toString()
            holder.binding.tvCreator.visibility = if (showCreator) View.VISIBLE else View.GONE
            holder.binding.tvCreator.text = team.creator

            holder.binding.tvCreator.setOnClickListener {
                onCreatorClicked?.invoke(team.ownerId)
            }

            holder.binding.likeButton.visibility = View.VISIBLE
            holder.binding.likeButton.setOnClickListener {
                it.isSelected = !it.isSelected

                if (it.isSelected) {
                    animateLikeButton(it)
                    onLiked?.invoke(team.ownerId, team.id)
                    val actualTextAsInt = holder.binding.tvLikeCount.text.toString().toIntOrNull() ?: 0
                    holder.binding.tvLikeCount.text =  (actualTextAsInt + 1).toString()
                } else {
                    onLikeRemove?.invoke(team.ownerId, team.id)
                    val actualTextAsInt = holder.binding.tvLikeCount.text.toString().toIntOrNull() ?: 0
                    holder.binding.tvLikeCount.text =  (actualTextAsInt - 1).toString()
                }
            }
        }

        pokemonList.forEachIndexed { index, teamPokemon ->

            val imageView = pokemonSlotViewList[index].first
            imageView.setImageResource(R.drawable.pokeball) // placeholder
            val textView = pokemonSlotViewList[index].second
            textView.text = ""
            if (teamPokemon != null) {
                ImageAltLoader.loadAnyImage(
                    imageView,
                    teamPokemon.pokemonInfos.imageUrl,
                    teamPokemon.pokemonInfos.altImageUrl,
                    teamPokemon.pokemonInfos.officialImageUrl
                )
                textView.text = teamPokemon.pokemonInfos.name
            } else {
                val text = holder.itemView.context.getString(R.string.slot_placeholder, index + 1)
                textView.text = text
            }
        }

//        holder.binding.cv.setOnLongClickListener {
//            onItemLongClicked(team)
//            true
//        }
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

    private fun getImageTextViewPairList(holder: ItemListPokemonteamSmallBinding): List<Pair<ImageView, TextView>> {
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