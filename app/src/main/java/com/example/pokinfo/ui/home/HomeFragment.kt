package com.example.pokinfo.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.transition.TransitionManager
import com.example.pokinfo.R
import com.example.pokinfo.adapter.home.PokeListAdapter
import com.example.pokinfo.data.enums.PokemonSortSetting
import com.example.pokinfo.databinding.FragmentHomeBinding
import com.example.pokinfo.ui.Extensions.animations.showOrHideChipGroupAnimated
import com.example.pokinfo.viewModels.PokeViewModel
import com.example.pokinfo.viewModels.SharedViewModel
import com.example.pokinfo.viewModels.factory.ViewModelFactory
import com.google.android.material.chip.ChipGroup

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null


    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private val pokeViewModel: PokeViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application, sharedViewModel)
    }
    private lateinit var adapter: PokeListAdapter
    private var isFilterBarExpanded = false


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val swipeRefreshLayout = binding.homeRefreshLayout

        binding.homeRefreshLayout.setOnClickListener {
            swipeRefreshLayout.isRefreshing = false
        }

        //pokeViewModel.fetchEveryPokemonData()
/*        binding.homeRefreshLayout.setOnRefreshListener {
            val isInitialized by requireContext().sharedPreferences("isInitialized", false)
            if (!isInitialized) pokeViewModel.initializeDataForApp()
            else swipeRefreshLayout.isRefreshing = false
        }*/

        pokeViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            swipeRefreshLayout.isRefreshing = isLoading
        }

        // show all pokemon and navigate if one is clicked
        pokeViewModel.pokemonList.observe(viewLifecycleOwner) {
            val typeNames = pokeViewModel.pokemonTypeNames
            // create adapter and set callback function
            adapter = PokeListAdapter(typeNames) { pokemonId ->
                // when callback invoked
                pokeViewModel.getSinglePokemonData(
                    pokemonId,
                    R.string.failed_load_single_pokemon_data
                )
                findNavController().navigate(
                    HomeFragmentDirections.actionNavHomeToNavHomeDetail(pokemonId)
                )
            }
            binding.rvPokeList.adapter = adapter
            binding.rvPokeList.setHasFixedSize(true)
            createFilterChips(binding.chipGroupFilter) // create sort filter chips
            adapter.submitList(it)

            // re enters the entered text when navigated back to home fragment
            binding.tilPokemonName.editText?.setText(pokeViewModel.getSearchInputPokemonList())
            binding.tilPokemonName.editText?.addTextChangedListener { text ->
                pokeViewModel.setSearchInputPokemonList(text.toString())
            }

            binding.tilPokemonName.setEndIconOnClickListener {
                isFilterBarExpanded = !isFilterBarExpanded
                TransitionManager.beginDelayedTransition(binding.topBar)
                showOrHideChipGroupAnimated(binding.scrollViewChips, isFilterBarExpanded)
            }

        }

        // observe the filter/search input changes
        pokeViewModel.sortedFilteredPokemonList.observe(viewLifecycleOwner) {
            adapter.submitList(it) {
                binding.rvPokeList.scrollToPosition(0)
            }
        }
    }

    // creates a chip for each PokemonSortFilter
    private fun createFilterChips(chipGroup: ChipGroup) {
        if (chipGroup.childCount == 0) { // only create chips if not already there
            PokemonSortSetting.entries.forEachIndexed { _, filter ->
                // slightly different version of chip to have 3 states instead of 2
                val chip = ThreeStateChip(chipGroup.context).apply {
                    text = filter.filterName

                    isCheckable = false
                    tag = filter // sets the filter as tag to use it to call the filter function

                    setOnClickListener {
                        chipGroup.children.forEach { child ->
                            if (child != this) {
                                (child as? ThreeStateChip)?.resetState()
                            } else {
                                (child as ThreeStateChip).nextState()
                            }
                            // disables all other chips if a chip is clicked
                        }
                        pokeViewModel.selectSortOption(
                            this.state, // filter state (asc, desc)
                            tag as PokemonSortSetting, // filter tag (which attribute to sort)
                        )
                    }
                }
                chipGroup.addView(chip)
            }

            // sets the active filter again if fragment is created again
            pokeViewModel.sortOptionLiveData.value?.let { selectedFilter ->
                chipGroup.children.forEach { chip ->
                    if ((chip as ThreeStateChip).tag == selectedFilter.first) {
                        chip.state = selectedFilter.second
                    }
                }
            }
            chipGroup.isSingleSelection = true
        }
    }
}