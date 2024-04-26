package com.example.pokinfo.data.models.fragmentDataclasses

import com.example.pokinfo.adapter.attacks.detail.AttackDescription
import com.example.pokinfo.data.models.database.type.PokemonTypeName
import com.example.pokinfo.data.models.database.versionAndLanguageNames.LanguageNames
import com.example.pokinfo.data.models.database.versionAndLanguageNames.VersionNames

// Attack-Query response will be mapped into this dataclass
data class PokemonMoveData(
    val typeId: Int,
    val accuracy: Int?,
    val power: Int,
    val pp: Int,
    val priority: Int,
    val moveTargetId: Int,
    val moveDamageClassId: Int,
    val generationId: Int,
    val names: List<Pair<String, Int>>, // name to languageId
    val attackDescriptions: List<AttackDescription>,
    val listOfPokemonIdsWhoLearn: List<Int>,
    val pokemonTypeNames: List<PokemonTypeName>, // typename to languageId
    val gameVersionList: List<VersionNames>,//
    val languageList: List<LanguageNames> // languagename to languageid
)



data class AttacksListData(
    val id: Int,
    val name: String = "No data found",
    val accuracy: Int,
    val moveDamageClassId: Int,
    val moveEffectId: Int? = null,
    val moveEffectChange: Int? = null,
    val moveTargetId: Int,
    val power: Int,
    val priority: Int,
    val pp: Int,
    val generationId: Int,
    val typeId: Int,
    val machineInfos: List<Triple<Int, Int, Int>>? = null,
)