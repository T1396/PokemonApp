package com.example.pokinfo.data.local

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.pokinfo.data.models.database.pokemon.PokemonForList
import com.example.pokinfo.data.models.database.pokemon.MoveInformation
import com.example.pokinfo.data.models.database.pokemon.LanguageNames
import com.example.pokinfo.data.models.database.pokemon.PkAbilityEffectText
import com.example.pokinfo.data.models.database.pokemon.PkAbilityFlavorText
import com.example.pokinfo.data.models.database.pokemon.PkAbilityInfo
import com.example.pokinfo.data.models.database.pokemon.PkAbilityName
import com.example.pokinfo.data.models.database.pokemon.PkMove
import com.example.pokinfo.data.models.database.pokemon.PkMoveNames
import com.example.pokinfo.data.models.database.pokemon.PkMoves
import com.example.pokinfo.data.models.database.pokemon.PkNames
import com.example.pokinfo.data.models.database.pokemon.PkSpecieInfo
import com.example.pokinfo.data.models.database.pokemon.Pokemon
import com.example.pokinfo.data.models.database.pokemon.PkAbilitiesToJoin
import com.example.pokinfo.data.models.database.pokemon.PkForms
import com.example.pokinfo.data.models.database.pokemon.PkMoveMachines
import com.example.pokinfo.data.models.database.pokemon.PkMoveVersionGroupDetail
import com.example.pokinfo.data.models.database.pokemon.PokemonData
import com.example.pokinfo.data.models.database.pokemon.PokemonDexEntries
import com.example.pokinfo.data.models.database.pokemon.PkEvolutionChain
import com.example.pokinfo.data.models.database.pokemon.PkEvolutionDetails
import com.example.pokinfo.data.models.database.pokemon.PokemonAbilitiesList
import com.example.pokinfo.data.models.database.pokemon.PokemonInsertStatus
import com.example.pokinfo.data.models.database.pokemon.VersionNames

@Dao
interface PokeDatabaseDao {

