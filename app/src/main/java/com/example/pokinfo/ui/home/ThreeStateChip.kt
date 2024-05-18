package com.example.pokinfo.ui.home

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import com.example.pokinfo.R
import com.example.pokinfo.data.enums.PokemonSortFilterState
import com.google.android.material.chip.Chip
import com.google.android.material.color.MaterialColors

class ThreeStateChip @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.chipStyle
) : Chip(context, attrs, defStyleAttr) {

    private val chipColors = ContextCompat.getColorStateList(context, R.color.chip_on_primary)
    private val chipTextColors = ContextCompat.getColorStateList(context, R.color.chip_on_primary_text)

    private val ascIcon = AppCompatResources.getDrawable(context, R.drawable.baseline_arrow_drop_up_24)
    private val descIcon = AppCompatResources.getDrawable(context, R.drawable.baseline_arrow_drop_down_24)

    var state: PokemonSortFilterState = PokemonSortFilterState.INACTIVE
        set(value) {
            field = value
            updateView()
        }

    fun nextState() {
        state = when(state) {
            PokemonSortFilterState.INACTIVE -> PokemonSortFilterState.DESCENDING
            PokemonSortFilterState.DESCENDING -> PokemonSortFilterState.ASCENDING
            PokemonSortFilterState.ASCENDING -> PokemonSortFilterState.INACTIVE
        }
    }

    fun resetState() {
        state = PokemonSortFilterState.INACTIVE
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
            PokemonSortFilterState.INACTIVE -> {
                isSelected = false
                chipIcon = null
            }
            PokemonSortFilterState.ASCENDING -> {
                isSelected = true
                chipIcon = ascIcon
            }
            PokemonSortFilterState.DESCENDING -> {
                isSelected = true
                chipIcon = descIcon
            }
        }
    }
}