package com.example.pokinfo.ui.teambuilder

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
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
import androidx.recyclerview.widget.RecyclerView
import com.example.pokinfo.R
import com.example.pokinfo.adapter.home.detail.AbilityEffectText
import com.example.pokinfo.adapter.teamAndTeambuilder.AllPokemonAdapter
import com.example.pokinfo.adapter.teamAndTeambuilder.SpinnerAdapter
import com.example.pokinfo.data.maps.typeColorMap
import com.example.pokinfo.data.models.database.pokemon.PokemonForList
import com.example.pokinfo.data.models.database.pokemon.PokemonTypeName
import com.example.pokinfo.data.models.database.pokemon.StatValues
import com.example.pokinfo.data.models.firebase.AttacksData
import com.example.pokinfo.data.models.firebase.PokemonTeam
import com.example.pokinfo.data.models.firebase.TeamPokemon
import com.example.pokinfo.data.models.fragmentDataclasses.TeamBuilderData
import com.example.pokinfo.data.util.ImageAltLoader.loadAnyImage
import com.example.pokinfo.databinding.FragmentTeamBuilderBinding
import com.example.pokinfo.ui.teambuilder.dialogs.DeletePokemonDialogFragment
import com.example.pokinfo.ui.teambuilder.dialogs.EnterTeamNameDialogFragment
import com.example.pokinfo.ui.teambuilder.dialogs.SaveDataDialogFragment
import com.example.pokinfo.ui.teambuilder.dialogs.UpdateTeamDialogFragment
import com.example.pokinfo.ui.teambuilder.extensions.TeamBuilderMapCreater
import com.example.pokinfo.ui.teambuilder.extensions.TeamBuilderMaps
import com.example.pokinfo.ui.teambuilder.extensions.overrideNavigationLogic
import com.example.pokinfo.viewModels.FirebaseViewModel
import com.example.pokinfo.viewModels.SharedViewModel
import com.example.pokinfo.viewModels.factory.ViewModelFactory
import com.example.pokinfo.viewModels.teambuilder.StatManagerViewModel
import com.example.pokinfo.viewModels.teambuilder.StatsEnum
import com.example.pokinfo.viewModels.teambuilder.TeamBuilderViewModel
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.slider.Slider

