package com.example.pokinfo.ui.abilities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.pokinfo.adapter.home.detail.AbilityEffectText
import com.example.pokinfo.adapter.home.detail.AbilitySheetAdapter
import com.example.pokinfo.data.models.database.pokemon.PokemonTypeName
import com.example.pokinfo.databinding.FragmentAbilitiesDetailBinding
import com.example.pokinfo.ui.misc.dialogs.showConfirmationDialog
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
    private lateinit var adapter: AbilitySheetAdapter
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
            val ability = abilityDetails.firstOrNull() ?: return@observe
            setupPokemonListRv(typeNames, ability)
        }
    }

    private fun setupPokemonListRv(
        typeNames: List<PokemonTypeName>,
        ability: AbilityEffectText
    ) {
        abilityViewModel.getListOfPokemonWithSpecificAbility { pokemonList ->
            adapter = AbilitySheetAdapter(ability, typeNames) { pokemonId ->
                showConfirmationDialog(
                    onConfirm = {
                        pokeViewModel.getSinglePokemonData(pokemonId)
                        findNavController().navigate(AbilitiesDetailFragmentDirections.actionAbilitiesDetailFragmentToNavHomeDetail(pokemonId))
                    }
                )
            }
            requireActivity().runOnUiThread {
                binding.rvPokeList.adapter = adapter
                adapter.submitList(pokemonList)
            }

        }
    }
}