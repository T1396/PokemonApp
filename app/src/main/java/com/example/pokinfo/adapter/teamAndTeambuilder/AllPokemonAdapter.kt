package com.example.pokinfo.adapter.teamAndTeambuilder

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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
    fun submitList(list: List<PokemonForList>, callback: (() -> Unit)? = null) {
        dataset = list
        notifyDataSetChanged()
        callback?.invoke()
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

        val bindMap = mapOf(
            1 to holder.binding.tvHpVal,
            2 to holder.binding.tvAtkVal,
            3 to holder.binding.tvDefVal,
            4 to holder.binding.tvSpAtkVal,
            5 to holder.binding.tvSpDefVal,
            6 to holder.binding.tvInitVal
        )
        val item = dataset[position]
        var isExpanded = false


        val ivPokemon = holder.binding.ivPokemon
        loadAnyImage(ivPokemon, item.imageUrl, item.altImageUrl, item.officialImageUrl)


        holder.binding.tvPokemonName.text = item.name

        // set primary type name
        val primTypeName = pokemonTypeNames.find { it.typeId == item.typeId1 }?.name
        holder.binding.tvPrimaryType.text = primTypeName

        // set primary cardview color
        val primaryTypeColorRes = typeColorMap.entries.find { it.key == item.typeId1 }?.value?.first
            ?: R.color.type_colour_unknown
        val primaryColor = ContextCompat.getColor(holder.itemView.context, primaryTypeColorRes)
        holder.binding.cvTypeOne.setCardBackgroundColor(primaryColor)

        if (item.typeId2 != null) {
            // if pokemon has a secondary type
                // set typename
                val secTypeName = pokemonTypeNames.find { it.typeId == item.typeId2 }?.name
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


