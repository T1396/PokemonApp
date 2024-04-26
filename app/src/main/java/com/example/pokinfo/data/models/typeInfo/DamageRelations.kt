package com.example.pokinfo.data.models.typeInfo

data class DamageRelations(
    val double_damage_from: List<TypeName>,
    val double_damage_to: List<TypeName>,
    val half_damage_from: List<TypeName>,
    val half_damage_to: List<TypeName>,
    val no_damage_from: List<TypeName>,
    val no_damage_to: List<TypeName>
)