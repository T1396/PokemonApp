package com.example.pokinfo.ui.teams

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.pokinfo.R
import com.example.pokinfo.adapter.teamAndTeambuilder.PokemonTeamAdapter
import com.example.pokinfo.adapter.teamAndTeambuilder.UserRowAdapter
import com.example.pokinfo.data.models.firebase.PokemonTeam
import com.example.pokinfo.databinding.FragmentTeamsBinding
import com.example.pokinfo.viewModels.FirebaseViewModel
import com.example.pokinfo.viewModels.teambuilder.TeamBuilderViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class TeamsFragment : Fragment() {
    private var _binding: FragmentTeamsBinding? = null
    private var teamType: TeamType = TeamType.MY_TEAMS


    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val fireBaseViewModel: FirebaseViewModel by activityViewModels()
    private val teamBuildViewModel: TeamBuilderViewModel by activityViewModels()


    fun setTeamType(teamType: TeamType) {
        this.teamType = teamType
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
        val displayMode = teamType
        val text = when (displayMode) {
            TeamType.SHARED_TEAMS -> R.string.no_shared_teams
            TeamType.PUBLIC_TEAMS -> R.string.no_public_teams
            else -> R.string.error
        }
        binding.tvNoTeams.setText(text)
        val adapter = PokemonTeamAdapter {
            when (displayMode) {
                TeamType.MY_TEAMS -> showOptionsForMyTeams(it)
                TeamType.SHARED_TEAMS -> showTeamOptionsDialog(it)
                TeamType.PUBLIC_TEAMS -> { }
            }
        }
        binding.rvTeams.adapter = adapter
        val teamLiveData = when (displayMode) {
            TeamType.MY_TEAMS -> fireBaseViewModel.ownPokemonTeams
            TeamType.SHARED_TEAMS -> fireBaseViewModel.sharedPokemonTeams
            TeamType.PUBLIC_TEAMS -> fireBaseViewModel.publicPokemonTeams
        }
        teamLiveData.observe(viewLifecycleOwner) { teams ->
            updateRecyclerView(teams, adapter, binding)
        }

    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Opens a dialog to ask the user what action to do with the clicked team
     */
    private fun showTeamOptionsDialog(pokemonTeam: PokemonTeam) {
        val layoutInflater = LayoutInflater.from(requireContext())
        val view = layoutInflater.inflate(R.layout.popup_shared_teams_options, null)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(view)
            .show()

        val copyTv = view.findViewById<TextView>(R.id.tvCopy)
        val removeAccessTv = view.findViewById<TextView>(R.id.tvRemoveAccess)

        copyTv.setOnClickListener {
            showCopyTeamDialog(pokemonTeam)
            dialog.dismiss()
        }
        removeAccessTv.setOnClickListener {
            showRemoveAccessDialog(pokemonTeam)
            dialog.dismiss()
        }
    }

    private fun showCopyTeamDialog(pokemonTeam: PokemonTeam) {
        MaterialAlertDialogBuilder(requireContext()).apply {
            setTitle(getString(R.string.copy_team_confirmation))
            setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            setPositiveButton(getString(R.string.copy_edit)) { dialog, _ ->
                fireBaseViewModel.insertTeamToFireStore(pokemonTeam) {
                    fireBaseViewModel.setTeamDisplayMode(TeamType.MY_TEAMS)
                    dialog.dismiss()
                }
            }
        }.show()
    }

    private fun showRemoveAccessDialog(pokemonTeam: PokemonTeam) {

        MaterialAlertDialogBuilder(requireContext()).apply {
            setTitle(getString(R.string.remove_access_confirmation))
            setMessage(getString(R.string.remove_access_text))
            setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            setPositiveButton(getString(R.string.delete)) { dialog, _ ->
                fireBaseViewModel.removeAccessToPokemonTeam(pokemonTeam)
                dialog.dismiss()
            }
        }.show()
    }

    /**
     * Opens a dialog to ask the user what action to do with the clicked team
     */
    private fun showOptionsForMyTeams(pokemonTeam: PokemonTeam) {
        val layoutInflater = LayoutInflater.from(requireContext())
        val view = layoutInflater.inflate(R.layout.popup_dialog_options, null)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(view)
            .show()

        val edit = view.findViewById<TextView>(R.id.editText)
        val delete = view.findViewById<TextView>(R.id.deleteText)
        val share = view.findViewById<TextView>(R.id.shareText)

        edit.setOnClickListener {
            teamBuildViewModel.setNewTeamIndex(0)
            findNavController().navigate(TeamsHostFragmentDirections.actionNavTeamsHostToNavTeambuilder(pokemonTeam.copy()))
            dialog.dismiss()
        }
        delete.setOnClickListener {
            showDeleteConfirmation(pokemonTeam)
            dialog.dismiss()
        }
        share.setOnClickListener {
            showShareTeamDialog(pokemonTeam)
            dialog.dismiss()
        }

    }

    private fun showShareTeamDialog(pokemonTeam: PokemonTeam) {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        fireBaseViewModel.getUsersToShareTeamsWith()
        val view = layoutInflater.inflate(R.layout.popup_share_team, null)
        val rvTeam = view.findViewById<RecyclerView>(R.id.rvTeam)
        val rvUsers = view.findViewById<RecyclerView>(R.id.rvUsers)
        val saveButton = view.findViewById<Button>(R.id.btnSave)
        val cancelButton = view.findViewById<Button>(R.id.btnCancel)
        val adapter = UserRowAdapter { userIds ->
            fireBaseViewModel.updateSelectedUserIds(userIds)
        }
        val teamAdapter = PokemonTeamAdapter { }
        rvUsers.adapter = adapter
        rvTeam.adapter = teamAdapter

        saveButton.setOnClickListener {
            val ids = fireBaseViewModel.selectedUserIds.value ?: return@setOnClickListener
            fireBaseViewModel.grantAccessToOtherUser(pokemonTeam, ids)
            bottomSheetDialog.dismiss()
            //fireBaseViewModel.updateSelectedUserIds(emptyList())
        }

        cancelButton.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        teamAdapter.submitList(listOf(pokemonTeam))

        fireBaseViewModel.allProfiles.observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }

        fireBaseViewModel.selectedUserIds.observe(viewLifecycleOwner) { selectedIds ->
            saveButton.isEnabled = selectedIds.isNotEmpty()
        }

        bottomSheetDialog.setContentView(view)
        bottomSheetDialog.show()
    }

    private fun showDeleteConfirmation(pokemonTeam: PokemonTeam) {
        MaterialAlertDialogBuilder(requireContext()).apply {
            setTitle(getString(R.string.delete_team_confirmation))
            setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            setPositiveButton(getString(R.string.delete)) { dialog, _ ->
                fireBaseViewModel.deletePokemonTeam(pokemonTeam)
                dialog.dismiss()
            }
        }.show()
    }
}