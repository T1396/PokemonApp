package com.example.pokinfo.data.models.fragmentDataclasses

data class TeamBuilderData(
    val ivList: List<Int>,
    val ivNames: List<String>,
    val evNames: List<String>,
    val evList: List<Int>,
    val gender: Int,
    val level: Int,
    val selectedAbility: Pair<String, String>
)