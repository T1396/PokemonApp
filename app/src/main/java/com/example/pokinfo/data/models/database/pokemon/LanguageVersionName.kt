package com.example.pokinfo.data.models.database.pokemon

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pokemon_language_names")
data class LanguageNames(
    @PrimaryKey
    val id: Int,
    val name: String,
    val languageId: Int,
)

@Entity(tableName = "pokemon_version_names")
data class VersionNames(
    @PrimaryKey val id: Long = 0,
    val versionId: Int,
    val name: String,
    val languageId: Int,
)



