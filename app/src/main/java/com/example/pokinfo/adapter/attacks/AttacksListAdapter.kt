package com.example.pokinfo.adapter.attacks

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.pokinfo.R
import com.example.pokinfo.data.models.database.type.PokemonTypeName
import com.example.pokinfo.data.models.fragmentDataclasses.AttacksListData
import com.example.pokinfo.databinding.ItemListAttacksListBinding

class AttacksListAdapter(
    // map which holds the type colours/icons to change the card backgrounds and icons
    private val typeColorMap: Map<Int, Triple<Int, Int, Int>>,
    // name of every pokemon type
    private var pokemonTypeNames: List<PokemonTypeName>,
    private val onAttackClicked: (Int) -> Unit // callback to navigate
) :
    RecyclerView.Adapter<AttacksListAdapter.ItemViewHolder>() {
    private var dataset: List<AttacksListData> = emptyList()

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(list: List<AttacksListData>, callback: (() -> Unit)? = null) {
        dataset = list
        notifyDataSetChanged()
        callback?.invoke()
    }

    fun setTypeNames(pokemonTypeNames: List<PokemonTypeName>) {
        this.pokemonTypeNames = pokemonTypeNames
    }

    inner class ItemViewHolder(val binding: ItemListAttacksListBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemListAttacksListBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return dataset.size
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = dataset[position]
        holder.binding.tvAttackNr.text = "${position.plus(1)}"
        holder.binding.tvAtkName.text = item.name
        holder.binding.tvPower.text = if (item.power > 0) item.power.toString() else "-"
        holder.binding.tvAccuracy.text =
            holder.itemView.context.getString(
                R.string.common_percent_placeholder,
                item.accuracy.toString()
            )

        // gets color and icon resource ids from map value on the specific typeId
        val colorRes = typeColorMap[item.typeId]?.first ?: -1
        val color = ContextCompat.getColor(holder.itemView.context, colorRes)
        holder.binding.cvAtkType.setCardBackgroundColor(color)
        val id = when (item.moveDamageClassId) {
            1 -> R.drawable.pokemon_status_atk_icon
            2 -> R.drawable.pokemon_atk_icon
            else -> R.drawable.pokemon_sp_atk_icon
        }
        val drawable = ContextCompat.getDrawable(holder.itemView.context, id)
        holder.binding.ivTypeAttack.setImageDrawable(drawable)
        // it.second is the typeID // it.first typeName
        val text = pokemonTypeNames.find { it.typeId == item.typeId }?.name ?: "???"
        holder.binding.tvAttackType.text = text


        holder.binding.clCard.setOnClickListener {
            // sends a query to get details from the clicked move
            onAttackClicked(item.id)
        }
    }

}


