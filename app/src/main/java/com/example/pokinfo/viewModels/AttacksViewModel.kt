package com.example.pokinfo.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.pokinfo.data.RepositoryProvider
import com.example.pokinfo.data.mapper.AttackMapper
import com.example.pokinfo.data.models.database.pokemon.PokemonForList
import com.example.pokinfo.data.models.fragmentDataclasses.AttacksListData
import com.example.pokinfo.data.models.fragmentDataclasses.PokemonMoveData
import com.example.pokinfo.data.util.AttackFilter
import com.example.pokinfo.data.util.AttackFilter2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

class AttacksViewModel(application: Application) : AndroidViewModel(application) {
    private var languageId by Delegates.notNull<Int>()
    fun setLangId(languageId: Int) {
        this.languageId = languageId
    }

    fun getLangId(): Int {
        return languageId
    }


    private val repository = RepositoryProvider.provideRepository(application)

    private var isDataLoaded = false // to limit the api calls

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
    private val _filteredAttackList = MutableLiveData<List<AttacksListData>>()
    val filteredAttackList: LiveData<List<AttacksListData>> = _filteredAttackList

    // search input
    private val _searchInputAttacks = MutableLiveData("")

    private val _allAttacksList = MutableLiveData<List<AttacksListData>>()

    private val _clickedAttack = MutableLiveData<PokemonMoveData>()
    val clickedAttack: LiveData<PokemonMoveData>
        get() = _clickedAttack


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
            val list = repository.getPokemonWhoLearnSpecificAttack(ids)
            onLoadFinished(list)
        }
    }

    /** Calls repository function to make an api call for all pokemon moves */
    fun loadAllAttacks(languageId: Int) {
        if (!isDataLoaded) {
            viewModelScope.launch {
                val moveData = repository.loadAllMoves(languageId)
                val mappedData = AttackMapper(languageId).mapData(moveData) //
                _allAttacksList.postValue(mappedData)
                _filteredAttackList.postValue(mappedData)
                isDataLoaded = true
            }
        }
    }

    /** Calls repository function to make an api call for a specific Move */
    fun loadSingleAttackDetail(moveId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val moveDetails = repository.loadSingleMoveDetails(moveId, languageId)
            val languageNames = repository.getLanguageNames()
            val versionNames = repository.getVersionNames(languageId)
            val typeNames = repository.getPokemonTypeNames(languageId)
            val mappedData = AttackMapper(languageId).mapClickedMoveData(
                moveDetails,
                languageNames,
                versionNames,
                typeNames
            )
            _clickedAttack.postValue(mappedData)
        }
    }
}