package com.example.pokinfo.ui.misc.dialogs

import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pokinfo.R
import com.example.pokinfo.adapter.home.PokeListAdapter
import com.example.pokinfo.data.models.database.pokemon.PokemonForList
import com.example.pokinfo.data.models.database.pokemon.PokemonTypeName
import com.example.pokinfo.databinding.PopupPokemonListBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

fun Fragment.showConfirmationDialog(
    onConfirm: () -> Unit,
    onCancel: (() -> Unit)? = null
) {
    MaterialAlertDialogBuilder(requireContext())
        .setTitle(getString(R.string.navigate_dialog_title))
        .setPositiveButton(getString(R.string.navigate)) { dialog, _ ->
            onConfirm()
            dialog.dismiss()
        }
        .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
            onCancel?.invoke()
            dialog.dismiss()
        }
        .show()
}


fun Fragment.openPokemonListDialog(
    listOfPokemon: List<PokemonForList>,
    title: String,
    typeNames: List<PokemonTypeName>,
    navigateCallback: (Int) -> Unit,
) {
    val binding = PopupPokemonListBinding.inflate(layoutInflater)
    val rvList = binding.rvPokemovelist
    rvList.layoutManager = LinearLayoutManager(requireContext())
    val dialog = MaterialAlertDialogBuilder(requireContext()).apply {
        setTitle(title)
        setView(binding.root)
        setPositiveButton(android.R.string.ok, null)
    }.create()

    val adapter = PokeListAdapter(typeNames) { pokemonId ->
        showConfirmationDialog(
            onConfirm = {
                navigateCallback(pokemonId)
            },
            onCancel = null
        )
        dialog.dismiss() // close dialog if pokemon is selected
    }
    rvList.adapter = adapter
    adapter.submitList(listOfPokemon)

    dialog.show()
}