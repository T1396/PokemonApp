package com.example.pokinfo.ui.teambuilder.extensions

import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.example.pokinfo.databinding.FragmentTeamBuilderBinding
import com.example.pokinfo.viewModels.teambuilder.StatsEnum
import com.google.android.material.card.MaterialCardView
import com.google.android.material.slider.Slider

object TeamBuilderMapCreater {
    fun createMaps(binding: FragmentTeamBuilderBinding): TeamBuilderMaps {
        val evIvBinding = binding.includeEvIvWindow
        val evEditAndSliderMap = linkedMapOf(
            StatsEnum.HP to Pair(evIvBinding.tilHpEvs.editText, evIvBinding.slider),
            StatsEnum.ATK to Pair(evIvBinding.tilAtkEvs.editText, evIvBinding.sliderAtk),
            StatsEnum.DEF to Pair(evIvBinding.tilDefEvs.editText, evIvBinding.sliderDef),
            StatsEnum.SPATK to Pair(evIvBinding.tilSpAtkEvs.editText, evIvBinding.sliderSpAtk),
            StatsEnum.SPDEF to Pair(evIvBinding.tilSpDefEvs.editText, evIvBinding.sliderSpDef),
            StatsEnum.INIT to Pair(evIvBinding.tilInitEvs.editText, evIvBinding.sliderInit),
        )
        // ex texts
        val tvEditTextMapEV = linkedMapOf(
            evIvBinding.tvHpShort to evIvBinding.tilHpEvs.editText,
            evIvBinding.tvAttack to evIvBinding.tilAtkEvs.editText,
            evIvBinding.tvDefense to evIvBinding.tilDefEvs.editText,
            evIvBinding.tvSpAttack to evIvBinding.tilSpAtkEvs.editText,
            evIvBinding.tvSpDefense to evIvBinding.tilSpDefEvs.editText,
            evIvBinding.tvInit to evIvBinding.tilInitEvs.editText,
        )
        // iv texts
        val tvEditTextMapIV = linkedMapOf(
            StatsEnum.HP to Pair(evIvBinding.tvHpShort, evIvBinding.tilHpIvs.editText),
            StatsEnum.ATK to Pair(evIvBinding.tvAttack, evIvBinding.tilAtkIvs.editText),
            StatsEnum.DEF to Pair(evIvBinding.tvDefense, evIvBinding.tilDefIvs.editText),
            StatsEnum.SPATK to Pair(evIvBinding.tvSpAttack, evIvBinding.tilSpAtkIvs.editText),
            StatsEnum.SPDEF to Pair(evIvBinding.tvSpDefense, evIvBinding.tilSpDefIvs.editText),
            StatsEnum.INIT to Pair(evIvBinding.tvInit, evIvBinding.tilInitIvs.editText),
        )
        // slot pokemon below searchbar
        val cvIvPair = listOf(
            binding.teamSlots.cvPokemon1 to binding.teamSlots.ivChosenPokemon1,
            binding.teamSlots.cvPokemon2 to binding.teamSlots.ivChosenPokemon2,
            binding.teamSlots.cvPokemon3 to binding.teamSlots.ivChosenPokemon3,
            binding.teamSlots.cvPokemon4 to binding.teamSlots.ivChosenPokemon4,
            binding.teamSlots.cvPokemon5 to binding.teamSlots.ivChosenPokemon5,
            binding.teamSlots.cvPokemon6 to binding.teamSlots.ivChosenPokemon6
        )
        val resultingValueList = linkedMapOf(
            StatsEnum.HP to evIvBinding.tvHpVal,
            StatsEnum.ATK to evIvBinding.tvAtkVal,
            StatsEnum.DEF to evIvBinding.tvDefVal,
            StatsEnum.SPATK to evIvBinding.tvSpAtkVal,
            StatsEnum.SPDEF to evIvBinding.tvSpDefVal,
            StatsEnum.INIT to evIvBinding.tvInitVal
        )
        val progressBarTVList = listOf(
            evIvBinding.progressBar to evIvBinding.tvHpBaseVal,
            evIvBinding.progressBarAtk to evIvBinding.tvAtkBaseVal,
            evIvBinding.progressBarDef to evIvBinding.tvDefBaseVal,
            evIvBinding.progressBarSpAtk to evIvBinding.tvSpAtkBaseVal,
            evIvBinding.progressBarSpDef to evIvBinding.tvSpDefBaseVal,
            evIvBinding.progressBarInit to evIvBinding.tvInitBaseVal
        )
        val attackBarBinding = binding.incChosenAttacks
        val attacksCardList = listOf(
            attackBarBinding.cvAttackOne to attackBarBinding.tvAttackOne,
            attackBarBinding.cvAttackTwo to attackBarBinding.tvAttackTwo,
            attackBarBinding.cvAttackThree to attackBarBinding.tvAttackThree,
            attackBarBinding.cvAttackFour to attackBarBinding.tvAttackFour,
        )

        return TeamBuilderMaps(
            evEditAndSliderMap = evEditAndSliderMap,
            tvEditTextMapEV = tvEditTextMapEV,
            tvEditTextMapIV = tvEditTextMapIV,
            cvIvPair = cvIvPair,
            resultingValueList = resultingValueList,
            progressBarTVList = progressBarTVList,
            attacksCardList = attacksCardList
        )
    }
}

data class TeamBuilderMaps(
    val evEditAndSliderMap: LinkedHashMap<StatsEnum, Pair<EditText?, Slider>>,
    val tvEditTextMapEV: LinkedHashMap<TextView, EditText?>,
    val tvEditTextMapIV: LinkedHashMap<StatsEnum, Pair<TextView, EditText?>>,
    val cvIvPair: List<Pair<MaterialCardView, ImageView>>,
    val resultingValueList: Map<StatsEnum, TextView>,
    val progressBarTVList: List<Pair<ProgressBar, TextView>>,
    var attacksCardList: List<Pair<MaterialCardView, TextView>>
)
