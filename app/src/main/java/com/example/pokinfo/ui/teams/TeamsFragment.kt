package com.example.pokinfo.ui.teams

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.pokinfo.R
import com.example.pokinfo.adapter.decoration.VerticalSpaceItemDecoration
import com.example.pokinfo.adapter.teamAndTeambuilder.TeamAdapterLarge
import com.example.pokinfo.data.enums.PokemonSortFilterState
import com.example.pokinfo.data.models.firebase.PokemonTeam
import com.example.pokinfo.databinding.FragmentTeamsBinding
import com.example.pokinfo.databinding.PopupDialogOptionsBinding
import com.example.pokinfo.databinding.PopupPublicTeamOptionsBinding
import com.example.pokinfo.databinding.PopupSharedTeamsOptionsBinding
import com.example.pokinfo.extensions.dpToPx
import com.example.pokinfo.ui.home.ThreeStateChip
import com.example.pokinfo.ui.teamBuilder.dialogs.EnterTeamNameDialogFragment
import com.example.pokinfo.viewModels.SharedViewModel
import com.example.pokinfo.viewModels.factory.ViewModelFactory
import com.example.pokinfo.viewModels.teambuilder.TeamBuilderViewModel
import com.example.pokinfo.viewModels.teams.TeamsViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlin.math.max

enum class TeamSortFilter(val stringRes: Int) {
    DATE(R.string.date_filter),
    LIKES(R.string.likes_filter),
    ALPHABETICAL(R.string.alphabetic_filter)
}


