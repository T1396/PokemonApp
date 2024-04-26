package com.example.pokinfo.ui.abilities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.pokinfo.R
import com.example.pokinfo.adapter.abilities.AbilityListAdapter
import com.example.pokinfo.data.util.AbilityFilter
import com.example.pokinfo.databinding.FragmentAbilitiesListBinding
import com.example.pokinfo.ui.misc.SkeletonConf
import com.example.pokinfo.viewModels.AbilityViewModel
import com.faltenreich.skeletonlayout.Skeleton
import com.faltenreich.skeletonlayout.applySkeleton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup


class AbilitiesListFragment : Fragment() {

    private var _binding: FragmentAbilitiesListBinding? = null
    private val binding get() = _binding!!
    private val abilityViewModel: AbilityViewModel by activityViewModels()
    private lateinit var adapter: AbilityListAdapter
    private lateinit var skeleton: Skeleton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentAbilitiesListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        abilityViewModel.getAllAbilities()

        adapter = AbilityListAdapter { abilityId ->
            // on Ability clicked load details and navigate
            abilityViewModel.getAbilityDetail(abilityId)
            findNavController().navigate(
                AbilitiesListFragmentDirections.actionNavAbilitiesToAbilitiesDetailFragment(
                    id
                )
            )
        }
        binding.rvAbilities.adapter = adapter
        showSkeleton()
        createFilterChips(binding.chipGroupFilter)

        binding.tietSearchAbility.addTextChangedListener { text ->
            abilityViewModel.setSearchInput(text.toString())
        }

        abilityViewModel.filteredAbilityList.observe(viewLifecycleOwner) { filteredList ->
            adapter.submitList(filteredList)
            onDataLoaded()
        }

    }

    private fun onDataLoaded() {
        // hides the skeleton and shows the loaded data
        skeleton.showOriginal()
    }



    private fun createFilterChips(chipGroup: ChipGroup) {
        if (chipGroup.childCount == 0) { // only create filter chips if no chips are there (if navigated back for example)
            val list = AbilityFilter.entries
            list.forEach { filter ->

                val chip = Chip(chipGroup.context).apply {
                    text = filter.filterName
                    isClickable = true
                    setPadding(8, 8, 8, 8)
                    isCheckable = true
                    isFocusable = true
                    tag =
                        filter // filter is tagged to the chip so it can be used for filter function call
                }
                // on click listener
                chip.setOnCheckedChangeListener { chip1, isChecked ->
                    abilityViewModel.selectAbilitiesFilter(chip1.tag as AbilityFilter, isChecked)
                }
                chipGroup.addView(chip)
            }
            chipGroup.isSingleSelection = true
        }
    }

    private fun showSkeleton() {
        skeleton = binding.rvAbilities.applySkeleton(
            R.layout.item_skeleton,
            itemCount = 9,
            config = SkeletonConf.darkMode
        )
        skeleton.showSkeleton()
    }



}