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
import com.example.pokinfo.adapter.home.detail.AbilityEffectText
import com.example.pokinfo.databinding.FragmentAbilitiesDetailBinding
import com.example.pokinfo.ui.misc.dialogs.openPokemonListDialog
import com.example.pokinfo.ui.misc.dialogs.showConfirmationDialog
import com.example.pokinfo.viewModels.AbilityViewModel
import com.example.pokinfo.viewModels.PokeViewModel


class AbilitiesDetailFragment : Fragment() {

    private var _binding: FragmentAbilitiesDetailBinding? = null
    private val binding get() = _binding!!
    private val pokeViewModel: PokeViewModel by activityViewModels()
    private val abilityViewModel: AbilityViewModel by activityViewModels()
    private var snapHelperAbility: PagerSnapHelper? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentAbilitiesDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val languageId = pokeViewModel.getLangId()
        abilityViewModel.abilityDetail.observe(viewLifecycleOwner) { response ->
            val typeNames = pokeViewModel.pokemonTypeNames
            val ability = response?.data?.firstOrNull()
            val abilityName =
                ability?.names?.nodes?.find { it.language_id == languageId }?.name
                    ?: ability?.name
            binding.tvAbilityName.text = abilityName

            val adapter = AbilityDetailAdapter()
            binding.rvAbility.adapter = adapter

            if (snapHelperAbility == null) {
                snapHelperAbility = PagerSnapHelper()
                snapHelperAbility?.attachToRecyclerView(binding.rvAbility)
            }
            // map data to AbilityDataClass for RecyclerView

            val list = ability?.effectTexts?.map { abilityData ->
                val effectTextLangId = abilityData.language_id
                AbilityEffectText(
                    id = abilityData.ability_id ?: -1,
                    name = ability.names.nodes.find { it.language_id == effectTextLangId }?.name
                        ?: "Error",
                    languageId = effectTextLangId ?: -1,
                    textLong = abilityData.effect,
                    textShort = abilityData.short_effect,
                    slot = -1
                )

            } ?: emptyList()
            // sorts the lists so that elements with the actual languageid comes first
            adapter.submitList(list.sortedWith(compareBy { it.languageId != pokeViewModel.getLangId() }))

            val idList = ability?.pokemonList?.map { it.pokemon_id ?: -1 } ?: emptyList()
            abilityViewModel.getPokemonListWhoLearnMove(idList) { pokemonList ->

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

        }
    }
}