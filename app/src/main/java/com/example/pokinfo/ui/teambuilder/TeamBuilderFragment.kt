package com.example.pokinfo.ui.teambuilder

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.pokinfo.R
import com.example.pokinfo.adapter.teamAndTeambuilder.AllPokemonAdapter
import com.example.pokinfo.adapter.teamAndTeambuilder.SpinnerAdapter
import com.example.pokinfo.data.maps.typeColorMap
import com.example.pokinfo.data.models.database.pokemon.PokemonForList
import com.example.pokinfo.data.models.firebase.AttacksData
import com.example.pokinfo.data.models.firebase.PokemonTeam
import com.example.pokinfo.data.models.firebase.TeamPokemon
import com.example.pokinfo.data.models.fragmentDataclasses.TeamBuilderData
import com.example.pokinfo.data.util.ImageAltLoader.loadAnyImage
import com.example.pokinfo.databinding.FragmentTeamBuilderBinding
import com.example.pokinfo.viewModels.FirebaseViewModel
import com.example.pokinfo.viewModels.PokeViewModel
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.slider.Slider
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout

class TeamBuilderFragment : Fragment() {
    //region variables
    private lateinit var binding: FragmentTeamBuilderBinding
    private val pokeViewModel: PokeViewModel by activityViewModels()
    private val firebaseViewModel: FirebaseViewModel by activityViewModels()
    private val args: TeamBuilderFragmentArgs by navArgs()
    private var isEditTeamMode = false // changes behaviour of some functions like save team

    private lateinit var pokeListAdapter: AllPokemonAdapter
    private lateinit var selectedAbility: Pair<String, String> // pair of name and effect text to display in spinner
    private var abilityList: List<Pair<String, String>>? =
        null // list of all abilities a pokemon can have


    // maps to easier run things in loops
    lateinit var sliderEditTextMap: LinkedHashMap<EditText?, Slider>
    lateinit var tvEditTextMapEV: LinkedHashMap<TextView, EditText?>
    lateinit var tvEditTextMapIV: LinkedHashMap<TextView, EditText?>
    lateinit var cvIvPair: List<Pair<MaterialCardView, ImageView>>

    /** Holds triples of IV/EV EditText and Progress Bar of a stat (hp, atk, etc.) to calc the resulting values*/
    lateinit var calculationMap: List<Triple<EditText?, EditText?, ProgressBar>>
    lateinit var resultValuesList: List<TextView>
    lateinit var progressBarTVList: List<Pair<ProgressBar, TextView>>
    lateinit var attacksCardList: List<Pair<MaterialCardView, TextView>>

    private val maleIconRes = R.drawable.baseline_male_24
    private val maleTextRes = R.string.male
    private val femaleIconRes = R.drawable.baseline_female_24
    private val femaleTextRes = R.string.female
    private val neutralIconRes = R.drawable.baseline_transgender_24
    private var genderFlag = true // stands for male initially
    private var isGenderless = false

    private var lastClickTime: Long = 0 // to prevent the user from clicking too fast
    private var isEvIvBarExpanded = false

    // flags to show snack bars if user needs to do anything before he can save pokemon/team
    private var isPokemonSelected = false
    private var isAtLeast1PokemonInTeam = false
    private var isSpinnerInitialized = false


    private var assignedEvs = 0
    val maxEvs =
        508 // each pokemon can have up to 508 ev's assigned to all stats like atk, hp, def etc

    // one stat can have 252 max.
    private var teamIndex = 0 // indicator which pokemon we are editing
    private var ignoreTextChanges =
        false // to ignore text changes when sometimes the name edittext gets set
    private var fabSavePokemon: FloatingActionButton? = null
    //endregion

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTeamBuilderBinding.inflate(inflater, container, false)
        createMaps(binding) // create the maps mentioned above directly
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fabSavePokemon?.isEnabled = true
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fabSavePokemon = activity?.findViewById(R.id.fabMain)
        overrideNavigationLogic()

