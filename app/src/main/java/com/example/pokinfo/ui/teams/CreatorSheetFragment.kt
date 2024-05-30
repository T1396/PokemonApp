package com.example.pokinfo.ui.teams

import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
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
        val bottomSheetDialog = dialog as? BottomSheetDialog
        val bottomSheetInternal = bottomSheetDialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheetInternal?.let {
            val bottomSheetBehavior = BottomSheetBehavior.from(it)
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            bottomSheetBehavior.peekHeight = BottomSheetBehavior.PEEK_HEIGHT_AUTO
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        dialog?.let { d ->
            val bottomSheet = d.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as? FrameLayout
            bottomSheet?.let { bs ->
                val layoutParams = bs.layoutParams
                layoutParams.height = if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    ViewGroup.LayoutParams.MATCH_PARENT
                } else {
                    ViewGroup.LayoutParams.WRAP_CONTENT
                }
                bs.layoutParams = layoutParams
                BottomSheetBehavior.from(bs).apply {
                    peekHeight = Resources.getSystem().displayMetrics.heightPixels
                    state = BottomSheetBehavior.STATE_EXPANDED
                }
            }
        }
    }
}
