package com.example.pokinfo.data.models.fragmentDataclasses

import com.example.pokinfo.data.models.firebase.EvIvData

data class TeamBuilderData(
    val ivList: List<EvIvData>,
    val evList: List<EvIvData>,
    val gender: Int,
    val level: Int,
)