package com.example.pokinfo.data.mapper

import com.example.pokeinfo.data.graphModel.AttackDetailsQuery
import com.example.pokeinfo.data.graphModel.AttacksQuery
import com.example.pokinfo.adapter.attacks.detail.AttackDescription
import com.example.pokinfo.data.models.database.type.PokemonTypeName
import com.example.pokinfo.data.models.database.versionAndLanguageNames.LanguageNames
import com.example.pokinfo.data.models.database.versionAndLanguageNames.VersionNames
import com.example.pokinfo.data.models.fragmentDataclasses.AttacksListData
import com.example.pokinfo.data.models.fragmentDataclasses.PokemonMoveData

class AttackMapper(private val languageId: Int) {


    //region Attacklist-Fragment
    fun mapData(allAttacksRaw: AttacksQuery.Data?): List<AttacksListData> {
        return mapToAttackList(allAttacksRaw?.moves)
    }

    private fun mapToAttackList(rawAttacksList: List<AttacksQuery.Move>?): List<AttacksListData> {
        val list = rawAttacksList?.map { move ->
            val defaultName = move.name
            AttacksListData(
                id = move.id,
                name = move.names.find { it.language_id == languageId }?.name ?: defaultName,
                accuracy = move.accuracy ?: 100,
                generationId = move.generation_id ?: 1,
                moveDamageClassId = move.move_damage_class_id ?: 1,
                moveTargetId = move.move_target_id ?: 1,
                power = move.power ?: 0,
                typeId = move.type_id ?: 0,
                machineInfos = createMachineInfo(move.machines), // call helperfunction to create machine infos
                priority = move.priority ?: 1,
                moveEffectId = move.move_effect_id,
                moveEffectChange = move.move_effect_chance,
                pp = move.pp ?: 0
            )
        }
        return list ?: emptyList()
    }

    private fun createMachineInfo(machinesList: List<AttacksQuery.Machine>): List<Triple<Int, Int, Int>>? {
        // machine (tm) infos
        if (machinesList.isEmpty()) return null

        val machineInfos = machinesList.map { machine ->
            Triple(
                machine.machine_number,
                machine.version_group_id ?: -1,
                machine.versionGroup?.generation_id ?: -1
            )
        }
        return machineInfos
    }

    //endregion

    //region AttackDetail Fragment
    fun mapClickedMoveData(
        data: AttackDetailsQuery.Data?,
        languageNames: List<LanguageNames>,
        versionNames: List<VersionNames>,
        pokemonTypeNames: List<PokemonTypeName>
    ): PokemonMoveData {

        val moveData = data?.move?.first()

        fun createAttackDescriptions(): List<AttackDescription> {
            // returns a list of AttackDescriptions in different gameVersions
            return data?.move?.firstOrNull()?.texts?.map { text ->
                val langName = languageNames.find { it.languageId == text.language_id }?.name ?: ""
                val gameVersionName =
                    versionNames.find { it.versionId == text.version_group_id && it.languageId == this.languageId }?.name
                        ?: "Unknown"
                AttackDescription(
                    languageId = text.language_id ?: 9,
                    text = text.flavor_text,
                    gameVersion = gameVersionName,
                    languageString = langName
                )
            } ?: emptyList()
        }
        // list of ids from pokemon who learn the move
        val idList = data?.pokemonList?.map { it.id } ?: emptyList()
        // pair of name to the specific languageId
        val names =
            data?.move?.firstOrNull()?.names?.map { Pair(it.name, it.language_id ?: -1) }
                ?: emptyList()
        // pair of typeName and typeId

        return PokemonMoveData(
            typeId = moveData?.type_id ?: 10001,
            accuracy = moveData?.accuracy,
            power = moveData?.power ?: 0,
            priority = moveData?.priority ?: 1,
            pp = moveData?.pp ?: 0,
            moveTargetId = moveData?.move_target_id ?: 1,
            moveDamageClassId = moveData?.move_damage_class_id ?: 1,
            names = names,
            attackDescriptions = createAttackDescriptions(),
            listOfPokemonIdsWhoLearn = idList,
            pokemonTypeNames = pokemonTypeNames,
            gameVersionList = versionNames,
            languageList = languageNames,
            generationId = moveData?.generation_id ?: 1
        )
    }

    //endregion

}