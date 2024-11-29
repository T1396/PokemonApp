package com.example.pokinfo.ui.attacks

import android.content.res.Configuration
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.transition.TransitionManager
import com.example.pokinfo.R
import com.example.pokinfo.adapter.home.detail.AttacksAdapter
import com.example.pokinfo.data.enums.AttackGenerationFilter
import com.example.pokinfo.data.enums.AttackTypeFilter
import com.example.pokinfo.data.models.database.pokemon.PokemonTypeName
import com.example.pokinfo.databinding.FragmentAttacksListBinding
import com.example.pokinfo.ui.Extensions.animations.showOrHideChipGroupAnimated
import com.example.pokinfo.ui.misc.SkeletonConf
import com.example.pokinfo.viewModels.AttacksViewModel
import com.example.pokinfo.viewModels.SharedViewModel
import com.example.pokinfo.viewModels.factory.ViewModelFactory
import com.faltenreich.skeletonlayout.Skeleton
import com.faltenreich.skeletonlayout.applySkeleton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup


class AttacksListFragment : Fragment() {

    private var _binding: FragmentAttacksListBinding? = null
    private val binding get() = _binding!!
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private val attacksViewModel: AttacksViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application, sharedViewModel)
    }
    private lateinit var adapter: AttacksAdapter
    private var layoutManagerState: Parcelable? = null // can save scrolling state
    private var isExpanded = false
    private var skeleton: Skeleton? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentAttacksListBinding.inflate(inflater, container, false)
        attacksViewModel.loadGenericData()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val swipeRefreshLayout = binding.attacksRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {
            if (attacksViewModel.isLoading.value == false) {
                attacksViewModel.loadAllAttacks()
                swipeRefreshLayout.isRefreshing = false
            } else {
                swipeRefreshLayout.isRefreshing = false
            }
        }

        attacksViewModel.pokeTypeNames.observe(viewLifecycleOwner) { typeNames ->
            setupListAdapter(typeNames)
        }

        attacksViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) showSkeleton() else skeleton?.showOriginal()
        }

        createFilterChips(binding.chipGroupFilter)
        createSecondFilterGroup(binding.chipGroupTypeFilter)

        attacksViewModel.filteredAttackList.observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }

        attacksViewModel.selectedAttackFilter.observe(viewLifecycleOwner) {
            updateFilterChips(it)
        }

        attacksViewModel.selectedAttackTypeFilter.observe(viewLifecycleOwner) {
            updateTypeFilterChips(it)
        }

        binding.tietSearch.addTextChangedListener { text ->
            attacksViewModel.setSearchInputAttacks(text.toString())
        }
        binding.textInputLayout.setEndIconOnClickListener {
            isExpanded = !isExpanded
            TransitionManager.beginDelayedTransition(binding.topBarLayout)
            showOrHideChipGroupAnimated(binding.scrollViewChips, isExpanded)
            showOrHideChipGroupAnimated(binding.scrollViewChips2, isExpanded)
        }
    }

    private fun setupListAdapter(typeNames: List<PokemonTypeName>) {
        adapter = AttacksAdapter(
            pokemonTypeNames = typeNames,
            showPosition = true,
            onAttackClicked = { attackId ->
                attacksViewModel.loadSingleAttackDetail(attackId)
                findNavController().navigate(
                    AttacksListFragmentDirections.actionNavAttacksToAttacksDetailFragment(
                        id
                    )
                )
            }
        )
        binding.rvAttackList.setHasFixedSize(true)
        binding.rvAttackList.adapter = adapter
    }

    override fun onPause() {
        super.onPause()
        layoutManagerState = binding.rvAttackList.layoutManager?.onSaveInstanceState()
    }

    override fun onResume() {
        super.onResume()
        layoutManagerState?.let {
            binding.rvAttackList.layoutManager?.onRestoreInstanceState(it)
        }
    }

    private fun createFilterChips(chipGroup: ChipGroup) {
        if (chipGroup.childCount == 0) { // only create filter chips if no chips are there (if navigated back for example)
            val list = AttackGenerationFilter.entries
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
                chip.setOnCheckedChangeListener { chip1, isChecked ->
                    attacksViewModel.selectAttacksFilter(chip1.tag as AttackGenerationFilter, isChecked)
                }
                chipGroup.addView(chip)
            }
            chipGroup.isSingleSelection = true
        }
    }

    private fun updateFilterChips(selectedFilter: AttackGenerationFilter?) {
        binding.chipGroupFilter.children.forEach { view ->
            (view as? Chip)?.let { chip ->
                chip.isChecked = chip.tag == selectedFilter
            }
        }
    }

    private fun createSecondFilterGroup(chipGroup: ChipGroup) {
        if (chipGroup.childCount == 0) {
            val list = AttackTypeFilter.entries
            list.forEach { filter ->
                val chip = Chip(chipGroup.context).apply {
                    text = filter.filterName
                    isClickable = true
                    isCheckable = true
                    chipBackgroundColor = ContextCompat.getColorStateList(requireContext(), R.color.chip_on_primary)
                    setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.chip_on_primary_text))
                    tag = filter
                }

                chip.setOnCheckedChangeListener { createdChip, isChecked ->
                    attacksViewModel.selectSecondAttackFilter(
                        createdChip.tag as AttackTypeFilter,
                        isChecked
                    )
                }
                chipGroup.addView(chip)
            }
            chipGroup.isSingleSelection = true
        }
    }

    /** */
    private fun updateTypeFilterChips(selectedFilter: AttackTypeFilter?) {
        binding.chipGroupTypeFilter.children.forEach { view ->
            (view as? Chip)?.let { chip ->
                chip.isChecked = chip.tag == selectedFilter
            }
        }
    }



    private fun showSkeleton() {
        val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val isNightMode = nightModeFlags == Configuration.UI_MODE_NIGHT_YES

        val config = if (isNightMode) SkeletonConf.darkMode else SkeletonConf.whiteMode
        skeleton = binding.rvAttackList.applySkeleton(
            R.layout.item_skeleton,
            itemCount = 9,
            config = config
        )
        skeleton?.showSkeleton()

    }
}

