package com.example.pokinfo.adapter.teamAndTeambuilder

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
import com.example.pokinfo.databinding.ItemListPokemonlistBinding


class AllPokemonAdapter(
    private val pokemonTypeNames: List<PokemonTypeName>,
    private val onItemClicked: (PokemonForList) -> Unit
) :
    RecyclerView.Adapter<AllPokemonAdapter.ItemViewHolder>() {
    private var dataset: List<PokemonForList> = emptyList()

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(list: List<PokemonForList>) {
        dataset = list
        notifyDataSetChanged()
    }



    inner class ItemViewHolder(val binding: ItemListPokemonlistBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemListPokemonlistBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return dataset.size
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val binding = holder.binding
        val bindMap = mapOf(
            1 to binding.tvHpVal,
            2 to binding.tvAtkVal,
            3 to binding.tvDefVal,
            4 to binding.tvSpAtkVal,
            5 to binding.tvSpDefVal,
            6 to binding.tvInitVal
        )
        val item = dataset[position]
        var isExpanded = false


        val ivPokemon = holder.binding.ivPokemon
        loadAnyImage(ivPokemon, item.imageUrl, item.altImageUrl, item.officialImageUrl)


        holder.binding.tvPokemonName.text = item.name
        bindPokeTypeCardView(holder, binding.cvTypeOne, item.typeId1, binding.tvPrimaryType)
        if (item.typeId2 == null) {
            holder.binding.cvTypeTwo.visibility = View.INVISIBLE
        } else {
            bindPokeTypeCardView(holder, binding.cvTypeTwo, item.typeId2, binding.tvSecondaryType)
            holder.binding.cvTypeTwo.visibility = View.VISIBLE
        }

        fillTableLayout(item, bindMap)


        // expand item
        holder.binding.ibExpand.setOnClickListener {
            isExpanded = !isExpanded
            holder.binding.tableLayout.visibility = if (isExpanded) View.VISIBLE else View.GONE
        }

        // if item is clicked do callback
        holder.binding.cltem.setOnClickListener {
            onItemClicked(item)
        }

    }

    private fun bindPokeTypeCardView(
        holder: AllPokemonAdapter.ItemViewHolder,
        cardView: CardView,
        typeId: Int,
        textView: TextView
    ) {
        val typeColorRes = typeColorMap.entries.find { it.key == typeId }?.value?.first
            ?: R.color.type_colour_unknown
        val color = ContextCompat.getColor(holder.itemView.context, typeColorRes)
        cardView.setCardBackgroundColor(color)
        val primTypeName = pokemonTypeNames.find { it.typeId == typeId }?.name
        textView.text = primTypeName
    }

    private fun fillTableLayout(
        item: PokemonForList,
        bindMap: Map<Int, TextView>
    ) {
        val stats = item.baseStats
        stats.forEachIndexed { index, statValues ->
            bindMap[index+1]?.text = statValues.statValue.toString()
        }
    }
}


