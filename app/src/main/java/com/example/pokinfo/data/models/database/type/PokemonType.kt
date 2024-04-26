package com.example.pokinfo.data.models.database.type

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity(tableName = "pokemon_types")
data class PokemonType(
    @PrimaryKey val id: Int,
    val name: String,
)

@Entity(tableName = "pokemon_types_moves_with_type")
data class MoveWithType(
    @PrimaryKey val name: String,
    val url: String,
    val typeId: Int // ID des Typs, dem die Attacke gehört
)

@Entity(tableName = "pokemon_types_damage_relations")
data class DamageRelation(
    @PrimaryKey val typeId: Int, // Id des Typs
    val doubleDamageFrom: List<String>,
    val doubleDamageTo: List<String>,
    val halfDamageFrom: List<String>,
    val halfDamageTo : List<String>,
    val noDamageFrom: List<String>,
    val noDamageTo: List<String>
)

@Entity(tableName = "pokemon_types_game_indices")
data class GameIndex(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameIndex: Int,
    val generationName: String,
    val generationUrl: String,
    val typeId: Int
)

@Parcelize
@Entity(tableName = "pokemon_types_type_names")
data class PokemonTypeName(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",
    val typeId: Int = -1,
    val languageName: String = "",
    val languageId: Int = -1
) : Parcelable

@Entity(tableName = "pokemon_types_pokemon_with_type")
data class PokemonWithThisType(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pokemonName: String,
    val slot: Int,
    val typeId: Int
)
