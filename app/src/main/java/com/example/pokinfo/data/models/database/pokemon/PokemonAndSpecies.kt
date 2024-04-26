package com.example.pokinfo.data.models.database.pokemon

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.util.Date

data class PokemonData(
    val pokemon: Pokemon,
    val abilityInfoList: List<PkAbilityInfo>,
    val abilitiesToJoin: List<PkAbilitiesToJoin>,
    val specyData: PkSpecieInfos,
    val formData: List<PkForms>,
    val pokemonMoves: PkMoves,
    val pokedexEntries: List<PokemonDexEntries>,
    val moves: List<PkMove>,
    val moveMachines: List<PkMoveMachines>,
    val moveNames: List<PkMoveNames>,
    val specyNames: List<PkNames>,
    val abilityFlavorTexts: List<PkAbilityFlavorText>,
    val abilityEffectTexts: List<PkAbilityEffectText>,
    val abilityNames: List<PkAbilityName>,
    val versionGroupDetails: List<PkMoveVersionGroupDetail>
)

data class MoveInformation(
    val pokemonMoves: PkMoves,
    val moveInfosGeneral: List<PkMove>,
    val moveMachines: List<PkMoveMachines>,
    val moveNames: List<PkMoveNames>,
    val moveVersionDetails: List<PkMoveVersionGroupDetail>
)


@Entity(tableName = "pokemon_insert_status")
data class PokemonInsertStatus(
    @PrimaryKey
    val pokemonId: Int,
    val status: String,
    @ColumnInfo(name = "timestamp")
    val timestamp: Date = Date()
)

@Entity(tableName = "pokemon")
data class Pokemon(
    @PrimaryKey
    val id: Int,
    val order: Int,
    val name: String,
    val height: Int,
    val weight: Int,
    val specieId: Int,
    val primaryType: PkType,
    val secondaryType: PkType?,
    val pkStatInfos: List<PkStatInfos>,
    val sprites: String
)

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
    val slot: Int // Slot hinzugefügt, um die Position der Fähigkeit zu bestimmen
)

@Entity(
    tableName = "pokemon_move_machines"
)
data class PkMoveMachines(
    @PrimaryKey(autoGenerate = true) val id : Long = 0,
    val moveId: Int,
    val machineNr: Int,
    val versionGroupId: Int,
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

data class PkType(
    val slot: Int,
    val typeId: Int,
)

@Entity(
    tableName = "pokemon_specy_names",
    foreignKeys = [
        ForeignKey(
            entity = PkSpecieInfos::class,
            parentColumns = ["id"],
            childColumns = ["speciesId"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [Index(value = ["speciesId"])]
)
data class PkNames(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val speciesId: Int,
    val name: String,
    val genus: String,
    val languageId: Int,
)

data class PkStatInfos(
    val statId: Int,
    val baseStat: Int,
    val defaultName: String,
)


@Entity(tableName = "pokemon_specy")
data class PkSpecieInfos(
    @PrimaryKey
    val id: Int,
    val pokemonId: Int,
    val isLegendary: Boolean,
    val isMythical: Boolean,
    val genderRate: Int?,
    val captureRate: Int?,
    val baseHappiness: Int?,
    val order: Int,
    val evolvesFromSpeciesId: Int?,
    val evolvesToSpeciesIds: List<Long>?,
    val name: String
)

@Entity(tableName = "pokemon_forms")
data class PkForms(
    @PrimaryKey val formId: Int,
    val speciesId: Int,
    val defaultName: String,
    val name: String,
    val pokemonId: Int,
    val formOrder: Int,
    val order: Int,
    val isBattleOnly: Boolean,
    val isMega: Boolean,
    val isDefault: Boolean,
    val sprites: String
)


@Entity(
    tableName = "pokemon_pokedex_entries",
    foreignKeys = [
        ForeignKey(
            entity = PkSpecieInfos::class,
            parentColumns = ["id"],
            childColumns = ["speciesId"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [Index(value = ["speciesId"])]
)
data class PokemonDexEntries(
    @PrimaryKey val id: Long = 0,
    val speciesId: Int,
    val text: String,
    val languageId: Int,
    val versionGroupId: Int
)


@Entity(
    tableName = "pokemon_moves_list",
    foreignKeys = [
        ForeignKey(
            entity = Pokemon::class,
            parentColumns = ["id"],
            childColumns = ["pokemonId"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
)
data class PkMoves(
    @PrimaryKey
    val pokemonId: Int,
    val moveIds: List<Long>
)

@Entity(tableName = "pokemon_move_data")
data class PkMove(
    @PrimaryKey val id: Long,
    val name: String,
    val typeId: Int,
    val accuracy: Int?,
    val effectText: String?,
    val moveDamageClassId: Int?,
    val type: String? = null,
    val power: Int? = null,
    val ap: Int? = null
)

@Entity(
    tableName = "pokemon_move_names",
    indices = [Index(value = ["moveId"])]
)
data class PkMoveNames(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val moveId: Int,
    val name: String,
    val languageId: Int
)

@Parcelize
@Entity(tableName = "pokemon_move_version_group_details")
data class PkMoveVersionGroupDetail(
    @PrimaryKey val id: Long = 0,
    val moveId: Long,
    val levelLearnedAt: Int?,
    val pokemonId: Int?,
    val moveLearnMethod: String?,
    val moveLearnMethodId: Int?,
    val versionGroupId: Int?
) : Parcelable


