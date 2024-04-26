package com.example.pokinfo.adapter.home.detail

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.pokinfo.R
import com.example.pokinfo.data.models.database.type.PokemonTypeName
import com.example.pokinfo.data.models.firebase.AttacksData
import com.example.pokinfo.databinding.ItemListTeamBuilderAttacksBinding

class AttacksAdapter(
    //map of typeId to colorRes, IconRes, TextColor
    private val typeColorMap: Map<Int, Triple<Int, Int, Int>>,
    private val pokemonTypeNames: List<PokemonTypeName>,
    private val showExpandButton: Boolean,
    private val onAttackSelected: ((List<AttacksData>) -> Unit)? = null,
) :
    ListAdapter<AttacksData, AttacksAdapter.ItemViewHolder>(com.example.pokinfo.data.util.DiffUtil()) {
    // only used if callback function is not null

    private val isSelectedBackgroundRes = R.drawable.card_selected_background


    private var selectedAttacks: MutableList<AttacksData> = mutableListOf()
    private var expandedAttackNames: MutableSet<String> = mutableSetOf()

    private fun isAttackExpanded(attack: AttacksData): Boolean {
        return expandedAttackNames.contains(attack.name)
    }

    private fun toggleAttackExpansion(attack: AttacksData) {
        if (expandedAttackNames.contains(attack.name)) {
            expandedAttackNames.remove(attack.name)
        } else {
            expandedAttackNames.add(attack.name)
        }
    }

    private fun sortAttacks() {
        val sortedList = currentList.sortedWith(compareByDescending<AttacksData> { it in selectedAttacks }.thenBy { selectedAttacks.indexOf(it) }.thenBy { it.name })
        submitList(sortedList)
        notifyDataSetChanged()
    }

    // selects attacks of a pokemon
    fun selectAttacks(listPos: List<AttacksData?>) {
        var changed = false
        listPos.forEach { attack ->
            if (attack != null) {
                val attackToSelect = currentList.find { it.name == attack.name }
                if (attackToSelect != null && attackToSelect !in selectedAttacks) {
                    selectedAttacks.add(attackToSelect)
                    changed = true
                } else if (attackToSelect != null && attackToSelect in selectedAttacks) {
                    selectedAttacks.remove(attackToSelect)
                    changed = true
                }
            }
        }
        if (changed) {
            sortAttacks()
/*            selectedAttacks.forEach { attack ->
                val pos = currentList.indexOf(attack)
                //notifyItemChanged(pos)
            }*/
            Log.d("callback", "Done")
            onAttackSelected?.invoke(selectedAttacks)
        }
    }

    inner class ItemViewHolder(val binding: ItemListTeamBuilderAttacksBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
            val binding = ItemListTeamBuilderAttacksBinding
                .inflate(LayoutInflater.from(parent.context), parent, false)
            return ItemViewHolder(binding)

    }

    override fun getItemCount(): Int {
        return currentList.size
    }



    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val attack = currentList[position]
        var isSelected: Boolean
        val isExpanded = isAttackExpanded(attack)
        // if callBackFunction is given in to adapter create selection behavior
        if (onAttackSelected != null) {
            isSelected = selectedAttacks.contains(attack)
            setIsSelectedBackground(holder.binding.clItem, isSelected)
            setCardClickListener(isSelected, holder.binding.clItem, attack)
        }

        if (showExpandButton) {
            holder.binding.ibExpand.visibility = View.VISIBLE
            holder.binding.tvLevelLearnedAt.visibility = View.GONE


            holder.binding.tvEffectText.visibility = if (isExpanded) View.VISIBLE else View.GONE
            holder.binding.tvEffectText.text = attack.effectText

            holder.binding.ibExpand.setOnClickListener {
                toggleAttackExpansion(attack)
                notifyItemChanged(position)
            }

        } else {
            holder.binding.tvLevelLearnedAt.visibility = View.VISIBLE
            holder.binding.ibExpand.visibility = View.GONE
        }



        holder.binding.tvEffectText.visibility = if (isExpanded) View.VISIBLE else View.GONE


        holder.binding.tvLevelLearnedAt.text =
            if (attack.levelLearned > 0) attack.levelLearned.toString() else "-"
        holder.binding.tvAtkName.text = attack.name
        holder.binding.tvPower.text = if (attack.power > 0) attack.power.toString() else "-"
        holder.binding.tvAccuracy.text = if (attack.accuracy == null) "-" else {
            holder.itemView.context.getString(R.string.common_percent_placeholder, attack.accuracy.toString())
        }

        // gets color and icon resource ids from map value on the specific typeId
        val colorRes = typeColorMap[attack.typeId]?.first ?: -1

        val color = ContextCompat.getColor(holder.itemView.context, colorRes)
        holder.binding.cvAtkType.setCardBackgroundColor(color)

        val id = when (attack.moveDamageClassId) {
            1 -> R.drawable.pokemon_status_atk_icon
            2 -> R.drawable.pokemon_atk_icon
            else -> R.drawable.pokemon_sp_atk_icon
        }
        val drawable = ContextCompat.getDrawable(holder.itemView.context, id)
        holder.binding.ivTypeAttack.setImageDrawable(drawable)

        val typeName = pokemonTypeNames.find { it.typeId == attack.typeId }?.name ?: "???"
        holder.binding.tvAttackType.text = typeName



    }

    private fun setIsSelectedBackground(card: ConstraintLayout, isSelected: Boolean) {
        if (isSelected) {
            card.setBackgroundResource(isSelectedBackgroundRes)
        } else {
            // Standardhintergrundfarbe oder eine andere Farbe für nicht ausgewählte Items
            card.setBackgroundColor(Color.TRANSPARENT)
        }
    }

    private fun setCardClickListener(
        isSelected: Boolean,
        clItem: ConstraintLayout,
        attack: AttacksData
    ) {

        clItem.setOnClickListener {
            if (isSelected) {
                selectedAttacks.remove(attack)
                onAttackSelected?.invoke(selectedAttacks)
                sortAttacks()

            } else if (selectedAttacks.size < 4) {
                selectedAttacks.add(attack)
                onAttackSelected?.invoke(selectedAttacks)
                sortAttacks()
            }
        }
    }

}
