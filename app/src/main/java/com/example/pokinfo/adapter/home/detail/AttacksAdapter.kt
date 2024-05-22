package com.example.pokinfo.adapter.home.detail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.pokinfo.R
import com.example.pokinfo.data.maps.typeColorMap
import com.example.pokinfo.data.models.database.pokemon.PokemonTypeName
import com.example.pokinfo.data.models.firebase.AttacksData
import com.example.pokinfo.data.util.AttacksDiffCallback
import com.example.pokinfo.databinding.ItemListAttacksBinding

/** Adapter to display pokemon attacks
 *  Depending on the constructor parameter the adapter can 
 *  offer a selection mode for this attacks which is used in the Team Builder Fragment
 * */
class AttacksAdapter(
    private val pokemonTypeNames: List<PokemonTypeName>,
    private val showLevel: Boolean = true,
    private val showPosition: Boolean = true,
    private val selectAttackEnabled: Boolean = false,
    private val onAttackClicked: ((Int) -> Unit)? = null,
) :
    ListAdapter<AttacksData, AttacksAdapter.ItemViewHolder>(AttacksDiffCallback()) {
    // only used if callback function is not null

    private var selectedAttacks: MutableList<AttacksData> = mutableListOf()

    private var expandedAttackNames: MutableSet<String> = mutableSetOf()


    inner class ItemViewHolder(val binding: ItemListAttacksBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemListAttacksBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)

    }

    override fun getItemCount(): Int {
        return currentList.size
    }


    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val attack = currentList[position]
        val isSelected = selectedAttacks.contains(attack)
        holder.binding.clItem.isSelected = isSelected

        /** on click listener */
        if (selectAttackEnabled) {
            // 
            setSelectAttacksClickListener(holder, isSelected, attack, position)
        } else {
            holder.binding.clItem.setOnClickListener {
                onAttackClicked?.invoke(attack.attackId) // for navigating to an attack the user clicked
            }
        }

        /** expand functionality */
        val isExpanded = isAttackExpanded(attack)
        holder.binding.tvEffectText.visibility = if (isExpanded) View.VISIBLE else View.GONE
        holder.binding.tvEffectText.text = attack.effectText
        holder.binding.ibExpand.setOnClickListener {
            toggleAttackExpansion(attack)
            notifyItemChanged(position)
        }

        holder.binding.tvLevelLearnedAt.visibility = if (showLevel) View.VISIBLE else View.GONE
        holder.binding.tvLevelLearnedAt.text = if (!showPosition) {
            if (attack.levelLearned > 0) attack.levelLearned.toString() else "-"
        } else {
            (position + 1).toString()
        }
        holder.binding.tvAtkName.text = attack.name
        holder.binding.tvPower.text = if (attack.power > 0) attack.power.toString() else "-"
        holder.binding.tvAccuracy.text = if (attack.accuracy == null) "-" else {
            holder.itemView.context.getString(
                R.string.common_percent_placeholder,
                attack.accuracy.toString()
            )
        }
        holder.binding.tvAP.text = attack.pp.toString()
        // gets color and icon resource ids from map value on the specific typeId
        val colorRes = typeColorMap[attack.typeId]?.first ?: -1

        val color = ContextCompat.getColor(holder.itemView.context, colorRes)
        holder.binding.cvAtkType.setCardBackgroundColor(color)

        val drawableRes = when (attack.moveDamageClassId) {
            1 -> R.drawable.pokemon_status_atk_icon
            2 -> R.drawable.pokemon_atk_icon
            else -> R.drawable.pokemon_sp_atk_icon
        }
        val drawable = ContextCompat.getDrawable(holder.itemView.context, drawableRes)
        holder.binding.ivTypeAttack.setImageDrawable(drawable)

        val typeName = pokemonTypeNames.find { it.typeId == attack.typeId }?.name ?: "???"
        holder.binding.tvAttackType.text = typeName
    }

    /** Set click listener functionality for every item */
    private fun setSelectAttacksClickListener(
        holder: ItemViewHolder,
        isSelected: Boolean,
        attack: AttacksData,
        position: Int
    ) {
        holder.binding.clItem.setOnClickListener {
            if (isSelected) {
                selectedAttacks.remove(attack)
                // updates the stroke of the background
                holder.binding.clItem.isSelected = false
            } else if (selectedAttacks.size < 4) {
                selectedAttacks.add(attack)
                // updates the stroke of the background
                holder.binding.clItem.isSelected = true
            }
            notifyItemChanged(position)
            onAttackClicked?.invoke(selectedAttacks.size)
        }
    }

    private fun isAttackExpanded(attack: AttacksData): Boolean {
        return expandedAttackNames.contains(attack.name)
    }

    private fun toggleAttackExpansion(attack: AttacksData) {
        if (isAttackExpanded(attack)) {
            expandedAttackNames.remove(attack.name)
        } else {
            expandedAttackNames.add(attack.name)
        }
    }

    fun getSelectedAttacks(): List<AttacksData> {
        return selectedAttacks
    }


    fun selectAttacks(attackList: List<AttacksData>) {
        selectedAttacks = attackList.toMutableList()
    }

}
