package com.example.pokinfo.ui.teams

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.example.pokinfo.adapter.decoration.StartEndDecoration
import com.example.pokinfo.adapter.teamAndTeambuilder.TeamAdapterSmall
import com.example.pokinfo.adapter.teamAndTeambuilder.UserRowAdapter
import com.example.pokinfo.data.models.firebase.PokemonTeam
import com.example.pokinfo.databinding.PopupShareTeamBinding
import com.example.pokinfo.viewModels.teams.TeamsViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ShareTeamSheet : BottomSheetDialogFragment() {
    private var _binding: PopupShareTeamBinding? = null
    private val binding get() = _binding!!
    private val teamsViewModel: TeamsViewModel by activityViewModels()
    private lateinit var pokemonTeam: PokemonTeam
    private lateinit var teamType: TeamType

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PopupShareTeamBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDialog()
    }

    private fun setupDialog() {
        binding.btnSave.isEnabled = false
        teamsViewModel.getUsersToShareTeamsWith()

        val userAdapter = UserRowAdapter(pokemonTeam.sharedWith) { userIds ->
            teamsViewModel.updateSelectedUserIds(userIds)
        }
        val teamAdapter = TeamAdapterSmall(teamType, onItemLongClicked = {})
        teamAdapter.submitList(listOf(pokemonTeam))

        binding.rvUsers.adapter = userAdapter
        binding.rvUsers.addItemDecoration(StartEndDecoration(requireContext(), 16))
        binding.rvTeam.adapter = teamAdapter

        teamsViewModel.allProfiles.observe(viewLifecycleOwner) {
            userAdapter.submitList(it)
        }
        teamsViewModel.selectedUserIds.observe(viewLifecycleOwner) { selectedIds ->
            binding.btnSave.isEnabled = selectedIds != pokemonTeam.sharedWith
        }

        binding.btnSave.setOnClickListener {
            teamsViewModel.grantAccessToOtherUser(pokemonTeam)
            dismiss()
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    companion object {
        fun newInstance(team: PokemonTeam, teamType: TeamType): ShareTeamSheet {
            val fragment = ShareTeamSheet()
            fragment.pokemonTeam = team
            fragment.teamType = teamType
            return fragment
        }
    }
}