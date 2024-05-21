package com.example.pokinfo.viewModels

import android.app.Application
import android.support.annotation.StringRes
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.pokeinfo.data.graphModel.PokeListQuery
import com.example.pokeinfo.data.graphModel.PokemonDetail1Query
import com.example.pokinfo.R
import com.example.pokinfo.adapter.home.detail.AbilityEffectText
import com.example.pokinfo.data.RepositoryProvider
import com.example.pokinfo.data.mapper.PokemonDatabaseMapper
import com.example.pokinfo.data.mapper.PokemonListMapper
import com.example.pokinfo.data.maps.imageTypeName
import com.example.pokinfo.data.maps.readableNames
import com.example.pokinfo.data.maps.typeColorMap
import com.example.pokinfo.data.maps.versionGroupMap
import com.example.pokinfo.data.models.database.pokemon.PokemonData
import com.example.pokinfo.data.models.database.pokemon.PokemonDexEntries
import com.example.pokinfo.data.models.database.pokemon.PokemonForList
import com.example.pokinfo.data.models.database.pokemon.LanguageNames
import com.example.pokinfo.data.models.database.pokemon.VersionNames
import com.example.pokinfo.data.models.firebase.AttacksData
import com.example.pokinfo.data.enums.PokemonSortFilter
import com.example.pokinfo.data.enums.PokemonSortFilterState
import com.example.pokinfo.data.models.database.pokemon.EvolutionStage
import com.example.pokinfo.data.models.database.pokemon.PkEvolutionDetails
import com.example.pokinfo.data.models.database.pokemon.PokemonTypeName
import com.example.pokinfo.data.util.UIState
import com.example.pokinfo.data.util.sharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PokeViewModel(application: Application, private val sharedViewModel: SharedViewModel) :
    AndroidViewModel(application) {

    private val repository = RepositoryProvider.provideRepository(application)

    private var isInitialized by application.sharedPreferences("isInitialized", false)

    private var languageId by application.sharedPreferences("languageId", 9)

    var versionNames: List<VersionNames> = emptyList()
        private set
    var languageNames: List<LanguageNames> = emptyList()
        private set

    var pokemonTypeNames: List<PokemonTypeName> = emptyList()
        private set

    private var _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    init {
        loadGenericData()
    }

    private fun loadGenericData(callback: (() -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            versionNames = repository.getVersionNames(languageId)
            languageNames = repository.getLanguageNames()
            pokemonTypeNames = repository.getPokemonTypeNames(languageId)
            callback?.invoke()
        }
    }

    fun getLangId(): Int {
        return languageId
    }


    /** Gets some important Data, List of all Pokemon, Language Names and Type Details
     * should be loaded just for the first time a user starts the app */
    fun initializeDataForApp() {

        fun showSnackBarWithRetryAction() {
            sharedViewModel.postMessage(R.string.error_fetching_data) {
                initializeDataForApp()
            }
            _isLoading.postValue(false)
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.postValue(true)
            sharedViewModel.postMessage(R.string.fetching_data)

            // wait once all data has been loaded
            val loadResults = listOf(
                async { loadTypeDetails() },
                async { loadLanguageAndVersionNames() },
                async { repository.loadAllPokemonsWithSprites(languageId) },
            ).awaitAll()



            if (loadResults.any { it == null }) {
                showSnackBarWithRetryAction()
                return@launch
            }

            loadGenericData()

            val pokemonList = PokemonListMapper()
                .mapData(loadResults[2] as PokeListQuery.Data, languageId)

            repository.insertAllPokemon(pokemonList).also { success ->
                isInitialized = success
                _isLoading.postValue(false)
                if (!success) showSnackBarWithRetryAction()
            }
        }
    }

    /** Gets some important Data, List of all Pokemon, Language Names and Type Details
     * should be loaded just for the first time a user starts the app */
    fun dadsa() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.postValue(true)
            sharedViewModel.postMessage(R.string.fetching_data)

            val data = repository.loadAllPokemonsWithSprites(languageId)
            if (data == null) {
                sharedViewModel.postMessage(R.string.error_fetching_data) {
                    initializeDataForApp()
                }
                _isLoading.postValue(false)
            } else {
                val mappingJob = async {
                    val dataMapper = PokemonListMapper()
                    dataMapper.mapData(data, languageId)
                }
                val typeDetails = async { loadTypeDetails() }
                val languageNames = async { loadLanguageAndVersionNames() }
                typeDetails.await()
                languageNames.await()
                val pokemonList = mappingJob.await()
                loadGenericData {
                    // once all of them finished insert the list into database
                    viewModelScope.launch(Dispatchers.IO) {
                        val isDone = repository.insertAllPokemon(pokemonList)
                        if (isDone) {
                            isInitialized =
                                true // will be saved into shared Prefs through property delegation

                        } else {
                            // when something failed while inserting pokemon to database
                            sharedViewModel.postMessage(R.string.error_fetching_data) {
                                initializeDataForApp()
                            }
                        }
                        _isLoading.postValue(false)
                    }
                }
            }
        }
    }

    /** loads for a specific pokemon species all details of his variations (forms) */
    fun loadFormDetails(callback: (List<PokemonForList>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val forms = when (val pkData = _clickedPokemon.value) {
                is UIState.Success -> pkData.data.formData
                else -> return@launch
            }
            val formPokemonIds = forms.map { it.pokemonId }
            val formList = repository.getPokemonTableEntries(formPokemonIds)
            callback(formList)
        }
    }

    //region functions to load commonly used things like type names language names version names

    /** Gets details about every type (water, fire, ...) and loads it into database*/
    private fun loadTypeDetails() {
        viewModelScope.launch {
            repository.loadTypeDetails()
        }
    }

    fun getPokemonImagesWithNames(pokemonId: Int, callback: (List<Pair<Int, String>>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = repository.getPokemonImagesWithNameRes(pokemonId)
            callback(list)
        }
    }


    /** Gets Language and Version names together */
    private fun loadLanguageAndVersionNames() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.loadLanguageAndVersionNames(languageId)
        }
    }

    /** Returns color/drawable/textColor resources for a specific pokemon type */
    fun getTypeColorAndIconId(typeId: Int): Triple<Int, Int, Int> {
        return typeColorMap[typeId] ?: Triple(
            R.color.type_colour_unknown,
            R.drawable.pokemon_type_icon_unknown,
            R.color.text_colour_unknown
        )
    }


    /** Maps the destination names to better readable Names (for home detail picture card) */
    private fun getReadableName(name: String): String? {
        return readableNames[name]
    }

    /** For Example: this function returns red/blue for gen1
     * gold/silver for gen2 and so on */
    private fun getFirstVersionOfGeneration(genNr: Int): Int? {
        return versionGroupMap.entries.find { entry ->
            entry.value == genNr
        }?.key?.first
    }

    /** For Example: this function returns 1 (for Generation 1)
     *  for the versionIds of pokemon yellow, red and blue */
    fun getGenerationOfVersion(versionId: Int): Int? {
        return versionGroupMap.entries.find {
            it.key.first == versionId
        }?.value
    }

    /** Small function for home detail Pokemon Images,
     * maps the key name to a better readable name */
    private fun getImageTypeName(key: Any?): String? {
        if (key is String) return imageTypeName[key]
        return null
    }

    /** Returns translated name of a pokemon */
    fun getTranslatedName(): String {
        val specieNames = when (val pkData = _clickedPokemon.value) {
            is UIState.Success -> pkData.data.specyNames
            else -> return "Error"
        }
        val name = specieNames.find { it.languageId == this.languageId }?.name
        return name ?: ""
    }

    //endregion


    //region homeFragment (pokemon List)
    private val _pokemonList = repository.pokemonList
    val pokemonList: LiveData<List<PokemonForList>>
        get() = _pokemonList

    private val _sortedFilteredPokemonList = MutableLiveData<List<PokemonForList>>()
    val sortedFilteredPokemonList: LiveData<List<PokemonForList>> = _sortedFilteredPokemonList

    private val _searchInputPokemonList = MutableLiveData("")

    /** Sets the input the user entered in the fragment to sort and filter */
    fun setSearchInputPokemonList(input: String) {
        _searchInputPokemonList.value = input
        sortAndFilterPokemon()
    }

    fun getSearchInputPokemonList(): String {
        return _searchInputPokemonList.value.orEmpty()
    }

    private val _filterStateLiveData =
        MutableLiveData<Pair<PokemonSortFilter, PokemonSortFilterState>>()
    val filterStateLiveData: LiveData<Pair<PokemonSortFilter, PokemonSortFilterState>>
        get() = _filterStateLiveData


    fun selectFilterAndState(
        filterState: PokemonSortFilterState,
        pokemonSortFilter: PokemonSortFilter,
    ) {
        _filterStateLiveData.value = Pair(pokemonSortFilter, filterState)
        sortAndFilterPokemon()
    }

    /**
     * Sorts the list of Pokemon in homeDetail Fragment and filters
     *
     */
    private fun sortAndFilterPokemon() {

        val searchInput = _searchInputPokemonList.value.orEmpty()
        val initialList = _pokemonList.value.orEmpty() // every pokemon

        val filteredList = initialList.filter { it.name.contains(searchInput, true) }
        val (sortFilter, filterState) = _filterStateLiveData.value ?: Pair(
            PokemonSortFilter.WEIGHT,
            PokemonSortFilterState.INACTIVE
        )

        val sortedFilteredList = sortList(filteredList, sortFilter, filterState)
        _sortedFilteredPokemonList.value = sortedFilteredList
    }

    private fun sortList(
        list: List<PokemonForList>,
        sortFilter: PokemonSortFilter,
        filterState: PokemonSortFilterState
    )
            : List<PokemonForList> {
        val comparator = when (sortFilter) {
            PokemonSortFilter.WEIGHT -> compareBy<PokemonForList> { it.weight }
            PokemonSortFilter.HEIGHT -> compareBy { it.height }
            PokemonSortFilter.NAME -> compareBy { it.name }
            PokemonSortFilter.STATS -> compareBy { it.baseStats.sumOf { stat -> stat.statValue } }
        }

        return when (filterState) {
            PokemonSortFilterState.ASCENDING -> list.sortedWith(comparator)
            PokemonSortFilterState.DESCENDING -> list.sortedWith(comparator.reversed())
            else -> list
        }
    }
    //endregion

    //region home detail

    private val _clickedPokemon = MutableLiveData<UIState<PokemonData>>()
    val clickedPokemon: LiveData<UIState<PokemonData>>
        get() = _clickedPokemon

    private fun pokemonAlreadyLoaded(pokemonId: Int): Boolean {
        return when (val currentState = _clickedPokemon.value) {
            is UIState.Success -> currentState.data.pokemon.id == pokemonId
            else -> false
        }
    }

    /**
     * Gets Data of a single Pokemon ( probably when clicked ) from the database or per api call
     *
     * @param pokemonId id of the pokemon which was clicked
     * @param errorMessageRes String resource that holds the error message for failure
     */
    fun getSinglePokemonData(
        pokemonId: Int,
        @StringRes errorMessageRes: Int = R.string.failed_load_single_pokemon_data,
        optionalCallback: (() -> Unit)? = null,
    ) {
        if (pokemonAlreadyLoaded(pokemonId)) {
            if (optionalCallback != null) optionalCallback()
            else return
        }


        viewModelScope.launch(Dispatchers.IO) {

            _clickedPokemon.postValue(UIState.Loading)
            val needsToBeLoaded = repository.checkIfPokemonNeedsToBeLoaded(pokemonId)
            if (needsToBeLoaded) { // load data and map it + save to database
                loadPokemonDataFromApiAndSave(pokemonId, errorMessageRes) {
                    optionalCallback?.invoke()
                }

            } else { // already loaded from api, just post data from DB
                val data =
                    repository.getPokemonDataFromDatabase(pokemonId, languageId, errorMessageRes)
                viewModelScope.launch(Dispatchers.Main) {
                    _clickedPokemon.value = data
                    optionalCallback?.invoke()
                }

            }
        }
    }

    fun fetchEveryPokemonData() {
        viewModelScope.launch(Dispatchers.IO) {
            sharedViewModel.postMessage("Started fetching EVERY POKEMON")
            var startId = 1
            val totalPokemons = 1025

            while (startId <= totalPokemons) {
                try {
                    val success = suspendCoroutine { continuation ->
                        loadPokemonDataFromApiAndSave(
                            startId,
                            R.string.failed_load_single_pokemon_data,
                            false
                        ) { isSuccess ->
                            continuation.resume(isSuccess)
                        }
                    }
                    if (success) {
                        startId++
                        if (startId % 20 == 0) {
                            sharedViewModel.postMessage("Loaded $startId Pokemon")
                            System.gc()
                        }
                    } else {
                        startId++
                        Log.d("PokeViewModel", "Failed to load and save pokemon $startId")
                    }
                } catch (e: Exception) {
                    Log.e("PokeViewModel", "Error loading pokemon $startId", e)
                }
            }

            sharedViewModel.postMessage("Finished fetching EVERY POKEMON")
            Log.d("PokeViewModel", "Finished getting data for every pokemon")
        }
    }

    private fun loadPokemonDataFromApiAndSave(
        pokemonId: Int,
        errorMessageRes: Int,
        postValue: Boolean = true,
        optionalCallback: ((Boolean) -> Unit)? = null,
    ) {
        fun failure() {
            sharedViewModel.postMessage(errorMessageRes) {
                getSinglePokemonData(pokemonId, errorMessageRes)
            }
            val errorException = Exception("Failed to load Pokemon Data...")
            _clickedPokemon.postValue(UIState.Error(errorException, errorMessageRes))
            optionalCallback?.invoke(false)
        }

        viewModelScope.launch(Dispatchers.IO) {
            val data = repository.loadSinglePokemonData(pokemonId, languageId)
            if (data?.data1 != null && data.data2 != null && data.data3 != null) {
                val mapper = PokemonDatabaseMapper(repository)
                mapper.savePokemonDetailsIntoDatabase(data, languageId) { isSuccess ->
                    if (isSuccess) {
                        viewModelScope.launch(Dispatchers.IO) {
                            val pokemonData = repository.getPokemonDataFromDatabase(
                                pokemonId,
                                languageId,
                                errorMessageRes
                            )
                            viewModelScope.launch(Dispatchers.Main) {
                                if (postValue) _clickedPokemon.value = pokemonData
                                optionalCallback?.invoke(true)
                            }
                        }
                    } else {
                        failure()
                    }
                }
            } else {
                failure()
            }
        }
    }


    /**
     * Filters all Attacks from 1 Pokemon in a specific generation (version of that generation) and sorts them in the end
     *
     * @param generationId id of the generation e.g. 1 for red/blue
     * @return the filtered list
     */
    fun getFilteredAttacks(generationId: Int): List<AttacksData> {
        val (moveData, moveNames, versionGroupDetails) = when (val pkData = _clickedPokemon.value) {
            is UIState.Success -> Triple(
                pkData.data.moves,
                pkData.data.moveNames,
                pkData.data.versionGroupDetails
            )

            else -> return emptyList()
        }
        val versionId = getFirstVersionOfGeneration(generationId)

        // move Data in the specific gen
        val moveDetailsGenFiltered =
            versionGroupDetails.filter { it.versionGroupId == versionId }

        // map it to a compact dataclass to insert it into a recyclerview than (and sort it)
        val filteredAttacksList = moveDetailsGenFiltered.map { versionGroupDetail ->
            val move = moveData.find { it.id == versionGroupDetail.moveId }
            val moveName = moveNames.find { it.moveId.toLong() == versionGroupDetail.moveId }?.name
            AttacksData(
                name = moveName ?: "Error",
                levelLearned = versionGroupDetail.levelLearnedAt ?: 0,
                accuracy = move?.accuracy,
                effectText = move?.effectText ?: "Error",
                moveDamageClassId = move?.moveDamageClassId ?: 1,
                power = move?.power ?: 0,
                typeId = move?.typeId ?: 10001,
                pp = move?.ap ?: 40
            )
        }.sortedWith(
            compareBy({ it.levelLearned == 0 },
                { it.levelLearned })
        ) // sort ascending but if levelLearned == 0 to the end
        return filteredAttacksList
    }

    /** Takes the spriteListJson and extracts sprites for each source  */
    fun extractSpritesWithCategories(onLoadFinished: ((Map<String, List<Pair<String, String>>>) -> Unit)) {
        val sprites = when (val pkData = _clickedPokemon.value) {
            is UIState.Success -> pkData.data.pokemon.sprites
            else -> return
        }
        val gson = Gson()
        val spriteListType = object : TypeToken<List<PokemonDetail1Query.Sprite>>() {}.type
        val spritesData: List<PokemonDetail1Query.Sprite> = gson.fromJson(sprites, spriteListType)
        val spriteMap = spritesData.first().sprites as Map<*, *>

        val categorizedMap = mutableMapOf<String, MutableList<Pair<String, String>>>()

        // function calls itself to lookup every map in it too
        fun goThroughMap(currentMap: Map<*, *>, name: String = "sprites") {
            for ((key, value) in currentMap) {
                when (value) {
                    // if value is String? it should be the url of the image or null
                    is String? -> {
                        val displayName = getReadableName(name) ?: name
                        val type = getImageTypeName(key) ?: key.toString()
                        // if there is a url
                        if (value != null) {
                            // if there is no entry for that name in categorizedMap a mutable list is created
                            // else just the pair is added
                            categorizedMap.getOrPut(displayName) { mutableListOf() }
                                // type of image (e.g. front_shiny and the value (url)
                                .add(Pair(type, value))
                        }
                    }
                    // when any value of the map is a map itself (e.g. the rootMap has the map "other" in it)
                    is Map<*, *> -> {
                        // call function with the keyName (e.g. "home", "crystal")
                        // as name parameter to make a map entry with its name for our final list
                        goThroughMap(value, key.toString())
                    }
                }
            }
        }
        goThroughMap(spriteMap)
        // sort the map with another function

        val map = createSortedMap(categorizedMap)
        onLoadFinished(map)

    }


    private fun createSortedMap(map: Map<String, List<Pair<String, String>>>): LinkedHashMap<String, List<Pair<String, String>>> {

        val sortOrder = listOf(
            "Default", "Home", "Showdown", "Dream World", "Official Artwork", "Gen1 Red-Blue",
            "Gen1 Yellow", "Gen2 Crystal", "Gen2 Gold", "Gen2 Silver", "Gen3 Ruby-Sapphire",
            "Gen3 Emerald", "Gen3 FireRed-LeafGreen", "Gen4 Diamond-Pearl", "Gen4 Platinum",
            "Gen4 HeartGold-SoulSilver", "Gen5 Black-White", "Gen5 Black-White animated",
            "Gen6 OmegaRuby - AlphaSapphire", "Gen6 X - Y", "Gen7 UltraSun-UltraMoon",
        )
        // creates a sorted Map of categorizedMap in the order of our sortOrder list
        val sortedMap = linkedMapOf<String, List<Pair<String, String>>>()
        sortOrder.forEach { name ->
            // if e.g. Official Artwork has no urls/ is null it will be skipped
            map[name]?.let { imageList -> sortedMap[name] = imageList }
        }
        return sortedMap
    }


    /**
     * Filters all Pokedex Info from a specific language
     *
     * @param languageId id of the language
     * @return the filtered list
     */
    fun filterPokedexInfo(languageId: Int = this.languageId): List<PokemonDexEntries> {
        val pokedexInfo = when (val pkData = _clickedPokemon.value) {
            is UIState.Success -> pkData.data.pokedexEntries
            else -> return emptyList()
        }
        val filteredList = pokedexInfo.filter { it.languageId == languageId }
        return filteredList.ifEmpty {
            listOf(
                PokemonDexEntries(
                    speciesId = -1,
                    text = "Error, no Data found",
                    languageId = languageId,
                    versionGroupId = -1
                )
            )
        }
    }

    /**
     * Maps the abilities of a pokemon to dataclasses to use in recyclerView
     * sorts them also in slot order
     */
    fun mapAbilitiesDetail(): List<AbilityEffectText> {
        val data = when (val pkData = _clickedPokemon.value) {
            is UIState.Success -> pkData.data
            else -> return emptyList()
        }
        val abilityNames = data.abilityNames
        val abilityIds = data.abilitiesToJoin.sortedBy { it.slot }
        val abilityData = data.abilityInfoList.map { ability ->
            val abilityEffectTexts = data.abilityEffectTexts
            val textLong = if (abilityEffectTexts.isEmpty()) "No Data found" else {
                abilityEffectTexts.find { it?.abilityId == ability.id }?.effectTextLong // ?. is necessary
            }
            val isHidden = abilityIds.find { it.abilityId == ability.id }?.isHidden ?: false
            val textShort =
                data.abilityFlavorTexts.lastOrNull { it.abilityId == ability.id }?.effectTextShort
            val name =
                abilityNames.find { it.abilityId == ability.id && it.languageId == languageId }?.name
            AbilityEffectText(
                abilityId = ability.id,
                isHidden = isHidden,
                name = name ?: "Error",
                textLong = textLong ?: "Error",
                textShort = textShort ?: "Error",
                slot = abilityIds.find { it.abilityId == ability.id }?.slot ?: -1,
                languageId = -1 // not needed
            )
        }
        return abilityData.sortedBy { it.slot }
    }

    /** Finds the root species ID in the evolutionary chain. */
    private fun findRootPokemon(
        speciesId: Int,
        evolutionDetails: List<PkEvolutionDetails>
    ): Int? {
        // Get parent species ID, if any.
        val parentSpecies =
            evolutionDetails.find { it.speciesId == speciesId }?.evolvesFromSpeciesId
        // Return current species if no parent, else recurse to find root.
        return if (parentSpecies == null) {
            speciesId
        } else {
            findRootPokemon(parentSpecies, evolutionDetails)
        }
    }

    /** Begins constructing the evolutionary tree starting from a clicked species. */
    fun startEvolutionTree(
        clickedSpeciesId: Int,
        evolutionDetails: List<PkEvolutionDetails>,
        pokemonList: List<PokemonForList> = _pokemonList.value ?: emptyList()
    ): List<EvolutionStage> {
        // Find the root species ID, return empty if not found.
        val rootSpeciesId =
            findRootPokemon(clickedSpeciesId, evolutionDetails) ?: return emptyList()
        // Find the Pokemon object by root species ID, return empty if not found.
        val rootPokemon = pokemonList.find { it.speciesId == rootSpeciesId } ?: return emptyList()
        // Construct the full evolutionary tree.
        val nextEvolutions = buildEvolutionTree(evolutionDetails, pokemonList, rootSpeciesId)
        return listOf(EvolutionStage(rootPokemon, null, nextEvolutions))
    }

    /** Builds the evolutionary stages recursively. */
    private fun buildEvolutionTree(
        evolutionDetails: List<PkEvolutionDetails>,
        pokemonList: List<PokemonForList> = _pokemonList.value ?: emptyList(),
        rootSpeciesId: Int?,
    ): List<EvolutionStage> {
        if (rootSpeciesId == null) return emptyList()
        // Filter for current evolutionary stages.
        val currentStages = evolutionDetails.filter { it.evolvesFromSpeciesId == rootSpeciesId }

        // Recursively construct evolution tree for each stage.
        val evolutionTree = currentStages.mapNotNull { detail ->
            val pokemonInfo = pokemonList.find { it.speciesId == detail.speciesId }
            pokemonInfo?.let {
                val nextStages = buildEvolutionTree(evolutionDetails, pokemonList, detail.speciesId)

                EvolutionStage(pokemonInfo, detail, nextStages)
            }
        }
        return evolutionTree
    }

    /** Gets all Pokemon which have specific ability the user is inspecting */
    fun getPokemonListWhoHaveAbility(
        ids: List<Long>,
        onLoadFinished: (List<PokemonForList>) -> Unit
    ) {

        viewModelScope.launch(Dispatchers.IO) {
            val list = repository.getPokemonListFromIdList(ids.map { it.toInt() })
            onLoadFinished(list)
        }
    }

    //endregion


}





