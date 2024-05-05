package com.example.pokinfo.ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
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
import com.example.pokinfo.data.models.database.pokemon.PkMoveVersionGroupDetail
import com.example.pokinfo.data.models.firebase.AttacksData
import com.example.pokinfo.data.util.UIState
import com.example.pokinfo.databinding.FragmentAttacksFullscreenBinding
import com.example.pokinfo.viewModels.PokeViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.color.MaterialColors

class FullScreenAttacksFragment : Fragment() {

    private lateinit var attacksAdapter: AttacksAdapter
    private lateinit var binding: FragmentAttacksFullscreenBinding
    private val pokeViewModel: PokeViewModel by activityViewModels()
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

        val pkData = pokeViewModel.clickedPokemon.value
        val pokemonName = pokeViewModel.getTranslatedName()
        if (pkData != null) {
            (activity as AppCompatActivity).supportActionBar?.title =
                getString(R.string.show_attacks_placeholder, pokemonName)

            val chipGroup = binding.chipGroupAttacksPopup
            val rvList = binding.rvPopupAttackList

            // in selection mode there are no chips to filter
            binding.scrollView.visibility = if (isSelectionMode) View.GONE else View.VISIBLE
            binding.tvLevel.visibility =
                if (isSelectionMode) View.GONE else View.VISIBLE // hide level for team builder
            binding.tvChooseAtkHeader.visibility = if (isSelectionMode) View.VISIBLE else View.GONE

            val typeNames = pokeViewModel.pokemonTypeNames

            if (!isSelectionMode) {
                attacksAdapter = AttacksAdapter(typeNames, false)
                // not needed in selection mode
                setChipsForEachGenAndListener(
                    chipGroup,
                    allAttackDetails
                ) // this submits the attacks also

            } else {
                selectedAttackList = pokeViewModel.teamBuilderSelectedAttacks.value ?: emptyList()
                attacksAdapter =
                    AttacksAdapter(typeNames, true) { selectedAttacks ->
                        updateAttacksSelectedCount(selectedAttacks)
                    }
                val attacksList = pokeViewModel.getAttacksFromNewestVersion()
                attacksAdapter.submitList(attacksList)
                attacksAdapter.selectAttacks(selectedAttackList)
            }
            rvList.adapter = attacksAdapter
        }

        binding.floatingActionButton.setOnClickListener {
            val attacks = attacksAdapter.getSelectedAttacks()
            Log.d("selectedAttacksFragment", attacks.toString())
            pokeViewModel.setSelectedAttacks(attacks)
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
        val unselectedColor = MaterialColors.getColor(
            requireView(),
            com.google.android.material.R.attr.colorOnSurface
        )
        val selectedColor =
            MaterialColors.getColor(requireView(), com.google.android.material.R.attr.colorTertiary)
        val textColorSelected = MaterialColors.getColor(
            requireView(),
            com.google.android.material.R.attr.colorOnTertiary
        )
        val textColorUnselected =
            MaterialColors.getColor(requireView(), com.google.android.material.R.attr.colorSurface)

        val states = arrayOf(
            intArrayOf(android.R.attr.state_selected), // Zustand: ausgewählt
            intArrayOf(-android.R.attr.state_selected) // Zustand: nicht ausgewählt
        )

        val colors = intArrayOf(
            selectedColor,
            unselectedColor
        )
        val textColors = intArrayOf(
            textColorSelected, // Farbe für den ausgewählten Zustand
            textColorUnselected // Farbe für den nicht ausgewählten Zustand
        )

        generationNrs.forEachIndexed { index, _ ->
            Chip(requireContext()).apply {
                text = getString(R.string.generation_nr_placeholder, generationNrs[index])
                isClickable = true
                isFocusable = false
                isCheckable = true
                chipBackgroundColor = ColorStateList(states, colors)
                setTextColor(ColorStateList(states, textColors))
                tag =
                    generationNrs[index] // use the number of the gen as tag to call viewmodel gen filter function on click
                chipGroup.addView(this)

                setOnCheckedChangeListener { _, _ ->
                    val list = pokeViewModel.getFilteredAttacks(this.tag as Int)
                    attacksAdapter.submitList(list)
                }
                // checks first chip so list will be submitted due to onCheckedChangeListener
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