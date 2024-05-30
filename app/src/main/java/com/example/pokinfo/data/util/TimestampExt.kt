package com.example.pokinfo.data.util

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

fun Timestamp.toGermanDateString(): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY)
    return sdf.format(this.toDate())
}