package com.example.pokinfo.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel


class SharedViewModel: ViewModel() {
    data class SnackbarMessageWithAction(
        val messageResId: Int? = null,
        val message: String? = null,
        val action: (() -> Unit)? = null
    )

    private val _snackBarSender = MutableLiveData<String>()
    val snackbarSender: LiveData<String>
        get() = _snackBarSender

    private val _snackbarResSender = MutableLiveData<Int>()
    val snackbarResSender: LiveData<Int>
        get() = _snackbarResSender

    private val _snackBarResWithAction = MutableLiveData<SnackbarMessageWithAction>()
    val snackBarResWithAction: LiveData<SnackbarMessageWithAction> get() = _snackBarResWithAction

    fun postMessage(messageRes: Int, action: (() -> Unit)? = null) {
        _snackBarResWithAction.postValue(SnackbarMessageWithAction(messageResId = messageRes, action = action))
    }

    fun postMessage(message: String, action: (() -> Unit)? = null) {
        _snackBarResWithAction.postValue(SnackbarMessageWithAction(message = message, action = action))
    }

    fun postMessage(message: String) {
        _snackBarSender.postValue(message)
    }

    fun postMessage(messageRes: Int) {
        _snackbarResSender.postValue(messageRes)
    }
}

