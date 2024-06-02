package com.example.pokinfo.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.pokinfo.R
import com.example.pokinfo.adapter.home.detail.AttacksAdapter
import com.example.pokinfo.data.models.database.pokemon.PkMoveVersionGroupDetail
import com.example.pokinfo.data.models.firebase.AttacksData
import com.example.pokinfo.data.util.UIState
import com.example.pokinfo.databinding.FragmentAttacksFullscreenBinding
import com.example.pokinfo.viewModels.PokeViewModel
import com.example.pokinfo.viewModels.SharedViewModel
import com.example.pokinfo.viewModels.factory.ViewModelFactory
import com.example.pokinfo.viewModels.teambuilder.TeamBuilderViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

/** Fragment can have 2 different use cases
 *  1. To just simply show Attacks of a pokemon within different generations (selection Mode = false)
 *  2. To show a list of the attacks a pokemon can learn in newest gens to select up to 4 of that for the team builder
 * */
class FullScreenAttacksFragment : Fragment() {

    private lateinit var attacksAdapter: AttacksAdapter
    private lateinit var binding: FragmentAttacksFullscreenBinding
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private val pokeViewModel: PokeViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application, sharedViewModel)
    }
    private val teamsViewModel: TeamBuilderViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application, sharedViewModel)
    }
    private val args: FullScreenAttacksFragmentArgs by navArgs()
    private lateinit var allAttackDetails: List<PkMoveVersionGroupDetail>

    private var isSelectionMode = false
    private var selectedAttackList: List<AttacksData> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        allAttackDetails = when (val pkData = pokeViewModel.clickedPokemon.value) {
            is UIState.Success -> pkData.data.versionGroupDetails
            else -> emptyList()
        }
        isSelectionMode = args.isSelectionMode
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAttacksFullscreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.floatingActionButton.visibility = View.GONE
        val pkData = if (!isSelectionMode) pokeViewModel.clickedPokemon.value else teamsViewModel.clickedPokemon.value
        val pokemonName = if (!isSelectionMode) pokeViewModel.getTranslatedName() else teamsViewModel.getTranslatedName()
        if (pkData != null) {
            (activity as AppCompatActivity).supportActionBar?.title =
                getString(R.string.show_attacks_placeholder, pokemonName)

            val chipGroup = binding.chipGroupAttacksPopup
            val rvList = binding.rvPopupAttackList

            // in selection mode there are no chips to filter for generations
            binding.scrollView.visibility = if (isSelectionMode) View.GONE else View.VISIBLE
            binding.tvLevel.visibility =
                if (isSelectionMode) View.GONE else View.VISIBLE // hide level for team builder
            binding.tvChooseAtkHeader.visibility = if (isSelectionMode) View.VISIBLE else View.GONE

            val typeNames = pokeViewModel.pokemonTypeNames

            if (!isSelectionMode) {
                attacksAdapter = AttacksAdapter(typeNames, showLevel = true, showPosition = false)
                setChipsForEachGenAndListener(chipGroup, allAttackDetails) // this submits the attacks also

            } else {
                selectedAttackList = teamsViewModel.teamBuilderSelectedAttacks.value ?: emptyList()
                attacksAdapter =
                    AttacksAdapter(typeNames, showLevel = false, selectAttackEnabled = true) { selectedAttacks ->
                        updateAttacksSelectedCount(selectedAttacks)
                    }
                val attacksList = teamsViewModel.getAttacksFromNewestVersion()
                attacksAdapter.submitList(attacksList)
                attacksAdapter.selectAttacks(selectedAttackList)
            }
            rvList.adapter = attacksAdapter
        }

        binding.floatingActionButton.setOnClickListener {
            teamsViewModel.setSelectedAttacks(attacksAdapter.getSelectedAttacks())
            teamsViewModel.updatePokemonAttacks()
            findNavController().navigateUp()
        }
    }

    private fun updateAttacksSelectedCount(attacksSelectedCount: Int) {
        val text = if (attacksSelectedCount > 0) getString(
            R.string.choose_your_attacks_selected,
            attacksSelectedCount
        ) else ""

        binding.tvNrAttacksSelected.text = text
        if (attacksSelectedCount > 0) {
            binding.floatingActionButton.visibility = View.VISIBLE
        } else {
            binding.floatingActionButton.visibility = View.GONE
        }
    }

    /**
     * Adds a chip button for each generation a Pokemon has moveData for and checks the first chip / submits list
     * each Chip filters when clicked the Attacks from the Generation the Chip stands for
     *
     * @param allAttacks list of all Attacks a pokemon has to give each chip a on click listener (to call the function)
     * @param chipGroup the chipGroup to add the chips to
     */
    private fun setChipsForEachGenAndListener(
        chipGroup: ChipGroup,
        allAttacks: List<PkMoveVersionGroupDetail>,
    ) {
        // set of all versions a pokemon has data for
        val versionGroupSet = allAttacks.map { it.versionGroupId }.toSet()
        val generationNrs = versionGroupSet.map {
            pokeViewModel.getGenerationOfVersion(it ?: 0)
        }.filterNot { it == 0 }.toSet().toList()


        generationNrs.forEachIndexed { index, _ ->
            Chip(requireContext()).apply {
                text = getString(R.string.generation_nr_placeholder, generationNrs[index])
                isCheckable = true
                isClickable = true
                chipBackgroundColor = ContextCompat.getColorStateList(requireContext(), R.color.chip_on_primary)
                setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.chip_on_primary_text))
                tag =
                    generationNrs[index] // use the number of the gen as tag to call viewmodel gen filter function on click
                setOnCheckedChangeListener { _, _ ->
                    val list = pokeViewModel.getFilteredAttacks(this.tag as Int)
                    attacksAdapter.submitList(list) {
                        binding.rvPopupAttackList.scrollToPosition(0)
                    }
                }
                // checks first chip so list will be submitted due to onCheckedChangeListener
                chipGroup.addView(this)
                if (index == 0) chipGroup.check(this.id)
            }

        }
        // makes sure 1 chip is at least checked all the time
        var lastCheckedId = chipGroup.checkedChipId
        chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isEmpty()) {
                group.check(lastCheckedId)
            } else {
                lastCheckedId = checkedIds[0]
            }
        }
    }


}