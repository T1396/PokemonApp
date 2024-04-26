package com.example.pokinfo.ui.attacks

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.pokinfo.R
import com.example.pokinfo.adapter.attacks.AttacksListAdapter
import com.example.pokinfo.data.util.AttackFilter
import com.example.pokinfo.data.util.AttackFilter2
import com.example.pokinfo.databinding.FragmentAttacksListBinding
import com.example.pokinfo.ui.misc.SkeletonConf
import com.example.pokinfo.viewModels.AttacksViewModel
import com.example.pokinfo.viewModels.PokeViewModel
import com.faltenreich.skeletonlayout.Skeleton
import com.faltenreich.skeletonlayout.applySkeleton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup


class AttacksListFragment : Fragment() {

    private var _binding: FragmentAttacksListBinding? = null
    private val binding get() = _binding!!
    private val pokeViewModel: PokeViewModel by activityViewModels()
    private val attacksViewModel: AttacksViewModel by activityViewModels()
    private lateinit var adapter: AttacksListAdapter
    private var layOutManagerState: Parcelable? = null // can save scrolling state
    private var isExpanded = false
    private lateinit var skeleton: Skeleton
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentAttacksListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        attacksViewModel.loadAllAttacks(attacksViewModel.getLangId())

        adapter = AttacksListAdapter(
            pokeViewModel.getTypeColorMap(), emptyList()
        ) { id ->
            // when clicked
            attacksViewModel.loadSingleAttackDetail(id)
            findNavController()
                .navigate(AttacksListFragmentDirections.actionNavAttacksToAttacksDetailFragment(id))
        }
        binding.rvAttackList.adapter = adapter
        showSkeleton()

        pokeViewModel.pokemonTypeNames.observe(viewLifecycleOwner) {
            adapter.setTypeNames(pokemonTypeNames = it)
        }

        createFilterChips(binding.chipGroupFilter)
        createSecondFilterGroup(binding.chipGroupTypeFilter)

        attacksViewModel.filteredAttackList.observe(viewLifecycleOwner) {
            adapter.submitList(it)
            onDataLoaded()
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

        binding.ibExpandFilter.setOnClickListener {
            isExpanded = !isExpanded
            animateFilterEntrance(isExpanded)
        }
    }

    private fun onDataLoaded() {
        // hides the skeleton and shows the loaded data
        skeleton.showOriginal()
    }

    override fun onPause() {
        super.onPause()
        layOutManagerState = binding.rvAttackList.layoutManager?.onSaveInstanceState()
    }

    override fun onResume() {
        super.onResume()
        layOutManagerState?.let {
            binding.rvAttackList.layoutManager?.onRestoreInstanceState(it)
        }
    }

    private fun createFilterChips(chipGroup: ChipGroup) {
        if (chipGroup.childCount == 0) { // only create filterchips if no chips are there (if navigated back for example)
            val list = AttackFilter.entries
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
                chip.setOnCheckedChangeListener { chip1, isChecked ->
                    attacksViewModel.selectAttacksFilter(chip1.tag as AttackFilter, isChecked)
                }
                chipGroup.addView(chip)
            }
            chipGroup.isSingleSelection = false // multiple filters can be enabled
        }
    }

    private fun updateFilterChips(selectedFilter: AttackFilter?) {
        binding.chipGroupFilter.children.forEach { view ->
            (view as? Chip)?.let { chip ->
                chip.isChecked = chip.tag == selectedFilter
            }
        }
    }

    private fun createSecondFilterGroup(chipGroup: ChipGroup) {
        if (chipGroup.childCount == 0) {
            val list = AttackFilter2.entries
            list.forEach { filter ->
                val chip = Chip(chipGroup.context).apply {
                    text = filter.filterName
                    isClickable = true
                    setPadding(8, 8, 8, 8)
                    isCheckable = true
                    isFocusable = true
                    tag = filter
                }

                chip.setOnCheckedChangeListener { createdChip, isChecked ->
                    attacksViewModel.selectSecondAttackFilter(
                        createdChip.tag as AttackFilter2,
                        isChecked
                    )
                }
                chipGroup.addView(chip)
            }
            chipGroup.isSingleSelection = true
        }
    }

    /** */
    private fun updateTypeFilterChips(selectedFilter: AttackFilter2?) {
        binding.chipGroupTypeFilter.children.forEach { view ->
            (view as? Chip)?.let { chip ->
                chip.isChecked = chip.tag == selectedFilter
            }
        }
    }

    private fun animateFilterEntrance(
        isExpanded: Boolean,
    ) {
        if (isExpanded) {
            // Alpha-Animation für beide ScrollViews
            val fadeInChips =
                ObjectAnimator.ofFloat(binding.scrollViewChips, "alpha", 0f, 1f)
            fadeInChips.duration = 500 // Dauer in Millisekunden

            val fadeInChips2 =
                ObjectAnimator.ofFloat(binding.scrollViewChips2, "alpha", 0f, 1f)
            fadeInChips2.duration = 500

            // Translation-Animation für beide ScrollViews (vertikal von oben nach unten)
            val slideInChips = ObjectAnimator.ofFloat(
                binding.scrollViewChips,
                "translationY",
                -binding.scrollViewChips.height.toFloat(),
                0f
            )
            slideInChips.duration = 500

            val slideInChips2 = ObjectAnimator.ofFloat(
                binding.scrollViewChips2,
                "translationY",
                -binding.scrollViewChips2.height.toFloat(),
                0f
            )
            slideInChips2.duration = 500

            // Sichtbarkeit setzen und Animationen starten
            binding.scrollViewChips.visibility = View.VISIBLE
            binding.scrollViewChips2.visibility = View.VISIBLE
            binding.ibExpandFilter.rotation = 180f

            val animatorSet = AnimatorSet()
            animatorSet.playTogether(fadeInChips, fadeInChips2, slideInChips, slideInChips2)
            animatorSet.start()
        } else {
            // Alpha-Animation für beide ScrollViews
            val fadeOutChips =
                ObjectAnimator.ofFloat(binding.scrollViewChips, "alpha", 1f, 0f)
            fadeOutChips.duration = 500

            val fadeOutChips2 =
                ObjectAnimator.ofFloat(binding.scrollViewChips2, "alpha", 1f, 0f)
            fadeOutChips2.duration = 500

            // translation from top to bottom
            val slideOutChips = ObjectAnimator.ofFloat(
                binding.scrollViewChips,
                "translationY",
                0f,
                -binding.scrollViewChips.height.toFloat()
            )
            slideOutChips.duration = 500
            val slideOutChips2 = ObjectAnimator.ofFloat(
                binding.scrollViewChips2,
                "translationY",
                0f,
                -binding.scrollViewChips2.height.toFloat()
            )
            slideOutChips2.duration = 500

            val animatorSet = AnimatorSet()
            animatorSet.playTogether(
                fadeOutChips,
                fadeOutChips2,
                slideOutChips,
                slideOutChips2
            )

            animatorSet.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    binding.scrollViewChips.visibility = View.GONE
                    binding.scrollViewChips2.visibility = View.GONE
                }
            })

            binding.ibExpandFilter.rotation = 0f
            animatorSet.start() // Start the Animation
        }
    }



    private fun showSkeleton() {
        skeleton = binding.rvAttackList.applySkeleton(
            R.layout.item_skeleton,
            itemCount = 9,
            config = SkeletonConf.darkMode
        )
        skeleton.showSkeleton()

    }
}

