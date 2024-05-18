package com.example.pokinfo.ui.teambuilder.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.example.pokinfo.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SaveDataDialogFragment : DialogFragment() {
    interface SaveDataListener {
        fun onSave()
        fun onDiscard()
    }

    private var listener: SaveDataListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = parentFragment as? SaveDataListener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.unsaved_info))
            .setPositiveButton(getString(R.string.save)) { _, _ -> listener?.onSave() }
            .setNegativeButton(getString(R.string.discard_changes)) { _, _ -> listener?.onDiscard() }
            .create()
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }
}
