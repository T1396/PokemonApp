package com.example.pokinfo.data

import android.support.annotation.StringRes
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.pokeinfo.data.graphModel.AbilityDetailQuery
import com.example.pokeinfo.data.graphModel.AllAbilitiesQuery
import com.example.pokeinfo.data.graphModel.AttackDetailsQuery
import com.example.pokeinfo.data.graphModel.AttacksQuery
import com.example.pokeinfo.data.graphModel.FormQuery
import com.example.pokeinfo.data.graphModel.PokeListQuery
import com.example.pokeinfo.data.graphModel.PokemonDetail1Query
import com.example.pokeinfo.data.graphModel.PokemonDetail2Query
import com.example.pokinfo.R
import com.example.pokinfo.data.local.PokeDatabase
import com.example.pokinfo.data.mapper.TypeInfoForDatabase
import com.example.pokinfo.data.mapper.TypeInfoMapper
import com.example.pokinfo.data.models.database.pokemon.PkAbilityInfo
import com.example.pokinfo.data.models.database.pokemon.PkMove
import com.example.pokinfo.data.models.database.pokemon.PokemonData
import com.example.pokinfo.data.models.database.pokemon.PokemonForList
import com.example.pokinfo.data.models.database.pokemon.PokemonInsertStatus
import com.example.pokinfo.data.models.database.type.PokemonTypeName
import com.example.pokinfo.data.models.database.versionAndLanguageNames.LanguageNames
import com.example.pokinfo.data.models.database.versionAndLanguageNames.VersionNames
import com.example.pokinfo.data.remote.PokeApi
import com.example.pokinfo.data.util.UIState
import com.google.firebase.Timestamp
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

data class PokemonDataWrapper(
    val data1: PokemonDetail1Query.Data?,
    val data2: PokemonDetail2Query.Data?,
    val data3: FormQuery.Data?
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


    // region database getter functions
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

    suspend fun getPokemonTableEntries(list: List<Int>): List<PokemonForList> {
        return try {
            database.pokeDao.getSpecificPokemons(list)
        } catch (e: Exception) {
            Log.d("Repository", "Failed to load pokemons from the table_list_pokemon table", e)
            emptyList()
        }
    }

    // returns 3 different imageUrls of a pokemon in case there is no data in anyone
    suspend fun getPokemonImages(pokemonId: Int): Triple<String, String, String>? {
        return try {
            val pokeData = database.pokeDao.getPokemonBasicInfos(pokemonId)
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
            val pokeData = database.pokeDao.getPokemonBasicInfos(pokemonId)
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
    suspend fun getPokemonWhoLearnSpecificAttack(ids: List<Int>): List<PokemonForList> {
        return try {
            database.pokeDao.getPkmonListWhoLearnMove(ids)
        } catch (e: Exception) {
            Log.d(TAG, "Failed to load Pokemons who learn specific move", e)
            emptyList()
        }
    }

    //endregion


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




    fun checkIfPokemonNeedsToBeLoaded(pokemonId: Int): Boolean {
        val insertStatus =
            database.pokeDao.checkPokemonInsertStatus(pokemonId)
        if (insertStatus == null || insertStatus.status != "Done" ||
            (Timestamp.now().toDate().time - insertStatus.timestamp.time) > SIXTY_DAYS_IN_MILLIS
        ) {
            return true //
        }
        return false
    }

    // api functions
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


    suspend fun loadAllMoves(languageId: Int): AttacksQuery.Data? {
        return try {
            api.retrofitGraphService.sendAttackListQuery(languageId)
        } catch (e: Exception) {
            Log.d(TAG, "Failed to load list of all Attacks", e)
            null
        }
    }

    suspend fun loadSingleMoveDetails(moveId: Int, languageId: Int): AttackDetailsQuery.Data? {
        return try {
            api.retrofitGraphService.sendAttackDetailQuery(moveId, languageId)
        } catch (e: Exception) {
            Log.d(TAG, "Failed to load Attack details for moveId: $moveId", e)
            null
        }
    }

    suspend fun loadAllAbilities(languageId: Int): AllAbilitiesQuery.Data? {
        return try {
            return api.retrofitGraphService.sendAbilitiesListQuery(languageId)
        } catch (e: Exception) {
            Log.d(TAG, "Failed to load list of all abilities", e)
            null
        }
    }

    suspend fun loadAbilityDetails(abilityId: Int): AbilityDetailQuery.Data? {
        return try {
            api.retrofitGraphService.sendAbilityDetailQuery(abilityId)
        } catch (e: Exception) {
            Log.d(TAG, "Failed to load Abilitydetails for abilityId: $abilityId", e)
            null
        }
    }

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
    //endregion


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

    // inserts a list of all Pokemon with few details into Database
    suspend fun insertAllPokemon(list: List<PokemonForList>): Boolean {
        return try {
            database.pokeDao.insertAllPokemon(list)
            true
        } catch (e: Exception) {
            Log.d(TAG, "Failed to insert pokemonList into DB (initialization)", e)
            false
        }
    }

    // region type details
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
            jobs.forEach { it.await() }
            insertTypeInfoIntoDatabase(typeInfoList)

        } catch (e: Exception) {
            Log.d(TAG, "Failed to load type Details from API", e)
        }
    }

    private suspend fun insertTypeInfoIntoDatabase(typeInfoList: List<TypeInfoForDatabase>) {
        try {
            database.pokeTypeDao.insertCompleteTypeInfos(typeInfoList)
        } catch (e: Exception) {
            Log.d(TAG, "Failed to insert Type Infos into Database")
        }
    }
    //endregion
}


