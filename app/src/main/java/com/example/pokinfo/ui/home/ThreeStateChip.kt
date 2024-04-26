package com.example.pokinfo.ui.home

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import androidx.appcompat.content.res.AppCompatResources
import com.example.pokinfo.R
import com.example.pokinfo.data.util.PokemonSortFilterState
import com.google.android.material.chip.Chip
import com.google.android.material.color.MaterialColors

class ThreeStateChip @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = com.google.android.material.R.attr.chipStyle
) : Chip(context, attrs, defStyleAttr) {

    private val defaultColorStateList = ColorStateList.valueOf(Color.RED)

    private val checkedColor = MaterialColors.getColorStateList(context, com.google.android.material.R.attr.colorPrimary, defaultColorStateList)
    private val defaultColor = context.getColorStateList(R.color.transparent)

    private val textColorChecked = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnPrimary, 0)
    private val textColorDefault = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnBackground, 0)

    private val ascIcon = AppCompatResources.getDrawable(context, R.drawable.baseline_arrow_drop_up_24)
    private val descIcon = AppCompatResources.getDrawable(context, R.drawable.baseline_arrow_drop_down_24)
    var state: PokemonSortFilterState = PokemonSortFilterState.INACTIVE
        set(value) {
            field = value
            updateView()
        }

    fun nextState() {
        state = when(state) {
            PokemonSortFilterState.INACTIVE -> PokemonSortFilterState.ASCENDING
            PokemonSortFilterState.ASCENDING -> PokemonSortFilterState.DESCENDING
            PokemonSortFilterState.DESCENDING -> PokemonSortFilterState.INACTIVE
        }
    }

    fun resetState() {
        state = PokemonSortFilterState.INACTIVE
    }

    init {
        isCheckable = false
        isClickable = true
    }

    private fun updateView() {

        when (state) {
            PokemonSortFilterState.INACTIVE -> {
                chipIcon = null
                chipBackgroundColor = defaultColor
                setTextColor(textColorDefault)
            }
            PokemonSortFilterState.ASCENDING -> {
                chipBackgroundColor = checkedColor
                chipIcon = ascIcon
                setTextColor(textColorChecked)
            }
            PokemonSortFilterState.DESCENDING -> {
                chipBackgroundColor = checkedColor
                chipIcon = descIcon
                setTextColor(textColorChecked)
            }
        }
    }
}