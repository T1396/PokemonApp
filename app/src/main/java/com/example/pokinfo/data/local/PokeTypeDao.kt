package com.example.pokinfo.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.pokinfo.data.mapper.TypeInfoForDatabase
import com.example.pokinfo.data.models.database.type.DamageRelation
import com.example.pokinfo.data.models.database.type.GameIndex
import com.example.pokinfo.data.models.database.type.MoveWithType
import com.example.pokinfo.data.models.database.type.PokemonType
import com.example.pokinfo.data.models.database.type.PokemonTypeName
import com.example.pokinfo.data.models.database.type.PokemonWithThisType

@Dao
interface PokemonTypeDao {

    @Transaction
    suspend fun insertCompleteTypeInfos(typeInfos: List<TypeInfoForDatabase>) {
        typeInfos.forEach { typeInfo ->
            insert(typeInfo.pokemonType)
            insertRelation(typeInfo.damageRelation)
            insertMovesByType(typeInfo.movesWithType)
            insertTypeNames(typeInfo.pokemonTypeNames)
            insertAllPokemonWithType(typeInfo.pokemonWithType)
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pokemonType: PokemonType)

    @Query("SELECT * from pokemon_types_type_names where languageId =:languageId")
    suspend fun getTypeNames(languageId: Int): List<PokemonTypeName>

    @Query("SELECT id FROM pokemon_types where name = :typeName")
    suspend fun getTypeId(typeName: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovesByType(movesByType: List<MoveWithType>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelation(damageRelation: DamageRelation)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGameIndices(gameIndexes: List<GameIndex>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTypeNames(pokemonTypeNames: List<PokemonTypeName>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPokemonWithType(pokemonWithType: List<PokemonWithThisType>)

}
