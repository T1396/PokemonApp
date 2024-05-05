package com.example.pokinfo.data.mapper

import com.example.pokeinfo.data.graphModel.PokeListQuery
import com.example.pokinfo.data.models.database.pokemon.PokemonForList
import com.example.pokinfo.data.models.database.pokemon.StatValues
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope


class PokemonListMapper {


    suspend fun mapData(data: PokeListQuery.Data, languageId: Int): List<PokemonForList> =
        coroutineScope {
            val listPokemon = data.response.data
            val batchSize = 100 // size of the batches to run parallel
            val batches =
                listPokemon.chunked(batchSize) // cut the list into smaller lists with batchsize

            // flatmap takes all the batches to map them to a list than
            val mappedPokemonList = batches.flatMap { batch ->


                batch.map { pokemonData ->
                    async { // run async
                        // get the data
                        val pokemon = pokemonData.pokemon
                        val stat = pokemon?.stats
                        if (pokemon == null) null
                        else {
                            // Map Infos to data object
                            val id = pokemonData.pokemon_id ?: -1
                            val typeId1 = pokemon.types.firstOrNull()?.type_id ?: 10001
                            val typeId2 = pokemon.types.getOrNull(1)?.type_id
                            var name: String?
                            name = pokemon.specy?.names?.data?.firstOrNull()?.name
                            val tempName: String? = name
                            if (!pokemon.is_default) {
                                name = pokemon.pokemon_v2_pokemonforms_aggregate.nodes.firstOrNull()?.pokemon_v2_pokemonformnames_aggregate?.nodes?.find { it.language_id == languageId }?.pokemon_name
                                if (name.isNullOrEmpty()) name = "$tempName (*)"
                            }
                            PokemonForList(
                                id = id,
                                height = pokemon.height ?: -1,
                                weight = pokemon.weight ?: -1,
                                speciesId = pokemon.pokemon_species_id ?: -1,
                                imageUrl = loadImageUrl("home", id),
                                altImageUrl = loadImageUrl("default", id),
                                isDefault = pokemon.is_default,
                                officialImageUrl = loadImageUrl("official", id),
                                name = name ?: "No name found",
                                stats = createStatValueList(stat) ?: emptyList(),
                                typeId1 = typeId1,
                                typeId2 = typeId2
                            )
                        }
                    }
                }.awaitAll()
                    .filterNotNull() // wait for each batch to finish and filter all null pokemon out

            }
            mappedPokemonList // return the list
        }

    private fun loadImageUrl(type: String, pokemonId: Int): String {
        val homeUrl =
            "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/home/"
        val defaultUrl = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/"
        val officialArtUrl =
            "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/"
        return when (type) {
            "home" -> "$homeUrl$pokemonId.png"
            "default" -> "$defaultUrl$pokemonId.png"
            "official" -> "$officialArtUrl$pokemonId.png"
            else -> {
                ""
            }
        }
    }

    private fun createStatValueList(stats: PokeListQuery.Stats?): List<StatValues>? {
        return stats?.list?.map {
            StatValues(
                statValue = it.base_stat,
                statId = it.stat_id ?: return null
            )
        }
    }
}