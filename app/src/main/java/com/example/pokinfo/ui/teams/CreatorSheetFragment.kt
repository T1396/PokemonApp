package com.example.pokinfo.ui.teams

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.SnapHelper
import coil.load
import coil.transform.CircleCropTransformation
import com.example.pokinfo.R
import com.example.pokinfo.adapter.teamAndTeambuilder.TeamAdapterSmall
import com.example.pokinfo.data.models.firebase.PublicProfile
import com.example.pokinfo.data.util.toGermanDateString
import com.example.pokinfo.databinding.SheetPublicProfileBinding
import com.example.pokinfo.viewModels.teams.TeamsViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class CreatorSheetFragment : BottomSheetDialogFragment() {
    private var _binding: SheetPublicProfileBinding? = null
    private val binding get() = _binding!!
    private val teamsViewModel: TeamsViewModel by activityViewModels()
    private var snapHelperTeams: SnapHelper? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SheetPublicProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val creatorId = arguments?.getString("creatorId") ?: return
        setupProfile(creatorId)
    }

    private fun setupProfile(creatorId: String) {
        teamsViewModel.getPublicUserData(creatorId) { creatorProfile ->
            updateUI(creatorProfile)
        }
    }

    private fun updateUI(creatorProfile: PublicProfile) {
        binding.imageView.load(creatorProfile.profilePicture) {
            error(R.drawable.pokeball)
            transformations(CircleCropTransformation())
        }
        binding.tvUserName.text = creatorProfile.username
        binding.tvCreatedTeamsValue.text = creatorProfile.teamsCount.toString()
        binding.tvReceivedLikesValue.text = creatorProfile.likeCount.toString()
        binding.tvPublicTeamsFromUser.text =
            getString(R.string.public_teams_from_user, creatorProfile.username)
        binding.tvRegisteredSince.text = getString(
            R.string.registered_since,
            creatorProfile.registrationDate.toGermanDateString()
        )

        teamsViewModel.getTeamsByUser(creatorProfile.userId) { pokemonTeams ->
            val adapter = TeamAdapterSmall(
                TeamType.PUBLIC_TEAMS,
                onItemLongClicked = {},
                onLiked = teamsViewModel::incrementLikeCount,
                onLikeRemove = teamsViewModel::decrementLikeCount
            )
            binding.rvUserTeams.adapter = adapter
            teamsViewModel.likedTeams.observe(viewLifecycleOwner) { likedTeams ->
                adapter.setLikedTeams(likedTeams)
            }
            adapter.submitList(pokemonTeams)
            if (snapHelperTeams == null) {
                snapHelperTeams = PagerSnapHelper()
                snapHelperTeams?.attachToRecyclerView(binding.rvUserTeams)
            }
        }

    }


    override fun onStart() {
        super.onStart()
        val dialog = dialog as? BottomSheetDialog
        dialog?.let {
            val bottomSheet = it.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            val behavior = BottomSheetBehavior.from(bottomSheet!!)

            val orientation = resources.configuration.orientation
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            } else {
                behavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
            behavior.peekHeight = BottomSheetBehavior.PEEK_HEIGHT_AUTO
        }
    }

}
