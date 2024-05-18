package com.example.pokinfo.viewModels.factory

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.pokinfo.viewModels.FirebaseViewModel
import com.example.pokinfo.viewModels.PokeViewModel
import com.example.pokinfo.viewModels.SharedViewModel
import com.example.pokinfo.viewModels.teambuilder.TeamBuilderViewModel

class ViewModelFactory(
    private val application: Application,
    private val sharedViewModel: SharedViewModel,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(TeamBuilderViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                TeamBuilderViewModel(application, sharedViewModel) as T
            }
            modelClass.isAssignableFrom(FirebaseViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                FirebaseViewModel(application, sharedViewModel) as T
            }
            modelClass.isAssignableFrom(PokeViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                PokeViewModel(application, sharedViewModel) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
