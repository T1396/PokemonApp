package com.example.pokinfo.ui.misc.dialogs

import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pokinfo.R
import com.example.pokinfo.adapter.home.PokeListAdapter
import com.example.pokinfo.data.models.database.pokemon.PokemonForList
import com.example.pokinfo.viewModels.PokeViewModel
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
    titleRes: Int,
    name: String? = null,
    navigateCallback:(Int) -> Unit
) {
    val title = if (name == null) getString(R.string.every_pokemon_with_ability) else getString(R.string.every_pokemon_with_move, name)
    val inflater = requireActivity().layoutInflater
    val view = inflater.inflate(R.layout.popup_pokemon_list, null)
    val rvList = view.findViewById<RecyclerView>(R.id.rvPokemovelist)
    rvList.layoutManager = LinearLayoutManager(requireContext())
    val dialog = MaterialAlertDialogBuilder(requireContext()).apply {
        setTitle(title)
        setView(view)
        setPositiveButton(android.R.string.ok, null)
    }.create()

    val adapter = PokeListAdapter { pokemonId ->
        navigateCallback(pokemonId)
        dialog.dismiss() // close dialog if pokemon is selected
    }
    rvList.adapter = adapter
    adapter.submitList(listOfPokemon)

    dialog.show()
}