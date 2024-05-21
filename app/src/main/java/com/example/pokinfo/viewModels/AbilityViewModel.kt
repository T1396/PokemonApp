package com.example.pokinfo.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.pokeinfo.data.graphModel.AbilityDetailQuery
import com.example.pokeinfo.data.graphModel.AllAbilitiesQuery
import com.example.pokinfo.R
import com.example.pokinfo.adapter.abilities.AbilityListAdapter
import com.example.pokinfo.adapter.home.detail.AbilityEffectText
import com.example.pokinfo.data.RepositoryProvider
import com.example.pokinfo.data.enums.AbilityFilter
import com.example.pokinfo.data.models.database.pokemon.LanguageNames
import com.example.pokinfo.data.models.database.pokemon.PokemonForList
import com.example.pokinfo.data.models.database.pokemon.PokemonTypeName
import com.example.pokinfo.data.models.database.pokemon.VersionNames
import com.example.pokinfo.data.util.sharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AbilityViewModel(application: Application, private val sharedViewModel: SharedViewModel) :
    AndroidViewModel(application) {
    private val repository = RepositoryProvider.provideRepository(application)

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

    private val _allAbilities = MutableLiveData<List<AbilityListAdapter.AbilityInfo>?>()
    private val abilityMapper: (AllAbilitiesQuery.Data?) -> List<AbilityListAdapter.AbilityInfo> = { response ->
        response?.response?.data?.map { ability ->
            AbilityListAdapter.AbilityInfo(
                abilityId = ability.id,
                name = ability.names.firstOrNull()?.name ?: "No Name found",
                generationNr = ability.generation_id ?: 1,
            )
        } ?: emptyList()
    }

    private var _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> get() = _isLoading


    private var _abilityDetails = MutableLiveData<AbilityDetailQuery.Data?>()
    val abilityDetail: LiveData<AbilityDetailQuery.Data?>
        get() = _abilityDetails

    // Define a mapper function for ability details similar to abilityMapper
    private val detailMapper: (AbilityDetailQuery.Data?) -> List<AbilityEffectText> = { data ->
        data?.data?.firstOrNull()?.let { detail ->
            detail.effectTexts.map { effectText ->
                val effectTextLangId = effectText.language_id
                AbilityEffectText(
                    abilityId = effectText.ability_id ?: -1,
                    name = detail.names.nodes.find { it.language_id == effectTextLangId }?.name
                        ?: "No name found",
                    languageId = effectTextLangId ?: -1,
                    textLong = effectText.effect,
                    textShort = effectText.short_effect,
                    slot = -1
                )
            }.sortedWith(compareBy { it.languageId != languageId })
        } ?: emptyList()
    }

    // Use the mapper to transform the LiveData from repository data
    fun prepareAbilityDetails() {
        val ability = _abilityDetails.value
        val mappedDetails = detailMapper(ability)
        _abilityEffectTexts.postValue(mappedDetails)
    }

    private val _abilityEffectTexts = MutableLiveData<List<AbilityEffectText>>()
    val abilityEffectTexts: LiveData<List<AbilityEffectText>> get() = _abilityEffectTexts


    private val _selectedAbilityFilter = MutableLiveData<AbilityFilter?>()
    private val selectedAbilityFilter: LiveData<AbilityFilter?>
        get() = _selectedAbilityFilter

    private val _filteredAbilityList = MutableLiveData<List<AbilityListAdapter.AbilityInfo>?>()
    val filteredAbilityList: LiveData<List<AbilityListAdapter.AbilityInfo>?>
        get() = _filteredAbilityList

    private val _searchInputAbility = MutableLiveData("")


    fun setSearchInput(input: String) {
        _searchInputAbility.value = input
        filterAbilities()
    }

    fun getAllAbilities() {
        _isLoading.value = true

        viewModelScope.launch(Dispatchers.IO) {
            val abilitiesList = repository.loadAllAbilities(languageId)

            if (abilitiesList == null) {
                sharedViewModel.postMessage(R.string.failed_to_load_abilities) {
                    // pass this function as lambda to make a retry function
                    getAllAbilities()

                }
                _allAbilities.postValue(null)
                _filteredAbilityList.postValue(null)
                _isLoading.postValue(false)
            } else {
                viewModelScope.launch(Dispatchers.Main) {
                    _allAbilities.value = abilityMapper.invoke(abilitiesList)
                    _filteredAbilityList.value = _allAbilities.value
                    _isLoading.value = false
                }
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
    fun getPokemonListWhoHaveAbility(onLoadFinished: (List<PokemonForList>) -> Unit) {
        val ids = _abilityDetails.value?.data?.firstOrNull()?.pokemonList?.mapNotNull {
            it.pokemon_id
        } ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val list = repository.getPokemonListFromIdList(ids)
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

    fun getAbilityName(): String {
        val ability = _abilityDetails.value?.data?.firstOrNull()
        val abilityName = ability?.names?.nodes?.find { it.language_id == this.languageId }?.name
        return abilityName ?: ability?.name ?: "No Name found"
    }

}
