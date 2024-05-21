package com.example.pokinfo.adapter.teamAndTeambuilder

import android.annotation.SuppressLint
import android.icu.text.SimpleDateFormat
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.pokinfo.R
import com.example.pokinfo.data.models.firebase.PokemonTeam
import com.example.pokinfo.data.util.ImageAltLoader.loadAnyImage
import com.example.pokinfo.databinding.ItemListPokemonteamBinding
import java.util.Locale


class PokemonTeamAdapter(private val onItemLongClicked: (PokemonTeam) -> Unit) :
    RecyclerView.Adapter<PokemonTeamAdapter.ItemViewHolder>() {
    private var dataset: List<PokemonTeam> = emptyList()


    @SuppressLint("NotifyDataSetChanged")
    fun submitList(list: List<PokemonTeam>, callback: (() -> Unit)? = null) {
        dataset = list
        notifyDataSetChanged()
        callback?.invoke()
    }

    inner class ItemViewHolder(val binding: ItemListPokemonteamBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemListPokemonteamBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return dataset.size
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val team = dataset[position]
        val list = getImageTextViewPairList(holder.binding)
        val pokemonList = team.pokemons

        holder.binding.tvTeamname.text = team.name
        // convert to european pattern
        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val date = team.timestamp.toDate()
        val formattedDate = sdf.format(date)
        holder.binding.tvCreationDate.text = formattedDate

        pokemonList.forEachIndexed { index, teamPokemon ->

            val imageView = list[index].first
            imageView.setImageResource(R.drawable.pokeball) // placeholder
            val textView = list[index].second
            textView.text = ""
            if (teamPokemon != null) {
                loadAnyImage(imageView, teamPokemon.pokemonInfos.imageUrl, teamPokemon.pokemonInfos.altImageUrl, teamPokemon.pokemonInfos.officialImageUrl)
                textView.text = teamPokemon.pokemonInfos.name
            } else {
                val text = holder.itemView.context.getString(R.string.slot_placeholder, index+1)
                textView.text = text
            }
        }

        holder.binding.cv.setOnLongClickListener {
            onItemLongClicked(team)
            true
        }



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