        val typeNames = pokeViewModel.pokemonTypeNames

        if (args.pokemonTeam != null) {
            // inserts if editing a team the values into live-data to display it with a observer
            pokeViewModel.insertTeam(pokemonTeam = args.pokemonTeam ?: PokemonTeam())
            isEditTeamMode = true
        }

        // display search results in a recyclerview
        pokeViewModel.filteredListTeamBuilder.observe(viewLifecycleOwner) {
            if (::pokeListAdapter.isInitialized) {
                pokeListAdapter.submitList(it)
                binding.rvPokeList.visibility =
                    if (it.isEmpty()) View.GONE else View.VISIBLE
            }
        }

        binding.tilPokemonName.editText?.addTextChangedListener {
            if (!ignoreTextChanges) {
                pokeViewModel.filterPokemonList(it.toString())
            }
        }

        // create an adapter to let the user choose from all pokemon
        pokeViewModel.everyPokemon.observe(viewLifecycleOwner) {

            pokeListAdapter = AllPokemonAdapter(
                pokemonTypeNames = typeNames,
                onItemClicked = { pokemon: PokemonForList ->
                    // if a pokemon is clicked (selected)
                    isPokemonSelected = true
                    binding.tilPokemonName.editText?.setText("")
                    binding.rvPokeList.visibility = View.GONE
                    pokeViewModel.getSinglePokemonData(
                        pokemon.id,
                        R.string.failed_load_single_pokemon_data
                    ) {
                        // insert to team when loaded
                        pokeViewModel.insertPokemonToTeam(teamIndex, postVal = true)
                        fabSavePokemon?.isEnabled = true
                    }
                }
            )
            binding.rvPokeList.adapter = pokeListAdapter
        }

        binding.abilitySpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    if (view != null && isSpinnerInitialized) {
                        // save selected ability
                        this@TeamBuilderFragment.selectedAbility =
                            abilityList?.get(position) ?: Pair("", "")
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

        binding.btnShowAttacks.setOnClickListener {
            // push pokemon state to live-data to not lose it when navigating
            extractValuesAndUpdatePokemonInTeam(
                createNewSlot = false,
                showSaveMessage = false
            )
            findNavController().navigate(
                TeamBuilderFragmentDirections.actionTeamBuildToFullScreenDialogFragment(
                    isSelectionMode = true
                )
            )
        }

        pokeViewModel.teamBuilderSelectedAttacks.observe(viewLifecycleOwner) {
            showSelectedAttacks(it)
        }

        setUpGenderSwitch()

        //setTextWatchers(binding)

        val statManager = StatManager(updateEvLeftTextView = ::updateEvLeftTextView)
        sliderEditTextMap.forEach { ( editText, slider) ->
            if (editText != null) {
                statManager.registerEditTextAndSlider(editText, slider)
            }
        }
        // Level-EditText behavior, can be between 1 and 100
        //addLevelTextWatcher(binding.tilPokemonLevel.editText)

        // expands the ev/iv layout
        binding.includeEvIvWindow.topLayout.setOnClickListener {
            isEvIvBarExpanded = !isEvIvBarExpanded
            binding.includeEvIvWindow.expandLayout.visibility =
                if (isEvIvBarExpanded) View.VISIBLE else View.GONE
            binding.scrollView.post {
                binding.scrollView.smoothScrollTo(0, binding.includeEvIvWindow.root.bottom)
            }
        }

        // save a pokemon (and create new slot if possible)
        fabSavePokemon?.setOnClickListener {
            if (!isPokemonSelected) {
                // show error message
                Snackbar.make(
                    view,
                    getString(R.string.you_need_to_select_1_pokemon),
                    Snackbar.LENGTH_SHORT
                ).show()
                return@setOnClickListener // cancel
            }
            // if all we need it selected extract values and insert to team
            updateTeamAndCreateNewSlotIfPossible()
        }

        // save team button (top - right)
        binding.btnTextSaveTeam.setOnClickListener {
            if (!isAtLeast1PokemonInTeam) {
                Snackbar.make(
                    view,
                    getString(R.string.need_to_save_first_pokemon),
                    Snackbar.LENGTH_SHORT
                ).show()
            } else {

                if (!isEditTeamMode) {
                    showEnterTeamNameDialog()
                } else {
                    showUpdateTeamDialog()
                }
            }
        }
        // observe the team to display it
        pokeViewModel.pokemonTeam.observe(viewLifecycleOwner) { team ->
            displayTeamPokemon(team)
        }
    }

