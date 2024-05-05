package com.example.pokinfo.data.util

sealed class UIState<out T> {
    data object Loading: UIState<Nothing>()
    data class Success<T>(val data: T): UIState<T>()
    data class Error(val exception: Exception, val messageResId: Int): UIState<Nothing>()
}