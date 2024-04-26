package com.example.pokinfo.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.pokinfo.R
import com.example.pokinfo.adapter.home.detail.AttacksAdapter
import com.example.pokinfo.data.maps.typeColorMap
import com.example.pokinfo.data.models.database.pokemon.PkMoveVersionGroupDetail
import com.example.pokinfo.data.models.firebase.AttacksData
import com.example.pokinfo.databinding.FragmentAttacksFullscreenBinding
import com.example.pokinfo.viewModels.PokeViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class FullScreenAttacksFragment : Fragment() {

    private lateinit var attacksAdapter: AttacksAdapter
    private lateinit var binding: FragmentAttacksFullscreenBinding
    private val pokeViewModel: PokeViewModel by activityViewModels()
    private val args: FullScreenAttacksFragmentArgs by navArgs()
    private lateinit var allAttackDetails: List<PkMoveVersionGroupDetail>

    private var isSelectionMode = false
    private lateinit var selectedAttackList: MutableList<AttacksData?>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        allAttackDetails = pokeViewModel.clickedPokemon.value?.versionGroupDetails ?: emptyList()
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

        val pkData = pokeViewModel.clickedPokemon.value
        val pokemonName = pokeViewModel.getTranslatedName()
        if (pkData != null) {
            (activity as AppCompatActivity).supportActionBar?.title =
                getString(R.string.show_attacks_placeholder, pokemonName)

            val chipGroup = binding.chipGroupAttacksPopup
            val rvList = binding.rvPopupAttackList

            // in selection mode there are no chips to filter
            binding.scrollView.visibility = if (isSelectionMode) View.GONE else View.VISIBLE
            binding.tvPlace.visibility = if (isSelectionMode) View.VISIBLE else View.GONE // adjust end space if selection mode
            binding.tvLevel.visibility = if (isSelectionMode) View.GONE else View.VISIBLE // hide level for team builder
            binding.tvChooseAtkHeader.visibility = if (isSelectionMode) View.VISIBLE else View.GONE

            pokeViewModel.pokemonTypeNames.observe(viewLifecycleOwner) { typeNames ->

                if (!isSelectionMode) {
                    attacksAdapter = AttacksAdapter(typeColorMap, typeNames, false)
                    // not needed in selection mode
                    setChipsForEachGenAndListener(
                        chipGroup,
                        allAttackDetails
                    ) // this submits the attacks also

                } else {
                    selectedAttackList = pokeViewModel.teamBuilderSelectedAttacks.value?.toMutableList() ?: MutableList(4) { null }
                    attacksAdapter =
                        AttacksAdapter(typeColorMap, typeNames, true) { selectedAttacks ->
                            manageSelectedAttacks(selectedAttacks)
                        }
                    val attacksList = pokeViewModel.getAttacksFromNewestVersion()
                    attacksAdapter.submitList(attacksList)
                    attacksAdapter.selectAttacks(selectedAttackList)
                }
                rvList.adapter = attacksAdapter
            }

            binding.floatingActionButton.setOnClickListener {
                if (::selectedAttackList.isInitialized) pokeViewModel.setSelectedAttacks(selectedAttackList)
                findNavController().navigateUp()
            }
        }

    }

    private fun manageSelectedAttacks(selectedAttacks: List<AttacksData>) {
        val nrAttacksSelected = selectedAttacks.size
        val text = if (nrAttacksSelected > 0) getString(R.string.choose_your_attacks_selected, nrAttacksSelected) else getString(R.string.choose_your_attacks)

        binding.tvChooseAtkHeader.text = text
        // if no attacks selected anymore, replace attackList with null
        if (selectedAttacks.isEmpty()) {
            binding.floatingActionButton.visibility = View.GONE
            selectedAttackList.replaceAll { null }
        } else {
            binding.floatingActionButton.visibility = View.VISIBLE
            selectedAttackList.forEachIndexed { index, _ ->
                val selectedAttack = selectedAttacks.getOrNull(index)
                if (selectedAttack != null) {
                    selectedAttackList[index] = selectedAttack
                } else {
                    selectedAttackList[index] = null
                }
            }
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
            val chip = Chip(requireContext())
            chip.text = getString(R.string.generation_nr_placeholder, generationNrs[index])
            chip.isClickable = true
            chip.isFocusable = false
            chip.isCheckable = true
            chip.tag = generationNrs[index] // use the number of the gen as tag to call viewmodel gen filter function on click
            chipGroup.addView(chip)

            chip.setOnCheckedChangeListener { _, _ ->
                val list = pokeViewModel.getFilteredAttacks(chip.tag as Int)
                attacksAdapter.submitList(list)
            }
            if (index == 0) chipGroup.check(chip.id) // checks first chip so list will be submitted due to onCheckedChangeListener
        }
        // makes sure 1 chip is at least checked all the time
        var lastCheckedId = -1
        chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isEmpty()) {
                group.check(lastCheckedId)
            } else {
                lastCheckedId = checkedIds[0]
            }
        }
    }


}