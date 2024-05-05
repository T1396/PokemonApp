package com.example.pokinfo.data.mapper

import com.example.pokeinfo.data.graphModel.AttacksQuery
import com.example.pokinfo.data.models.fragmentDataclasses.AttacksListData

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


    //endregion

}