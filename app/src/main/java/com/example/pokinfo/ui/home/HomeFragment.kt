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
import com.example.pokinfo.R
import com.example.pokinfo.adapter.home.PokeListAdapter
import com.example.pokinfo.data.enums.PokemonSortFilter
import com.example.pokinfo.databinding.FragmentHomeBinding
import com.example.pokinfo.viewModels.PokeViewModel
import com.google.android.material.chip.ChipGroup

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val pokeViewModel: PokeViewModel by activityViewModels()
    private lateinit var adapter: PokeListAdapter


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

            // show all pokemons and navigate if one is clicked
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
                createFilterChips(binding.chipGroupFilter) // create sort filter chips
                adapter.submitList(it)
                binding.tilPokemonName.editText?.setText(pokeViewModel.getSearchInputPokemonList())
                binding.tilPokemonName.editText?.addTextChangedListener { text ->
                    pokeViewModel.setSearchInputPokemonList(text.toString())
                }


        }

        // observe the filter/searchinput changes
        pokeViewModel.sortedFilteredPokemonList.observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }
    }

    // creates a chip for each PokemonSortFilter
    private fun createFilterChips(chipGroup: ChipGroup) {
        if (chipGroup.childCount == 0) { // Nur Filterchips erstellen, wenn noch keine vorhanden sind
            PokemonSortFilter.entries.forEachIndexed { _, filter ->
                // slightly different version of chip to have 3 states instead of 2
                val chip = ThreeStateChip(chipGroup.context).apply {
                    text = filter.filterName
                    setPadding(8, 8, 8, 8)
                    isCheckable = false
                    tag = filter // sets the filter as tag to use it to call the filterfunction

                    setOnClickListener {
                        chipGroup.children.forEach { child ->
                            if (child != this) {
                                (child as? ThreeStateChip)?.resetState()
                            } else {
                                (child as ThreeStateChip).nextState()
                            }
                            // disables all other chips if a chip is clicked
                        }
                        pokeViewModel.selectFilterAndState(
                            this.state, // filter state (asc, desc)
                            tag as PokemonSortFilter, // filter tag (which attribute to sort)
                        )
                    }
                }
                chipGroup.addView(chip)
            }

            // sets the active filter again if fragment is created again
            pokeViewModel.filterStateLiveData.value?.let { selectedFilter ->
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