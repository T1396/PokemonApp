package com.example.pokinfo.adapter.home


import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.pokinfo.data.models.database.pokemon.PokemonForList
import com.example.pokinfo.data.util.ImageAltLoader.loadAnyImage
import com.example.pokinfo.databinding.ItemListPokemonBinding

class PokeListAdapter(
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

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = dataset[position]
        holder.binding.tvNr.text = "${position.plus(1)}"
        holder.binding.tvPokemonName.text = item.name
        val ivPokemon = holder.binding.ivPokemon
        loadAnyImage(ivPokemon, item.imageUrl, item.altImageUrl, item.officialImageUrl)

        holder.binding.clPokemonListItem.setOnClickListener {
            onItemClicked(item.id)
        }
    }

}