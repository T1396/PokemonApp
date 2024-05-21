package com.example.pokinfo.adapter.home


import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.pokinfo.R
import com.example.pokinfo.data.maps.typeColorMap
import com.example.pokinfo.data.models.database.pokemon.PokemonForList
import com.example.pokinfo.data.models.database.pokemon.PokemonTypeName
import com.example.pokinfo.data.util.ImageAltLoader.loadAnyImage
import com.example.pokinfo.databinding.ItemListPokemonBinding
import java.util.Locale

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
        val binding = holder.binding
        val item = dataset[position]
        val formattedPosition = String.format(Locale.ROOT, "%03d", position.plus(1))
        holder.binding.tvNr.text = "#$formattedPosition"
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

        holder.binding.clPokemonListItem.setOnClickListener {
            onItemClicked(item.id)
        }
    }

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