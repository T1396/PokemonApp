package com.example.pokinfo.data

import android.app.Application
import com.example.pokinfo.data.local.PokeDatabase
import com.example.pokinfo.data.remote.PokeApi

/** ensures only 1 instance of repository is used in all view models */
object RepositoryProvider {
    private var repository: Repository? = null

    fun provideRepository(application: Application): Repository {
        if (repository == null) {
            repository = Repository(PokeApi, PokeDatabase.getDatabase(application))
        }
        return repository!!
    }
}
