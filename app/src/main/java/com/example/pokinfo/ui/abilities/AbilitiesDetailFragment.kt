package com.example.pokinfo.ui.abilities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.pokinfo.R
import com.example.pokinfo.adapter.home.PokeListAdapter
import com.example.pokinfo.adapter.home.detail.AbilityEffectText
import com.example.pokinfo.data.models.database.pokemon.PokemonTypeName
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
    private lateinit var adapter: PokeListAdapter
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

        val typeNames = pokeViewModel.pokemonTypeNames

        abilityViewModel.abilityDetail.observe(viewLifecycleOwner) {
            abilityViewModel.prepareAbilityDetails()
        }

        abilityViewModel.abilityEffectTexts.observe(viewLifecycleOwner) { abilityDetails ->
            val ability = abilityDetails.firstOrNull()
            setupPokemonListRv(typeNames, ability)
            ability?.let {
                binding.tvAbilityName.text = it.name
                binding.details.tvEffectLongText.text = it.textLong
                binding.details.tvAbilityEffectText.text = it.textShort
            }
        }
    }

    private fun setupPokemonListRv(
        typeNames: List<PokemonTypeName>,
        ability: AbilityEffectText?
    ) {
        abilityViewModel.getPokemonListWhoHaveAbility { pokemonList ->
            adapter = PokeListAdapter(
                typeNames = typeNames,
                onItemClicked = { id ->
                    openPokemonListDialog(
                        listOfPokemon = pokemonList,
                        title = getString(R.string.every_pokemon_with_ability, ability?.name),
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
            )
            adapter.submitList(pokemonList)
        }
    }
}