package com.example.pokinfo.data.mapper

import com.example.pokinfo.data.maps.languageMap
import com.example.pokinfo.data.models.database.type.DamageRelation
import com.example.pokinfo.data.models.database.type.MoveWithType
import com.example.pokinfo.data.models.database.type.PokemonType
import com.example.pokinfo.data.models.database.type.PokemonWithThisType
import com.example.pokinfo.data.models.database.type.PokemonTypeName
import com.example.pokinfo.data.models.typeInfo.Type

/** Helper Class to map Type Information from the pokeapi to dataclasses to save in the database */
class TypeInfoMapper {
    fun mapTypeInfo(typeInfo: Type): TypeInfoForDatabase {
        // the root data for the type
        val dbType = PokemonType(
            id = typeInfo.id,
            name = typeInfo.name,
        )
        // every pokemon attack with this type
        val movesWithTypeList = typeInfo.moves.map {
            MoveWithType(
                name = it.name,
                url = it.url,
                typeId = typeInfo.id
            )
        }
        // how the type related to other types
        val damageRelationDB = DamageRelation(
            typeId = typeInfo.id,
            doubleDamageFrom = typeInfo.damage_relations.double_damage_from.map { it.name },
            doubleDamageTo = typeInfo.damage_relations.double_damage_to.map { it.name },
            halfDamageFrom = typeInfo.damage_relations.half_damage_from.map { it.name },
            halfDamageTo = typeInfo.damage_relations.half_damage_to.map { it.name },
            noDamageFrom = typeInfo.damage_relations.no_damage_from.map { it.name },
            noDamageTo = typeInfo.damage_relations.no_damage_to.map { it.name },
        )
        // names in languages
        val pokemonTypeNames = typeInfo.names.map { element ->
            val languageId = languageMap.entries
                .find {
                    it.value == element.language.name}?.key ?: -1
                PokemonTypeName(
                    name = element.name,
                    typeId = typeInfo.id,
                    languageId = languageId,
                    languageName = element.language.name
                )

        }
        // pokemons with that type
        val pokemonsWithTypeList = typeInfo.pokemon.map {
            PokemonWithThisType(
                pokemonName = it.pokemon.name,
                slot = it.slot,
                typeId = typeInfo.id

            )
        }
        return TypeInfoForDatabase(
            pokemonType = dbType,
            movesWithType = movesWithTypeList,
            damageRelation = damageRelationDB,
            pokemonTypeNames = pokemonTypeNames,
            pokemonWithType = pokemonsWithTypeList
        )
    }
}

data class TypeInfoForDatabase(
    val pokemonType: PokemonType,
    val movesWithType: List<MoveWithType>,
    val damageRelation: DamageRelation,
    val pokemonTypeNames: List<PokemonTypeName>,
    val pokemonWithType: List<PokemonWithThisType>,
)