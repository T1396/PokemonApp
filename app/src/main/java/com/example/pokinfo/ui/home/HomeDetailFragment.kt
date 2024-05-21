package com.example.pokinfo.ui.home


import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import com.example.pokinfo.R
import com.example.pokinfo.adapter.home.detail.AbilityEffectText
import com.example.pokinfo.adapter.home.detail.AbilitySheetAdapter
import com.example.pokinfo.adapter.home.detail.ImageViewPagerAdapter
import com.example.pokinfo.adapter.home.detail.ImagesAdapter
import com.example.pokinfo.adapter.home.detail.PokedexEntryAdapter
import com.example.pokinfo.data.maps.typeColorMap
import com.example.pokinfo.data.models.database.pokemon.LanguageNames
import com.example.pokinfo.data.models.database.pokemon.PkEvolutionDetails
import com.example.pokinfo.data.models.database.pokemon.PkMoveVersionGroupDetail
import com.example.pokinfo.data.models.database.pokemon.PkNames
import com.example.pokinfo.data.models.database.pokemon.PokemonAbilitiesList
import com.example.pokinfo.data.models.database.pokemon.PokemonData
import com.example.pokinfo.data.models.database.pokemon.PokemonTypeName
import com.example.pokinfo.data.models.database.pokemon.VersionNames
import com.example.pokinfo.data.util.NoScrollLayoutManager
import com.example.pokinfo.data.util.ScrollStateListener
import com.example.pokinfo.data.util.UIState
import com.example.pokinfo.databinding.FragmentHomeDetailBinding
import com.example.pokinfo.databinding.IncludeHomeDetailBottomSheetBinding
import com.example.pokinfo.ui.misc.dialogs.openPokemonListDialog
import com.example.pokinfo.ui.misc.dialogs.showConfirmationDialog
import com.example.pokinfo.viewModels.PokeViewModel
import com.example.pokinfo.viewModels.SharedViewModel
import com.github.mikephil.charting.charts.RadarChart
import com.github.mikephil.charting.data.RadarData
import com.github.mikephil.charting.data.RadarDataSet
import com.github.mikephil.charting.data.RadarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Locale


class HomeDetailFragment : Fragment() {

    private var _binding: FragmentHomeDetailBinding? = null
    private val args: HomeDetailFragmentArgs by navArgs()

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private val pokeViewModel: PokeViewModel by activityViewModels()
    private lateinit var pokedexAdapter: PokedexEntryAdapter
    private var snapHelperPokedexTexts: SnapHelper? = null
    private lateinit var sheetBinding: IncludeHomeDetailBottomSheetBinding
    private lateinit var statMap: Map<String, Pair<TextView, TextView>>
    // links the english name of the stat to the corresponding textViews


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sheetBinding = binding.bottomSheetBinding
        createStatMap()


        val swipeRefreshLayout = binding.swipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {
            swipeRefreshLayout.isRefreshing = false
            pokeViewModel.getSinglePokemonData(args.pokemonId, R.string.failed_load_single_pokemon_data) {

            }

        }


        fillImagePager(args.pokemonId)

        val languageNames = pokeViewModel.languageNames
        val versionNames = pokeViewModel.versionNames


