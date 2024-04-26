package com.example.pokinfo.data.util

import android.content.Context
import java.lang.IllegalArgumentException
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun <T> Context.sharedPreferences(keyName: String, defaultValue: T) = SharedPreferencesDelegate(this, keyName, defaultValue)

class SharedPreferencesDelegate<T>(
    context: Context,
    private val keyName: String,
    private val defaultValue: T
): ReadWriteProperty<Any?, T> {

    private val sharedPreferences by lazy {
        // check if data has been loaded already
        context.getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
    }
    @Suppress("UNCHECKED_CAST")
    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return when(defaultValue) {
            is Boolean -> sharedPreferences.getBoolean(keyName, defaultValue as Boolean) as T
            is Int -> sharedPreferences.getInt(keyName, defaultValue as Int) as T
            is String -> sharedPreferences.getString(keyName, defaultValue as String) as T
            else -> throw IllegalArgumentException("Unsupported Type")
        }
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        with(sharedPreferences.edit()) {
            when (value) {
                is Boolean -> putBoolean(keyName, value)
                is Int -> putInt(keyName, value)
                is String -> putString(keyName, value)
                else -> throw IllegalArgumentException("Unsupported Type")
            }
            apply()
        }
    }
}

