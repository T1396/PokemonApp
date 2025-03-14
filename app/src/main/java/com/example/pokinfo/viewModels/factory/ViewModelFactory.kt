package com.example.pokinfo.viewModels.factory

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.pokinfo.viewModels.AbilityViewModel
import com.example.pokinfo.viewModels.AttacksViewModel
import com.example.pokinfo.viewModels.AuthenticationViewModel
import com.example.pokinfo.viewModels.PokeViewModel
import com.example.pokinfo.viewModels.SharedViewModel
import com.example.pokinfo.viewModels.teambuilder.TeamBuilderViewModel
import com.example.pokinfo.viewModels.teams.TeamsViewModel

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
            modelClass.isAssignableFrom(AuthenticationViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                AuthenticationViewModel(application, sharedViewModel) as T
            }
            modelClass.isAssignableFrom(PokeViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                PokeViewModel(application, sharedViewModel) as T
            }
            modelClass.isAssignableFrom(AttacksViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                AttacksViewModel(application, sharedViewModel) as T
            }
            modelClass.isAssignableFrom(AbilityViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                AbilityViewModel(application, sharedViewModel) as T
            }
            modelClass.isAssignableFrom(TeamsViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                TeamsViewModel(application, sharedViewModel) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
