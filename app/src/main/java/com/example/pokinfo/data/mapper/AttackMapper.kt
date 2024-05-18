package com.example.pokinfo.data.mapper

import com.example.pokeinfo.data.graphModel.AttacksQuery
import com.example.pokinfo.data.models.firebase.AttacksData

class AttackMapper(private val languageId: Int) {


    //region Attacklist-Fragment
    fun mapData(allAttacksRaw: AttacksQuery.Data?): List<AttacksData> {
        return mapToAttackList(allAttacksRaw?.moves)
    }

    private fun mapToAttackList(rawAttacksList: List<AttacksQuery.Move>?): List<AttacksData> {
        val list = rawAttacksList?.map { move ->
            val defaultName = move.name
            AttacksData(
                attackId = move.id,
                name = move.names.find { it.language_id == languageId }?.name ?: defaultName,
                accuracy = move.accuracy ?: 100,
                generationId = move.generation_id ?: 1,
                moveDamageClassId = move.move_damage_class_id ?: 1,
                typeId = move.type_id ?: 10001,
                power = move.power ?: 0,
                pp = move.pp ?: 0,
                effectText = move.pokemon_v2_moveeffect?.pokemon_v2_moveeffecteffecttexts?.firstOrNull()?.short_effect ?: "No effect Text found..."
            )
        }
        return list ?: emptyList()
    }

    private fun createMachineInfo(machinesList: List<AttacksQuery.Machine>): List<Triple<Int, Int, Int>>? {
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


    //endregion

}