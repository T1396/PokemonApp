package com.example.pokinfo.adapter.home.detail

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.pokinfo.R
import com.example.pokinfo.data.maps.typeColorMap
import com.example.pokinfo.data.models.database.pokemon.PokemonForList
import com.example.pokinfo.data.models.database.pokemon.PokemonTypeName
import com.example.pokinfo.data.util.ImageAltLoader.loadAnyImage
import com.example.pokinfo.databinding.HomeDetailAbilitySheetHeaderBinding
import com.example.pokinfo.databinding.ItemListPokemonBinding


class AbilitySheetAdapter(
    private val ability: AbilityEffectText,
    private val typeNames: List<PokemonTypeName>,
    private val onItemClicked: (Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var pokemonList: List<PokemonForList> = emptyList()

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_POKEMON = 1
    }


    @SuppressLint("NotifyDataSetChanged")
    fun submitList(pokemonList: List<PokemonForList>) {
        this.pokemonList = pokemonList
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) TYPE_HEADER else TYPE_POKEMON
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(
                binding = HomeDetailAbilitySheetHeaderBinding.inflate(inflater, parent, false)
            )

            TYPE_POKEMON -> PokemonViewHolder(
                binding = ItemListPokemonBinding.inflate(inflater, parent, false),
                onItemClicked = onItemClicked
            )

            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> holder.bind(ability)
            is PokemonViewHolder -> holder.bind(pokemonList[position - 1], typeNames)
        }
    }

    override fun getItemCount() = pokemonList.size + 1

    class HeaderViewHolder(private val binding: HomeDetailAbilitySheetHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(ability: AbilityEffectText) {
            binding.tvEveryPokeWithAbilityHeader.text = itemView.context.getString(R.string.every_pokemon_with_ability, ability.name)
            binding.tvEffectLongText.text = ability.textLong
            binding.tvAbilityEffectText.text = ability.textShort
        }
    }

    class PokemonViewHolder(
        private val binding: ItemListPokemonBinding,
        private val onItemClicked: (Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(pokemon: PokemonForList, typeNames: List<PokemonTypeName>) {
            loadAnyImage(binding.ivPokemon, pokemon.imageUrl, pokemon.altImageUrl, pokemon.officialImageUrl)
            binding.tvPokemonName.text = pokemon.name
            binding.tvNr.text = pokemon.speciesId.toString()

            // set primary type cardview
            val primaryTypeColorRes =
                typeColorMap.entries.find { it.key == pokemon.typeId1 }?.value?.first
                    ?: R.color.type_colour_unknown
            val primaryColor = ContextCompat.getColor(itemView.context, primaryTypeColorRes)
            binding.cvTypeOne.setCardBackgroundColor(primaryColor)
            val primTypeName = typeNames.find { it.typeId == pokemon.typeId1 }?.name
            binding.tvPrimaryType.text = primTypeName

            // set secondary type cardview
            if (pokemon.typeId2 != null) {
                val secTypeName = typeNames.find { it.typeId == pokemon.typeId2 }?.name
                binding.tvSecondaryType.text = secTypeName
                // set card color
                val secondaryTypeColorRes =
                    typeColorMap.entries.find { it.key == pokemon.typeId2 }?.value?.first
                        ?: R.color.type_colour_unknown
                val secondaryColor =
                    ContextCompat.getColor(itemView.context, secondaryTypeColorRes)
                binding.cvTypeTwo.setCardBackgroundColor(secondaryColor)
                // make cardview visible
                binding.cvTypeTwo.visibility = View.VISIBLE
            } else {
                binding.cvTypeTwo.visibility = View.INVISIBLE
            }

            binding.clPokemonListItem.setOnClickListener {
                onItemClicked(pokemon.id)
            }
        }
    }
}


data class AbilityEffectText(
    val abilityId: Int,
    val name: String = "No data found",
    val slot: Int,
    val textLong: String = "",
    val textShort: String,
    val isHidden: Boolean = false,
    val languageId: Int,
)