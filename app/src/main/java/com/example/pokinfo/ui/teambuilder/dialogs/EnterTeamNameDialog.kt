package com.example.pokinfo.ui.teambuilder.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import com.example.pokinfo.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout

class EnterTeamNameDialogFragment : DialogFragment() {
    interface EnterTeamNameListener {
        fun onTeamNameEntered(name: String)
    }

    private var listener: EnterTeamNameListener? = null
    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = parentFragment as? EnterTeamNameListener ?: context as? EnterTeamNameListener
        Log.d("EnterTeamName", "Listener assigned: $listener")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = layoutInflater.inflate(R.layout.popup_create_team_dialog, null)
        val inputLayout = view.findViewById<TextInputLayout>(R.id.tilTeamName)

        return MaterialAlertDialogBuilder(requireContext())
            .setView(view)
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton(getString(R.string.save), null) // Initially set to null
            .create().apply {
                setOnShowListener {
                    val posButton = getButton(AlertDialog.BUTTON_POSITIVE)
                    posButton.isEnabled = false


                    inputLayout.editText?.addTextChangedListener {
                        val isNotBlank = !it.isNullOrBlank()
                        inputLayout.error = if (isNotBlank) null else getString(R.string.enter_name_error)
                        posButton.isEnabled = isNotBlank
                    }

                    posButton.setOnClickListener {
                        val teamName = inputLayout.editText?.text.toString().trim()
                        Log.d("Listener", listener.toString())
                        listener?.onTeamNameEntered(teamName)
                        dismiss()
                    }
                }
            }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }
}