    //region update Team functions
    /** Updates the chosen Attacks to the Pokemon and than gets the other values
     *  like level gender etc to update the pokemon in the team with the chosen values */
    private fun updateTeamAndCreateNewSlotIfPossible() {
        pokeViewModel.updatePokemonAttacks(teamIndex) // save chosen attacks
        extractValuesAndUpdatePokemonInTeam(
            createNewSlot = true,
            showSaveMessage = true
        ) // save chosen pokemon with details
        teamIndex = pokeViewModel.getEmptySlotFromTeam() // returns -1 if no empty slot
        if (teamIndex == -1) {
            fabSavePokemon?.isEnabled = false
            highlightSelectedPokemonSlot()
            binding.tilPokemonName.editText?.setText("")
        }
        isPokemonSelected = false
        pokeViewModel.setSelectedAttacks(emptyList())
        isAtLeast1PokemonInTeam = true
    }

    /** Gets the values from all editTexts etc and updates the team pokemon with them
     * @param createNewSlot if true, the user pressed the save button on the bottom and a new slot will be created */
    private fun extractValuesAndUpdatePokemonInTeam(
        createNewSlot: Boolean,
        showSaveMessage: Boolean
    ) {
        val ivList = tvEditTextMapIV.values.map { it?.text.toString().toIntOrNull() ?: 0 }
        val ivNames = tvEditTextMapIV.keys.map { it.text.toString() }
        val evList = tvEditTextMapEV.values.map { it?.text.toString().toIntOrNull() ?: 0 }
        val evNames = tvEditTextMapEV.keys.map { it.text.toString() }
        val gender = if (isGenderless) -1 else if (genderFlag) 1 else 0
        val level = binding.tilPokemonLevel.editText?.text.toString().toIntOrNull() ?: 100
        val pokemonData =
            TeamBuilderData(ivList, ivNames, evNames, evList, gender, level, selectedAbility)
        if (::selectedAbility.isInitialized) {
            pokeViewModel.updatePokemonValuesInTeam(
                createNewSlot = createNewSlot,
                showSaveMessage = showSaveMessage,
                pokemonDataFromUser = pokemonData,
                teamIndex = teamIndex,
            )
        }
    }
    //endregion

    //region functions to display pokemons/stats etc

    /** Displays the Pokemon Details or if its a new slot default values */
    private fun displayPokemonDetails(pkData: TeamPokemon?) {

        ignoreTextChanges =
            true // prevents AddTextChangedListener (Pokemonname) to submit a search result
        binding.tilPokemonName.editText?.setText(pkData?.pokemonInfos?.name ?: "")
        binding.rvPokeList.visibility = View.GONE // gets visible somehow if reentered the fragment
        ignoreTextChanges = false
        displaySlotOnIndex(teamIndex, pkData) // displays the card with image (slot 1-6)
        // abilitySpinner
        if (pkData?.pokemonId == 0) {
            binding.abilitySpinner.adapter = null
        } else {
            // abilities / spinner adapter
            val abilityList =
                pokeViewModel.mapAbilitiesDetail() // gets list of all possible abilities for that pokemon
            this.abilityList = abilityList.map { Pair(it.name, it.textShort) }
            val adapter = SpinnerAdapter(
                requireContext(),
                androidx.transition.R.layout.support_simple_spinner_dropdown_item,
                this.abilityList ?: listOf(Pair("", "")),
            )
            binding.abilitySpinner.adapter = adapter
        }
        isSpinnerInitialized = true

        // displays chosen attacks if there is any attack
        val attacks =
            listOf(pkData?.attackOne, pkData?.attackTwo, pkData?.attackThree, pkData?.attackFour)
        if (attacks.filterNotNull().isNotEmpty()) {
            pokeViewModel.setSelectedAttacks(attacks.filterNotNull()) // observer will display attacks
        }

        setUpGenderSwitch(pkData?.gender ?: 1)

        // display level etc into edit texts etc
        fillPokemonValuesIntoEditTextsAndTV(pkData)
    }

