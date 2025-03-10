package com.example.pokinfo.data

import android.support.annotation.StringRes
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.pokeinfo.data.graphModel.AbilityDetailQuery
import com.example.pokeinfo.data.graphModel.AllAbilitiesQuery
import com.example.pokeinfo.data.graphModel.AttackDetailsQuery
import com.example.pokeinfo.data.graphModel.AttacksQuery
import com.example.pokeinfo.data.graphModel.PokeListQuery
import com.example.pokeinfo.data.graphModel.PokemonDetail1Query
import com.example.pokeinfo.data.graphModel.PokemonDetail2Query
import com.example.pokeinfo.data.graphModel.PokemonDetail3Query
import com.example.pokeinfo.data.graphModel.PokemonWithAbilityQuery
import com.example.pokinfo.R
import com.example.pokinfo.data.local.PokeDatabase
import com.example.pokinfo.data.mapper.TypeInfoForDatabase
import com.example.pokinfo.data.mapper.TypeInfoMapper
import com.example.pokinfo.data.models.database.pokemon.PkAbilityInfo
import com.example.pokinfo.data.models.database.pokemon.PkMove
import com.example.pokinfo.data.models.database.pokemon.PokemonData
import com.example.pokinfo.data.models.database.pokemon.PokemonForList
import com.example.pokinfo.data.models.database.pokemon.PokemonInsertStatus
import com.example.pokinfo.data.models.database.pokemon.PokemonTypeName
import com.example.pokinfo.data.models.database.pokemon.LanguageNames
import com.example.pokinfo.data.models.database.pokemon.VersionNames
import com.example.pokinfo.data.remote.PokeApi
import com.example.pokinfo.data.util.UIState
import com.google.firebase.Timestamp
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

data class PokemonDataWrapper(
    val data1: PokemonDetail1Query.Data?,
    val data2: PokemonDetail2Query.Data?,
    val data3: PokemonDetail3Query.Data?
)

private const val SIXTY_DAYS_IN_MILLIS = 5184000000
private const val TAG = "REPOSITORY"

class Repository(private val api: PokeApi, private val database: PokeDatabase) {

    // home - list all pokemon
    private var _pokemonList = database.pokeDao.getPokemonList()
    val pokemonList: LiveData<List<PokemonForList>>
        get() = _pokemonList

    suspend fun getVersionNames (languageId: Int): List<VersionNames> {
        return database.pokeDao.getVersionNames(languageId)
    }

    suspend fun getLanguageNames(): List<LanguageNames> {
        return database.pokeDao.getLanguageNames()
    }

    suspend fun getPokemonTypeNames(languageId: Int): List<PokemonTypeName> {
        return database.pokeTypeDao.getTypeNames(languageId)
    }

    // home-detail
    private var _clickedPokemon = MutableLiveData<UIState<PokemonData>>()
    val clickedPokemon: LiveData<UIState<PokemonData>>
        get() = _clickedPokemon

    // all pokemon for teambuilder + forms of pokemon
    private val _teambuilderPokeList = database.pokeDao.getEveryPokemon()
    val teambuilderPokeList: LiveData<List<PokemonForList>>
        get() = _teambuilderPokeList


    //region database getter functions

    // returns all moves actually existing in the database to prevent adding them again or duplicated
    suspend fun getAllMovesFromDB(): List<PkMove> {
        return try {
            database.pokeDao.getAllMoves()
        } catch (e: Exception) {
            Log.d(TAG, "Failed to load all moves from Database", e)
            emptyList()
        }
    }

    // returns all abilities actually existing in the database to prevent adding them again or duplicated
    suspend fun getAbilitiesFromDB(): List<PkAbilityInfo> {
        return try {
            database.pokeDao.getAllAbilities()
        } catch (e: Exception) {
            Log.d(TAG, "Failed to load all Abilities from DB", e)
            emptyList()
        }
    }

    /** Returns a list of all pokemon matching with the ids in the given in list
     *  @param idList list of ids of the pokemon to fetch
     *  @return list of the pokemon to display in a list
     * */
    suspend fun getPokemonTableEntries(idList: List<Int>): List<PokemonForList> {
        return try {
            database.pokeDao.getSpecificPokemon(idList)
        } catch (e: Exception) {
            Log.d(TAG, "Failed to load pokemons from the table_list_pokemon table", e)
            emptyList()
        }
    }

    // returns 3 different imageUrls of a pokemon in case there is no data in anyone
    suspend fun getPokemonImages(pokemonId: Int): Triple<String, String, String>? {
        return try {
            val pokeData = database.pokeDao.getPokemonBasicInfo(pokemonId)
            val image1 = pokeData.imageUrl
            val image2 = pokeData.altImageUrl
            val image3 = pokeData.officialImageUrl
            Triple(image1, image2, image3)
        } catch (e: Exception) {
            Log.d(TAG, "Failed to load Pokemon Images for pokemonId: $pokemonId", e)
            null
        }
    }