    @Transaction
    suspend fun insertAllPokemonData(pokemonData: PokemonData) {
        insertPokemonData(pokemonData.pokemon)
        insertSpecyInfo(pokemonData.specyData)
        insertAbilityInfo(pokemonData.abilityInfoList)
        insertMoves(pokemonData.moves)

        insertFormInfos(pokemonData.formData)
        insertPokedexEntries(pokemonData.pokedexEntries)
        insertPokemonMoves(pokemonData.pokemonMoves)

        insertMoveNames(pokemonData.moveNames)
        insertVersionDetail(pokemonData.versionGroupDetails)
        insertSpecyNames(pokemonData.specyNames)
        insertMoveMachines(pokemonData.moveMachines)

        insertAbilitiesJoin(pokemonData.abilitiesToJoin)
        insertAbilityNames(pokemonData.abilityNames)
        insertAbilityFlavorTexts(pokemonData.abilityFlavorTexts)
        insertAbilityEffectTexts(pokemonData.abilityEffectTexts)

        if (pokemonData.evolutionChain != null && pokemonData.evolutionDetails != null ) {
            val existingChain = getEvolutionChainById(pokemonData.evolutionChain.id)

            if (existingChain == null) {
                insertEvolutionChain(pokemonData.evolutionChain)
                insertEvolutionDetails(pokemonData.evolutionDetails)
            }

        }
        pokemonData.abilitiesPokemonList?.let {
            insertAbilityPokemonList(it)
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAbilityPokemonList(list: List<PokemonAbilitiesList>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvolutionChain(evolutionChain: PkEvolutionChain)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvolutionDetails(evolutionSpecies: List<PkEvolutionDetails>)

    @Query("SELECT * FROM pokemon_evolution_chain WHERE id = :chainId")
    suspend fun getEvolutionChainById(chainId: Int): PkEvolutionChain?

    @Query("SELECT * FROM pokemon_evolutions WHERE evolutionChainId =:chainId")
    suspend fun getEvolutionDetails(chainId: Int): List<PkEvolutionDetails>



    suspend fun getInfosForOnePokemon(pokemonId: Int, languageId: Int): PokemonData {
        val mainData = getPokemonData(pokemonId)
        val specyInfo = getSpecyInfo(mainData.specieId)
        val pokeNames = getSpecyNames(mainData.specieId)
        val formInfos = getPokemonForms(mainData.specieId, pokemonId)
        val abilitiesToJoins = getAbilitieIds(pokemonId)
        val abilityInfo = abilitiesToJoins.map {
            getAbility(it.abilityId)
        }
        val abilityNames = abilitiesToJoins.map {
            getAbilityName(it.abilityId, languageId)
        }
        val flavorTexts = abilitiesToJoins.flatMap {
            getAbilityFlavorTexts(it.abilityId)
        }
        val effectTexts = abilitiesToJoins.map {
            getAbilityEffectText(it.abilityId)
        }
        val pokedexEntries = getPokedexEntries(specyInfo.id)

        val abilitiesPokemonList = abilitiesToJoins.map { join ->
            getPokemonWithAbility(join.abilityId)
        }

        val moveInfo = getMoveInfosForAPokemon(pokemonId, languageId)

        val evoChain = getEvolutionChainById(specyInfo.evolutionChainId ?: -1)
        val evoDetails = if (evoChain != null) getEvolutionDetails(evoChain.id) else null

        return PokemonData(
            pokemon = mainData,
            abilityInfoList = abilityInfo,
            abilityNames = abilityNames,
            abilitiesToJoin = abilitiesToJoins,
            specyData = specyInfo,
            pokemonMoves = moveInfo.pokemonMoves,
            pokedexEntries = pokedexEntries,
            moves = moveInfo.moveInfosGeneral,
            moveNames = moveInfo.moveNames,
            moveMachines = moveInfo.moveMachines,
            specyNames = pokeNames,
            abilityFlavorTexts = flavorTexts,
            abilityEffectTexts = effectTexts,
            versionGroupDetails = moveInfo.moveVersionDetails,
            formData = formInfos,
            evolutionChain = evoChain,
            evolutionDetails = evoDetails,
            abilitiesPokemonList = abilitiesPokemonList
        )

    }

    suspend fun getMoveInfosForAPokemon(pokemonId: Int, languageId: Int): MoveInformation {
        val pokemonMoveInfos = getPokemonMoveInfos(pokemonId) // attacks a pokemon can learn
        val moveInfos = pokemonMoveInfos.moveIds.map {
            getMoveData(it.toInt())
        }
        val moveNames = pokemonMoveInfos.moveIds.map {
            val moveName = getMoveName(it.toInt(), languageId)
            moveName
        }
        val moveMachines = pokemonMoveInfos.moveIds.flatMap {
            getMoveMachines(it.toInt())
        }
        val versionMoveDetails = getPokemonMoveVGDs(pokemonId)
        return MoveInformation(
            pokemonMoves = pokemonMoveInfos,
            moveMachines = moveMachines,
            moveInfosGeneral = moveInfos,
            moveNames = moveNames,
            moveVersionDetails = versionMoveDetails,

        )
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAbilityEffectTexts(texts: List<PkAbilityEffectText>)

    @Query("SELECT * FROM pokemon_pokemon_abilities WHERE abilityId =:abilityId")
    suspend fun getPokemonWithAbility(abilityId: Int): PokemonAbilitiesList

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAbilityInfo(info: List<PkAbilityInfo>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAbilityNames(names: List<PkAbilityName>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPokemon(pokemonList: List<PokemonForList>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAbilityFlavorTexts(texts: List<PkAbilityFlavorText>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAbilitiesJoin(list: List<PkAbilitiesToJoin>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFormInfos(list: List<PkForms>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLanguageNames(names: List<LanguageNames>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoves(infos: List<PkMove>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoveMachines(moveMachines: List<PkMoveMachines>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoveNames(moveNames: List<PkMoveNames>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPokedexEntries(pokedexEntries: List<PokemonDexEntries>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPokemon(pokemonSimple: PokemonForList): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPokemonData(pokemon: Pokemon)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPokemonMoves(moves: PkMoves)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpecyInfo(info: PkSpecieInfo)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpecyNames(names: List<PkNames>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVersionDetail(versionDetail: List<PkMoveVersionGroupDetail>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVersionNames(names: List<VersionNames>)

    @Query("SELECT * from pokemon_ability_names WHERE abilityId =:abilityId AND languageId =:languageId")
    suspend fun getAbilityName(abilityId: Int, languageId: Int): PkAbilityName

    @Query("SELECT * FROM pokemon_language_names")
    suspend fun getLanguageNames(): List<LanguageNames>

    @Query("SELECT * FROM table_list_pokemon ORDER BY speciesId, isDefault DESC")
    fun getPokemonList(): LiveData<List<PokemonForList>>

    @Query("SELECT * FROM table_list_pokemon WHERE id IN (:list)")
    suspend fun getSpecificPokemon(list: List<Int>) : List<PokemonForList>

    @Query("SELECT * FROM table_list_pokemon WHERE id =:id")
    suspend fun getPokemonBasicInfo(id: Int): PokemonForList

    @Query("SELECT * FROM table_list_pokemon LIMIT 1025")
    fun getEveryPokemon(): LiveData<List<PokemonForList>>

    @Query("SELECT * FROM table_list_pokemon WHERE id IN (:ids)")
    suspend fun getPokemonListFromIdList(ids: List<Int>) : List<PokemonForList>

    @Query("SELECT * FROM pokemon WHERE id =:pokemonId")
    suspend fun getPokemonData(pokemonId: Int): Pokemon

    @Query("SELECT * FROM pokemon_ability_effect_texts WHERE abilityId =:abilityId")
    suspend fun getAbilityEffectText(abilityId: Int): PkAbilityEffectText

    @Query("SELECT * FROM pokemon_ability_flavor_texts WHERE abilityId =:abilityId")
    suspend fun getAbilityFlavorTexts(abilityId: Int): List<PkAbilityFlavorText>
    @Query("SELECT * FROM pokemon_abilities_join WHERE pokemonId =:pokemonId")
    suspend fun getAbilitieIds(pokemonId: Int): List<PkAbilitiesToJoin>

    @Query("SELECT * from pokemon_abilities WHERE id =:abilityId")
    suspend fun getAbility(abilityId: Int): PkAbilityInfo

    @Query("SELECT * FROM pokemon_move_data")
    suspend fun getAllMoves(): List<PkMove>

    @Query("SELECT id FROM pokemon_move_data WHERE name =:name")
    suspend fun getMoveId(name: String?): Int

    @Query("SELECT * FROM pokemon_forms where speciesId =:speciesId AND pokemonId !=:pokemonId")
    fun getPokemonForms(speciesId: Int, pokemonId: Int): List<PkForms>

    @Query("SELECT * FROM pokemon_move_version_group_details WHERE pokemonId =:pokemonId")
    suspend fun getPokemonMoveVGDs(pokemonId: Int): List<PkMoveVersionGroupDetail>

    @Query("SELECT * FROM pokemon_specy WHERE id =:pokemonId")
    suspend fun getSpecyInfo(pokemonId: Int): PkSpecieInfo

    @Query("SELECT * FROM pokemon_specy_names WHERE speciesId =:specyId AND languageId <= 10")
    suspend fun getSpecyNames(specyId: Int): List<PkNames>

    @Query("SELECT * FROM pokemon_move_data WHERE id =:moveId")
    suspend fun getMoveData(moveId: Int): PkMove

    @Query("SELECT * from pokemon_moves_list WHERE pokemonId =:pokemonId")
    suspend fun getPokemonMoveInfos(pokemonId: Int): PkMoves

    @Query("SELECT * FROM pokemon_move_machines WHERE moveId =:moveId")
    suspend fun getMoveMachines(moveId: Int): List<PkMoveMachines>

    @Query("SELECT * FROM pokemon_version_names WHERE languageId =:languageId")
    suspend fun getVersionNames(languageId: Int): List<VersionNames>

    @Query("SELECT * FROM pokemon_pokedex_entries WHERE speciesId =:speciesId")
    suspend fun getPokedexEntries(speciesId: Int): List<PokemonDexEntries>

    @Query("SELECT * FROM pokemon_move_names where moveId =:moveId AND languageId =:languageId")
    suspend fun getMoveName(moveId: Int, languageId: Int): PkMoveNames

    @Query("SELECT * FROM pokemon_ability_names")
    fun getAllAbilityNames(): List<PkAbilityName>

    @Query("SELECT * FROM pokemon_abilities")
    suspend fun getAllAbilities(): List<PkAbilityInfo>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun setPokemonInsertStatus(pokemonInsertStatus: PokemonInsertStatus)

    @Query("SELECT * from pokemon_insert_status where pokemonId =:pokemonId")
    fun checkPokemonInsertStatus(pokemonId: Int): PokemonInsertStatus?

}