class TeamsFragment : Fragment(), EnterTeamNameDialogFragment.EnterTeamNameListener {
    private var _binding: FragmentTeamsBinding? = null
    private var teamType: TeamType = TeamType.MY_TEAMS
    private var menuProvider: MenuProvider? = null


    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val teamsViewModel: TeamsViewModel by activityViewModels()
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private val teamBuildViewModel: TeamBuilderViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application, sharedViewModel)
    }
    private lateinit var adapter: TeamAdapterLarge
    private var selectedTeamId: String = ""
    override fun onResume() {
        super.onResume()
        when (teamType) {
            TeamType.PUBLIC_TEAMS -> {
                teamsViewModel.listenForLikedTeams()
            }

            else -> {}
        }
        menuProvider?.let { existingProvider ->
            val menuHost: MenuHost = requireActivity()
            menuHost.removeMenuProvider(existingProvider)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val teamTypePosition = arguments?.getInt("position")
        teamType = when (teamTypePosition) {
            1 -> TeamType.SHARED_TEAMS
            2 -> TeamType.PUBLIC_TEAMS
            else -> TeamType.MY_TEAMS
        }

    }

    private fun createMenuProvider(): MenuProvider {
        return object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.main, menu)
                val sec = menu.findItem(R.id.action_secondary)
                sec.isVisible = (teamType == TeamType.PUBLIC_TEAMS)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                // Behandle Menüaktionen
                return when (menuItem.itemId) {
                    R.id.action_secondary -> {
                        true
                    }

                    else -> false
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        when (teamType) {
            TeamType.PUBLIC_TEAMS -> {
                teamsViewModel.stopListeningForLikedTeams()
            }

            else -> {}

        }
        menuProvider?.let { existingProvider ->
            val menuHost: MenuHost = requireActivity()
            menuHost.removeMenuProvider(existingProvider)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTeamsBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        teamsViewModel.fetchOwnTeams()

        binding.tvNoTeams.setText(teamType.noTeamsTextRes)
        adapter = TeamAdapterLarge(
            teamType = teamType,
            onItemLongClicked = ::showOptionsDependingOnMode,
            onCreatorClicked = ::showCreatorSheet,
            onLiked = teamsViewModel::incrementLikeCount,
            onLikeRemove = teamsViewModel::decrementLikeCount
        )

        if (teamType == TeamType.PUBLIC_TEAMS) {
            // fetch liked teams to display the liked ones in the public teams list
            teamsViewModel.likedTeams.observe(viewLifecycleOwner) { likedTeams ->
                adapter.setLikedTeams(likedTeams)
            }
            binding.filterScrollBar.visibility = View.VISIBLE
            TeamSortFilter.entries.forEach { filter ->
                ThreeStateChip(requireContext()).apply {
                    text = getString(filter.stringRes)
                    isCheckable = false
                    tag = filter

                    setOnClickListener { _ ->
                        binding.chipGroup.children.forEach { child ->
                            if (child != this) {
                                (child as? ThreeStateChip)?.resetState()
                            } else {
                                (child as? ThreeStateChip)?.nextState()
                            }
                        }
                        teamsViewModel.selectFilterAndState(tag as TeamSortFilter, this.state)
                    }
                    binding.chipGroup.addView(this)
                    if (filter == TeamSortFilter.LIKES) {
                        this.state = PokemonSortFilterState.DESCENDING
                    }
                }
            }
        }


        binding.rvTeams.adapter = adapter
        binding.rvTeams.addItemDecoration(VerticalSpaceItemDecoration(requireContext(), 8))
        val teamLiveData = when (teamType) {
            TeamType.MY_TEAMS -> teamsViewModel.ownPokemonTeams
            TeamType.SHARED_TEAMS -> teamsViewModel.sharedPokemonTeams
            TeamType.PUBLIC_TEAMS -> teamsViewModel.publicPokemonTeams
        }
        teamLiveData.observe(viewLifecycleOwner) { teams ->
            updateRecyclerView(teams, adapter, binding)
        }

        when (teamType) {
            TeamType.MY_TEAMS -> {
                binding.swipeRefresh.isEnabled = false
            }

            TeamType.SHARED_TEAMS -> {
                binding.swipeRefresh.setOnRefreshListener {
                    teamsViewModel.fetchSharedTeams()
                }
            }

            TeamType.PUBLIC_TEAMS -> {

                binding.swipeRefresh.setOnRefreshListener {
                    teamsViewModel.fetchPublicPokemonTeams {
                        binding.swipeRefresh.isRefreshing = false
                    }
                }
                binding.rvTeams.setPadding(0, dpToPx(requireContext(), 48), 0, 0)

               binding.rvTeams.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)

                        // Berechne das neue Alpha basierend auf der aktuellen Scroll-Position
                        val scrolledOffset = recyclerView.computeVerticalScrollOffset()
                        val filterBarHeight = binding.filterScrollBar.height
                        val alpha = max(0f, 1f - (scrolledOffset / filterBarHeight.toFloat()))

                        // Setze das Alpha der HorizontalScrollView
                        binding.filterScrollBar.alpha = alpha

                        // Setze die Sichtbarkeit basierend auf dem Alpha-Wert
                        binding.filterScrollBar.visibility = if (alpha > 0) View.VISIBLE else View.INVISIBLE
                    }
                })

            }
        }

    }

    private fun showOptionsDependingOnMode(
        team: PokemonTeam,
        displayMode: TeamType,
    ) {
        when (displayMode) {
            TeamType.MY_TEAMS -> showOptionsForMyTeams(team)
            TeamType.SHARED_TEAMS -> showOptionsForSharedTeams(team)
            TeamType.PUBLIC_TEAMS -> showOptionsForPublicTeams(team)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateRecyclerView(
        teams: List<PokemonTeam>,
        adapter: TeamAdapterLarge,
        binding: FragmentTeamsBinding,
    ) {

        adapter.submitList(teams) {
            binding.rvTeams.post {
                binding.rvTeams.smoothScrollToPosition(0)
            }
        }
        if (teams.isNotEmpty()) {
            binding.rvTeams.visibility = View.VISIBLE
            binding.tvNoTeams.visibility = View.GONE
        } else {
            binding.rvTeams.visibility = View.GONE
            binding.tvNoTeams.visibility = View.VISIBLE
        }
    }

    /**
     * Opens a dialog to ask the user what action to do with the clicked team
     */
    private fun showOptionsForMyTeams(pokemonTeam: PokemonTeam) {
        selectedTeamId = pokemonTeam.id
        val binding = PopupDialogOptionsBinding.inflate(layoutInflater)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .show()

        binding.changeNameText.setOnClickListener {
            showEnterTeamNameDialog(pokemonTeam.isPublic, pokemonTeam.id, pokemonTeam.name)
            dialog.dismiss()
        }
        binding.editText.setOnClickListener {
            openTeamBuilderWithPokemonTeam(pokemonTeam, true)
            dialog.dismiss()
        }
        binding.deleteText.setOnClickListener {
            showDeleteConfirmation(pokemonTeam)
            dialog.dismiss()
        }
        binding.shareText.setOnClickListener {
            showShareTeamDialog(pokemonTeam)
            dialog.dismiss()
        }

    }

    private fun showOptionsForPublicTeams(team: PokemonTeam) {
        val binding = PopupPublicTeamOptionsBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .show()

        binding.tvShowAndEdit.setOnClickListener {
            openTeamBuilderWithPokemonTeam(team, false)
            dialog.dismiss()
        }

        binding.tvCopyAndSave.setOnClickListener {
            showCopyTeamDialog(team)
            dialog.dismiss()
        }
    }


    /**
     * Opens a dialog to ask the user what action to do with his long clicked team
     */
    private fun showOptionsForSharedTeams(pokemonTeam: PokemonTeam) {
        val binding = PopupSharedTeamsOptionsBinding.inflate(layoutInflater)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .show()

        binding.tvCopy.setOnClickListener {
            showCopyTeamDialog(pokemonTeam)
            dialog.dismiss()
        }
        binding.tvRemoveAccess.setOnClickListener {
            showRemoveAccessDialog(pokemonTeam)
            dialog.dismiss()
        }
    }


    private fun showCreatorSheet(creatorId: String) {
        val fragment = CreatorSheetFragment().apply {
            arguments = Bundle().apply {
                putString("creatorId", creatorId)
            }
        }
        fragment.show(parentFragmentManager, "creatorSheet")
    }

    /** Opens a sheet and lets the user choose different user to share his team with */
    private fun showShareTeamDialog(pokemonTeam: PokemonTeam) {
        val dialogFragment = ShareTeamSheet.newInstance(pokemonTeam, teamType)
        dialogFragment.show(parentFragmentManager, "shareTeamDialog")
    }


    /** Shows a dialog which asks the user to enter a name for the team, or to enter a new name for the team when updated */
    private fun showEnterTeamNameDialog(isPublic: Boolean, teamId: String, name: String) {
        EnterTeamNameDialogFragment.newInstance(isPublic, teamId, name)
            .show(childFragmentManager, "EnterTeamNameDialog")
    }

    /** Creates a copy of the clicked team, inserts it to firestore and navigates to the my Teams tab */
    private fun showCopyTeamDialog(pokemonTeam: PokemonTeam) {
        MaterialAlertDialogBuilder(requireContext()).apply {
            setTitle(getString(R.string.copy_team_confirmation))
            setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            setPositiveButton(getString(R.string.copy_edit)) { dialog, _ ->
                teamsViewModel.insertTeamCopyToFireStore(pokemonTeam) {
                    teamsViewModel.fetchOwnTeams()
                    // leads to tab switch
                    teamsViewModel.setTeamDisplayMode(TeamType.MY_TEAMS)
                    dialog.dismiss()
                }
            }
        }.show()
    }

    /** Lets user remove a shared team from his list */
    private fun showRemoveAccessDialog(pokemonTeam: PokemonTeam) {

        MaterialAlertDialogBuilder(requireContext()).apply {
            setTitle(getString(R.string.remove_access_confirmation, pokemonTeam.creator))
            setMessage(getString(R.string.remove_access_text))
            setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            setPositiveButton(getString(R.string.delete)) { dialog, _ ->
                teamsViewModel.removeAccessToPokemonTeam(pokemonTeam)
                teamsViewModel.fetchSharedTeams()
                dialog.dismiss()
            }
        }.show()
    }


    private fun openTeamBuilderWithPokemonTeam(
        pokemonTeam: PokemonTeam,
        editMode: Boolean,
    ) {
        teamBuildViewModel.setNewTeamIndex(0)
        findNavController().navigate(
            TeamsHostFragmentDirections.actionNavTeamsHostToNavTeambuilder(
                pokemonTeam.copy(),
                editMode
            )
        )
    }


    private fun showDeleteConfirmation(pokemonTeam: PokemonTeam) {
        MaterialAlertDialogBuilder(requireContext()).apply {
            setTitle(getString(R.string.delete_team_confirmation))
            setMessage(getString(R.string.delete_team_confirmation))
            setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            setPositiveButton(getString(R.string.delete)) { dialog, _ ->
                teamsViewModel.deletePokemonTeam(pokemonTeam)
                teamsViewModel.fetchOwnTeams()
                dialog.dismiss()
            }
        }.show()
    }

    override fun onTeamNameEntered(name: String, isPublic: Boolean, teamId: String) {
        teamsViewModel.updateTeamNameAndPublicity(name, isPublic, teamId)
        teamsViewModel.fetchOwnTeams()
    }


}