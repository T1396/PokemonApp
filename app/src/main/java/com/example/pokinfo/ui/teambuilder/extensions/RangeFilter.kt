package com.example.pokinfo.ui.teambuilder.extensions

import android.text.InputFilter

val ivRangeFilter = InputFilter { source, start, end, dest, dstart, dend ->
    try {
        val resultText = StringBuilder(dest).insert(dstart, source, start, end).toString()
        val input = resultText.toInt()
        if (input in 0..31) {
            null
        } else {
            ""
        }
    } catch (nfe: NumberFormatException) {
        ""
    }
}