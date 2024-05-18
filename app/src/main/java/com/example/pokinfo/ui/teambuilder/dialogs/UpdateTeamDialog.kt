package com.example.pokinfo.ui.teambuilder.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.example.pokinfo.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class UpdateTeamDialogFragment : DialogFragment() {
    interface UpdateTeamListener {
        fun onUpdate()
        fun onCopy()
        fun onDiscard()
    }

    private var listener: UpdateTeamListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = parentFragment as? UpdateTeamListener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_title_update_team))
            .setMessage(getString(R.string.dialog_text_update_team))
            .setPositiveButton(getString(R.string.yes_update)) { _, _ -> listener?.onUpdate() }
            .setNeutralButton(getString(R.string.make_a_copy)) { _, _ -> listener?.onCopy() }
            .setNegativeButton(getString(R.string.discard_changes)) { _, _ -> listener?.onDiscard() }
            .create()
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }
}
