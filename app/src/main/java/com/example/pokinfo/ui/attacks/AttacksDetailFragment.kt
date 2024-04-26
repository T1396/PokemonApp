package com.example.pokinfo.ui.attacks

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.pokinfo.R
import com.example.pokinfo.R.drawable
import com.example.pokinfo.R.string
import com.example.pokinfo.adapter.attacks.detail.AttackDescription
import com.example.pokinfo.adapter.attacks.detail.DescriptionAdapter
import com.example.pokinfo.data.models.database.pokemon.PokemonForList
import com.example.pokinfo.data.models.database.versionAndLanguageNames.LanguageNames
import com.example.pokinfo.data.util.NoScrollLayoutManager
import com.example.pokinfo.databinding.FragmentAttacksDetailBinding
import com.example.pokinfo.ui.misc.dialogs.openPokemonListDialog
import com.example.pokinfo.ui.misc.dialogs.showConfirmationDialog
import com.example.pokinfo.viewModels.AttacksViewModel
import com.example.pokinfo.viewModels.PokeViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder


class AttacksDetailFragment : Fragment() {

    private var _binding: FragmentAttacksDetailBinding? = null
    private val binding get() = _binding!!
    private val pokeViewModel: PokeViewModel by activityViewModels()
    private val attacksViewModel: AttacksViewModel by activityViewModels()

    private lateinit var descAdapter: DescriptionAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentAttacksDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnShowPokemonWithMove.isEnabled = false


        // Our actual language ID
        val languageId = pokeViewModel.getLangId()
        attacksViewModel.clickedAttack.observe(viewLifecycleOwner) { moveData ->

            // changes the colour/drawable of the card related to the type of the move
            val typeColourTriple =
                pokeViewModel.getTypeColorAndIconId(moveData.typeId) // Pair<ColorId , DrawableId>

            val languageList = moveData.languageList
            val names = moveData.names
            val typeNames = moveData.pokemonTypeNames

            // gets the name of the elemental Type from typeNameList
            val typeName =
                typeNames.find { it.typeId == moveData.typeId }?.name ?: "Unknown"
            binding.tvAttackType.text = typeName


            val cardColor = ContextCompat.getColor(requireContext(), typeColourTriple.first)
            val otherDrawable =
                ContextCompat.getDrawable(requireContext(), typeColourTriple.second)
            binding.ivAttackType.setImageDrawable(otherDrawable)

            // set color and drawable depending if attack is damage attack or status ...
            binding.cvAtkType.setCardBackgroundColor(cardColor)
            val attackCategoryRes =
                when (moveData.moveDamageClassId) { // damage class id defines if its attack move or status etc
                    1 -> drawable.pokemon_status_atk_icon
                    2 -> drawable.pokemon_atk_icon
                    else -> drawable.pokemon_sp_atk_icon
                }
            val attackTypeDrawable = ContextCompat.getDrawable(
                requireContext(),
                attackCategoryRes
            )
            binding.ivTypeAttack.setImageDrawable(attackTypeDrawable)

            // bind name, power accuracy and pp
            binding.tvAttackName.text = names.find { it.second == languageId }?.first
                ?: "error finding move Name"
            binding.tvPower.text =
                if (moveData.power > 0) moveData.power.toString() else "-" // if move power is null or 0 its a status attack prop.
            binding.tvAccuracy.text =
                if (moveData.accuracy == null) "-" else "${moveData.accuracy}" // if accuracy is null it cant miss
            binding.tvPP.text = "${moveData.pp}"


            // if translated name isn t available default name (english) will be taken
            val defaultName = names.find { it.second == 9 }?.first ?: "error"
            val attackName = names.find { it.second == languageId }?.first ?: defaultName
            binding.tvAttackName.text = attackName


            val list = moveData.attackDescriptions
            // Create Adapter and pass function to call if language button pressed
            descAdapter = DescriptionAdapter {
                openLanguageMenu(languageList, list)
            }
            binding.rvDescription.adapter = descAdapter
            binding.rvDescription.layoutManager = NoScrollLayoutManager(requireContext())

            descAdapter.submitList(list.filter { it.languageId == languageId })
            // creates a list of IDs of pokemon who can learn the move to load a list from database
            // maps the id of every Pokemon who learns the move to a list to search that mons in the database
            // get the pokemonList and once finished callback to activate the button to show pokemon with that attack
            val idList = moveData.listOfPokemonIdsWhoLearn
            attacksViewModel.getPokemonListWhoLearnMove(idList) { pokemonList ->
                activateButtonFunction(binding.btnShowPokemonWithMove, pokemonList, attackName)
            }

        }
    }


    // activated the button to show pokemon with a attack and sets the on click listener
    // active so a list dialog it opened
    private fun activateButtonFunction(button: Button, listOfPokemon: List<PokemonForList>, attackName: String) {
        requireActivity().runOnUiThread { // else there will be error because only original thread is allowed to touch its view
            button.isEnabled = true
            button.setOnClickListener {
                openListDialog(listOfPokemon, attackName)
            }
        }
    }
    @SuppressLint("InflateParams")
    private fun openListDialog(listOfPokemon: List<PokemonForList>, attackName: String) {
        openPokemonListDialog(listOfPokemon, string.every_pokemon_with_move, attackName) { pokemonId ->
            // another dialog to ask user if he surely wants to navigate
            showConfirmationDialog(
                onConfirm = {
                    pokeViewModel.getSinglePokemonData(
                        pokemonId,
                        string.failed_load_single_pokemon_data
                    ) {
                        findNavController().navigate(AttacksDetailFragmentDirections.actionNavAttacksDetailToNavHomeDetail(pokemonId))
                    } // no callback needed
                }
            )
        }
    }

    private fun openLanguageMenu(
        languageList: List<LanguageNames>,
        attackDescriptions: List<AttackDescription>
    ) {
        val title = getString(string.choose_language)
        val nameList = languageList.map { it.name }.toTypedArray()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setNegativeButtonIcon(null)
            .setItems(nameList) { _, which ->
                val languageId = languageList[which].languageId
                val filteredList = attackDescriptions.filter { it.languageId == languageId }
                descAdapter.submitList(filteredList)
            }.show()
    }


}