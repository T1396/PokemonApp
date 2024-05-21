package com.example.pokinfo.data.models.firebase

import android.os.Parcelable
import com.example.pokinfo.data.models.database.pokemon.PokemonForList
import com.google.firebase.Timestamp
import kotlinx.parcelize.Parcelize

@Parcelize
data class PokemonTeam(
    var id: String = "",
    var name: String = "",
    var pokemons: List<TeamPokemon?> = List(6) { null },
    val timestamp: Timestamp = Timestamp(0, 0)
) : Parcelable {
    fun toHashMap(): Map<String, Any?> {
        return hashMapOf(
            "Name" to name,
            "id" to id,
            "Pokemon 1" to pokemons[0],
            "Pokemon 2" to pokemons[1],
            "Pokemon 3" to pokemons[2],
            "Pokemon 4" to pokemons[3],
            "Pokemon 5" to pokemons[4],
            "Pokemon 6" to pokemons[5],
            "Creation Date" to Timestamp.now().toDate()
        )
    }

    @Suppress("UNCHECKED_CAST")
    companion object {
        @JvmStatic
        fun fromMap(map: Map<String, Any?>, documentId: String): PokemonTeam? {
            if (map.isEmpty()) {
                return null
            }
            val name = map["Name"] as? String ?: ""
            val pokemons = List(6) { index ->
                (map["Pokemon ${index + 1}"] as? Map<String, Any?>)?.let {
                    TeamPokemon.fromMap(it)
                }
            }
            val timestamp = ((map["Creation Date"] as? Timestamp)?.let {
                Timestamp(it.seconds, it.nanoseconds)
            } ?: Timestamp(0, 0))
            return PokemonTeam(documentId, name, pokemons, timestamp)
        }
    }
}

@Parcelize
data class TeamPokemon(
    val pokemonId: Int = 0,
    val pokemonInfos: PokemonForList = PokemonForList(),
    var level: Int = 100,
    var gender: Int = 0,
    var attackOne: AttacksData? = null,
    var attackTwo: AttacksData? = null,
    var attackThree: AttacksData? = null,
    var attackFour: AttacksData? = null,
    var evList: List<EvIvData> = emptyList(),
    var ivList: List<EvIvData> = emptyList(),
    var abilityId: Int = 0
) : Parcelable {

    @Suppress("UNCHECKED_CAST")
    companion object {
        @JvmStatic
        fun fromMap(map: Map<String, Any?>): TeamPokemon? {
            if (map.isEmpty()) {
                return null
            }

            return TeamPokemon(
                pokemonId = (map["pokemonId"] as? Number)?.toInt() ?: 0,
                pokemonInfos = (map["pokemonInfos"] as? Map<String, Any?>)?.let {
                    PokemonForList.fromMap(
                        it
                    )
                } ?: PokemonForList(),
                level = (map["level"] as? Number)?.toInt() ?: 100,
                gender = (map["gender"] as? Number)?.toInt() ?: 0,
                attackOne = (map["attackOne"] as? Map<String, Any?>)?.let { AttacksData.fromMap(it) },
                attackTwo = (map["attackTwo"] as? Map<String, Any?>)?.let { AttacksData.fromMap(it) },
                attackThree = (map["attackThree"] as? Map<String, Any?>)?.let {
                    AttacksData.fromMap(
                        it
                    )
                },
                attackFour = (map["attackFour"] as? Map<String, Any?>)?.let { AttacksData.fromMap(it) },
                evList = (map["evList"] as? List<Map<String, Any?>>)?.map { EvIvData.fromMap(it) }
                    ?: emptyList(),
                ivList = (map["ivList"] as? List<Map<String, Any?>>)?.map { EvIvData.fromMap(it) }
                    ?: emptyList(),
                abilityId = (map["abilityId"] as? Number)?.toInt() ?: 0
            )
        }
    }
}

@Parcelize
data class EvIvData(
    val statId: Int = -1,
    val value: Int = 0,
) : Parcelable {
    companion object {
        fun fromMap(map: Map<String, Any?>): EvIvData {
            return EvIvData(
                statId = (map["statId"] as? Number)?.toInt() ?: -1,
                value = (map["value"] as? Number)?.toInt() ?: 0
            )
        }
    }
}

@Parcelize
data class AttacksData(
    val attackId: Int = -1,
    val name: String = "No data found",
    val levelLearned: Int = 0,
    val accuracy: Int? = null,
    val effectText: String = "",
    val moveDamageClassId: Int = 0,
    val power: Int = 0,
    val pp: Int = 0,
    val generationId: Int = 0,
    val typeId: Int = 0,
    var isExpanded: Boolean = false
) : Parcelable
{
    companion object {
        fun fromMap(map: Map<String, Any?>): AttacksData {
            return AttacksData(
                attackId = (map["attackId"] as? Number)?.toInt() ?: -1,
                name = map["name"] as? String ?: "No data found",
                levelLearned = (map["levelLearned"] as? Number)?.toInt() ?: 0,
                accuracy = (map["accuracy"] as? Number)?.toInt(),
                effectText = map["effectText"] as? String ?: "",
                moveDamageClassId = (map["moveDamageClassId"] as? Number)?.toInt() ?: 0,
                power = (map["power"] as? Number)?.toInt() ?: 0,
                pp = (map["pp"] as? Number)?.toInt() ?: 0,
                typeId = (map["typeId"] as? Number)?.toInt() ?: 0,
            )
        }
    }
}
