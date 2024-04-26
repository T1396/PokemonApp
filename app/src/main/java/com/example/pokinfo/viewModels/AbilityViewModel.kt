package com.example.pokinfo.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.pokeinfo.data.graphModel.AbilityDetailQuery
import com.example.pokeinfo.data.graphModel.AllAbilitiesQuery
import com.example.pokinfo.adapter.abilities.AbilityInfo
import com.example.pokinfo.data.RepositoryProvider
import com.example.pokinfo.data.models.database.pokemon.PokemonForList
import com.example.pokinfo.data.util.AbilityFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.properties.Delegates

class AbilityViewModel(application: Application) : AndroidViewModel(application) {
    private var languageId by Delegates.notNull<Int>()
    fun setLangId(languageId: Int) {
        this.languageId = languageId
    }

    private val repository = RepositoryProvider.provideRepository(application)

    private val _allAbilities = MutableLiveData<List<AbilityInfo>>()
    private val abilityMapper: (AllAbilitiesQuery.Data?) -> List<AbilityInfo> = { response ->
        response?.response?.data?.map { ability ->
            AbilityInfo(
                abilityId = ability.id,
                name = ability.names.firstOrNull()?.name ?: "No Name found",
                generationNr = ability.generation_id ?: 1,
            )
        } ?: emptyList()
    }

    private var _abilityDetails = MutableLiveData<AbilityDetailQuery.Data?>()
    val abilityDetail: LiveData<AbilityDetailQuery.Data?>
        get() = _abilityDetails

    private val _selectedAbilityFilter = MutableLiveData<AbilityFilter?>()
    private val selectedAbilityFilter: LiveData<AbilityFilter?>
        get() = _selectedAbilityFilter

    private val _filteredAbilityList = MutableLiveData<List<AbilityInfo>>()
    val filteredAbilityList: LiveData<List<AbilityInfo>>
        get() = _filteredAbilityList

    private val _searchInputAbility = MutableLiveData("")


    fun setSearchInput(input: String) {
        _searchInputAbility.value = input
        filterAbilities()
    }

    fun getAllAbilities() {
        viewModelScope.launch(Dispatchers.IO) {
            val abilitiesList = repository.loadAllAbilities(languageId)

            withContext(Dispatchers.Main) {
                _allAbilities.value = (abilityMapper.invoke(abilitiesList))
                _filteredAbilityList.value = (_allAbilities.value)
            }
        }
    }

    fun getAbilityDetail(abilityId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val abilityDetail = repository.loadAbilityDetails(abilityId)
            _abilityDetails.postValue(abilityDetail)
        }
    }

    /** Gets all Pokemon who learns the move the user is inspecting */
    fun getPokemonListWhoLearnMove(ids: List<Int>, onLoadFinished: (List<PokemonForList>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = repository.getPokemonWhoLearnSpecificAttack(ids)
            onLoadFinished(list)
        }
    }

    fun selectAbilitiesFilter(filter: AbilityFilter, isSelected: Boolean) {
        if (isSelected) {
            _selectedAbilityFilter.value = filter
        } else {
            _selectedAbilityFilter.value = null // remove filter
        }
        filterAbilities()
    }

    private fun filterAbilities() {
        val searchInput = _searchInputAbility.value.orEmpty()
        val currentFilters = selectedAbilityFilter.value
        val allAbilities = _allAbilities.value.orEmpty()

        // filter depending on the active filter (currentFilters)
        val filteredList = allAbilities.filter { ability ->
            // first filter with searchInput
            ability.name.contains(searchInput, true) &&
                    (currentFilters == null || ability.generationNr == currentFilters.genId)
            // if a filter is selected filter the ability where genID match with filter genID else don't filter
        }
        _filteredAbilityList.postValue(filteredList)
    }

}