    /** Returns a list of OriginName to Url Pairs of one Pokemon*/
    suspend fun getPokemonImagesWithNameRes(pokemonId: Int): List<Pair<Int, String>> {
        return try {
            val pokeData = database.pokeDao.getPokemonBasicInfo(pokemonId)
            listOf(
                Pair(R.string.pokemon_home, pokeData.imageUrl),
                Pair(R.string.default_sprites, pokeData.altImageUrl),
                Pair(R.string.official_artwork, pokeData.officialImageUrl)
            )
        } catch (e: Exception) {
            Log.d(
                TAG,
                "Failed to load Pokemon Images with name Ressources for pokemonId: $pokemonId",
                e
            )
            emptyList()
        }
    }

    // gets a list of all pokemon who learns a specific attack (for attacksdetailfragment)
    suspend fun getPokemonListFromIdList(ids: List<Int>): List<PokemonForList> {
        return try {
            database.pokeDao.getPokemonListFromIdList(ids)
        } catch (e: Exception) {
            Log.d(TAG, "Failed to load Pokemons who learn specific move", e)
            emptyList()
        }
    }

    /** Gets details of a pokemon
     *  @param pokemonId id of the pokemon
     *  @param languageId to return results in a specific language
     * */
    suspend fun getPokemonDataFromDatabase(
        pokemonId: Int,
        languageId: Int,
        @StringRes errorMessageRes: Int,
    ): UIState<PokemonData>{
        return try {
            val pokemonData = database.pokeDao.getInfosForOnePokemon(pokemonId, languageId)
            UIState.Success(pokemonData)

        } catch (e: Exception) {
            Log.d(TAG, "Failed to fetch pokemon data from Database", e)
            UIState.Error(e, errorMessageRes)
        }

    }
    //endregion


    /** Checks if a database entry of a pokemon exists
     *  to reduce api calls
     * */
    fun checkIfPokemonNeedsToBeLoaded(pokemonId: Int): Boolean {
        val insertStatus =
            database.pokeDao.checkPokemonInsertStatus(pokemonId)
        //
        return insertStatus == null || insertStatus.status != "Done"
    }


    /** Determines if the details of a pokemon species is already inserted into the database
     *  to prevent multiple insertions of that */
    suspend fun doesSpeciesExistAlready(speciesId: Int): Boolean {
        return try {
            database.pokeDao.existsSpecies(speciesId)
        } catch (e: Exception) {
            Log.d(TAG, "Failed to check if species exists already in database", e)
            false
        }
    }

    //region api functions
    /** Sends 3 queries to get details for a single pokemon like learned attacks, pokedex entries etc.
     *  @param pokemonId id of the pokemon
     *  @param languageId to get some details in a specific language
     * */
    suspend fun loadSinglePokemonData(
        pokemonId: Int,
        languageId: Int,
    ): PokemonDataWrapper? {
        return try {
            val basicData = api.retrofitGraphService.firstPokemonQuery(pokemonId, languageId)
            val extendData = api.retrofitGraphService.secondPokemonQuery(pokemonId, languageId)
            val speciesId = basicData?.pokemon?.firstOrNull()?.specy?.id ?: -1
            val lastData = api.retrofitGraphService.thirdPokemonQuery(speciesId, languageId)
            PokemonDataWrapper(basicData, extendData, lastData)

        } catch (e: Exception) {
            Log.d(TAG, "Failed to load single Pokemon Data for ID: $pokemonId", e)
            null
        }
    }

    /** Sends a query to get all available pokemon moves
     *  @param languageId to get results in a specific language
     * */
    suspend fun loadAllMoves(languageId: Int): AttacksQuery.Data? {
        return try {
            api.retrofitGraphService.sendAttackListQuery(languageId)
        } catch (e: Exception) {
            Log.d(TAG, "Failed to load list of all Attacks", e)
            null
        }
    }

    /** Sends a query to graphql to get details of a single pokemon move (attack)
     *  @param moveId id of the move to get details for
     *  @param languageId to get results in a specific language
     * */
    suspend fun loadSingleMoveDetails(moveId: Int, languageId: Int): AttackDetailsQuery.Data? {
        return try {
            api.retrofitGraphService.sendAttackDetailQuery(moveId, languageId)
        } catch (e: Exception) {
            Log.d(TAG, "Failed to load Attack details for moveId: $moveId", e)
            null
        }
    }

    /** Gets a list of every available ability for all pokemon
     * @param languageId to get results in a specific language
     * */
    suspend fun loadAllAbilities(languageId: Int): AllAbilitiesQuery.Data? {
        return try {
            return api.retrofitGraphService.sendAbilitiesListQuery(languageId)
        } catch (e: Exception) {
            Log.d(TAG, "Failed to load list of all abilities", e)
            null
        }
    }

