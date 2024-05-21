package com.example.pokinfo.ui.abilities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.transition.TransitionManager
import com.example.pokinfo.R
import com.example.pokinfo.adapter.abilities.AbilityListAdapter
import com.example.pokinfo.data.enums.AbilityFilter
import com.example.pokinfo.databinding.FragmentAbilitiesListBinding
import com.example.pokinfo.ui.Extensions.animations.showOrHideChipGroupAnimated
import com.example.pokinfo.ui.misc.SkeletonConf
import com.example.pokinfo.viewModels.AbilityViewModel
import com.example.pokinfo.viewModels.SharedViewModel
import com.example.pokinfo.viewModels.factory.ViewModelFactory
import com.faltenreich.skeletonlayout.Skeleton
import com.faltenreich.skeletonlayout.applySkeleton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup


class AbilitiesListFragment : Fragment() {

    private var _binding: FragmentAbilitiesListBinding? = null
    private val binding get() = _binding!!
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private val abilityViewModel: AbilityViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application, sharedViewModel)
    }
    private lateinit var adapter: AbilityListAdapter
    private lateinit var skeleton: Skeleton
    private var isFilterBarExpanded = false
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

        val swipeRefreshLayout = binding.abilitiesRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {
            if (abilityViewModel.isLoading.value == false) {
                abilityViewModel.getAllAbilities()
                swipeRefreshLayout.isRefreshing = false
            } else {
                swipeRefreshLayout.isRefreshing = false
            }

        }

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

        abilityViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) showSkeleton() else skeleton.showOriginal()
        }
        createFilterChips(binding.chipGroupFilter)

        binding.tietSearchAbility.addTextChangedListener { text ->
            abilityViewModel.setSearchInput(text.toString())
        }

        abilityViewModel.filteredAbilityList.observe(viewLifecycleOwner) { filteredList ->
            if (filteredList != null) {
                adapter.submitList(filteredList) {
                    binding.rvAbilities.scrollToPosition(0)
                }
            }
        }


        binding.tilSearchField.setEndIconOnClickListener {
            isFilterBarExpanded = !isFilterBarExpanded
            TransitionManager.beginDelayedTransition(binding.topBarAbility)
            showOrHideChipGroupAnimated(binding.scrollViewChips, isFilterBarExpanded)
        }

    }

    private fun createFilterChips(chipGroup: ChipGroup) {
        if (chipGroup.childCount == 0) { // only create filter chips if no chips are there (if navigated back for example)
            val list = AbilityFilter.entries
            list.forEach { filter ->

                val chip = Chip(chipGroup.context).apply {
                    text = filter.filterName
                    isClickable = true
                    isCheckable = true
                    chipBackgroundColor = ContextCompat.getColorStateList(requireContext(), R.color.chip_on_primary)
                    setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.chip_on_primary_text))
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
            config = SkeletonConf.whiteMode
        )
        skeleton.showSkeleton()
    }


}