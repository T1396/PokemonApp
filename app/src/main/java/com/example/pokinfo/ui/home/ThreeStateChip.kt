package com.example.pokinfo.ui.home

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import com.example.pokinfo.R
import com.example.pokinfo.data.enums.PokemonSortOption
import com.google.android.material.chip.Chip

class ThreeStateChip @JvmOverloads constructor(
    context: Context,
    private val ascendingFirst: Boolean = false,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.chipStyle
) : Chip(context, attrs, defStyleAttr) {

    private val chipColors = ContextCompat.getColorStateList(context, R.color.chip_on_primary)
    private val chipTextColors = ContextCompat.getColorStateList(context, R.color.chip_on_primary_text)

    private val ascIcon = AppCompatResources.getDrawable(context, R.drawable.baseline_arrow_drop_up_24)
    private val descIcon = AppCompatResources.getDrawable(context, R.drawable.baseline_arrow_drop_down_24)

    var state: PokemonSortOption = PokemonSortOption.INACTIVE
        set(value) {
            field = value
            updateView()
        }

    fun nextState() {
        state = if (!ascendingFirst) when(state) {
            PokemonSortOption.INACTIVE -> PokemonSortOption.DESCENDING
            PokemonSortOption.DESCENDING -> PokemonSortOption.ASCENDING
            PokemonSortOption.ASCENDING -> PokemonSortOption.INACTIVE
        } else when(state) {
            PokemonSortOption.INACTIVE -> PokemonSortOption.ASCENDING
            PokemonSortOption.ASCENDING -> PokemonSortOption.DESCENDING
            PokemonSortOption.DESCENDING -> PokemonSortOption.INACTIVE

        }
    }

    fun resetState() {
        state = PokemonSortOption.INACTIVE
    }

    init {
        isCheckable = false
        isClickable = true
        chipBackgroundColor = chipColors
        setTextColor(chipTextColors)
        chipIconTint = chipTextColors
    }

    private fun updateView() {

        when (state) {
            PokemonSortOption.INACTIVE -> {
                isSelected = false
                chipIcon = null
            }
            PokemonSortOption.ASCENDING -> {
                isSelected = true
                chipIcon = ascIcon
            }
            PokemonSortOption.DESCENDING -> {
                isSelected = true
                chipIcon = descIcon
            }
        }
    }
}