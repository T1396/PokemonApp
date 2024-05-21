package com.example.pokinfo.adapter.home


import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.pokinfo.R
import com.example.pokinfo.data.maps.typeColorMap
import com.example.pokinfo.data.models.database.pokemon.PokemonForList
import com.example.pokinfo.data.models.database.pokemon.PokemonTypeName
import com.example.pokinfo.data.util.ImageAltLoader.loadAnyImage
import com.example.pokinfo.data.util.PokemonDiffCallback
import com.example.pokinfo.databinding.ItemListPokemonBinding
import java.util.Locale

class PokeListAdapter(
    private val typeNames: List<PokemonTypeName>,
    private val onItemClicked: (pokemonId: Int) -> Unit
) : ListAdapter<PokemonForList, PokeListAdapter.ItemViewHolder>(PokemonDiffCallback()) {

    inner class ItemViewHolder(val binding: ItemListPokemonBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding =
            ItemListPokemonBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return currentList.size
    }


    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val binding = holder.binding
        val item = currentList[position]
        // ensures e.g. "4" will be displayed as "004"
        val formattedPosition = String.format(Locale.ROOT, "%03d", position.plus(1))
        holder.binding.tvNr.text = holder.itemView.context.getString(R.string.formatted_number, formattedPosition)

        R.string.failed_load_single_pokemon_data

        // bind name and image
        holder.binding.tvPokemonName.text = item.name
        val ivPokemon = holder.binding.ivPokemon
        loadAnyImage(ivPokemon, item.imageUrl, item.altImageUrl, item.officialImageUrl)

        // bind pokemon types
        bindPokeTypeCardView(holder, binding.cvTypeOne, item.typeId1, binding.tvPrimaryType)
        if (item.typeId2 == null) {
            holder.binding.cvTypeTwo.visibility = View.INVISIBLE
        } else {
            bindPokeTypeCardView(holder, binding.cvTypeTwo, item.typeId2, binding.tvSecondaryType)
            holder.binding.cvTypeTwo.visibility = View.VISIBLE
        }

        // set onclick listener to callback function
        holder.binding.clPokemonListItem.setOnClickListener {
            onItemClicked(item.id)
        }
    }

    /** Gets the color for the pokemon type and the type name and displays it into the card / textview given as parameter */
    private fun bindPokeTypeCardView(
        holder: ItemViewHolder,
        cardView: CardView,
        typeId: Int,
        textView: TextView
    ) {
        val typeColorRes = typeColorMap.entries.find { it.key == typeId }?.value?.first
            ?: R.color.type_colour_unknown
        val color = ContextCompat.getColor(holder.itemView.context, typeColorRes)
        cardView.setCardBackgroundColor(color)
        val primTypeName = typeNames.find { it.typeId == typeId }?.name
        textView.text = primTypeName
    }

}