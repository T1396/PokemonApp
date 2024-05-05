package com.example.pokinfo.adapter.home


import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.pokinfo.R
import com.example.pokinfo.data.maps.typeColorMap
import com.example.pokinfo.data.models.database.pokemon.PokemonForList
import com.example.pokinfo.data.models.database.type.PokemonTypeName
import com.example.pokinfo.data.util.ImageAltLoader.loadAnyImage
import com.example.pokinfo.databinding.ItemListPokemonBinding

class PokeListAdapter(
    private val typeNames: List<PokemonTypeName>,
    private val onItemClicked: (pokemonId: Int) -> Unit
) :
    RecyclerView.Adapter<PokeListAdapter.ItemViewHolder>() {
    private var dataset: List<PokemonForList> = emptyList()

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(list: List<PokemonForList>) {
        dataset = list
        notifyDataSetChanged()
    }

    inner class ItemViewHolder(val binding: ItemListPokemonBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding =
            ItemListPokemonBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return dataset.size
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {

        val item = dataset[position]
        val formattedPosition = String.format("%03d", position.plus(1))
        holder.binding.tvNr.text = "#$formattedPosition"
        holder.binding.tvPokemonName.text = item.name
        val ivPokemon = holder.binding.ivPokemon
        loadAnyImage(ivPokemon, item.imageUrl, item.altImageUrl, item.officialImageUrl)

        // set primary cardview color
        val primaryTypeColorRes = typeColorMap.entries.find { it.key == item.typeId1 }?.value?.first
            ?: R.color.type_colour_unknown
        val primaryColor = ContextCompat.getColor(holder.itemView.context, primaryTypeColorRes)
        holder.binding.cvTypeOne.setCardBackgroundColor(primaryColor)
        val primTypeName = typeNames.find { it.typeId == item.typeId1 }?.name
        holder.binding.tvPrimaryType.text = primTypeName

        if (item.typeId2 != null) {
            // if pokemon has a secondary type
            // set typename
            val secTypeName = typeNames.find { it.typeId == item.typeId2 }?.name
            holder.binding.tvSecondaryType.text = secTypeName
            // set card color
            val secondaryTypeColorRes =
                typeColorMap.entries.find { it.key == item.typeId2 }?.value?.first
                    ?: R.color.type_colour_unknown
            val secondaryColor =
                ContextCompat.getColor(holder.itemView.context, secondaryTypeColorRes)
            holder.binding.cvTypeTwo.setCardBackgroundColor(secondaryColor)
            // make cardview visible
            holder.binding.cvTypeTwo.visibility = View.VISIBLE
        } else {
            holder.binding.cvTypeTwo.visibility = View.INVISIBLE
        }

        holder.binding.clPokemonListItem.setOnClickListener {
            onItemClicked(item.id)
        }
    }

}