    /** Fills the chosen pokemon values into the edit texts, if the inserted pokemon is "empty" it will be
     *  filled with default values
     * @param pokemonData the infos of a pokemon (can also be a empty dataclass object to "reset" the editTexts etc
     */
    private fun fillPokemonValuesIntoEditTextsAndTV(
        pokemonData: TeamPokemon?,
    ) {
        // set level and stats
        binding.tilPokemonLevel.editText?.setText((pokemonData?.level ?: 100).toString())
        // set base values ( atk / def etc) + progress bars
        progressBarTVList.forEachIndexed { index, (progressBar, textview) ->
            val stats = pokemonData?.pokemonInfos?.stats
            if (!stats.isNullOrEmpty()) {
                progressBar.progress = pokemonData.pokemonInfos.stats[index].statValue
                textview.text = pokemonData.pokemonInfos.stats[index].statValue.toString()
            } else {
                progressBar.progress = 0
                textview.text = ""
            }
        }
        // ev/iv values and resulting values
        tvEditTextMapIV.values.forEachIndexed { index, editText ->
            val textToSet =
                if (pokemonData?.ivList?.isNotEmpty() == true) pokemonData.ivList[index].value else 31
            editText?.setText(textToSet.toString())
        }
        tvEditTextMapEV.values.forEachIndexed { index, editText ->
            val textToSet =
                if (pokemonData?.evList?.isNotEmpty() == true) pokemonData.evList[index].value else 0
            editText?.setText(textToSet.toString())
        }
        resultValuesList.forEachIndexed { index, _ ->
            getCalculatedValueAndSetToTV(index)
        }
        // selected ability
        val selectedAbilityPos =
            abilityList?.indexOfFirst { it.first == pokemonData?.abilityName } ?: -1
        if (selectedAbilityPos != -1) binding.abilitySpinner.setSelection(selectedAbilityPos)

    }

    private fun setUpGenderSwitch(pokemonGender: Int = 1) {
        // fill in chosen gender if its there or neutral if pokemon has no gender
        if (pokemonGender == -1) {
            // e.g. Legendary Pokemon have no gender
            isGenderless = true
            binding.switchMaleFemale.text = getString(R.string.gender_neutral)
            val neutralGenderIcon = ContextCompat.getDrawable(requireContext(), neutralIconRes)
            binding.switchMaleFemale.buttonDrawable = neutralGenderIcon
            binding.switchMaleFemale.isEnabled = false // disable switch
        } else {
            // normal behaviour
            isGenderless = false
            binding.switchMaleFemale.isEnabled = true
            binding.switchMaleFemale.isChecked = pokemonGender != 0 // sets the chosen gender
            updateGenderSwitchAppearance(pokemonGender != 0)
            binding.switchMaleFemale.setOnCheckedChangeListener { _, isChecked ->
                updateGenderSwitchAppearance(isChecked) // isChecked means pokemon is female
            }
        }
    }

