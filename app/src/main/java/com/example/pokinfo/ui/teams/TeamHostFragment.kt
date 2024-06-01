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
import com.example.pokinfo.databinding.FragmentTeamsHostBinding
import com.example.pokinfo.viewModels.SharedViewModel
import com.example.pokinfo.viewModels.factory.ViewModelFactory
import com.example.pokinfo.viewModels.teambuilder.TeamBuilderViewModel
import com.example.pokinfo.viewModels.teams.TeamsViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayoutMediator


enum class TeamType(val tabNr: Int, val tabTextRes: Int, val noTeamsTextRes: Int){
    MY_TEAMS(0, R.string.my_teams, R.string.seems_like_you_have_no_saved_teams_want_to_create_one),
    SHARED_TEAMS(1, R.string.shared_teams, R.string.no_shared_teams),
    PUBLIC_TEAMS(2, R.string.public_teams, R.string.no_public_teams);
}


class TeamsPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        val frag = TeamsFragment()
        frag.arguments = Bundle().apply {
            putInt("position", position)
        }
        return frag
    }
}

class TeamsHostFragment : Fragment() {
    private lateinit var binding: FragmentTeamsHostBinding
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private val teamBuildViewModel: TeamBuilderViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application, sharedViewModel)
    }
    private val teamsViewModel: TeamsViewModel by activityViewModels {
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
            val teamType = TeamType.entries.find { it.tabNr == position } ?: TeamType.MY_TEAMS
            tab.text = getString(teamType.tabTextRes)
        }.attach()

        teamsViewModel.selectedTab.observe(viewLifecycleOwner) { event ->
            // ensures tab is not set again after layout changes or similar with event class
            event.getContentIfNotHandled()?.let { teamType ->
                binding.viewPager.currentItem = teamType.tabNr
            }
        }


        fabAddTeam?.setOnClickListener {
            // delete old values if present
            teamBuildViewModel.resetTeamData()
            findNavController().navigate(TeamsHostFragmentDirections.actionNavTeamsHostToNavTeambuilder())
        }
    }

    // save active tab
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("currentTab", binding.viewPager.currentItem)
    }

    // restore active tab
    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        savedInstanceState?.getInt("currentTab")?.let {
            binding.viewPager.currentItem = it
        }
    }
}
