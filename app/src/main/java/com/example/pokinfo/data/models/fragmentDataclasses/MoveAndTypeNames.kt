package com.example.pokinfo.data.models.fragmentDataclasses


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