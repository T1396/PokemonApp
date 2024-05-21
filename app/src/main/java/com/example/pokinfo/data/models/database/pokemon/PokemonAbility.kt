package com.example.pokinfo.data.models.database.pokemon

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey



@Entity(
    tableName = "pokemon_pokemon_abilities",
    primaryKeys = ["abilityId"],
    foreignKeys = [
        ForeignKey(entity = PkAbilityInfo::class, parentColumns = ["id"], childColumns = ["abilityId"])
    ],
    indices = [Index(value = ["abilityId"])]
)
data class PokemonAbilitiesList(
    val abilityId: Int,
    val pokemonIds: List<Long>
)

/** Link Table to hold each pokemon ability references */
@Entity(
    tableName = "pokemon_abilities_join",
    primaryKeys = ["pokemonId", "abilityId", "slot"], // Slot als Teil des Primärschlüssels hinzugefügt
    foreignKeys = [
        ForeignKey(entity = Pokemon::class, parentColumns = ["id"], childColumns = ["pokemonId"]),
        ForeignKey(
            entity = PkAbilityInfo::class,
            parentColumns = ["id"],
            childColumns = ["abilityId"]
        )
    ],
    indices = [Index(value = ["pokemonId"]), Index(value = ["abilityId"])]
)
data class PkAbilitiesToJoin(
    val pokemonId: Int,
    val abilityId: Int,
    val isHidden: Boolean,
    val slot: Int // Slot hinzugefügt, um die Position der Fähigkeit zu bestimmen
)

@Entity(
    tableName = "pokemon_ability_flavor_texts",
    foreignKeys = [
        ForeignKey(
            entity = PkAbilityInfo::class,
            parentColumns = ["id"],
            childColumns = ["abilityId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["abilityId"])]
)
data class PkAbilityFlavorText(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val abilityId: Int,
    val effectTextShort: String,
    val versionGroupId: Int,
    val languageId: Int
)

@Entity(
    tableName = "pokemon_ability_effect_texts",
    foreignKeys = [
        ForeignKey(
            entity = PkAbilityInfo::class,
            parentColumns = ["id"],
            childColumns = ["abilityId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["abilityId"])]
)
data class PkAbilityEffectText(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val abilityId: Int,
    val effectTextLong: String,
    val languageId: Int
)

@Entity(tableName = "pokemon_abilities")
data class PkAbilityInfo(
    @PrimaryKey val id: Int,
    val name: String,
)

@Entity(
    tableName = "pokemon_ability_names",
    foreignKeys = [
        ForeignKey(
            entity = PkAbilityInfo::class,
            parentColumns = ["id"],
            childColumns = ["abilityId"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [Index(value = ["abilityId"])]
)
data class PkAbilityName(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val abilityId: Int,
    val name: String,
    val languageId: Int
)