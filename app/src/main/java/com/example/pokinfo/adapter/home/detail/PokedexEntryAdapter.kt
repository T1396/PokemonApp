package com.example.pokinfo.adapter.home.detail

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.pokinfo.R
import com.example.pokinfo.data.models.database.pokemon.PokemonDexEntries
import com.example.pokinfo.data.models.database.versionAndLanguageNames.LanguageNames
import com.example.pokinfo.data.models.database.versionAndLanguageNames.VersionNames
import com.example.pokinfo.databinding.ItemListPokedexBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder


class PokedexEntryAdapter(
    private val languageNames: List<LanguageNames>,
    private val versionNames: List<VersionNames>,
    private val onLangButtonClicked: () -> Unit,
    private val onVersionSelected: (Int) -> Unit
) :
    RecyclerView.Adapter<PokedexEntryAdapter.ItemViewHolder>() {
    private var dataset: List<PokemonDexEntries> = emptyList()

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(list: List<PokemonDexEntries>) {
        dataset = list
        notifyDataSetChanged()
    }

    inner class ItemViewHolder(val binding: ItemListPokedexBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding =
            ItemListPokedexBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return dataset.size
    }

    fun getPositionOfVersion(versionId: Int): Int {
        return dataset.indexOfFirst { it.versionGroupId == versionId }
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = dataset[position]
        val languageName = languageNames.find { it.languageId == item.languageId }?.name
        val gameVersionName = versionNames.find { it.versionId == item.versionGroupId }?.name
        holder.binding.tvPokedexText.text = item.text
        holder.binding.btnPokedexLanguage.text = languageName
        holder.binding.btnPkdexGameVersion.text = gameVersionName

        holder.binding.btnPokedexLanguage.setOnClickListener {
            onLangButtonClicked()
        }
        holder.binding.btnPkdexGameVersion.setOnClickListener {
            showVersionDialog(onVersionSelected, holder.itemView.context)
        }
    }

    private fun showVersionDialog(onVersionSelected: (Int) -> Unit, context: Context) {
        val filteredVersions = versionNames.filter { versionName-> dataset.any { it.versionGroupId == versionName.versionId } }
        val versionNamesArray = filteredVersions.map { it.name }.toTypedArray()
        val title = context.getString(R.string.choose_version)
        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setItems(versionNamesArray) { _, which ->
                val selectedVersionId = filteredVersions[which].versionId
                onVersionSelected(selectedVersionId)
            }
            .show()
    }
}