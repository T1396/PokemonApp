package com.example.pokinfo.adapter.teamAndTeambuilder

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.pokinfo.R

class SpinnerAdapter(context: Context,
                     resource: Int,
                     objects: List<Pair<String, String>>)
    : ArrayAdapter<Pair<String, String>>(context, resource, objects) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createItemView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createItemView(position, convertView, parent)
    }

    private fun createItemView(position: Int, convertView: View?, parent: ViewGroup): View {
        val (name, effect) = getItem(position) ?: Pair("Error", "Error")
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.spinner_item, parent, false)
        val nameTextView = view.findViewById<TextView>(R.id.nameTextView)
        val effectTextView = view.findViewById<TextView>(R.id.effectTextView)
        nameTextView.text = name
        effectTextView.text = effect
        return view
    }
}