package com.example.pokinfo.ui.teambuilder

import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.pokinfo.R
import com.example.pokinfo.databinding.FragmentTeamBuilderBinding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.slider.Slider

fun TeamBuilderFragment.createMaps(binding: FragmentTeamBuilderBinding) {
    binding.apply {
        // region maps
        // map of editTexts to their relating slider, each one has a range of 0-252
        val evIvBinding = binding.includeEvIvWindow
        sliderEditTextMap = linkedMapOf(
            evIvBinding.tilHpEvs.editText to evIvBinding.slider,
            evIvBinding.tilAtkEvs.editText to evIvBinding.sliderAtk,
            evIvBinding.tilDefEvs.editText to evIvBinding.sliderDef,
            evIvBinding.tilSpAtkEvs.editText to evIvBinding.sliderSpAtk,
            evIvBinding.tilSpDefEvs.editText to evIvBinding.sliderSpDef,
            evIvBinding.tilInitEvs.editText to evIvBinding.sliderInit,
        )
        // ex texts
        tvEditTextMapEV = linkedMapOf(
            evIvBinding.tvHpShort to evIvBinding.tilHpEvs.editText,
            evIvBinding.tvAttack to evIvBinding.tilAtkEvs.editText,
            evIvBinding.tvDefense to evIvBinding.tilDefEvs.editText,
            evIvBinding.tvSpAttack to evIvBinding.tilSpAtkEvs.editText,
            evIvBinding.tvSpDefense to evIvBinding.tilSpDefEvs.editText,
            evIvBinding.tvInit to evIvBinding.tilInitEvs.editText,
        )
        // iv texts
        tvEditTextMapIV = linkedMapOf(
            evIvBinding.tvHpShort to evIvBinding.tilHpIvs.editText,
            evIvBinding.tvAttack to evIvBinding.tilAtkIvs.editText,
            evIvBinding.tvDefense to evIvBinding.tilDefIvs.editText,
            evIvBinding.tvSpAttack to evIvBinding.tilSpAtkIvs.editText,
            evIvBinding.tvSpDefense to evIvBinding.tilSpDefIvs.editText,
            evIvBinding.tvInit to evIvBinding.tilInitIvs.editText
        )
        // slot pokemon below searchbar
        cvIvPair = listOf(
            binding.teamSlots.cvPokemon1 to binding.teamSlots.ivChosenPokemon1,
            binding.teamSlots.cvPokemon2 to binding.teamSlots.ivChosenPokemon2,
            binding.teamSlots.cvPokemon3 to binding.teamSlots.ivChosenPokemon3,
            binding.teamSlots.cvPokemon4 to binding.teamSlots.ivChosenPokemon4,
            binding.teamSlots.cvPokemon5 to binding.teamSlots.ivChosenPokemon5,
            binding.teamSlots.cvPokemon6 to binding.teamSlots.ivChosenPokemon6
        )
        calculationMap = listOf(
            Triple(
                evIvBinding.tilHpIvs.editText,
                evIvBinding.tilHpEvs.editText,
                evIvBinding.progressBar
            ),
            Triple(
                evIvBinding.tilAtkIvs.editText,
                evIvBinding.tilAtkEvs.editText,
                evIvBinding.progressBarAtk
            ),
            Triple(
                evIvBinding.tilDefIvs.editText,
                evIvBinding.tilDefEvs.editText,
                evIvBinding.progressBarDef
            ),
            Triple(
                evIvBinding.tilSpAtkIvs.editText,
                evIvBinding.tilSpAtkEvs.editText,
                evIvBinding.progressBarSpAtk
            ),
            Triple(
                evIvBinding.tilSpDefIvs.editText,
                evIvBinding.tilSpDefEvs.editText,
                evIvBinding.progressBarSpDef
            ),
            Triple(
                evIvBinding.tilInitIvs.editText,
                evIvBinding.tilInitEvs.editText,
                evIvBinding.progressBarInit
            ),
        )
        resultValuesList = listOf(
            evIvBinding.tvHpVal,
            evIvBinding.tvAtkVal,
            evIvBinding.tvDefVal,
            evIvBinding.tvSpAtkVal,
            evIvBinding.tvSpDefVal,
            evIvBinding.tvInitVal
        )
        progressBarTVList = listOf(
            evIvBinding.progressBar to evIvBinding.tvHpBaseVal,
            evIvBinding.progressBarAtk to evIvBinding.tvAtkBaseVal,
            evIvBinding.progressBarDef to evIvBinding.tvDefBaseVal,
            evIvBinding.progressBarSpAtk to evIvBinding.tvSpAtkBaseVal,
            evIvBinding.progressBarSpDef to evIvBinding.tvSpDefBaseVal,
            evIvBinding.progressBarInit to evIvBinding.tvInitBaseVal
        )
        val attackBarBinding = binding.incChosenAttacks
        attacksCardList = listOf(
            attackBarBinding.cvAttackOne to attackBarBinding.tvAttackOne,
            attackBarBinding.cvAttackTwo to attackBarBinding.tvAttackTwo,
            attackBarBinding.cvAttackThree to attackBarBinding.tvAttackThree,
            attackBarBinding.cvAttackFour to attackBarBinding.tvAttackFour,
        )


    }
}
/*
fun TeamBuilderFragment.setTextWatchers(binding: FragmentTeamBuilderBinding) {
    sliderEditTextMap.values.forEach { slider -> // set ranges
        slider.valueFrom = 0f
        slider.valueTo = 252f
        slider.stepSize = 1f
    }

    sliderEditTextMap.keys.forEach { editText ->
        editText?.let {
            addTextWatcher( // sets the maximum values the user can enter
                editText = it,
                maxValue = 252,
                sliderEditTextMap = sliderEditTextMap, // sliders are connected to ev edit texts so we need to pass them too
                isEvWatcher = true // addTextWatcher will perform differently for ev edit texts
            )
        }
    }
    // Iv's maximum value is 31 for each base stat
    tvEditTextMapIV.values.forEach { editText ->
        editText?.let {
            addTextWatcher(
                editText = it,
                maxValue = 31,
                isEvWatcher = false
            )
        }
    }


    // adjust slider behavior and connect their value changes to editTexts
    sliderEditTextMap.forEach { (editText, slider) ->
        slider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {}
            override fun onStopTrackingTouch(slider: Slider) {
                // when slider is stopped from user get sum of all ev's and update the value
                //  of the slider if e.g. only 4 evs are left and the slider is on a higher value
                val editTexts = sliderEditTextMap.keys
                val totalEvsExcludingCurrent = editTexts.sumOf {
                    it?.text.toString().toIntOrNull() ?: 0
                } - (editText?.text.toString().toIntOrNull() ?: 0)
                // maximum value that can be assigned to the actual slider
                val remainingEvs = maxEvs - totalEvsExcludingCurrent
                val correctValue =
                    slider.value.toInt().coerceAtMost(remainingEvs.coerceAtMost(252))
                slider.value = correctValue.toFloat()
                editText?.setText(correctValue.toString())
            }
        })
    }
}*/

fun TeamBuilderFragment.overrideNavigationLogic() {
    // override back-navigation (physical)
    val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            // Hier Ihre Logik, was passieren soll, wenn der Zurück-Button gedrückt wird
            showUnsavedChangesDialog()
        }
    }
    // override backpressed dispatcher and toolbar navigation to warn the user about losing data
    requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    (activity as? AppCompatActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(true)
    (activity as? AppCompatActivity)?.findViewById<MaterialToolbar>(R.id.mainToolbar)
        ?.setNavigationOnClickListener {
            showUnsavedChangesDialog()
        }
}