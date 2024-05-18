package com.example.pokinfo.ui.attacks

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.pokinfo.R
import com.example.pokinfo.data.models.database.pokemon.PokemonForList
import com.example.pokinfo.data.models.database.pokemon.PokemonTypeName
import com.example.pokinfo.databinding.FragmentAttacksDetailBinding
import com.example.pokinfo.ui.misc.dialogs.openPokemonListDialog
import com.example.pokinfo.viewModels.AttacksViewModel
import com.example.pokinfo.viewModels.PokeViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder


class AttacksDetailFragment : Fragment() {

    private var _binding: FragmentAttacksDetailBinding? = null
    private val binding get() = _binding!!
    private val pokeViewModel: PokeViewModel by activityViewModels()
    private val attacksViewModel: AttacksViewModel by activityViewModels()
    private var actualLanguageId = -1
    private var actualVersionId: Int? = null

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

        val typeNames = pokeViewModel.pokemonTypeNames

        // Our actual language ID
        actualLanguageId = pokeViewModel.getLangId()
        attacksViewModel.clickedAttack.observe(viewLifecycleOwner) { moveData ->
            val moveDetails = moveData?.move?.firstOrNull()
            val idList = moveData?.pokemonList?.map { it.id } ?: emptyList()
            actualVersionId = attacksViewModel.getFirstVersionId()
            // changes the colour/drawable of the card related to the type of the move
            val typeColourTriple =
                pokeViewModel.getTypeColorAndIconId(
                    moveDetails?.type_id ?: 10001
                ) // Pair<ColorId , DrawableId>

            val names = moveDetails?.names

            // gets the name of the elemental Type from typeNameList
            val typeName =
                typeNames.find { it.typeId == moveDetails?.type_id }?.name ?: "Unknown"
            binding.tvAttackType.text = typeName


            val cardColor = ContextCompat.getColor(requireContext(), typeColourTriple.first)
            val otherDrawable =
                ContextCompat.getDrawable(requireContext(), typeColourTriple.second)
            binding.ivAttackType.setImageDrawable(otherDrawable)

            // set color and drawable depending if attack is damage attack or status ...
            binding.cvAtkType.setCardBackgroundColor(cardColor)
            val attackCategoryRes =
                when (moveDetails?.move_damage_class_id) { // damage class id defines if its attack move or status etc
                    1 -> R.drawable.pokemon_status_atk_icon
                    2 -> R.drawable.pokemon_atk_icon
                    else -> R.drawable.pokemon_sp_atk_icon
                }
            val attackTypeDrawable = ContextCompat.getDrawable(
                requireContext(),
                attackCategoryRes
            )
            binding.ivTypeAttack.setImageDrawable(attackTypeDrawable)

            // bind name, power accuracy and pp
            binding.tvAttackName.text = names?.find { it.language_id == actualLanguageId }?.name
                ?: "error finding move Name"
            binding.tvPower.text =
                if (moveDetails?.power != null && moveDetails.power > 0) moveDetails.power.toString() else "-" // if move power is null or 0 its a status attack prop.
            binding.tvAccuracy.text =
                if (moveDetails?.accuracy == null) "-" else "${moveDetails.accuracy}" // if accuracy is null it cant miss
            binding.tvPP.text = "${moveDetails?.pp}"


            // if translated name isn t available default name (english) will be taken
            val defaultName = names?.find { it.language_id == 9 }?.name ?: "No name found"
            val attackName = names?.find { it.language_id == actualLanguageId }?.name ?: defaultName

            binding.tvAttackName.text = attackName
            fillAttackDescription()
            setLanguageSelectionButton()

            // creates a list of IDs of pokemon who can learn the move to load a list from database
            // maps the id of every Pokemon who learns the move to a list to search that mons in the database
            // get the pokemonList and once finished callback to activate the button to show pokemon with that attack
            attacksViewModel.getPokemonListWhoLearnMove(idList) { pokemonList ->
                activateButtonFunction(
                    binding.btnShowPokemonWithMove,
                    pokemonList,
                    attackName,
                    typeNames
                )
            }
        }
    }

    private fun fillAttackDescription() {

        val description = attacksViewModel.getAttackDescription(actualLanguageId, actualVersionId ?: 1)


        binding.tvAttackDescription.text = description?.flavor_text
        binding.btnLanguage.text = attacksViewModel.getLanguageName(actualLanguageId)
        Log.d("actualLanguageId", actualVersionId.toString())
        binding.btnGameVersion.text = attacksViewModel.getVersionName(actualVersionId ?: -1)
    }

    private fun setLanguageSelectionButton() {
        binding.btnLanguage.setOnClickListener {
            openLanguageMenu()
        }
        binding.btnGameVersion.setOnClickListener {
            showVersionDialog()
        }
    }


    // activated the button to show pokemon with a attack and sets the on click listener
    // active so a list dialog it opened
    private fun activateButtonFunction(
        button: Button,
        listOfPokemon: List<PokemonForList>,
        attackName: String,
        typeNames: List<PokemonTypeName>
    ) {
        requireActivity().runOnUiThread { // else there will be error because only original thread is allowed to touch its view
            button.isEnabled = true
            button.setOnClickListener {
                openListDialog(listOfPokemon, attackName, typeNames)
            }
        }
    }

    @SuppressLint("InflateParams")
    private fun openListDialog(
        listOfPokemon: List<PokemonForList>,
        attackName: String,
        typeNames: List<PokemonTypeName>
    ) {
        openPokemonListDialog(
            listOfPokemon = listOfPokemon,
            title = getString(R.string.every_pokemon_with_move, attackName),
            typeNames
        ) { pokemonId ->
            // fetch pokemon data and navigate
            pokeViewModel.getSinglePokemonData(
                pokemonId,
                R.string.failed_load_single_pokemon_data
            )
            findNavController().navigate(
                AttacksDetailFragmentDirections.actionNavAttacksDetailToNavHomeDetail(
                    pokemonId
                )
            )
        }
    }


    private fun showVersionDialog() {

        val versionNames = attacksViewModel.getAvailableVersions(actualLanguageId)
        val versionNamesArray = versionNames.map { it.name }.toTypedArray()
        val title = getString(R.string.choose_version)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setItems(versionNamesArray)
            { _, which ->
                actualVersionId = versionNames[which].versionId
                fillAttackDescription()
            }
            .show()
    }



    private fun openLanguageMenu(
    ) {
        val title = getString(R.string.choose_language)
        val nameList = attacksViewModel.getAvailableLanguageNames()
        val array = nameList.map { it.name }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setNegativeButtonIcon(null)
            .setItems(array) { _, which ->
                actualLanguageId = nameList[which].languageId
                actualVersionId = attacksViewModel.getFirstVersionIdOfLanguage(actualLanguageId)
                fillAttackDescription()
            }.show()
    }
}