class TeamBuilderFragment : Fragment(), SaveDataDialogFragment.SaveDataListener,
    DeletePokemonDialogFragment.DeletePokemonListener,
    EnterTeamNameDialogFragment.EnterTeamNameListener, UpdateTeamDialogFragment.UpdateTeamListener {

    //region variables
    private lateinit var binding: FragmentTeamBuilderBinding
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private val pokeViewModel: TeamBuilderViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application, sharedViewModel)
    }
    private val statViewModel: StatManagerViewModel by activityViewModels()
    private val firebaseViewModel: FirebaseViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application, sharedViewModel)
    }

    private val args: TeamBuilderFragmentArgs by navArgs()
    private var isEditTeamMode = false // changes behaviour of some functions like save team

    private var pokeListAdapter: AllPokemonAdapter? = null
    private var abilityList: List<AbilityEffectText> =
        emptyList() // list of all abilities a pokemon can have


    // maps to easier run things in loops
    private lateinit var evEditAndSliderMap: LinkedHashMap<StatsEnum, Pair<EditText?, Slider>>
    private lateinit var tvEditTextMapEV: LinkedHashMap<TextView, EditText?>
    private lateinit var tvEditTextMapIV: LinkedHashMap<StatsEnum, Pair<TextView, EditText?>>
    private lateinit var cvIvPair: List<Pair<MaterialCardView, ImageView>>

    /** Holds triples of IV/EV EditText and Progress Bar of a stat (hp, atk, etc.) to calc the resulting values*/
    private lateinit var resultingValueList: Map<StatsEnum, TextView>
    private lateinit var progressBarTVList: List<Pair<ProgressBar, TextView>>
    private lateinit var attacksCardList: List<Pair<MaterialCardView, TextView>>

    private val maleIconRes = R.drawable.baseline_male_24
    private val maleTextRes = R.string.male
    private val femaleIconRes = R.drawable.baseline_female_24
    private val femaleTextRes = R.string.female
    private val neutralIconRes = R.drawable.baseline_transgender_24
    private var pokemonIsMale = true // stands for male initially
    private var isGenderless = false

    private var lastClickTime: Long = 0 // to prevent the user from clicking too fast
    private var isEvIvBarExpanded = false

    // flags to show snack bars if user needs to do anything before he can save pokemon/team
    private var isPokemonSelected = false
    private var isAtLeast1PokemonInTeam = false
    private var isSpinnerInitialized = false

    // one stat can have 252 max.
    private var ignoreTextChanges =
        false // to ignore text changes when sometimes the name edittext gets set
    private var fabSavePokemon: FloatingActionButton? = null
    //endregion

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTeamBuilderBinding.inflate(inflater, container, false)
        val maps = TeamBuilderMapCreater.createMaps(binding)
        assignMaps(maps)
        fabSavePokemon = activity?.findViewById(R.id.fabMain)
        overrideNavigationLogic()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fabSavePokemon?.isEnabled = true
    }

    private fun adjustRecyclerViewHeight(recyclerView: RecyclerView, maxItems: Int) {
        recyclerView.post {
            val totalItemCount = recyclerView.adapter?.itemCount ?: 0
            if (totalItemCount == 0) {
                recyclerView.layoutParams = recyclerView.layoutParams.apply {
                    height = 0
                }
                return@post
            }

            val oneItemHeight = (recyclerView.getChildAt(0)?.measuredHeight) ?: 0


            val totalHeight = (oneItemHeight * totalItemCount.coerceAtMost(maxItems)) +
                    (6 * dpToPx(recyclerView.context, (totalItemCount).coerceAtMost(maxItems)))

            recyclerView.layoutParams = recyclerView.layoutParams.apply {
                height = totalHeight
            }
        }
    }

    private fun dpToPx(context: Context, dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), context.resources.displayMetrics
        ).toInt()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val typeNames = pokeViewModel.pokemonTypeNames

        if (args.pokemonTeam != null) {
            // inserts team into live-data to display it with a observer
            pokeViewModel.insertTeam(pokemonTeam = args.pokemonTeam ?: PokemonTeam())
            isEditTeamMode = true
        }

        pokeViewModel.pokemonTeam.observe(viewLifecycleOwner) { team ->
            displayPokemonTeam(team)
        }

        // display search results in a recyclerview
        pokeViewModel.filteredListTeamBuilder.observe(viewLifecycleOwner) {
            pokeListAdapter?.let { pokeListAdapter ->
                pokeListAdapter.submitList(it) {
                    adjustRecyclerViewHeight(binding.rvPokeList, 5)
                }
                binding.rvPokeList.visibility =
                    if (it.isEmpty()) View.GONE else View.VISIBLE
            }
        }

        binding.tilPokemonName.editText?.addTextChangedListener { text ->
            if (!ignoreTextChanges) {
                pokeViewModel.filterPokemonList(text.toString())
            }
        }

        // create an adapter to let the user choose from all pokemon
        pokeViewModel.everyPokemon.observe(viewLifecycleOwner) {
            setupPokemonRecyclerView(typeNames)
        }

        pokeViewModel.pokemonAbilities.observe(viewLifecycleOwner) { abilityList ->
            updateAbilitySpinner(abilityList)
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
                        pokeViewModel.setSelectedAbility(abilityList[position].abilityId)
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

        binding.btnShowAttacks.setOnClickListener {
            saveChangesAndOpenAttacksFragment()
        }

        pokeViewModel.teamBuilderSelectedAttacks.observe(viewLifecycleOwner) {
            displaySelectedAttacks(it)
        }

        setUpGenderSwitch()

        statViewModel.evsMap.observe(viewLifecycleOwner) { stats ->
            setEvValuesToEditsAndSliders(stats)
        }

        statViewModel.calculatedStats.observe(viewLifecycleOwner) { statMap ->
            displayCalculatedValues(statMap)
        }

        statViewModel.ivsMap.observe(viewLifecycleOwner) { stats ->
            setIvValuesToEditTexts(stats)
        }

        statViewModel.level.observe(viewLifecycleOwner) { level ->
            binding.tilPokemonLevel.editText?.setText(level.toString())
        }

        binding.tilPokemonLevel.editText?.addTextChangedListener {
            val level = it.toString().toIntOrNull() ?: 100
            statViewModel.updateLevel(level)
        }

        statViewModel.remainingEvs.observe(viewLifecycleOwner) { remainingEvs ->
            updateRemainingEvsAndDisableSliders(remainingEvs)
        }

        setEvEditTextAndSliderListener()

        tvEditTextMapIV.forEach { (stat, tvEditTextPair) ->
            val editText = tvEditTextPair.second
            editText?.addTextChangedListener {
                val newValue = it.toString().toIntOrNull() ?: 31
                statViewModel.updateIvStat(stat, newValue)
            }
        }

        // expands the ev/iv layout
        binding.includeEvIvWindow.topLayout.setOnClickListener {
            showOrHideEvIvStats()
        }

        // save a pokemon (and create new slot if possible)
        fabSavePokemon?.setOnClickListener {
            if (!isPokemonSelected) {
                // show error message
                sharedViewModel.postMessage(R.string.you_need_to_select_1_pokemon)
                return@setOnClickListener
            }
            updateTeamAndCreateNewSlotIfPossible()
            toggleEvIvLayout(isEvIvBarExpanded = false)
        }

        // save team button (top - right)
        binding.btnTextSaveTeam.setOnClickListener {
            if (!isAtLeast1PokemonInTeam) {
                sharedViewModel.postMessage(getString(R.string.need_to_save_first_pokemon))
            } else {
                if (!isEditTeamMode) showEnterTeamNameDialog() else showUpdateTeamDialog()
            }
        }
    }

    private fun setEvEditTextAndSliderListener() {
        evEditAndSliderMap.forEach { (stat, editTextAndSlider) ->
            val (editText, slider) = editTextAndSlider
            editText?.addTextChangedListener {
                val newValue = it.toString().toIntOrNull() ?: 0
                statViewModel.updateEvStat(stat, newValue)
            }
            slider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
                override fun onStartTrackingTouch(p0: Slider) {}
                override fun onStopTrackingTouch(p0: Slider) {
                    var value = slider.value.toInt() // prevents recursive loop

                    val valueBefore = statViewModel.evsMap.value?.getValue(stat) ?: 0
                    val remainingEvs = (statViewModel.remainingEvs.value ?: 0) + valueBefore
                    val maxStatValue = remainingEvs.coerceAtMost(252)
                    value = value.coerceAtMost(maxStatValue)
                    statViewModel.updateEvStat(stat, value)
                    slider.value = value.toFloat()
                }
            })
        }
    }

    private fun assignMaps(maps: TeamBuilderMaps) {
        evEditAndSliderMap = maps.evEditAndSliderMap
        tvEditTextMapEV = maps.tvEditTextMapEV
        tvEditTextMapIV = maps.tvEditTextMapIV
        cvIvPair = maps.cvIvPair
        resultingValueList = maps.resultingValueList
        progressBarTVList = maps.progressBarTVList
        attacksCardList = maps.attacksCardList
    }

    private fun updateRemainingEvsAndDisableSliders(remainingEvs: Int?) {
        binding.includeEvIvWindow.tvEvIvLeft.text =
            getString(R.string.remaining_ev_s_508, remainingEvs)
        evEditAndSliderMap.forEach { (_, editTextAndSlider) ->
            val slider = editTextAndSlider.second
            val isSliderEnabled = !(remainingEvs == 0 && slider.value == 0f)
            slider.isEnabled = isSliderEnabled
        }
    }

    private fun setIvValuesToEditTexts(stats: Map<StatsEnum, Int>) {
        stats.forEach { (stat, value) ->
            val (_, editText) = tvEditTextMapIV[stat] ?: return@forEach
            if (editText?.text.toString().toIntOrNull() != value) {
                editText?.setText(value.toString())
            }
        }
    }

    private fun setEvValuesToEditsAndSliders(stats: Map<StatsEnum, Int>) {
        stats.forEach { (stat, value) ->
            val (editText, slider) = evEditAndSliderMap[stat] ?: return@forEach
            if (editText?.text.toString().toIntOrNull() != value) {
                editText?.setText(value.toString())
                slider.value = value.toFloat()
            }
        }
    }

    /** Displays the resulting values of each stat (atk, hp etc) depending on level assigned ev/iv and so on */
    private fun displayCalculatedValues(statMap: Map<StatsEnum, Int>?) {
        if (statMap != null) {
            statMap.forEach { (stat, value) ->
                val textView = resultingValueList[stat]
                textView?.text = if (value > 0) value.toString() else ""
            }
        } else {
            resultingValueList.forEach { (_, textView) ->
                textView.text = ""
            }
        }
    }

    /** saves changes to live-data for team and opens attack fragment to select pokemon attacks */
    private fun saveChangesAndOpenAttacksFragment() {
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

    private fun updateAbilitySpinner(abilityList: List<AbilityEffectText>) {
        if (abilityList.isEmpty()) {
            binding.abilitySpinner.adapter = null
        } else {
            // abilities / spinner adapter
            this.abilityList = abilityList
            val adapter = SpinnerAdapter(
                requireContext(),
                androidx.appcompat.R.layout.support_simple_spinner_dropdown_item,
                abilityList,
            )
            binding.abilitySpinner.adapter = adapter
            binding.btnShowAttacks.isEnabled = true
            isSpinnerInitialized = true
        }
    }

    /** created an adapter to show all chose able pokemon and */
    private fun setupPokemonRecyclerView(typeNames: List<PokemonTypeName>) {
        pokeListAdapter = AllPokemonAdapter(
            pokemonTypeNames = typeNames,
            onItemClicked = { pokemon ->
                handlePokemonClick(pokemon)
            }
        )
        binding.rvPokeList.adapter = pokeListAdapter
    }

    /** calls loading function when a pokemon is clicked */
    private fun handlePokemonClick(pokemon: PokemonForList) {
        // if a pokemon is clicked (selected)
        isPokemonSelected = true
        binding.tilPokemonName.editText?.setText("")
        binding.rvPokeList.visibility = View.GONE
        pokeViewModel.getSinglePokemonData(
            pokemon.id,
            R.string.failed_load_single_pokemon_data
        ) {
            // insert to team when loaded
            pokeViewModel.insertPokemonToTeam()
            fabSavePokemon?.isEnabled = true
        }
    }

    private fun showOrHideEvIvStats() {
        if (isPokemonSelected) {
            isEvIvBarExpanded = !isEvIvBarExpanded
            toggleEvIvLayout(isEvIvBarExpanded)
        } else {
            sharedViewModel.postMessage(R.string.you_need_to_select_1_pokemon)
        }
    }

    private fun toggleEvIvLayout(isEvIvBarExpanded: Boolean) {
        binding.includeEvIvWindow.expandLayout.visibility =
            if (isEvIvBarExpanded) View.VISIBLE else View.GONE
        binding.scrollView.post {
            binding.scrollView.smoothScrollTo(0, binding.includeEvIvWindow.root.bottom)
        }
    }

    //region update Team functions

    /** Updates the chosen Attacks to the Pokemon and than gets the other values
     *  like level gender etc to update the pokemon in the team with the chosen values */
    private fun updateTeamAndCreateNewSlotIfPossible() {
        extractValuesAndUpdatePokemonInTeam(
            createNewSlot = true,
            showSaveMessage = true
        ) // save chosen pokemon with details
        if (pokeViewModel.isTeamFull()) {
            fabSavePokemon?.isEnabled = false
            highlightSelectedPokemonSlot(teamIndex = -1)
        }
        binding.tilPokemonName.editText?.setText("")
        statViewModel.reset() // resets the evs ivs level base values
        binding.abilitySpinner.adapter = null
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
        val gender = if (isGenderless) -1 else if (pokemonIsMale) 1 else 0
        val pokemonData = TeamBuilderData(
            ivList = statViewModel.ivsList,
            evList = statViewModel.evsList,
            gender = gender,
            level = statViewModel.level.value ?: 100
        )
        pokeViewModel.updatePokemonValuesInTeam(
            createNewSlot = createNewSlot,
            showSaveMessage = showSaveMessage,
            pokemonDataFromUser = pokemonData,
        )
    }
    //endregion

    //region functions to display pokemon/stats etc

    /** Displays the Pokemon Details or if its a new slot default values */
    private fun displayPokemonDetails(pkData: TeamPokemon?) {
        ignoreTextChanges =
            true // prevents AddTextChangedListener (Pokemon name) to submit a search result
        binding.tilPokemonName.editText?.setText(pkData?.pokemonInfos?.name ?: "")
        binding.rvPokeList.visibility = View.GONE // gets visible somehow if reentered the fragment
        ignoreTextChanges = false

        displaySlotOnIndex(
            pokeViewModel.teamIndex,
            pkData
        ) // displays the card with image (slot 1-6)

        val attacks =
            listOf(pkData?.attackOne, pkData?.attackTwo, pkData?.attackThree, pkData?.attackFour)
        pokeViewModel.setSelectedAttacks(attacks.filterNotNull()) // observer will display attacks

        pkData?.evList?.let { statViewModel.insertEvs(it) }
        pkData?.ivList?.let { statViewModel.insertIvs(it) }
        pkData?.level?.let { statViewModel.updateLevel(it) }

        setUpGenderSwitch(pkData?.gender ?: 1)

        val selectedAbilityPos =
            abilityList.indexOfFirst { it.abilityId == pkData?.abilityId }
        if (selectedAbilityPos != -1) binding.abilitySpinner.setSelection(selectedAbilityPos)

        pkData?.pokemonInfos?.baseStats?.let {
            statViewModel.setPokemonBaseValues(it)
            updateProgressBars(it)
        } // updates calculated values ui
    }

    /** Update progress bars to display the base Stats of a pokemon */
    private fun updateProgressBars(stats: List<StatValues>) {
        progressBarTVList.forEachIndexed { index, (progressBar, textview) ->
            if (stats.isNotEmpty()) {
                progressBar.progress = stats[index].statValue
                textview.text = stats[index].statValue.toString()
            } else {
                progressBar.progress = 0
                textview.text = ""
            }
        }
    }

    /** Sets up gender switch, disables it when pokemon gender is neutral */
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

    /** Changes switch icon and text depending of the pokemon gender */
    private fun updateGenderSwitchAppearance(isFemale: Boolean) {
        val iconRes = if (isFemale) femaleIconRes else maleIconRes
        val textRes = if (isFemale) femaleTextRes else maleTextRes
        binding.switchMaleFemale.buttonDrawable =
            ContextCompat.getDrawable(requireContext(), iconRes)
        binding.switchMaleFemale.text = getString(textRes)
    }

    /** Loads the Data or resets it whether the clicked Slot is empty or not and selects the card and highlights it*/
    private fun loadAndDisplaySelectedPokemon() {
        val pokemonToDisplay = pokeViewModel.getPokemonOnTeamIndex()
        // new "empty" pokemon
        if (pokemonToDisplay == null || pokemonToDisplay.pokemonId == 0) {
            isPokemonSelected = false
            statViewModel.reset()
            binding.abilitySpinner.adapter = null
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

    /** Function to make the slots (and the bar) visible and load the imageUrls into the card,
     *  or a placeholder if there is no pokemon selected */
    private fun displaySlotOnIndex(index: Int, pokemon: TeamPokemon?) {
        // make the bar in which the pokemon will be displayed visible
        if (binding.teamSlots.clTeamSlots.visibility == View.GONE) {
            binding.teamSlots.clTeamSlots.visibility = View.VISIBLE
        }
        val (cardView, imageView) = cvIvPair[index] // get card view imageview from list with index

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
                pokeViewModel.setNewTeamIndex(index)
                loadAndDisplaySelectedPokemon()
            }
        }
        cardView.setOnLongClickListener {
            showDeletePokemonDialog(cardView.tag as Int)
            true
        }

    }

    /** Highlights the latest clicked/chosen Pokemon Slot */
    private fun highlightSelectedPokemonSlot(teamIndex: Int = pokeViewModel.teamIndex) {
        val cardViewList = cvIvPair.map { it.first }
        cardViewList.forEachIndexed { index, cardView ->
            if (index == teamIndex) {
                val color = ContextCompat.getColor(requireContext(), R.color.cardViewHighlighted)
                cardView.setCardBackgroundColor(color)
            } else {
                cardView.setCardBackgroundColor(Color.TRANSPARENT)
            }
        }
    }

    private fun hidePokemonSlot(index: Int) {
        val (cardView, _) = cvIvPair[index]
        if (cardView.visibility == View.VISIBLE) {
            cardView.setOnLongClickListener(null)
            cardView.setOnClickListener(null)
            cardView.visibility = View.INVISIBLE
        }
    }

    /** Displays every Pokemon in the Team (and empty slots (?) and loads
     *  details for the pokemon on the actual team index */
    private fun displayPokemonTeam(team: PokemonTeam?) {
        team?.pokemons?.forEachIndexed { index, teamPokemon ->
            if (teamPokemon != null) {
                displaySlotOnIndex(index, teamPokemon) // shows image of every team pokemon
                if (index == pokeViewModel.teamIndex) {
                    loadAndDisplaySelectedPokemon()
                }
            } else {
                // hide slot when null
                hidePokemonSlot(index)
            }
        }
    }

    /** Displays every chosen Attack, function will be invoked from observer */
    private fun displaySelectedAttacks(selectedAttacks: List<AttacksData>) {
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
                val colorRes = typeColorMap[attack.typeId]?.first ?: continue
                val color = ContextCompat.getColor(requireContext(), colorRes)
                cardView.setCardBackgroundColor(color)

            } else {
                cardView.visibility = View.GONE
            }
        }
    }

    //endregion

    //region dialogs

    private fun showSaveDataDialog() {
        SaveDataDialogFragment().show(childFragmentManager, "SaveDataDialog")
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
        EnterTeamNameDialogFragment().show(childFragmentManager, "EnterTeamNameDialog")
    }

    /** Dialog to update or create a copy of a team when save-team button is clicked */
    private fun showUpdateTeamDialog() {
        UpdateTeamDialogFragment().show(childFragmentManager, "UpdateTeamDialog")
    }

    private fun showDeletePokemonDialog(index: Int) {
        DeletePokemonDialogFragment.newInstance(index).show(childFragmentManager, "DeletePokemonDialog")
    }
    //endregion

    //region dialog callbacks

    override fun onSave() {
        showEnterTeamNameDialog()
    }

    override fun onUpdate() {
        val team = pokeViewModel.getPokemonTeam()
        if (team != null) {
            val success = firebaseViewModel.updateTeam(team)
            if (success) findNavController().navigateUp()
        }
    }

    override fun onCopy() {
        showEnterTeamNameDialog()
    }

    override fun onDiscard() {
        findNavController().navigateUp()
    }

    override fun onDeleteConfirmed(index: Int) {
        pokeViewModel.deletePokemonFromTeam(index)
    }

    override fun onTeamNameEntered(name: String) {
        val team = pokeViewModel.getPokemonTeam()
        if (team != null) {
            team.name = name
            firebaseViewModel.insertTeamToFireStore(team) { success ->
                if (success) findNavController().navigateUp()
            }
        } else {
            sharedViewModel.postMessage(R.string.failed_to_insert_team)
        }
    }
    //endregion

}