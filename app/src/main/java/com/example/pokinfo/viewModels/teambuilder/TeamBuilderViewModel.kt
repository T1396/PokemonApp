package com.example.pokinfo.viewModels.teambuilder

import android.app.Application
import android.support.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.example.pokinfo.R
import com.example.pokinfo.adapter.home.detail.AbilityEffectText
import com.example.pokinfo.data.RepositoryProvider
import com.example.pokinfo.data.mapper.PokemonDatabaseMapper
import com.example.pokinfo.data.models.database.pokemon.PokemonData
import com.example.pokinfo.data.models.database.pokemon.PokemonForList
import com.example.pokinfo.data.models.database.pokemon.StatValues
import com.example.pokinfo.data.models.database.pokemon.PokemonTypeName
import com.example.pokinfo.data.models.database.pokemon.LanguageNames
import com.example.pokinfo.data.models.database.pokemon.VersionNames
import com.example.pokinfo.data.models.firebase.AttacksData
import com.example.pokinfo.data.models.firebase.PokemonTeam
import com.example.pokinfo.data.models.firebase.TeamPokemon
import com.example.pokinfo.data.models.fragmentDataclasses.TeamBuilderData
import com.example.pokinfo.data.util.UIState
import com.example.pokinfo.data.util.sharedPreferences
import com.example.pokinfo.viewModels.SharedViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TeamBuilderViewModel(
    application: Application,
    private val sharedViewModel: SharedViewModel
    ) : AndroidViewModel(application) {
    private val repository = RepositoryProvider.provideRepository(application)
    private var languageId by application.sharedPreferences("languageId", 9)

    private val _clickedPokemon = MutableLiveData<UIState<PokemonData>>()
    val clickedPokemon: LiveData<UIState<PokemonData>>
        get() = _clickedPokemon


    var versionNames: List<VersionNames> = emptyList()
        private set
    var languageNames: List<LanguageNames> = emptyList()
        private set
    var pokemonTypeNames: List<PokemonTypeName> = emptyList()
        private set

    init {
        loadGenericData()
    }

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
     * @param errorMessageRes String Resource that holds the error message for failure
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
                val data =
                    repository.getPokemonDataFromDatabase(pokemonId, languageId, errorMessageRes)
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

    private fun loadGenericData() {
        viewModelScope.launch(Dispatchers.IO) {
            versionNames = repository.getVersionNames(languageId)
            languageNames = repository.getLanguageNames()
            pokemonTypeNames = repository.getPokemonTypeNames(languageId)
        }
    }


    //region team builder // teams fragment

    private val _pokemonTeam = MutableLiveData(PokemonTeam())
    val pokemonTeam: LiveData<PokemonTeam?>
        get() = _pokemonTeam

    /** When new team is created */
    fun resetTeamData() {
        _pokemonTeam.postValue(PokemonTeam())
        _selectedAbility.postValue(-1)
        resetChosenAttacks()
        teamIndex = 0
    }

    /** Inserts a pokemon into a team when chosen */
    fun insertPokemonToTeam() {
        val team = _pokemonTeam.value
        val pokemonList = team?.pokemons?.toMutableList()
        pokemonList?.let { pokeList ->
            if (_clickedPokemon.value is UIState.Success) {
                mapPokemonDataToTeamPokemon((_clickedPokemon.value as UIState.Success<PokemonData>).data) { mappedPokemon ->
                    pokeList[teamIndex] = mappedPokemon
                    team.pokemons = pokeList
                    _pokemonTeam.postValue(team)
                }
            }
        }
    }

    /** Delete a Pokemon from the team and creates a new slot if there is no one already,
     *  also updates the live-data to update UI */
    fun deletePokemonFromTeam(index: Int) {
        val team = _pokemonTeam.value
        val pokemonList = team?.pokemons?.toMutableList()
        pokemonList?.let { pokeList ->
            pokeList.removeAt(index) // remove old pokemon
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
            // if there is a pokemon with id 0 its a placeholder we don t want to get
        }
        if (teamPokemon != null) {
            team.pokemons = teamPokemon
        }
        return team
    }


    /** Gets the first empty slot of the actual pokemon team,
     *  first empty slot will either have a null value or a empty object (pokemonId = 0) */
    private fun getEmptySlotFromTeam(): Int {
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

    var teamIndex: Int = 0
        private set(value) {
            if (value in 0..5) field = value
        }

    fun setNewTeamIndex(index: Int) {
        teamIndex = index
    }

    fun getPokemonOnTeamIndex(): TeamPokemon? {
        return _pokemonTeam.value?.pokemons?.getOrNull(teamIndex)
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

    /** Updates the Attacks chosen in the Fragment */
    fun updatePokemonAttacks() {
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


    private val _pokemonAbilities: LiveData<List<AbilityEffectText>> = _clickedPokemon.map {
        mapAbilitiesDetail()
    }
    val pokemonAbilities: LiveData<List<AbilityEffectText>> get() = _pokemonAbilities

    private val _selectedAbility = MutableLiveData<Int?>(null)

    fun setSelectedAbility(abilityId: Int) {
        _selectedAbility.postValue(abilityId)
    }

    /**
     * Maps the abilities of a pokemon to dataclasses to use in recyclerView
     * sorts them also in slot order
     */
    private fun mapAbilitiesDetail(): List<AbilityEffectText> {
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
            val textShort =
                data.abilityFlavorTexts.lastOrNull { it.abilityId == ability.id }?.effectTextShort
            val name =
                abilityNames.find { it.abilityId == ability.id && it.languageId == languageId }?.name
            AbilityEffectText(
                abilityId = ability.id,
                name = name ?: "Error",
                textLong = textLong ?: "Error",
                textShort = textShort ?: "Error",
                slot = abilityIds.find { it.abilityId == ability.id }?.slot ?: -1,
                languageId = -1 // not needed
            )
        }
        return abilityData.sortedBy { it.slot }
    }

    private fun resetChosenAttacks() {
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
                newPokemon?.specyNames?.find { it.languageId == this@TeamBuilderViewModel.languageId }?.name

            val pokemonForList = PokemonForList(
                id = newPokemon?.pokemon?.id ?: -1,
                name = pokemonName ?: "Error",
                weight = newPokemon?.pokemon?.weight ?: -1,
                height = newPokemon?.pokemon?.height ?: -1,
                imageUrl = images.first,
                altImageUrl = images.second,
                officialImageUrl = images.third,
                baseStats = newPokemon?.pokemon?.pkStatInfos?.map {
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

    fun isTeamFull(): Boolean {
        val team = _pokemonTeam.value ?: return false
        val amountOfPokemon = team.pokemons.filter { pokemon ->
            pokemon != null && pokemon.pokemonId != 0
        }.size
        return amountOfPokemon >= 6
    }

    /** Gets the chosen values (level, iv, ev etc) in the team builder fragment to update the values in the team-live data,*/
    fun updatePokemonValuesInTeam(
        createNewSlot: Boolean,
        showSaveMessage: Boolean,
        pokemonDataFromUser: TeamBuilderData
    ) {

        viewModelScope.launch(Dispatchers.IO) {
            val team = pokemonTeam.value
            val pokemonList = team?.pokemons?.toMutableList() // get copy of team list
            val chosenPokemon = pokemonList?.getOrNull(teamIndex)
            if (chosenPokemon == null) {
                sharedViewModel.postMessage(R.string.you_need_to_select_1_pokemon)
                return@launch
            } else {
                // update the values on the instance of the pokemon
                chosenPokemon.ivList = pokemonDataFromUser.ivList
                chosenPokemon.evList = pokemonDataFromUser.evList
                chosenPokemon.abilityId = _selectedAbility.value ?: -1
                chosenPokemon.gender = pokemonDataFromUser.gender
                chosenPokemon.level = pokemonDataFromUser.level

                if (createNewSlot) {
                    // empty slots can possibly be null or a empty data object (pokemon id == 0)
                    val hasTeamFreeSlot = team.pokemons.count { it == null || it.pokemonId == 0 } > 0
                    if (hasTeamFreeSlot) {
                        val nextFreeSlot = getEmptySlotFromTeam()
                        if (nextFreeSlot in 1..5) {
                            pokemonList[nextFreeSlot] = TeamPokemon()
                            teamIndex = nextFreeSlot
                        }
                    }
                }
                if (showSaveMessage) {
                    sharedViewModel.postMessage(R.string.sucessfully_saved_pokemon)
                }
                team.pokemons = pokemonList // update the team
                _pokemonTeam.postValue(team) // post updated team
            }
        }
    }

    /** Gets a list of all Attacks a Pokemon can learn in the newest Version they are included
     * @return the filtered list
     */
    fun getAttacksFromNewestVersion(): List<AttacksData> {
        val pokemonDetails = when (val pkData = _clickedPokemon.value) {
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
                typeId = move?.typeId ?: 10001,
                pp = move?.ap ?: 40
            )
        }.distinctBy { it.name }.sortedBy { it.name }
        return attacksDataList
    }
}