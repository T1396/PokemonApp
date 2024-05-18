package com.example.pokinfo.ui.teambuilder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.pokinfo.R
import com.example.pokinfo.adapter.teamAndTeambuilder.PokemonTeamAdapter
import com.example.pokinfo.data.models.firebase.PokemonTeam
import com.example.pokinfo.databinding.FragmentTeamsBinding
import com.example.pokinfo.viewModels.FirebaseViewModel
import com.example.pokinfo.viewModels.SharedViewModel
import com.example.pokinfo.viewModels.factory.ViewModelFactory
import com.example.pokinfo.viewModels.teambuilder.TeamBuilderViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar

class TeamsFragment : Fragment() {

    private var _binding: FragmentTeamsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private val fireBaseViewModel: FirebaseViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application, sharedViewModel)
    }
    private val teamsViewModel: TeamBuilderViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application, sharedViewModel)
    }
    private var fabAddTeam: FloatingActionButton? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTeamsBinding.inflate(inflater, container, false)
        fabAddTeam = requireActivity().findViewById(R.id.fabMain)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = PokemonTeamAdapter {
            showEditTeamConfirmation(it)
        }
        binding.rvTeams.adapter = adapter
        fireBaseViewModel.listenForTeamsInFireStore {

            fireBaseViewModel.pokemonTeams.observe(viewLifecycleOwner) { teams ->
                if (teams.isNotEmpty()) {
                    adapter.submitList(teams.sortedByDescending { it.timestamp.seconds })
                    binding.rvTeams.visibility = View.VISIBLE
                    binding.tvNoTeams.visibility = View.GONE
                }
            }
        }

        fabAddTeam?.setOnClickListener {
            // delete old values if present
            teamsViewModel.resetTeamData()
            findNavController().navigate(TeamsFragmentDirections.actionNavTeambuilderToTeamBuild())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fireBaseViewModel.stopListeningForTeams()
        _binding = null
    }

    /**
     * Opens a dialog to ask the user if what action to do with the clicked team
     */
    private fun showEditTeamConfirmation(pokemonTeam: PokemonTeam) {
        val layoutInflater = LayoutInflater.from(requireContext())
        val view = layoutInflater.inflate(R.layout.popup_dialog_options, null)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(view)
            .show()

        val edit = view.findViewById<TextView>(R.id.editText)
        val delete = view.findViewById<TextView>(R.id.deleteText)
        val share = view.findViewById<TextView>(R.id.shareText)

        edit.setOnClickListener {
            teamsViewModel.setNewTeamIndex(0)
            findNavController().navigate(TeamsFragmentDirections.actionNavTeambuilderToTeamBuild(pokemonTeam))
            dialog.dismiss()
        }
        delete.setOnClickListener {
            showDeleteConfirmation(pokemonTeam)
            dialog.dismiss()
        }
        share.setOnClickListener {
            Snackbar.make(view, "To be done....", Snackbar.LENGTH_SHORT).show()
        }

    }

    private fun showDeleteConfirmation(pokemonTeam: PokemonTeam) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.delete_team_confirmation))
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(getString(R.string.delete)) { dialog, _ ->
                fireBaseViewModel.deletePokemonTeam(pokemonTeam)
                dialog.dismiss()
            }
            .show()
    }
}