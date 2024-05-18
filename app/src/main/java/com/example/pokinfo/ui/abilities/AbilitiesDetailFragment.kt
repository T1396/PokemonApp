package com.example.pokinfo.ui.abilities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.PagerSnapHelper
import com.example.pokinfo.R
import com.example.pokinfo.adapter.abilities.AbilityDetailAdapter
import com.example.pokinfo.databinding.FragmentAbilitiesDetailBinding
import com.example.pokinfo.ui.misc.dialogs.openPokemonListDialog
import com.example.pokinfo.viewModels.AbilityViewModel
import com.example.pokinfo.viewModels.PokeViewModel
import com.example.pokinfo.viewModels.SharedViewModel
import com.example.pokinfo.viewModels.factory.ViewModelFactory


class AbilitiesDetailFragment : Fragment() {

    private var _binding: FragmentAbilitiesDetailBinding? = null
    private val binding get() = _binding!!
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private val pokeViewModel: PokeViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application, sharedViewModel)
    }
    private val abilityViewModel: AbilityViewModel by activityViewModels()
    private var snapHelperAbility: PagerSnapHelper? = null
    private lateinit var adapter: AbilityDetailAdapter
    private var languageId = 9


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentAbilitiesDetailBinding.inflate(inflater, container, false)
        languageId = pokeViewModel.getLangId()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        createAdapter()
        val typeNames = pokeViewModel.pokemonTypeNames

        abilityViewModel.abilityDetail.observe(viewLifecycleOwner) {
            abilityViewModel.prepareAbilityDetails()


            val abilityName = abilityViewModel.getAbilityName()
            binding.tvAbilityName.text = abilityName


            abilityViewModel.getPokemonListWhoLearnMove { pokemonList ->

                binding.btnShowPokemonWithAbility.setOnClickListener {
                    // shows dialog with list of all pokemon who learns the ability
                    openPokemonListDialog(
                        listOfPokemon = pokemonList,
                        title = getString(R.string.every_pokemon_with_ability, abilityName),
                        typeNames = typeNames,
                    ) { pokemonId ->
                        // another dialog to ask user if he surely wants to navigate
                        pokeViewModel.getSinglePokemonData(
                            pokemonId,
                            R.string.failed_load_single_pokemon_data
                        )
                        findNavController().navigate(
                            AbilitiesDetailFragmentDirections.actionAbilitiesDetailFragmentToNavHomeDetail(
                                pokemonId
                            )
                        )
                    }
                }
            }


            abilityViewModel.abilityEffectTexts.observe(viewLifecycleOwner) { abilityDetails ->
                adapter.submitList(abilityDetails)
            }
        }
    }

    private fun createAdapter() {
        adapter = AbilityDetailAdapter()
        binding.rvAbility.adapter = adapter
        if (snapHelperAbility == null) {
            snapHelperAbility = PagerSnapHelper()
            snapHelperAbility?.attachToRecyclerView(binding.rvAbility)
        }
    }
}