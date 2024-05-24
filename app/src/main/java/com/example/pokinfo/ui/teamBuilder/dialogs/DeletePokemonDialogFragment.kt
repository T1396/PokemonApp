package com.example.pokinfo.ui.teamBuilder.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.example.pokinfo.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class DeletePokemonDialogFragment : DialogFragment() {
    interface DeletePokemonListener {
        fun onDeleteConfirmed(index: Int)
    }

    companion object {
        private const val ARG_INDEX = "index"

        fun newInstance(index: Int) = DeletePokemonDialogFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_INDEX, index)
            }
        }
    }

    private var listener: DeletePokemonListener? = null
    private var index: Int = -1

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = parentFragment as? DeletePokemonListener
        index = arguments?.getInt(ARG_INDEX) ?: -1
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.delete_pokemon_title))
            .setMessage(getString(R.string.delete_pokemon_confirmation))
            .setPositiveButton(getString(R.string.delete)) { _, _ -> listener?.onDeleteConfirmed(index) }
            .setNegativeButton(getString(R.string.cancel), null)
            .create()
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }
}
