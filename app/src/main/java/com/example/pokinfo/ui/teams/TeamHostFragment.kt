package com.example.pokinfo.ui.teams

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.pokinfo.R
import com.example.pokinfo.adapter.teamAndTeambuilder.PokemonTeamAdapter
import com.example.pokinfo.data.models.firebase.PokemonTeam
import com.example.pokinfo.databinding.FragmentTeamsBinding
import com.example.pokinfo.databinding.FragmentTeamsHostBinding
import com.example.pokinfo.viewModels.FirebaseViewModel
import com.example.pokinfo.viewModels.SharedViewModel
import com.example.pokinfo.viewModels.factory.ViewModelFactory
import com.example.pokinfo.viewModels.teambuilder.TeamBuilderViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayoutMediator


enum class TeamType{
    MY_TEAMS,
    SHARED_TEAMS,
    PUBLIC_TEAMS;
}


fun updateRecyclerView(
    teams: List<PokemonTeam>,
    adapter: PokemonTeamAdapter,
    binding: FragmentTeamsBinding
) {
    adapter.submitList(teams.sortedByDescending { it.timestamp.seconds })
    if (teams.isNotEmpty()) {
        binding.rvTeams.visibility = View.VISIBLE
        binding.tvNoTeams.visibility = View.GONE
    } else {
        binding.rvTeams.visibility = View.GONE
        binding.tvNoTeams.visibility = View.VISIBLE
    }
}

class TeamsPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        val fragment = when(position) {
            0 -> TeamsFragment().also { it.setTeamType(TeamType.MY_TEAMS) }
            1 -> TeamsFragment().also { it.setTeamType(TeamType.SHARED_TEAMS) }
            2 -> TeamsFragment().also { it.setTeamType(TeamType.PUBLIC_TEAMS) }
            else -> throw IllegalStateException("Invalid position $position")
        }
        return fragment
    }
}

class TeamsHostFragment : Fragment() {
    private lateinit var binding: FragmentTeamsHostBinding
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private val teamBuildViewModel: TeamBuilderViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application, sharedViewModel)
    }
    private val fireBaseViewModel: FirebaseViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application, sharedViewModel)
    }
    private var fabAddTeam: FloatingActionButton? = null



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentTeamsHostBinding.inflate(inflater, container, false)
        fabAddTeam = requireActivity().findViewById(R.id.fabMain)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = TeamsPagerAdapter(this)
        binding.viewPager.adapter = adapter
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "My Teams"
                1 -> "Shared for you"
                2 -> "Public teams"
                else -> ""
            }
        }.attach()


        fireBaseViewModel.selectedTab.observe(viewLifecycleOwner) {
            binding.viewPager.currentItem = when(it) {
                TeamType.MY_TEAMS -> 0
                TeamType.SHARED_TEAMS -> 1
                TeamType.PUBLIC_TEAMS -> 2
                null -> 0
            }
        }


        fabAddTeam?.setOnClickListener {
            // delete old values if present
            teamBuildViewModel.resetTeamData()
            findNavController().navigate(TeamsHostFragmentDirections.actionNavTeamsHostToNavTeambuilder())
        }
    }
}
