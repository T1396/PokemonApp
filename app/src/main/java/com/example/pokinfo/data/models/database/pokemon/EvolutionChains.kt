package com.example.pokinfo.data.models.database.pokemon

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Tables for pokemon evolutions, where different pokemon can belong to a evolutionChainId
 *  PkEvolutionDetails holds the pre and after evolution - species Id
 */

@Entity(tableName = "pokemon_evolution_chain")
data class PkEvolutionChain(
    @PrimaryKey val id: Int,
)

@Entity(
    tableName = "pokemon_evolutions",
    indices = [Index(value = ["evolvesFromSpeciesId"]), Index(value = ["evolutionChainId"])]
)
data class PkEvolutionDetails(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val speciesId: Int,
    val evolutionChainId: Int,
    val evolvesToSpeciesId: Int?,
    val evolvesFromSpeciesId: Int?,
    val minLevel: Int?
)

/**
 * Dataclass to use the Evolution Details in views, in a recursive way
 * @evolutionDetails is null if the pokemon that is the actual Stage is the root of the evolution
 * */
data class EvolutionStage(
    val pokemonInfos: PokemonForList,
    val evolutionDetails: PkEvolutionDetails?,
    val nextEvolutions: List<EvolutionStage> = emptyList()
)