    private fun updateGenderSwitchAppearance(isFemale: Boolean) {
        val iconRes = if (isFemale) femaleIconRes else maleIconRes
        val textRes = if (isFemale) femaleTextRes else maleTextRes
        binding.switchMaleFemale.buttonDrawable =
            ContextCompat.getDrawable(requireContext(), iconRes)
        binding.switchMaleFemale.text = getString(textRes)
    }

    /** Function to make the slots (and the bar) visible and load the imageUrls into the card,
     *  or a placeholder if there is no pokemon selected */
    private fun displaySlotOnIndex(index: Int, pokemon: TeamPokemon?) {
        // make the bar in which the pokemon will be displayed visible
        if (binding.teamSlots.clTeamSlots.visibility == View.GONE) {
            binding.teamSlots.clTeamSlots.visibility = View.VISIBLE
        }
        val (cardView, imageView) = cvIvPair[index] // get cardview imageview from list with index
        cardView.tag = index // save index to use it later
        val url = pokemon?.pokemonInfos?.imageUrl ?: ""
        val altUrl = pokemon?.pokemonInfos?.altImageUrl ?: ""
        val officialUrl = pokemon?.pokemonInfos?.officialImageUrl ?: ""
        loadAnyImage(imageView, url, altUrl, officialUrl)
        cardView.visibility = View.VISIBLE

        /** ClickListener to switch the slots */
        cardView.setOnClickListener {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime >= 700) { // only 1 click every 700ms
                lastClickTime = currentTime
                fabSavePokemon?.isEnabled = true
                teamIndex = index
                loadDataOfSelectedSlot()
            }
        }
        cardView.setOnLongClickListener {
            showDeletePokemonDialog(cardView.tag as Int)
            true
        }
    }

    private fun hidePokemonSlot(index: Int) {
        val (cardView, _) = cvIvPair[index] // get cardview imageview from list with index
        cardView.setOnLongClickListener(null)
        cardView.setOnClickListener(null)
        cardView.visibility = View.INVISIBLE
    }

    /** Loads the Data or resets it whether the clicked Slot is empty or not and selects the card and highlights it*/
    private fun loadDataOfSelectedSlot() {
        val pokemonToDisplay = pokeViewModel.pokemonTeam.value?.pokemons?.get(teamIndex)
        // new "empty" pokemon
        if (pokemonToDisplay == null || pokemonToDisplay.pokemonId == 0) {
            isPokemonSelected = false
            displayPokemonDetails(pokemonToDisplay)
        } else {
            // will load the details of a pokemon (attacks abilities ...)
            pokeViewModel.getSinglePokemonData(
                pokemonToDisplay.pokemonId,
                R.string.failed_load_single_pokemon_data
            ) {
                // callback
                isPokemonSelected = true
                displayPokemonDetails(pokemonToDisplay)
            }
        }
        highlightSelectedPokemonSlot()
    }

    /** Highlights the latest clicked/chosen Pokemon Slot */
    private fun highlightSelectedPokemonSlot() {
        val cvList = cvIvPair
        cvList.forEachIndexed { index, pair ->
            val cv = pair.first
            if (index == teamIndex) {
                val color = ContextCompat.getColor(requireContext(), R.color.cardViewHighlighted)
                cv.setCardBackgroundColor(color)
            } else {
                cv.setCardBackgroundColor(Color.TRANSPARENT)
            }
        }
    }

    /** Displays every Pokemon in the Team (and empty slots (?) and loads
     *  details for the pokemon on the actual team index */
    private fun displayTeamPokemon(team: PokemonTeam?) {
        team?.pokemons?.forEachIndexed { index, teamPokemon ->
            if (teamPokemon != null) {
                displaySlotOnIndex(index, teamPokemon) // shows image of every teampokemon
                if (index == teamIndex) {

                    loadDataOfSelectedSlot()
                    // to display selected attacks
                    val attackList = listOf(
                        teamPokemon.attackOne,
                        teamPokemon.attackTwo,
                        teamPokemon.attackThree,
                        teamPokemon.attackFour
                    )
                    if (attackList.any { it != null }) pokeViewModel.setSelectedAttacks(attackList.filterNotNull())
                }
            } else {
                // hide slot when null
                hidePokemonSlot(index)
            }
        }
    }

    /** Displays every chosen Attack, function will be invoked from observer */
    private fun showSelectedAttacks(selectedAttacks: List<AttacksData>) {
        Log.d("selectedAttacks", selectedAttacks.toString())
        if (selectedAttacks.isEmpty()) {
            binding.incChosenAttacks.root.visibility = View.GONE
            return
        }
        // a pokemon can have up to 4 attacks
        for (i in 0..3) {
            val (cardView, textView) = attacksCardList[i]
            val attack = selectedAttacks.getOrNull(i)
            if (attack != null) {
                binding.incChosenAttacks.root.visibility = View.VISIBLE
                cardView.visibility = View.VISIBLE
                textView.text = attack.name
                val colorRes = typeColorMap[attack.typeId]?.first
                    ?: -1 // map holds type colors for each pokemon type
                if (colorRes != -1) {
                    val color = ContextCompat.getColor(requireContext(), colorRes)
                    cardView.setCardBackgroundColor(color)
                }
            } else {
                cardView.visibility = View.GONE
            }
        }

    }

    //endregion

    //region dialogs

    /** Reminds the user */
    private fun showSaveDataDialog() {
        val context = context
        if (context != null) {
            MaterialAlertDialogBuilder(context)
                .setTitle(getString(R.string.unsaved_info))
                .setPositiveButton(getString(R.string.save)) { _, _ ->
                    showEnterTeamNameDialog()
                }
                .setNegativeButton(getString(R.string.discard_changes)) { _, _ ->
                    findNavController().navigateUp()
                }
                .create().show()
        }
    }

    fun showUnsavedChangesDialog() {
        // just show reminder dialogs if there are any data
        if (pokeViewModel.pokemonTeam.value?.pokemons?.any { it != null } == true) {
            if (!isEditTeamMode) {
                showSaveDataDialog()
            } else {
                showUpdateTeamDialog()
            }
        } else {
            findNavController().navigateUp()
        }
    }

    /** Shows a dialog which asks the user to enter a name for the team to insert it than */
    private fun showEnterTeamNameDialog() {
        if (isAdded) {
            val layoutInflater = LayoutInflater.from(requireContext())
            val view = layoutInflater.inflate(R.layout.popup_create_team_dialog, null)
            val inputLayout = view.findViewById<TextInputLayout>(R.id.tilTeamName)

            val errorText = getString(R.string.enter_name_error)
            inputLayout.error = errorText

            val dialog = MaterialAlertDialogBuilder(requireContext())
                .setView(view) // Use the inflated view
                .setNegativeButton(getString(R.string.cancel), null)
                .setPositiveButton(getString(R.string.save), null) // Initially set to null
                .create()

            dialog.setOnShowListener {
                val posButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                posButton.isEnabled = false // Initially disable the button

                inputLayout.editText?.addTextChangedListener {
                    inputLayout.error = if (it.isNullOrBlank()) errorText else null
                    posButton.isEnabled = !it.isNullOrBlank()
                } // user can save once he entered any name

                posButton.setOnClickListener {
                    val teamName = inputLayout.editText?.text.toString().trim()
                    val team = pokeViewModel.getPokemonTeam()

                    if (team == null) Snackbar.make(
                        view,
                        R.string.failed_to_insert_team,
                        Snackbar.LENGTH_SHORT
                    ).show()
                    else {
                        team.name = teamName // change the name and upload the team
                        firebaseViewModel.insertTeamToFireStore(team) { isSuccess ->
                            if (isSuccess) {
                                findNavController().navigateUp()
                            }
                        }
                    }
                    dialog.dismiss()
                }
            }
            dialog.show()
        }
    }

    /** Dialog to update or create a copy of a team when save-team button is clicked */
    private fun showUpdateTeamDialog() {
        if (isAdded) {
            val builder = MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.dialog_title_update_team))
                .setMessage(getString(R.string.dialog_text_update_team))
                .setPositiveButton(getString(R.string.yes_update)) { _, _ ->
                    val team = pokeViewModel.getPokemonTeam()
                    if (team != null) {
                        Snackbar.make(binding.root, " update team...", Snackbar.LENGTH_SHORT).show()
                        firebaseViewModel.updateTeam(team)
                        findNavController().navigateUp()

                    } else {
                        Snackbar.make(
                            binding.root,
                            "Failed to update team...",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }
                .setNeutralButton(getString(R.string.make_a_copy)) { _, _ ->
                    showEnterTeamNameDialog()
                }
                .setNegativeButton(getString(R.string.discard_changes)) { _, _ ->
                    findNavController().navigateUp()
                }
            val dialog = builder.create()
            dialog.show()
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).apply {
                typeface = Typeface.DEFAULT_BOLD
            }
        }
    }

    private fun showDeletePokemonDialog(teamIndex: Int) {
        if (isAdded) {
            val pokemonName = pokeViewModel.getNameFromTeamPokemon(teamIndex)
            val builder = MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.delete_pokemon_title, pokemonName))
                .setMessage(getString(R.string.delete_pokemon_confirmation, pokemonName))
                .setPositiveButton(getString(R.string.delete)) { _, _ ->
                    pokeViewModel.deletePokemonFromTeam(teamIndex)
                }
                .setNegativeButton(R.string.cancel, null)
            val dialog = builder.create()
            dialog.show()
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).apply { typeface = Typeface.DEFAULT_BOLD }
        }
    }
    //endregion

    //region text watcher behaviour


 /*   *//**
     * Adds a textWatcher to the editText that is given in as parameter that corrects numbers
     * higher than the maxValue Parameter to give them a maximal number to use, if 'isEvWatcher'
     * is true and sliderEditTextMap not null helper function is called to update the ev texts correctly
     *
     * @param editText the editText that was changed before
     * @param maxValue the highest number the editText can display
     * @param sliderEditTextMap map of EditTexts to their relating slider, optional (only given in for ev edit texts)
     * @param isEvWatcher if true helper function 'updateEvs' is called and the slider is corrected if the value is too high
     *//*
    fun addTextWatcher(
        editText: EditText,
        maxValue: Int,
        sliderEditTextMap: Map<EditText?, Slider>? = null,
        isEvWatcher: Boolean
    ) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(text: Editable) {
                val num = text.toString().toIntOrNull()
                // if input is higher than maxValue correct it to the maxValue
                if (num != null && num > maxValue) {
                    editText.setText(maxValue.toString())
                    editText.setSelection(editText.text.length) // set cursor to end of edittext
                }
                if (isEvWatcher && sliderEditTextMap != null) {
                    handleEvInputs(
                        editText,
                        sliderEditTextMap
                    ) // corrects the values if they're too high
                }
                // update the resulting value with the tag of the editText (represents a value 0-5 for each stat (hp, atk, def etc))
                val index = (editText.tag as? String)?.toIntOrNull()
                if (index != null) getCalculatedValueAndSetToTV(index) // sets the resulting values at the right of the screen next to IV Edit texts
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            }
        })
    }*/

    /*private fun addLevelTextWatcher(
        levelEditText: EditText?
    ) {
        val maxLevel = 100
        val minLevel = 1

        // Hinzufügen des Focus Change Listeners.
        levelEditText?.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) { // Wenn das EditText den Fokus verliert.
                val actualLevel = levelEditText.text.toString().toIntOrNull()
                    ?: minLevel // Standardwert auf minLevel setzen, wenn das Feld leer ist.
                val correctedLevel = when {
                    actualLevel < minLevel -> minLevel
                    actualLevel > maxLevel -> maxLevel
                    else -> actualLevel
                }

                if (levelEditText.text.toString() != correctedLevel.toString()) {
                    levelEditText.setText(correctedLevel.toString())
                    levelEditText.setSelection(levelEditText.text.length) // Setze den Cursor ans Ende des Texts.
                }

                // calculate the values on the new level
                (0..5).forEach {
                    getCalculatedValueAndSetToTV(it)
                }
            }
        }
    }*/

