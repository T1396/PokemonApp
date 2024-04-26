package com.example.pokinfo.data.models.database.pokemon

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "table_list_pokemon")
data class PokemonForList(
    @PrimaryKey
    val id: Int = -1,
    val name: String = "",
    val weight: Int = 0,
    val height: Int = 0,
    val imageUrl: String = "",
    val altImageUrl: String = "",
    val officialImageUrl: String = "",
    val stats: List<StatValues> = emptyList(),
    val typeId1: Int = 0,
    val typeId2: Int? = null
) : Parcelable
{
    companion object {
        fun fromMap(map: Map<String, Any?>): PokemonForList {
            return PokemonForList(
                id = (map["id"] as? Number)?.toInt() ?: -1,
                name = map["name"] as? String ?: "",
                weight = (map["weight"] as? Number)?.toInt() ?: -1,
                height = (map["height"] as? Number)?.toInt() ?: -1,
                imageUrl = map["imageUrl"] as? String ?: "",
                altImageUrl = map["altImageUrl"] as? String ?: "",
                stats = (map["stats"] as? List<Map<String, Any?>>)?.map { StatValues.fromMap(it) } ?: emptyList(),
                typeId1 = (map["typeId1"] as? Number)?.toInt() ?: -1,
                typeId2 = (map["typeId2"] as? Number)?.toInt(),
            )
        }
    }
}
@Parcelize
data class StatValues(
    val statValue : Int = 0,
    val statId: Int = 0
) : Parcelable
{
    companion object {
        fun fromMap(map: Map<String, Any?>): StatValues {
            return StatValues(
                statValue = (map["statValue"] as? Number)?.toInt() ?: -1,
                statId = (map["statId"] as? Number)?.toInt() ?: -1,
            )
        }
    }
}
