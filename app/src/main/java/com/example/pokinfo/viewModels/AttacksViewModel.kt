package com.example.pokinfo.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.pokeinfo.data.graphModel.AttackDetailsQuery
import com.example.pokeinfo.data.graphModel.AttacksQuery
import com.example.pokinfo.R
import com.example.pokinfo.data.RepositoryProvider
import com.example.pokinfo.data.models.database.pokemon.PokemonForList
import com.example.pokinfo.data.models.database.pokemon.LanguageNames
import com.example.pokinfo.data.models.database.pokemon.VersionNames
import com.example.pokinfo.data.enums.AttackFilter
import com.example.pokinfo.data.enums.AttackFilter2
import com.example.pokinfo.data.models.database.pokemon.PokemonTypeName
import com.example.pokinfo.data.models.firebase.AttacksData
import com.example.pokinfo.data.util.sharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AttacksViewModel(application: Application, private val sharedViewModel: SharedViewModel) : AndroidViewModel(application) {
    /** Variables **/
    private val repository = RepositoryProvider.provideRepository(application)

    private var languageId by application.sharedPreferences("languageId", 9)

    private var isDataLoaded = false // to limit the api calls

    var versionNames: List<VersionNames> = emptyList()
        private set
    var languageNames: List<LanguageNames> = emptyList()
        private set

    var pokemonTypeNames: List<PokemonTypeName> = emptyList()
        private set

    private val _pokeTypeNames = MutableLiveData<List<PokemonTypeName>>()
    val pokeTypeNames: LiveData<List<PokemonTypeName>> get() = _pokeTypeNames


    fun loadGenericData() {
        viewModelScope.launch(Dispatchers.IO) {
            versionNames = repository.getVersionNames(languageId)
            languageNames = repository.getLanguageNames()
            val pokeTypeNames = repository.getPokemonTypeNames(languageId)
            _pokeTypeNames.postValue(pokeTypeNames)
            loadAllAttacks()
        }
    }
    //region Attacks Fragment

    // live-data to manage filters
    private val _selectedAttackFilter = MutableLiveData<AttackFilter?>()
    val selectedAttackFilter: LiveData<AttackFilter?>
        get() = _selectedAttackFilter

    // secondary filter
    private val _selectedAttackTypeFilter = MutableLiveData<AttackFilter2?>()
    val selectedAttackTypeFilter: LiveData<AttackFilter2?>
        get() = _selectedAttackTypeFilter

    // filtered Attacks Live-Data
    private val _filteredAttackList = MutableLiveData<List<AttacksData>?>()
    val filteredAttackList: LiveData<List<AttacksData>?> = _filteredAttackList

    // search input
    private val _searchInputAttacks = MutableLiveData("")

    private val _allAttacksList = MutableLiveData<List<AttacksData>?>()

    private val _clickedAttack = MutableLiveData<AttackDetailsQuery.Data?>()
    val clickedAttack: LiveData<AttackDetailsQuery.Data?>
        get() = _clickedAttack

    private var _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    fun getLangId(): Int {
        return languageId
    }

    fun getLanguageName(languageId: Int): String {
        return languageNames.find { it.languageId == languageId }?.name ?: "No language name found"
    }

    fun setSearchInputAttacks(input: String) {
        _searchInputAttacks.value = input
        filterAttacks()
    }

    private fun filterAttacks() {
        val searchInput = _searchInputAttacks.value.orEmpty()
        val primaryFilter = _selectedAttackFilter.value
        val secondaryFilter = _selectedAttackTypeFilter.value
        val allAttacks = _allAttacksList.value.orEmpty()

        // primary filter
        var filteredList = allAttacks.filter { attack ->
            attack.name.contains(searchInput, true) &&
                    (primaryFilter == null || attack.generationId == primaryFilter.genId)
        }
        // secondary filters
        filteredList = filteredList.filter {
            when (secondaryFilter) {
                AttackFilter2.PHYSICAL_ATTACKS -> it.moveDamageClassId == 2
                AttackFilter2.SPECIAL_ATTACKS -> it.moveDamageClassId != 1 && it.moveDamageClassId != 2
                AttackFilter2.STATUS_ATTACKS -> it.moveDamageClassId == 1
                null -> {
                    true
                } // if null don't filter
            }
        }
        _filteredAttackList.postValue(filteredList)
    }

    /** Function to set actual (primary) filter in move-list fragment */
    fun selectAttacksFilter(filter: AttackFilter, isSelected: Boolean) {
        _selectedAttackFilter.value = if (isSelected) filter else null
        filterAttacks()
    }

    /** Function to set actual (secondary) filter in move-list fragment */
    fun selectSecondAttackFilter(filter: AttackFilter2, isSelected: Boolean) {
        _selectedAttackTypeFilter.value = if (isSelected) filter else null
        filterAttacks()
    }

    /** Gets all Pokemon who learns the move the user is inspecting */
    fun getPokemonListWhoLearnMove(ids: List<Int>, onLoadFinished: (List<PokemonForList>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = repository.getPokemonListFromIdList(ids)
            onLoadFinished(list)
        }
    }

    /** Calls repository function to make an api call for all pokemon moves */
    fun loadAllAttacks() {
        if (!isDataLoaded) {
            _isLoading.postValue(true)

            viewModelScope.launch(Dispatchers.IO) {
                val moveData = repository.loadAllMoves(languageId)

                if (moveData == null) {
                    sharedViewModel.postMessage(R.string.failed_to_load_attacks) {
                        // retry action for the posted snackbar is this function
                        loadAllAttacks()
                    }
                    _allAttacksList.postValue(null)
                    _filteredAttackList.postValue(null)
                    _isLoading.postValue(false)
                } else {
                    val attackList = mapToAttackList(moveData.moves)
                    _allAttacksList.postValue(attackList)
                    _filteredAttackList.postValue(attackList)
                    _isLoading.postValue(false)
                    isDataLoaded = true
                }

            }
        }
    }

    private fun mapToAttackList(rawAttacksList: List<AttacksQuery.Move>): List<AttacksData> {
        val list = rawAttacksList.map { move ->
            val defaultName = move.name
            AttacksData(
                attackId = move.id,
                name = move.names.find { it.language_id == languageId }?.name ?: defaultName,
                accuracy = move.accuracy ?: 100,
                generationId = move.generation_id ?: 1,
                moveDamageClassId = move.move_damage_class_id ?: 1,
                typeId = move.type_id ?: 10001,
                power = move.power ?: 0,
                pp = move.pp ?: 0,
                effectText = move.pokemon_v2_moveeffect?.pokemon_v2_moveeffecteffecttexts?.firstOrNull()?.short_effect ?: "No effect Text found..."
            )
        }
        return list
    }

    /** Calls repository function to make an api call for a specific Move */
    fun loadSingleAttackDetail(moveId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val moveDetails = repository.loadSingleMoveDetails(moveId, languageId)
            _clickedAttack.postValue(moveDetails)
        }
    }

    fun getAttackDescription(languageId: Int, versionId: Int): AttackDetailsQuery.Text? {
        val data = _clickedAttack.value?.move?.firstOrNull()?.texts
        var attackDescription =
            data?.find { it.language_id == languageId && it.version_group_id == versionId }
        if (attackDescription == null) {
            attackDescription = data?.firstOrNull { it.language_id == languageId }
        }
        return attackDescription
    }

    fun getFirstVersionId(): Int {
        val data = _clickedAttack.value?.move?.firstOrNull()?.texts
        return data?.mapNotNull { it.version_group_id }?.min() ?: -1
    }

    fun getFirstVersionIdOfLanguage(languageId: Int): Int? {
        val data =
            _clickedAttack.value?.move?.firstOrNull()?.texts?.filter { it.language_id == languageId }
        return data?.mapNotNull { it.version_group_id }?.minOrNull()
    }

    fun getAvailableLanguageNames(): List<LanguageNames> {
        val attacks = _clickedAttack.value?.move?.firstOrNull()?.texts?.map { it.language_id }
            ?: return emptyList()
        return languageNames.filter { attacks.any { it == languageId } }
    }

    fun getAvailableVersions(languageId: Int): List<VersionNames> {
        val data = _clickedAttack.value?.move?.firstOrNull() ?: return emptyList()
        val filtered = data.texts.filter { it.language_id == languageId }
        val versionIds = filtered.map { it.version_group_id }.toSet()
        return versionNames.filter { it.versionId in versionIds }
    }

    fun getVersionName(actualVersionId: Int): String {
        return versionNames.find { it.versionId == actualVersionId }?.name
            ?: "No version name found"
    }
}