        pokeViewModel.clickedPokemon.observe(viewLifecycleOwner) { uiState ->
            val typeNames = pokeViewModel.pokemonTypeNames
            val languageId = pokeViewModel.getLangId()
            when (uiState) {
                is UIState.Loading -> {
                    // show loading indicator
                    binding.clLoadingIndicator.visibility = View.VISIBLE
                    //sheetBinding.wholeLayout.visibility = View.INVISIBLE
                    binding.tvError.visibility = View.GONE
                    //binding.scrollView.setOnTouchListener { v, event -> true }
                }

                is UIState.Success -> {
                    binding.clLoadingIndicator.visibility = View.GONE
                    sheetBinding.wholeLayout.visibility = View.VISIBLE
                    binding.tvError.visibility = View.GONE
                    fillUI(uiState.data, languageId, typeNames, versionNames, languageNames)
                    // Re-enable scrolling when data successfully loaded
                    //binding.scrollView.setOnTouchListener(null)  // Remove custom touch listener

                }

                is UIState.Error -> {
                    binding.clLoadingIndicator.visibility = View.GONE

                    binding.tvError.visibility = View.VISIBLE
                    //binding.scrollView.setOnTouchListener { v, event -> true }

                }
            }
        }
    }


    private fun fillUI(
        pkData: PokemonData,
        languageId: Int,
        typeNames: List<PokemonTypeName>,
        versionNames: List<VersionNames>,
        languageNames: List<LanguageNames>
    ) {
        val name = pkData.pokemon.displayName
        val specieNames = pkData.specyNames
        val translatedName = specieNames.find { it.languageId == languageId }?.name

        if (pkData.formData.isNotEmpty()) {
            enableFormDialog(translatedName, typeNames)
        }
        if (pkData.pokemon.id == pkData.specyData?.id) {
            //overlayView = sheetBinding.arrowOverlay
            if (pkData.evolutionChain != null && !pkData.evolutionDetails.isNullOrEmpty()) {
                createEvolutionView(pkData.specyData.id, pkData.evolutionDetails)
                sheetBinding.cvEvolutions.visibility = View.VISIBLE
                sheetBinding.tvEvolutionsHeader.text = getString(R.string.evolution_header, translatedName)
            }
        }

        sheetBinding.tvPokemonName.text = name

        sheetBinding.tvPokedexNr.text = pkData.specyData?.id.toString()

        loadTypeCardView(
            pkData.pokemon.primaryType.typeId,
            sheetBinding.cvTypeOne,
            sheetBinding.ivTypePokemon1,
            sheetBinding.tvTypePokemon1,
            typeNames.find { it.typeId == pkData.pokemon.primaryType.typeId }?.name
                ?: "Error"
        )
        loadTypeCardView(
            pkData.pokemon.secondaryType?.typeId,
            sheetBinding.cvTypeTwo,
            sheetBinding.ivTypePokemon2,
            sheetBinding.tvTypePokemon2,
            typeNames.find { it.typeId == pkData.pokemon.secondaryType?.typeId }?.name
                ?: "Error"
        )


        val statValues = pkData.pokemon.pkStatInfos
        // Bind Stat Values (Atk, Def etc.) in the right cardview
        val statList = mutableListOf<Float>()
        statValues.forEach {
            val bindings = statMap[it.defaultName] // load the correct textViews,
            bindings?.first?.text = it.baseStat.toString()
            statList.add(it.baseStat.toFloat())
        }
        fillProgressBars(statList)
        // fill values to left CardView (radar chart)
        fillRadarChart(
            sheetBinding.radarChart, statList.toFloatArray()
        )

        setNamePopupAndPokedexAdapter(specieNames, versionNames, languageNames)


        // Abilities Card
        val abilitiesList = pokeViewModel.mapAbilitiesDetail()
        setupAbilityChips(abilitiesList, pkData.abilitiesPokemonList ?: emptyList(), typeNames)



        sheetBinding.btnShowAttacks.setOnClickListener {
            openAttacksList(pkData.versionGroupDetails)
        }
        // sets a chip for each generation a pokemon has move learn data and submits the first chip info


        val weight = String.format(Locale.ROOT, "%.1f", pkData.pokemon.weight / 10.0)
        // Basic Info Card
        val height = String.format(Locale.ROOT, "%.1f", pkData.pokemon.height / 10.0)
        sheetBinding.tvHeightValue.text = getString(R.string.m, height)
        sheetBinding.tvWeightValue.text = getString(R.string.kg, weight)
        sheetBinding.tvCatchrate.text = pkData.specyData?.captureRate.toString()
        sheetBinding.tvHappinessValue.text = pkData.specyData?.baseHappiness.toString()
        sheetBinding.tvGenderValues.text = setGenderText(pkData.specyData?.genderRate ?: -1)


        // map of all sprites categorized on their origins
        pokeViewModel.extractSpritesWithCategories { sortedSpriteMap ->

            val imageAdapter = ImagesAdapter(viewLifecycleOwner.lifecycleScope)
            // creates chip buttons for each category
            createButtons(sortedSpriteMap, imageAdapter)
            // custom layoutManager to disable scrolling
            sheetBinding.rvImages.layoutManager = NoScrollLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
            ).also {
                it.setScrollEnabled(false)
                sheetBinding.rvImages.addOnScrollListener(ScrollStateListener(it))
            }
            sheetBinding.rvImages.adapter = imageAdapter

            // gets the first key name to give it to the adapter as parameter to display it
            val firstName = sortedSpriteMap.keys.firstOrNull()
            sortedSpriteMap[firstName]?.let { imageList ->
                imageAdapter.setCategoryName(firstName ?: "No Data found")
                imageAdapter.submitList(imageList)
            }
        }


        sheetBinding.ibPicturesNext.setOnClickListener {
            val layoutManager = sheetBinding.rvImages.layoutManager as NoScrollLayoutManager
            val currentPosition = layoutManager.findFirstVisibleItemPosition()
            val itemCount = sheetBinding.rvImages.adapter?.itemCount ?: 0
            if (currentPosition < itemCount - 1) {
                layoutManager.setScrollEnabled(true)
                sheetBinding.rvImages.smoothScrollToPosition(currentPosition + 1)
            }
        }

        sheetBinding.ibPicturesPrevious.setOnClickListener {
            val layoutManager = sheetBinding.rvImages.layoutManager as NoScrollLayoutManager
            val currentPosition = layoutManager.findFirstVisibleItemPosition()
            if (currentPosition > 0) {
                layoutManager.setScrollEnabled(true)
                sheetBinding.rvImages.smoothScrollToPosition(currentPosition - 1)
            }
        }
    }

    private fun setupAbilityChips(
        abilitiesList: List<AbilityEffectText>,
        abilitiesPokemonList: List<PokemonAbilitiesList>,
        typeNames: List<PokemonTypeName>
    ) {
        val chipGroup = sheetBinding.chipGroupAbilities
        chipGroup.removeAllViews()
        abilitiesList.forEach { ability ->
            val chip = Chip(requireContext()).apply {
                text = if (ability.isHidden) "${ability.name} (Hidden)" else ability.name
                isClickable = true
                isCheckable = false
                setOnClickListener {
                    showAbilitySheet(ability, abilitiesPokemonList, typeNames)
                }
            }
            chipGroup.addView(chip)
        }
    }

    private fun showAbilitySheet(
        ability: AbilityEffectText,
        abilitiesPokemonList: List<PokemonAbilitiesList>,
        typeNames: List<PokemonTypeName>
    ) {
        val pokemonIdList = abilitiesPokemonList.find { it.abilityId == ability.abilityId }
            ?.pokemonIds ?: emptyList()
        pokeViewModel.getPokemonListWhoHaveAbility(pokemonIdList) { pokemonList ->
            requireActivity().runOnUiThread {
                val bottomSheetDialog = BottomSheetDialog(requireContext())

                val view = layoutInflater.inflate(R.layout.layout_ability_bottom_sheet, null)
                val abilityNameTV = view.findViewById<TextView>(R.id.tvAbilityNameSheet)
                abilityNameTV.text = ability.name


                val recyclerView = view.findViewById<RecyclerView>(R.id.rvAbilitySheet)
                val adapter = AbilitySheetAdapter(ability, typeNames) { pokemonId ->
                    showConfirmationDialog(
                        onConfirm = {
                            pokeViewModel.getSinglePokemonData(pokemonId)
                            bottomSheetDialog.dismiss()
                        }
                    )
                }

                recyclerView.adapter = adapter
                adapter.submitList(pokemonList.sortedBy { it.speciesId })
                bottomSheetDialog.setContentView(view)
                bottomSheetDialog.show()


                val bottomSheetInternal = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout?
                val behavior = BottomSheetBehavior.from(bottomSheetInternal!!)

                val displayMetrics = Resources.getSystem().displayMetrics
                val screenHeight = displayMetrics.heightPixels
                behavior.peekHeight = (screenHeight / 2)
                behavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED


            }
        }

    }

    /** Gets the evolutionStages of a pokemon and sets this stage to a custom evolution view to show the evolution tree */
    private fun createEvolutionView(speciesId: Int, evolutionDetails: List<PkEvolutionDetails>) {
        val evolutionStages = pokeViewModel.startEvolutionTree(
            clickedSpeciesId = speciesId,
            evolutionDetails = evolutionDetails
        ).toMutableList()
        val evolutionView = sheetBinding.evolutionArrows
        evolutionView.setPokemon(evolutionStages.removeFirst())
    }

    /** Shows an info icon if the pokemon has various forms and opens a dialog to navigate to them */
    private fun enableFormDialog(
        translatedName: String?,
        typeNames: List<PokemonTypeName>
    ) {
        pokeViewModel.loadFormDetails { pokemonFormList ->
            binding.ibFormInfos.visibility = View.VISIBLE
            binding.ibFormInfos.setOnClickListener {
                openPokemonListDialog(
                    listOfPokemon = pokemonFormList,
                    title = getString(R.string.pokemon_forms, translatedName ?: "No name found"),
                    typeNames = typeNames,
                    navigateCallback = { pokemonId ->
                        pokeViewModel.getSinglePokemonData(
                            pokemonId,
                            R.string.failed_load_single_pokemon_data
                        )
                    }
                )
            }
        }
    }

    /** Gets a set of images a pokemon has and fills them into the top pager */
    private fun fillImagePager(pokemonId: Int) {
        pokeViewModel.getPokemonImagesWithNames(pokemonId) { imageList ->
            val resolvedList = imageList.map { nameIdImagePair ->
                val text = getString(nameIdImagePair.first)
                Pair(text, nameIdImagePair.second)
            }
            requireActivity().runOnUiThread {
                binding.pager.adapter = ImageViewPagerAdapter(resolvedList)
            }
        }
    }

    /** Sets the on click listener for the Nr / Name Card view to show names in different languages
     * and creates the pokedexAdapter  */
    private fun setNamePopupAndPokedexAdapter(
        specieNames: List<PkNames>,
        versionNames: List<VersionNames>,
        languageNames: List<LanguageNames>
    ) {
        // once Language Names are loaded set showNames on click listener
        sheetBinding.cvPokemonNumberName.setOnClickListener {
            showNamesPopup(specieNames, languageNames)
        }
        //sheetBinding.rvPokedexFlavor.layoutManager = NoScrollLayoutManager(requireContext())
        // bind Pokedex Entries Card / recyclerview
        pokedexAdapter = PokedexEntryAdapter(
            languageNames,
            versionNames,
            { openLanguageMenu(languageNames, pokedexAdapter) }, // callback fun 1
            { versionId -> onVersionSelected(versionId, pokedexAdapter) } // callback fun2
        )
        sheetBinding.rvPokedexFlavor.adapter = pokedexAdapter
        val pokedexList = pokeViewModel.filterPokedexInfo()
        pokedexAdapter.submitList(pokedexList)
        // to prevent more than 1 snap helper gets attached
        if (snapHelperPokedexTexts == null) {
            snapHelperPokedexTexts = PagerSnapHelper()
            snapHelperPokedexTexts?.attachToRecyclerView(sheetBinding.rvPokedexFlavor)
        }
    }

    /** Shows a material dialog with all names of a pokemon */
    private fun showNamesPopup(specieNames: List<PkNames>, languageNames: List<LanguageNames>) {
        val namesWithLanguage = specieNames.map { nameInfo ->
            val languageName = languageNames.find { it.languageId == nameInfo.languageId }?.name
                ?: "No language data"
            "${nameInfo.name} - $languageName"
        }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Pokemon Names in other Languages")
            .setItems(namesWithLanguage) { _, _ ->
            }
            .show()
    }

    /**
     * Adds a chip button for each Image Origin to add filter/submit functions to every chip
     *
     * @param sortedMap sorted Map of imageSources a pokemon has (e.g. "Home" or "Gen1 Yellow") to the url lists
     * @param adapter reference to the adapter, used to give the chips a submit on click functionality
     */
    private fun createButtons(
        sortedMap: Map<String, List<Pair<String, String>>>,
        adapter: ImagesAdapter
    ) {
        // list of all origins e.g. official artwork, home, showdown +++
        val keys = sortedMap.keys.toList()
        val chipGroup = sheetBinding.chipGroupImage
        chipGroup.removeAllViews() // remove old chips that may were created with a pokemon before

        if (keys.isEmpty()) {
            return
        }

        keys.forEachIndexed { index, text ->
            val chip = Chip(requireContext())
            chip.text = text
            chip.isClickable = true
            chip.isCheckable = true
            chip.isFocusable = false
            chipGroup.addView(chip)


            // e.g. keys[0] would be mostly "Default" than "Home" etc
            val categoryName = keys[index]
            // each chip submits images from the relating origin with the name of the origin
            chip.setOnClickListener {
                sortedMap[categoryName]?.let { imageList ->
                    adapter.setCategoryName(categoryName)
                    adapter.submitList(imageList) {
                        sheetBinding.rvImages.scrollToPosition(0)
                    }
                }
            }
        }

        // keeps track of the checked chip button so that it cant be disabled
        var lastCheckedId = chipGroup[0].id
        chipGroup.check(lastCheckedId) // check the first chip -> submits the first list of images
        chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isEmpty()) {
                group.check(lastCheckedId)
            } else {
                // saves the lastly checked chip id so if it is clicked again (unchecked) it gets checked immediately
                lastCheckedId = checkedIds[0]
            }
        }
    }

    /**
     * Opens a dialog when pokedex Info languageButton is clicked
     * @param languageList the list of all languages to be chosen from
     * @param adapter reference to pass to submitPokedexInfo function
     *
     */
    private fun openLanguageMenu(
        languageList: List<LanguageNames>,
        adapter: PokedexEntryAdapter,
    ) {
        val nameList = languageList.map { it.name }.toTypedArray()
        val title = getString(R.string.choose_language)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setItems(nameList) { _, which ->
                val languageId = which + 1
                val list = pokeViewModel.filterPokedexInfo(languageId)
                adapter.submitList(list)
            }.show()
    }

    /** Changed Version Entry for the Pokemon Pokedex Data */
    private fun onVersionSelected(versionId: Int, adapter: PokedexEntryAdapter) {
        val position =
            adapter.getPositionOfVersion(versionId)
        if (position != -1) {
            sheetBinding.rvPokedexFlavor.scrollToPosition(position)
        }
    }

    /**
     * Creates a text depending on the gender rate
     *
     * @param genderRate gender ratio of the pokemon in eight (8 = 100% female, 1 = 12.5% female, -1 = genderless)
     */
    private fun setGenderText(genderRate: Int): String {
        if (genderRate == -1) return "Neutral"
        val femaleText = "${genderRate * 12.5} ♀"
        val maleText = if (genderRate == 8) "" else " - ${(8 - genderRate) * 12.5} ♂"
        return "$femaleText$maleText"
    }

    private fun openAttacksList(versionGroupDetails: List<PkMoveVersionGroupDetail>) {
        if (versionGroupDetails.isEmpty()) {
            sharedViewModel.postMessage(R.string.no_attacks_available)
            return
        }
        findNavController().navigate(
            HomeDetailFragmentDirections.actionNavHomeDetailToFullScreenDialogFragment(
                isSelectionMode = false
            )
        )
    }

    //region Functions to fill Bars / TypeCardViews/ Radar chart
    private fun fillProgressBars(statList: List<Float>) {
        val listBindings = listOf(
            sheetBinding.includeStats.hpBar,
            sheetBinding.includeStats.atkBar,
            sheetBinding.includeStats.defBar,
            sheetBinding.includeStats.spAtkBar,
            sheetBinding.includeStats.spDefBar,
            sheetBinding.includeStats.initBar,
        )
        listBindings.forEachIndexed { index, bar ->
            bar.progress = statList[index].toInt()
        }
    }

    /**
     * Changes the colour of the Type Card views depending on the Pokemon type - ID
     *
     * @param typeId the ID of the pokemon type
     * @param cardView Card view to be edited
     * @param imageView to Display Type Icon
     * @param textView to set Typename Text
     * @param name the TypeName to be displayed in the TextView
     */
    private fun loadTypeCardView(
        typeId: Int?,
        cardView: MaterialCardView,
        imageView: ImageView,
        textView: TextView,
        name: String
    ) {
        // if typeId is null pokemon does not have second type
        if (typeId == null) {
            cardView.visibility = View.INVISIBLE
            return
        }
        // gets color and icon resource ids from map value on the specific typeId
        val colorRes = typeColorMap[typeId]?.first ?: -1
        val iconRes = typeColorMap[typeId]?.second ?: -1
        val icon = ContextCompat.getDrawable(requireContext(), iconRes)
        val color = ContextCompat.getColor(requireContext(), colorRes)
        if (color != -1) cardView.setCardBackgroundColor(color)
        if (iconRes != -1) imageView.setImageDrawable(icon)
        textView.text = name
    }


    private fun fillRadarChart(
        radarChart: RadarChart, valArray: FloatArray
    ) {
        val entries = ArrayList<RadarEntry>(List(6) { RadarEntry(0f) }).apply {
            valArray.forEachIndexed { index, statValue ->
                when (index) {
                    3 -> set(5, RadarEntry(scale(statValue)))
                    5 -> set(3, RadarEntry(scale(statValue)))
                    else -> set(index, RadarEntry(scale(statValue)))
                }
            }
        }

        val statNameList = listOf(
            getString(R.string.base_value_name_hp_short),
            getString(R.string.base_value_name_atk_short),
            getString(R.string.base_value_name_def_short),
            getString(R.string.base_value_name_init_short),
            getString(R.string.base_value_name_spDef_short),
            getString(R.string.base_value_name_spAtk_short)
        )

        // create set with stats and change some minor values
        val color = MaterialColors.getColor(
            requireContext(),
            com.google.android.material.R.attr.colorOnBackground,
            0
        )
        val set = RadarDataSet(entries, "")
        set.color = color
        set.lineWidth = 0.5f
        set.setDrawValues(false)
        set.setDrawFilled(true)
        set.fillColor = Color.argb(80, 20, 40, 255)

        val data = RadarData(set)

        radarChart.xAxis.valueFormatter = IndexAxisValueFormatter(statNameList)
        radarChart.xAxis.textColor = color
        radarChart.xAxis.yOffset = 5f

        radarChart.xAxis.granularity = 1f


        radarChart.yAxis.axisMinimum = 0f
        radarChart.xAxis.textSize = 14f
        radarChart.yAxis.axisMaximum = 255f
        radarChart.yAxis.setDrawLabels(false)

        radarChart.legend.isEnabled = false
        radarChart.animation = null
        radarChart.isRotationEnabled = false
        radarChart.description.isEnabled = false
        radarChart.yAxis.maxWidth = 255f
        radarChart.xAxis.axisMaximum = 255f
        radarChart.xAxis.axisMaximum = 255f
        radarChart.yAxis.maxWidth = 200f
        radarChart.webLineWidth = 2f
        radarChart.webLineWidthInner = 1f
        radarChart.webColorInner = Color.GRAY
        radarChart.webAlpha = 20
        radarChart.data = data
        radarChart.isHighlightPerTapEnabled = false
        // push values to radarChart
        radarChart.invalidate()
    }

    private fun scale(value: Float): Float {
        return (value / 255f) * 280f
    }
    //endregion

    private fun createStatMap() {
        statMap =
            mapOf(
                "hp" to Pair(sheetBinding.includeStats.tvHpVal, sheetBinding.includeStats.tvHpName),
                "attack" to Pair(
                    sheetBinding.includeStats.tvAtkVal,
                    sheetBinding.includeStats.tvAtkName
                ),
                "defense" to Pair(
                    sheetBinding.includeStats.tvDefVal,
                    sheetBinding.includeStats.tvDefName
                ),
                "special-attack" to Pair(
                    sheetBinding.includeStats.tvSpAtkVal,
                    sheetBinding.includeStats.tvSpAtkName
                ),
                "special-defense" to Pair(
                    sheetBinding.includeStats.tvSpDefVal,
                    sheetBinding.includeStats.tvSpDefName
                ),
                "speed" to Pair(
                    sheetBinding.includeStats.tvInitVal,
                    sheetBinding.includeStats.tvInitName
                )
            )
    }

}
