package com.example.pokinfo.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.pokinfo.viewModels.teambuilder.TeamBuilderViewModel

class SharedViewModel: ViewModel() {
    private val _snackBarSender = MutableLiveData<String>()
    val snackbarSender: LiveData<String>
        get() = _snackBarSender

    private val _snackbarResSender = MutableLiveData<Int>()
    val snackbarResSender: LiveData<Int>
        get() = _snackbarResSender

    fun postMessage(message: String) {
        _snackBarSender.postValue(message)
    }

    fun postMessage(messageRes: Int) {
        _snackbarResSender.postValue(messageRes)
    }
}