/*    *//**
     * Handles the correct usage of EV's (explanation: Every Pokemon can have up to 508 Ev's
     * distributed over all values, e.g. 252 in attack, 252 in hp and 4 in defense)
     * if the totalEVs Value is higher than 508 it will be corrected (e.g. if you enter 80 while you have just 50 points
     * left, it will be corrected to 50), also updates the slider to that value
     *
     * @param currentEditText the editText that was changed before
     * @param sliderEditTextMap map of all edit texts and their relating slider*//*

    fun handleEvInputs(currentEditText: EditText, sliderEditTextMap: Map<EditText?, Slider>) {
        val editTexts = sliderEditTextMap.keys.toList()
        val totalEvs = editTexts.sumOf { it?.text.toString().toIntOrNull() ?: 0 } // uncorrected
        var enteredValue = currentEditText.text.toString().toIntOrNull() ?: 0 // uncorrected
        val remainingEvs = maxEvs - (totalEvs - enteredValue)
        enteredValue = enteredValue.coerceAtMost(252)
            .coerceAtMost(remainingEvs) // correct the value if its too high

        // update/ correct the entered value if its too high
        if ((currentEditText.text.toString().toIntOrNull() ?: 0) > enteredValue) {
            // to prevent infinite loop between textwatcher and setText
            currentEditText.setText(enteredValue.toString())
            currentEditText.setSelection(currentEditText.text.length) // set cursor to end of the text
        }
        sliderEditTextMap[currentEditText]?.let {
            it.value = enteredValue.toFloat()
        }
        // update the text
        assignedEvs = editTexts.sumOf { it?.text.toString().toIntOrNull() ?: 0 }
        updateEvLeftTextView()
        // handles the condition of every slider (activated or not if e.g. 508 points are already assigned)
        sliderEditTextMap.forEach { (editText, slider) ->
            val isSliderValueZero = (editText?.text.toString().toIntOrNull() ?: 0) == 0
            // disables all sliders with value '0' (or empty) , if 508 points are assigned
            slider.isEnabled = !(assignedEvs >= maxEvs && isSliderValueZero)
        }
    }*/

    private fun updateEvLeftTextView(maxEvs: Int, assignedEvs: Int) {
        val text = getString(R.string.remaining_ev_s_508, (maxEvs - assignedEvs))
        binding.includeEvIvWindow.tvEvIvLeft.text = text
    }

    private fun getCalculatedValueAndSetToTV(index: Int) {
        // gets corresponsing editTexts/progressbar from map
        val (ivEditText, evEditText, progressBar) = calculationMap[index]
        if (progressBar.progress == 0) return // means no base value is filled in, so there is no pokemon right now to calculate stats for
        // calculates the resulting value a stat will have depending on ev/iv/level/baseStatVal
        val calcedVal = pokeViewModel.calculateStat(
            level = binding.tilPokemonLevel.editText?.text.toString().toIntOrNull() ?: 100,
            ivValue = ivEditText?.text?.toString()?.toIntOrNull() ?: 31,
            baseValue = progressBar.progress,
            evValue = evEditText?.text?.toString()?.toIntOrNull() ?: 0,
            isHp = index == 0 // index 0 means the editTexts is for HP, and those
            // is calculated slightly different than the others
        )
        val textView = resultValuesList[index]
        textView.text = if (calcedVal == 0) "" else calcedVal.toString()
    }
    //endregion
}