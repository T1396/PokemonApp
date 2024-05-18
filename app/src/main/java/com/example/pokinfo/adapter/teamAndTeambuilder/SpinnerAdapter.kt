package com.example.pokinfo.adapter.teamAndTeambuilder

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.pokinfo.R
import com.example.pokinfo.adapter.home.detail.AbilityEffectText

class SpinnerAdapter(
    context: Context,
    resource: Int,
    abilityList: List<AbilityEffectText>
): ArrayAdapter<AbilityEffectText>(context, resource, abilityList) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createItemView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createItemView(position, convertView, parent)
    }

    private fun createItemView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.spinner_item, parent, false)
        val ability = getItem(position) ?: return View(context)
        val nameTextView = view.findViewById<TextView>(R.id.nameTextView)
        val effectTextView = view.findViewById<TextView>(R.id.effectTextView)
        nameTextView.text = ability.name
        effectTextView.text = ability.textShort
        return view
    }
}