    /** Sends a query to graphql to get details of a single Ability
     * @param abilityId id of the ability to get details for
     * */
    suspend fun loadAbilityDetails(abilityId: Int): AbilityDetailQuery.Data? {
        return try {
            api.retrofitGraphService.sendAbilityDetailQuery(abilityId)
        } catch (e: Exception) {
            Log.d(TAG, "Failed to load Abilitydetails for abilityId: $abilityId", e)
            null
        }
    }

    /** Loads the available language and Version Names from graphQL via query
     *  And inserts them into database
     *  @param languageId to eventually get results in other languages
     * */
    suspend fun loadLanguageAndVersionNames(languageId: Int) {
        try {
            val response = api.retrofitGraphService.sendLanguageVersionNameQuery(languageId)
            val data = response?.languageNames?.map {
                LanguageNames(
                    id = it.id,
                    name = it.name,
                    languageId = it.language_id ?: -1
                )
            } ?: emptyList()
            database.pokeDao.insertLanguageNames(data)
            val versionNames = response?.pokemon_v2_versionname?.map {
                VersionNames(
                    id = it.id.toLong(),
                    versionId = it.version_id ?: -1,
                    name = it.name,
                    languageId = it.language_id ?: -1
                )
            } ?: emptyList()
            database.pokeDao.insertVersionNames(versionNames)
        } catch (e: Exception) {
            Log.d(TAG, "Failed to load language/Version Names with apolloClient", e)
        }
    }

    /** makes an api call to get a list of all pokemons with sprites for it and returns it to the viewmodel to map it */
    suspend fun loadAllPokemonsWithSprites(languageId: Int): PokeListQuery.Data? {
        return try {
            val pokeList = api.retrofitGraphService.sendPokeListQuery(languageId)
            pokeList
        } catch (e: Exception) {
            Log.d(TAG, "Failed to load list of every Pokemon to initialize App", e)
            null
        }
    }

    /** Returns a list of all Pokemon which can hold the ability corresponsing to abilityId
     *  @param abilityId id of the ability which the pokemon must have
     * */
    suspend fun getPokemonListOfAbility(abilityId: Int): PokemonWithAbilityQuery.Data? {
        return try {
            api.retrofitGraphService.sendAbilityPokemonListQuery(abilityId)
        } catch (e: Exception) {
            Log.d(TAG, "Failed to load pokemonList for a ability (pokemons who has that ability", e)
            null
        }
    }

    /** Loads the details (names, typeIds, strengthens, weaknesses) of all Pokemon Types and inserts in into the database
     * */
    suspend fun loadTypeDetails() = coroutineScope {
        try {
            val jobs = mutableListOf<Deferred<Boolean>>()
            val semaphore = Semaphore(5)
            val typeInfoMapper = TypeInfoMapper()
            val typeInfoList = mutableListOf<TypeInfoForDatabase>()
            // 18 types actually
            (1..18).forEach { index ->
                val job = async {
                    semaphore.withPermit {
                        val typeInfoResponse = api.retrofitService.getTypeInfos(index)
                        val typeInfo = typeInfoMapper.mapTypeInfo(typeInfoResponse)
                        typeInfoList.add(typeInfo)
                    }
                }
                jobs.add(job)
            }
            // wait for every job to finish
            jobs.awaitAll()
            insertTypeInfoIntoDatabase(typeInfoList)

        } catch (e: Exception) {
            Log.d(TAG, "Failed to load Pokemon type Details from API", e)
        }
    }
    //endregion

    //region database insertions

    /** Inserts a list of pokemon into the database and returns true when succeeded else false
     *  @param pokemonList list of the pokemon - data
     *  */
    suspend fun insertAllPokemon(pokemonList: List<PokemonForList>): Boolean {
        return try {
            database.pokeDao.insertAllPokemon(pokemonList)
            true
        } catch (e: Exception) {
            Log.d(TAG, "Failed to insert pokemonList into DB (initialization)", e)
            false
        }
    }

    /** Inserts the pokemon type infos into the database */
    private suspend fun insertTypeInfoIntoDatabase(typeInfoList: List<TypeInfoForDatabase>) {
        try {
            database.pokeTypeDao.insertCompleteTypeInfo(typeInfoList)
        } catch (e: Exception) {
            Log.d(TAG, "Failed to insert Type Info into Database")
        }
    }

    suspend fun insertPokemonDataIntoDB(
        pokemonData: PokemonData,
        onSaveFinish: (Boolean) -> Unit
    ) {
        try {
            database.pokeDao.insertAllPokemonData(pokemonData)
            val insertStatus = PokemonInsertStatus(
                pokemonId = pokemonData.pokemon.id,
                status = "Done"
            )
            database.pokeDao.setPokemonInsertStatus(insertStatus)
            onSaveFinish(true)
        } catch (e: Exception) {
            Log.d(TAG, "Failed to insert Pokemon Data into Database", e)
            onSaveFinish(false)
        }
    }
    //endregion
}


