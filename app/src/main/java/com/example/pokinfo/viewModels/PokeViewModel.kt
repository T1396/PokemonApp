package com.example.pokinfo.viewModels

import android.app.Application
import android.support.annotation.StringRes
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
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
import com.example.pokinfo.data.models.database.pokemon.StatValues
import com.example.pokinfo.data.models.database.versionAndLanguageNames.LanguageNames
import com.example.pokinfo.data.models.database.versionAndLanguageNames.VersionNames
import com.example.pokinfo.data.models.firebase.AttacksData
import com.example.pokinfo.data.models.firebase.EvIvData
import com.example.pokinfo.data.models.firebase.PokemonTeam
import com.example.pokinfo.data.models.firebase.TeamPokemon
import com.example.pokinfo.data.models.fragmentDataclasses.TeamBuilderData
import com.example.pokinfo.data.enums.PokemonSortFilter
import com.example.pokinfo.data.enums.PokemonSortFilterState
import com.example.pokinfo.data.models.database.type.PokemonTypeName
import com.example.pokinfo.data.util.UIState
import com.example.pokinfo.data.util.sharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class PokeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RepositoryProvider.provideRepository(application)

    private var isInitialized by application.sharedPreferences("isInitialized", false)

    private var languageId by application.sharedPreferences("languageId", 9)

    var versionNames: List<VersionNames> = emptyList()
        private set
    var languageNames: List<LanguageNames> = emptyList()
        private set

    var pokemonTypeNames: List<PokemonTypeName> = emptyList()
        private set

    init {
        loadGenericData()
    }

    private fun loadGenericData() {
        viewModelScope.launch(Dispatchers.IO) {
            versionNames = repository.getVersionNames(languageId)
            languageNames = repository.getLanguageNames()
            pokemonTypeNames = repository.getPokemonTypeNames(languageId)
        }
    }


    fun setLangId(languageId: Int) {
        this.languageId = languageId
        loadGenericData()
    }
    fun getLangId(): Int {
        return languageId
    }

    private val _snackBarMessageSender = MutableLiveData<Int>()
    val snackBarMessageSender: LiveData<Int>
        get() = _snackBarMessageSender


    /** Gets some important Data, List of all Pokemon, Language Names and Type Details
     * should be loaded just for the first time a user starts the app */
    fun initializeDataForApp() {
        viewModelScope.launch(Dispatchers.IO) {
            _snackBarMessageSender.postValue(R.string.fetching_data)

            val data = repository.loadAllPokemonsWithSprites(languageId) ?: return@launch

            val mappingJob = async{
                val dataMapper = PokemonListMapper()
                dataMapper.mapData(data, languageId)
            }
            val typeDetails = async { loadTypeDetails() }
            val languageNames = async { loadLanguageAndVersionNames() }
            typeDetails.await()
            languageNames.await()
            val pokemonList = mappingJob.await()
            // once all of them finished insert the list into database
            val isDone = repository.insertAllPokemon(pokemonList)
            if (isDone) {
                isInitialized = true // will be saved into shared Prefs through property delegation
            } else {
                // when something failed while inserting pokemon to database
                _snackBarMessageSender.postValue(R.string.error_fetching_data)
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

    /** Small function for home detail Pokemon Images,
     * maps the key name to a better readable name */
    fun getTypeColorMap(): Map<Int, Triple<Int, Int, Int>> {
        return typeColorMap
    }

    /** Maps the destination names to better readable Names (for homedetail picture card) */
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
        val specyNames = when (val pkData = _clickedPokemon.value) {
            is UIState.Success -> pkData.data.specyNames
            else -> return "Error"
        }
        val name = specyNames.find { it.languageId == this.languageId }?.name
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

    fun getSearchInputPokemonList(): String{
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
        val (sortFilter, filterState) = _filterStateLiveData.value ?: Pair(PokemonSortFilter.WEIGHT, PokemonSortFilterState.INACTIVE)

        val sortedFilteredList = sortList(filteredList, sortFilter, filterState)
        _sortedFilteredPokemonList.value = sortedFilteredList
    }

    private fun sortList(list: List<PokemonForList>, sortFilter: PokemonSortFilter, filterState: PokemonSortFilterState)
    : List<PokemonForList> {
        val comparator = when(sortFilter) {
            PokemonSortFilter.WEIGHT -> compareBy<PokemonForList> {it.weight}
            PokemonSortFilter.HEIGHT -> compareBy {it.height}
            PokemonSortFilter.NAME -> compareBy {it.name}
            PokemonSortFilter.STATS -> compareBy {it.stats.sumOf { stat -> stat.statValue }}
        }

        return when(filterState) {
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

    private fun pokemonAlreadyLoaded(pokemonId: Int) : Boolean {
        return when (val currentState = _clickedPokemon.value) {
            is UIState.Success -> currentState.data.pokemon.id == pokemonId
            else -> false
        }
    }

    /**
     * Gets Data of a single Pokemon ( probably when clicked ) from the database or per apicall
     *
     * @param pokemonId id of the pokemon which was clicked
     * @param errorMessageRes String Ressource that holds the error message for failure
     */
    fun getSinglePokemonData(
        pokemonId: Int,
        @StringRes errorMessageRes: Int,
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
                val data = repository.getPokemonDataFromDatabase(pokemonId, languageId, errorMessageRes)
                viewModelScope.launch(Dispatchers.Main) {
                    _clickedPokemon.value = data
                    optionalCallback?.invoke()
                }

            }
        }
    }

    private fun loadPokemonDataFromApiAndSave(
        pokemonId: Int,
        errorMessageRes: Int,
        optionalCallback: (() -> Unit)? = null,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val data = repository.loadSinglePokemonData(pokemonId, languageId)
            if (data?.data1 != null && data.data2 != null && data.data3 != null) {
                val mapper = PokemonDatabaseMapper(repository)
                mapper.savePokemonDetailsIntoDatabase(data, languageId) { isSuccess ->
                    if (isSuccess) {
                        viewModelScope.launch(Dispatchers.IO) {
                            val pokemonData = repository.getPokemonDataFromDatabase(pokemonId, languageId, errorMessageRes)
                            viewModelScope.launch (Dispatchers.Main){
                                _clickedPokemon.value = pokemonData
                                optionalCallback?.invoke()
                            }
                        }
                    } else _snackBarMessageSender.postValue(errorMessageRes)
                }
            } else _snackBarMessageSender.postValue(errorMessageRes)
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
            is UIState.Success -> Triple(pkData.data.moves, pkData.data.moveNames, pkData.data.versionGroupDetails)
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
        val sprites = when(val pkData = _clickedPokemon.value) {
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
            "Gen1 Yellow", "Gen2 Crystal", "Gen2 Gold", "Gen2 Silver", "Gen3 Ruby-Saphhire",
            "Gen3 Emerald", "Gen3 Firered-Leafgreen", "Gen4 Diamond-Pearl", "Gen4 Platinum",
            "Gen4 Heartgold-Soulsilver", "Gen5 Black-White", "Gen5 Black-White animated",
            "Gen6 OmegaRuby - Alphasapphire", "Gen6 X - Y", "Gen7 Ultrasun-Ultramoon",
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
     * Filters all Pokedex Infos from a specific language
     *
     * @param languageId id of the language
     * @return the filtered list
     */
    fun filterPokedexInfos(languageId: Int = this.languageId): List<PokemonDexEntries> {
        val pokedexInfos = when(val pkData = _clickedPokemon.value) {
            is UIState.Success -> pkData.data.pokedexEntries
            else -> return emptyList()
        }
        val filteredList = pokedexInfos.filter { it.languageId == languageId }
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
                abilityEffectTexts.find { it?.abilityId == ability.id }?.effectTextLong // ?. is neccessary
            }
            val textShort =
                data.abilityFlavorTexts.lastOrNull { it.abilityId == ability.id }?.effectTextShort
            val name =
                abilityNames.find { it.abilityId == ability.id && it.languageId == languageId }?.name
            AbilityEffectText(
                id = ability.id,
                name = name ?: "Error",
                textLong = textLong ?: "Error",
                textShort = textShort ?: "Error",
                slot = abilityIds.find { it.abilityId == ability.id }?.slot ?: -1,
                languageId = -1 // not needed
            )
        }
        return abilityData.sortedBy { it.slot }
    }
    //endregion

    //region teambuilder // teamsfragment

    private val _pokemonTeam = MutableLiveData(PokemonTeam())
    val pokemonTeam: LiveData<PokemonTeam?>
        get() = _pokemonTeam

    /** When new team is created */
    fun resetTeamData() {
        _pokemonTeam.value = PokemonTeam()
    }

    /** Inserts a pokemon into a team when chosen
     * @param teamIndex the position in the team
     * @param postVal if true the pokemon team live-data will be updated */
    fun insertPokemonToTeam(teamIndex: Int, postVal: Boolean) {
        val team = _pokemonTeam.value
        val pokemonList = team?.pokemons?.toMutableList()
        pokemonList?.let { pokeList ->
            if (_clickedPokemon.value is UIState.Success) {
                mapPokemonDataToTeamPokemon((_clickedPokemon.value as UIState.Success<PokemonData>).data) { mappedPokemon ->
                    pokeList[teamIndex] = mappedPokemon
                    team.pokemons = pokeList
                    if (postVal) _pokemonTeam.postValue(team)
                }
            }
        }
    }

    /** Delete a Pokemon from the team and creates a new slot if there is no one already,
     *  also updates the live-data to update UI */
    fun deletePokemonFromTeam(teamIndex: Int) {
        val team = _pokemonTeam.value
        val pokemonList = team?.pokemons?.toMutableList()
        pokemonList?.let {pokeList ->
            pokeList.removeAt(teamIndex) // remove old pokemon
            if (!pokeList.any { it?.pokemonId == 0 }) {
                pokeList.add(TeamPokemon()) // add empty placeholder data object if there is not one already
            } else {
                pokeList.add(null) // useful to hide this slot in the fragment than
            }
            team.pokemons = pokeList
            _pokemonTeam.value = team
        }
    }

    /** Insert a Team into the live-data to be able to edit it */
    fun insertTeam(pokemonTeam: PokemonTeam) {
        _pokemonTeam.value = pokemonTeam
    }

    /** Gets the actual Team in the Live-Data, but removes "empty slots" (placeholder)
     *  to insert it into fire store without useless information */
    fun getPokemonTeam(): PokemonTeam? {
        val team = _pokemonTeam.value
        val teamPokemon = team?.pokemons?.toMutableList()
        teamPokemon?.forEachIndexed { index, pokemon ->
            if (pokemon?.pokemonId == 0) teamPokemon[index] = null
            // if there is a pokemon with id 0 its a placeholder we dont want to get
        }
        if (teamPokemon != null) {
            team.pokemons = teamPokemon
        }
        return team
    }

    fun getNameFromTeamPokemon(teamIndex: Int): String {
        val team = _pokemonTeam.value?.pokemons
        return team?.get(teamIndex)?.pokemonInfos?.name ?: ""
    }

    /** Gets the first empty slot of the actual pokemon team,
     *  first empty slot will either have a null value or a empty object (pokemonId = 0) */
    fun getEmptySlotFromTeam(): Int {
        val team = _pokemonTeam.value?.pokemons
        return team?.indexOfFirst { it == null || it.pokemonId == 0 } ?: -1
    }

    private val _everyPokemon = repository.teambuilderPokeList
    val everyPokemon: LiveData<List<PokemonForList>>
        get() = _everyPokemon

    private val _filteredListTeamBuilder = MutableLiveData<List<PokemonForList>>()
    val filteredListTeamBuilder: LiveData<List<PokemonForList>>
        get() = _filteredListTeamBuilder

    /** Filter function for team builder fragment, no input shows no results */
    fun filterPokemonList(input: String) {
        val filteredList = if (input.isBlank()) {
            emptyList()
        } else {
            _everyPokemon.value.orEmpty().filter { pokemon ->
                pokemon.name.contains(input, true)
            }
        }
        _filteredListTeamBuilder.value = filteredList
    }

    /** stores the selected attacks */
    private val _teamBuilderSelectedAttacks = MutableLiveData<List<AttacksData>>()
    val teamBuilderSelectedAttacks: LiveData<List<AttacksData>>
        get() = _teamBuilderSelectedAttacks

    fun setSelectedAttacks(attacks: List<AttacksData>) {
        _teamBuilderSelectedAttacks.value = attacks
    }

    /** Updates the Attacks chosen in the Fragment */
    fun updatePokemonAttacks(teamIndex: Int) {
        if (teamIndex == -1) return
        val team = _pokemonTeam.value
        val pokemonToUpdate = team?.pokemons?.getOrNull(teamIndex)
        if (pokemonToUpdate != null) {
            val attacks = _teamBuilderSelectedAttacks.value
            pokemonToUpdate.attackOne = attacks?.getOrNull(0)
            pokemonToUpdate.attackTwo = attacks?.getOrNull(1)
            pokemonToUpdate.attackThree = attacks?.getOrNull(2)
            pokemonToUpdate.attackFour = attacks?.getOrNull(3)
        }
    }

    fun resetChosenAttacks() {
        _teamBuilderSelectedAttacks.postValue(emptyList())
    }

    /** Mapping function to map the details from _clickedPokemon to a PokemonData object for teamBuilder Fragment */
    private fun mapPokemonDataToTeamPokemon(
        newPokemon: PokemonData?,
        callback: (TeamPokemon) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val images =
                repository.getPokemonImages(newPokemon?.pokemon?.id ?: -1) ?: Triple("", "", "")

            val pokemonName =
                newPokemon?.specyNames?.find { it.languageId == this@PokeViewModel.languageId }?.name

            val pokemonForList = PokemonForList(
                id = newPokemon?.pokemon?.id ?: -1,
                name = pokemonName ?: "Error",
                weight = newPokemon?.pokemon?.weight ?: -1,
                height = newPokemon?.pokemon?.height ?: -1,
                imageUrl = images.first,
                altImageUrl = images.second,
                officialImageUrl = images.third,
                stats = newPokemon?.pokemon?.pkStatInfos?.map {
                    StatValues(
                        statValue = it.baseStat,
                        statId = it.statId
                    )
                } ?: emptyList(),
                typeId1 = newPokemon?.pokemon?.primaryType?.typeId ?: 10001,
                typeId2 = newPokemon?.pokemon?.secondaryType?.typeId
            )

            val teamPok = TeamPokemon(
                pokemonId = newPokemon?.pokemon?.id ?: -1,
                pokemonInfos = pokemonForList,
                level = 100,
                gender = if (newPokemon?.specyData?.genderRate == -1) -1 else 0
            )
            callback(teamPok)
        }
    }

    /** Gets the chosen values (level, iv, ev etc) in the teambuilder fragment to update the values in the team-live data,*/
    fun updatePokemonValuesInTeam(
        createNewSlot: Boolean,
        showSaveMessage: Boolean,
        teamIndex: Int,
        pokemonDataFromUser: TeamBuilderData
    ) {

        val team = pokemonTeam.value
        val pokemonList = team?.pokemons?.toMutableList() // get copy of team list
        val chosenPokemon = pokemonList?.getOrNull(teamIndex)
        if (chosenPokemon == null) {
            _snackBarMessageSender.value = R.string.you_need_to_select_1_pokemon
            return
        } else {

            val evData = pokemonDataFromUser.evList.mapIndexed { index, value ->
                EvIvData(
                    statName = pokemonDataFromUser.evNames[index],
                    value = value
                )
            }
            val ivData = pokemonDataFromUser.ivList.mapIndexed { index, value ->
                EvIvData(
                    statName = pokemonDataFromUser.ivNames[index],
                    value = value
                )
            }
            // update the values on the instance of the pokemon
            chosenPokemon.ivList = ivData
            chosenPokemon.evList = evData
            chosenPokemon.abilityEffect = pokemonDataFromUser.selectedAbility.second
            chosenPokemon.abilityName = pokemonDataFromUser.selectedAbility.first
            chosenPokemon.gender = pokemonDataFromUser.gender
            chosenPokemon.level = pokemonDataFromUser.level

            if (createNewSlot) {
                // empty slots can possibly be null or a empty data object (pokemon id == 0)
                val hasTeamFreeSlot = team.pokemons.count { it == null || it.pokemonId == 0 } > 0
                if (hasTeamFreeSlot) {
                    val nextFreeSlot = getEmptySlotFromTeam()
                    if (nextFreeSlot != 6) pokemonList[nextFreeSlot] = TeamPokemon()
                }
            }
            if (showSaveMessage) _snackBarMessageSender.postValue(R.string.sucessfully_saved_pokemon)
            team.pokemons = pokemonList // update the team
            _pokemonTeam.postValue(team) // post updated team
        }
    }

    /** Calculates the resulting value depending on the base/iv/ev Values for that stat and
     *  the Pokemon Level (e.g. Attack or HP or Init)
     * @return resulting Stat
     */
    fun calculateStat(
        level: Int,
        baseValue: Int,
        ivValue: Int,
        evValue: Int,
        isHp: Boolean = false
    ): Int {
        // hp is calculated different
        if (baseValue == 0) return 0
        return if (isHp) {
            (((2 * baseValue + ivValue + (evValue / 4)) * level) / 100) + level + 10
        } else {
            (((2 * baseValue) + ivValue + (evValue / 4)) * level) / 100 + 5
        }
    }

    /** Gets a list of all Attacks a Pokeon can learn in the newest Version they are included
     * @return the filtered list
     */
    fun getAttacksFromNewestVersion(): List<AttacksData> {
        val pokemonDetails = when(val pkData = _clickedPokemon.value) {
            is UIState.Success -> pkData.data
            else -> return emptyList()
        }
        val moves = pokemonDetails.moves
        val moveNames = pokemonDetails.moveNames
        val versionSet =
            pokemonDetails.versionGroupDetails.map { it.versionGroupId }.toSet()
        // gets the highest version in which the pokemon has attack data
        val highestVersion = versionSet.maxByOrNull { it ?: 1 } ?: 1
        val attacksInVersion = pokemonDetails.versionGroupDetails.filter {
            it.versionGroupId == highestVersion
        } // filter attacks only from that version

        // create AttacksData objects
        val attacksDataList = attacksInVersion.map { moveDetail ->
            val move = moves.find { it.id == moveDetail.moveId }
            val moveName = moveNames.find { it.moveId.toLong() == moveDetail.moveId }?.name
            AttacksData(
                name = moveName ?: "Error",
                levelLearned = moveDetail.levelLearnedAt ?: 0,
                accuracy = move?.accuracy,
                effectText = move?.effectText ?: "No effect text found",
                moveDamageClassId = move?.moveDamageClassId ?: -1,
                power = move?.power ?: 0,
                typeId = move?.typeId ?: 10001
            )
        }.distinctBy { it.name }.sortedBy { it.name }
        return attacksDataList
    }
    //endregion

}





