package com.example.pokinfo.ui.teamBuilder.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import com.example.pokinfo.R
import com.example.pokinfo.databinding.PopupCreateTeamDialogBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class EnterTeamNameDialogFragment : DialogFragment() {
    interface EnterTeamNameListener {
        fun onTeamNameEntered(name: String, isPublic: Boolean, teamId: String)
    }

    companion object {
        fun newInstance(isPublic: Boolean, teamId: String, oldName: String): EnterTeamNameDialogFragment {
            val args = Bundle()
            args.putBoolean("isPublic", isPublic)
            args.putString("teamId", teamId)
            args.putString("oldName", oldName)
            val fragment = EnterTeamNameDialogFragment()
            fragment.arguments = args
            return fragment
        }
    }

    private var listener: EnterTeamNameListener? = null
    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = parentFragment as? EnterTeamNameListener ?: context as? EnterTeamNameListener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = PopupCreateTeamDialogBinding.inflate(layoutInflater)

        // get values that are eventually set over companion object
        val isPublicInitial = arguments?.getBoolean("isPublic", false) ?: false
        binding.cbPublic.isChecked = isPublicInitial
        val oldTeamName = arguments?.getString("oldName")
        val teamId = arguments?.getString("teamId") ?: ""
        if (oldTeamName != null) binding.tvTitle.text = getString(R.string.enter_new_teamname_title)


        binding.tilTeamName.editText?.addTextChangedListener {
            val isValidName = !it.isNullOrBlank() && it.toString() != oldTeamName
            binding.tilTeamName.error = if (isValidName) null else if (oldTeamName != null) {
                if (it.toString() == oldTeamName) {
                    getString(R.string.need_new_name_error)
                } else {
                    getString(R.string.enter_name_error)
                }
            } else {
                getString(R.string.enter_name_error)
            }
            binding.saveButton.isEnabled = isValidName
        }
        binding.tilTeamName.editText?.setText("")


        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create().apply {
                setOnShowListener {
                    binding.saveButton.isEnabled = false


                    binding.tilTeamName.editText?.setOnFocusChangeListener { v, hasFocus ->
                        if (hasFocus) {
                            resizeHeightOnSmallDevices(binding)
                        }
                    }
                    binding.cancelButton.setOnClickListener {
                        dismiss()
                    }
                    binding.saveButton.setOnClickListener {
                        val teamName = binding.tilTeamName.editText?.text.toString().trim()
                        val isPublic = binding.cbPublic.isChecked
                        listener?.onTeamNameEntered(teamName, isPublic, teamId)
                        dismiss()
                    }
                }
            }
    }

    private fun resizeHeightOnSmallDevices(binding: PopupCreateTeamDialogBinding) {
        val displayMetrics = requireContext().resources.displayMetrics
        val screenHeightDp = displayMetrics.heightPixels / displayMetrics.density
        if (screenHeightDp < 700) {
            binding.scrollView.smoothScrollTo(0, binding.root.bottom)
            val maxHeight = (displayMetrics.heightPixels * 0.7).toInt()
            dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, maxHeight)
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }
}
