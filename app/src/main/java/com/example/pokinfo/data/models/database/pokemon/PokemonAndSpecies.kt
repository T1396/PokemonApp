package com.example.pokinfo.data.models.database.pokemon

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/** Data classes to unite different Pokemon Details */
data class PokemonData(
    val pokemon: Pokemon,
    val abilityInfoList: List<PkAbilityInfo>,
    val abilitiesToJoin: List<PkAbilitiesToJoin>,
    val specyData: PkSpecieInfo,
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
    val versionGroupDetails: List<PkMoveVersionGroupDetail>,
    val evolutionChain: PkEvolutionChain?,
    val evolutionDetails: List<PkEvolutionDetails>?,
    val abilitiesPokemonList: List<PokemonAbilitiesList>? = null
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
    val displayName: String,
    val height: Int,
    val weight: Int,
    val specieId: Int,
    val primaryType: PkType,
    val secondaryType: PkType?,
    val pkStatInfos: List<PkStatInfos>,
    val sprites: String
)

data class PkType(
    val slot: Int,
    val typeId: Int,
)

@Entity(
    tableName = "pokemon_specy_names",
    foreignKeys = [
        ForeignKey(
            entity = PkSpecieInfo::class,
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
data class PkSpecieInfo(
    @PrimaryKey
    val id: Int,
    val pokemonId: Int,
    val isLegendary: Boolean,
    val isMythical: Boolean,
    val genderRate: Int?,
    val captureRate: Int?,
    val baseHappiness: Int?,
    val order: Int,
    val evolutionChainId: Int?,
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
            entity = PkSpecieInfo::class,
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



