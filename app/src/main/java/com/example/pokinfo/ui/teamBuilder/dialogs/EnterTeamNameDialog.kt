package com.example.pokinfo.ui.teamBuilder.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.CheckBox
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import com.example.pokinfo.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout

class EnterTeamNameDialogFragment : DialogFragment() {
    interface EnterTeamNameListener {
        fun onTeamNameEntered(name: String, isPublic: Boolean)
    }

    private var listener: EnterTeamNameListener? = null
    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = parentFragment as? EnterTeamNameListener ?: context as? EnterTeamNameListener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = layoutInflater.inflate(R.layout.popup_create_team_dialog, null)
        val inputLayout = view.findViewById<TextInputLayout>(R.id.tilTeamName)
        val checkBox = view.findViewById<CheckBox>(R.id.cbPublic)

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
                        val isPublic = checkBox.isChecked
                        listener?.onTeamNameEntered(teamName, isPublic)
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
