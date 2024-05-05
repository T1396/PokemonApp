package com.example.pokinfo.ui.teambuilder

import android.widget.EditText
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import com.google.android.material.slider.Slider
import kotlin.math.max

class StatManager(private val updateEvLeftTextView: (Int, Int) -> Unit) {
    private val maxEvs = 508
    private val maxEvsPerStat = 252
    private val sliderEditTextMap = LinkedHashMap<EditText, Slider>()
    private val editTextIvMap = LinkedHashMap<TextView, EditText>()
    private var assignedEvs = 0

    fun registerEditTextAndSlider(editText: EditText, slider: Slider) {
        sliderEditTextMap[editText] = slider
        initSlider(slider, editText)
        initEditText(editText)
    }

    private fun initSlider(slider: Slider, editText: EditText) {
        slider.apply {
            valueFrom = 0f
            valueTo = maxEvsPerStat.toFloat()
            stepSize = 1f
            addOnChangeListener { _, value, fromUser ->
                // prevents recursive updates
                if (fromUser && editText.text.toString() != value.toInt().toString()) {
                    updateEv(editText, value.toInt(), fromSlider = true)
                }
            }
        }
    }


    private fun initEditText(editText: EditText) {
        editText.addTextChangedListener { changedEv ->
            val newValue = changedEv.toString().toIntOrNull() ?: 0
            if (newValue != sliderEditTextMap[editText]?.value?.toInt()) {
                updateEv(editText, newValue, fromSlider = false)
            }
        }
    }

    private fun updateEv(editText: EditText, input: Int, fromSlider: Boolean) {
        var newValue = input.coerceAtMost(maxEvsPerStat)
        val currentValue = editText.text.toString().toIntOrNull() ?: 0
        val newTotal = assignedEvs - currentValue + newValue

        if (newTotal > maxEvs) {
            newValue -= (newTotal - maxEvs)
            newValue = max(newValue, 0)
        }
        if (editText.text.toString() != newValue.toString()) {
            editText.setText(newValue.toString())
            editText.setSelection(editText.text.length)
        }
        if (!fromSlider && sliderEditTextMap[editText]?.value != newValue.toFloat()) {
            sliderEditTextMap[editText]?.value = newValue.toFloat()
        }

        updateTotalEvs()
    }

    private fun updateTotalEvs() {
        assignedEvs = sliderEditTextMap.keys.sumOf { it.text.toString().toIntOrNull() ?: 0 }
        updateEvLeftTextView(maxEvs, assignedEvs)
    }
}