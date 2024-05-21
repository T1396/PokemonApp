package com.example.pokinfo.data.mapper

import com.example.pokeinfo.data.graphModel.PokemonDetail1Query
import com.example.pokeinfo.data.graphModel.PokemonDetail2Query
import com.example.pokeinfo.data.graphModel.PokemonDetail3Query
import com.example.pokinfo.data.PokemonDataWrapper
import com.example.pokinfo.data.Repository
import com.example.pokinfo.data.models.database.pokemon.PkAbilitiesToJoin
import com.example.pokinfo.data.models.database.pokemon.PkAbilityEffectText
import com.example.pokinfo.data.models.database.pokemon.PkAbilityFlavorText
import com.example.pokinfo.data.models.database.pokemon.PkAbilityInfo
import com.example.pokinfo.data.models.database.pokemon.PkAbilityName
import com.example.pokinfo.data.models.database.pokemon.PkEvolutionChain
import com.example.pokinfo.data.models.database.pokemon.PkEvolutionDetails
import com.example.pokinfo.data.models.database.pokemon.PkForms
import com.example.pokinfo.data.models.database.pokemon.PkMove
import com.example.pokinfo.data.models.database.pokemon.PkMoveMachines
import com.example.pokinfo.data.models.database.pokemon.PkMoveNames
import com.example.pokinfo.data.models.database.pokemon.PkMoveVersionGroupDetail
import com.example.pokinfo.data.models.database.pokemon.PkMoves
import com.example.pokinfo.data.models.database.pokemon.PkNames
import com.example.pokinfo.data.models.database.pokemon.PkSpecieInfo
import com.example.pokinfo.data.models.database.pokemon.PkStatInfos
import com.example.pokinfo.data.models.database.pokemon.PkType
import com.example.pokinfo.data.models.database.pokemon.Pokemon
import com.example.pokinfo.data.models.database.pokemon.PokemonAbilitiesList
import com.example.pokinfo.data.models.database.pokemon.PokemonData
import com.example.pokinfo.data.models.database.pokemon.PokemonDexEntries
import com.google.gson.Gson
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope


/** Extracts Data of a Pokemon to insert it afterwards into the database
 *  Details of a Pokemon includes:
 *  PokemonDetails, Forms, Moves, Sprites, Abilities, Types
 *
 * */
class PokemonDatabaseMapper(private val repository: Repository) {

    data class MoveWrapper(
        val moves: List<PkMove>,
        val moveNames: List<PkMoveNames>,
        val moveMachines: List<PkMoveMachines>,
        val versionGroupDetails: List<PkMoveVersionGroupDetail>
    )

    //
    private val moveIdSet = mutableSetOf<Long>()

    suspend fun savePokemonDetailsIntoDatabase(
        pokemonDataWrapper: PokemonDataWrapper,
        languageId: Int,
        onSaved: (Boolean) -> Unit,
    ) = coroutineScope {

        val part1 = pokemonDataWrapper.data1?.pokemon?.firstOrNull()
        val part2 = pokemonDataWrapper.data2
        val part3 = pokemonDataWrapper.data3




        if (part1 != null && part2 != null && part3 != null) {
            val gson = Gson()
            val sprites = part1.sprites
            val spritesJson = gson.toJson(sprites)

            val pokemonId = part1.id
            val allSpeciesInfo = part1.specy
            val type1 = part1.typeInfos.firstOrNull()
            val type2 = part1.typeInfos.getOrNull(1)
            val abilities = part2.pokemon.firstOrNull()?.abilities
            val formInfo = part3.pokemon_v2_pokemonform_aggregate.nodes
            val evolutionDetails = getEvolutionDetails(part3, pokemonId, allSpeciesInfo?.id)

            //
            val allExistingAbilities = repository.getAbilitiesFromDB()
            val newAbilities = mutableListOf<PokemonDetail2Query.Ability>()
            abilities?.forEach { ability ->
                if (!allExistingAbilities.any { it.id == ability.ability_id }) {
                    newAbilities.add(ability)
                }
            }
            val (abilityInfo, namesAndTexts) = getAllAbilityDetails(newAbilities)
            val (abilityNames, effectTexts, flavorTexts) = namesAndTexts

            // create a db entry with some basic info like sprites types and other small things
            val pokeDbEntry = createPokemonDbEntry(
                part1,
                part3,
                pokemonId,
                languageId,
                type1,
                type2,
                spritesJson
            )
            // species info and names
            val (speciesInfo, specieNames) = getSpeciesInfo(pokemonId, allSpeciesInfo)
            val formList = mapFormInfo(formInfo, speciesInfo.id)
            val pokedexData = part2.pokemon.firstOrNull()?.specy
            val pokedexEntries = getPokedexTexts(pokedexData)

            // get all moves that exists in the database to exclude those to not insert it more than 1 time in to database
            val allExistingMoves = repository.getAllMovesFromDB()
            val moveData = processPokemonMoves(
                part2.pokemon.firstOrNull()?.moves ?: emptyList(),
                allExistingMoves,
                pokemonId,
            )

            // creates a list of every move id the pokemon can learn
            val pkMoves = PkMoves(
                pokemonId = pokemonId,
                moveIds = moveIdSet.toList().sortedBy { it }
            )
            // join list / table holds info which pokemon have which ability
            val joinList = abilities?.map {
                PkAbilitiesToJoin(
                    pokemonId = pokemonId,
                    abilityId = it.ability_id ?: -1,
                    slot = it.slot,
                    isHidden = it.is_hidden
                )
            } ?: emptyList()
            // put all chunks into one Data type to give it to the repo

            val abilityPokemonLists = newAbilities.map { ability ->
                async {
                    if (ability.ability_id != null) {
                        val data =
                            repository.getPokemonListOfAbility(ability.ability_id)?.pokemon_v2_pokemonability_aggregate
                        val pokemonIds =
                            data?.nodes?.map { it.pokemon_v2_pokemon?.id?.toLong() } ?: emptyList()
                        val abilityId = ability.ability_id
                        val ac = PokemonAbilitiesList(abilityId, pokemonIds.filterNotNull())
                        ac
                    } else {
                        null
                    }
                }
            }.awaitAll()

            val pokemonData = PokemonData(
                pokemon = pokeDbEntry,
                abilityInfoList = abilityInfo,
                abilitiesToJoin = joinList,
                pokemonMoves = pkMoves,
                pokedexEntries = pokedexEntries,
                moves = moveData.moves,
                moveMachines = moveData.moveMachines,
                moveNames = moveData.moveNames,
                specyData = speciesInfo,
                specyNames = specieNames,
                abilityFlavorTexts = flavorTexts,
                abilityEffectTexts = effectTexts,
                abilityNames = abilityNames,
                versionGroupDetails = moveData.versionGroupDetails,
                formData = formList,
                evolutionChain = evolutionDetails?.first,
                evolutionDetails = evolutionDetails?.second,
                abilitiesPokemonList = abilityPokemonLists.filterNotNull()
            )

            repository.insertPokemonDataIntoDB(pokemonData) { success ->
                onSaved(success)
            }
        } else {
            onSaved(false)
        }
    }


    private suspend fun createPokemonDbEntry(
        part1: PokemonDetail1Query.Pokemon,
        part3: PokemonDetail3Query.Data,
        pokemonId: Int,
        languageId: Int,
        type1: PokemonDetail1Query.TypeInfo?,
        type2: PokemonDetail1Query.TypeInfo?,
        spritesJson: String,
    ): Pokemon = coroutineScope {
        val stats = part1.stats.map {
            async {
                PkStatInfos(
                    statId = it.stat_id ?: -1,
                    baseStat = it.base_stat,
                    defaultName = it.pokemon_v2_stat?.name ?: ""
                )
            }
        }.awaitAll()
        var displayName =
            part3.pokemon_v2_pokemonform_aggregate.nodes.find { it.pokemon_id == pokemonId }?.pokemon_v2_pokemonformnames
                ?.find { it.language_id == languageId }?.pokemon_name
        if (displayName.isNullOrEmpty()) {
            displayName = part1.specy?.allNames?.find { it.language_id == languageId }?.name
        }
        return@coroutineScope Pokemon(
            id = part1.id,
            specieId = part1.specy?.id ?: -1,
            order = part1.order ?: -1,
            name = part1.defaultName,
            height = part1.height ?: -1,
            weight = part1.weight ?: -1,
            primaryType = PkType(
                slot = type1?.slot ?: 1,
                typeId = type1?.type_id ?: 10001
            ),
            secondaryType = if (type2 == null) {
                null
            } else {
                PkType(
                    slot = type2.slot,
                    typeId = type2.type_id ?: 10001
                )
            },
            pkStatInfos = stats,
            sprites = spritesJson,
            displayName = displayName ?: ""
        )
    }

    private suspend fun processPokemonMoves(
        pokemonMoveLearnDetails: List<PokemonDetail2Query.Move>,
        allExistingMoves: List<PkMove>,
        pokemonId: Int,
    ): MoveWrapper = coroutineScope {

        val alreadyAddedMoveIdSet = mutableSetOf<Int?>()
        val moveListResult = mutableListOf<PkMove>()
        val moveNamesResult = mutableListOf<PkMoveNames>()
        val moveMachinesResult = mutableListOf<PkMoveMachines>()
        val moveVersionVersionGroupDetails = mutableListOf<PkMoveVersionGroupDetail>()
        moveIdSet.clear()

        val versionGroupDetails = pokemonMoveLearnDetails.map { move ->

            async {

                val moveId = move.move_id
                moveIdSet.add(moveId?.toLong() ?: -1)
                synchronized(alreadyAddedMoveIdSet) {
                    if (!alreadyAddedMoveIdSet.contains(moveId)) {

                        val moveExistsInDB = allExistingMoves.any { it.id.toInt() == moveId }

                        if (!moveExistsInDB) {
                            alreadyAddedMoveIdSet.add(moveId)


                            val dbMove = PkMove(
                                id = moveId?.toLong() ?: -1,
                                name = move.move?.name ?: "error",
                                accuracy = move.move?.accuracy,
                                typeId = move.move?.type_id ?: 10001,
                                power = move.move?.power ?: 0,
                                ap = move.move?.pp ?: 0,
                                effectText = move.move?.effect?.texts?.lastOrNull()?.short_effect,
                                moveDamageClassId = move.move?.move_damage_class_id
                            )

                            val moveNames = move.move?.nameTranslated?.map { element ->
                                PkMoveNames(
                                    moveId = moveId ?: -1,
                                    name = element.name,
                                    languageId = element.language_id ?: -1
                                )
                            } ?: emptyList()

                            val moveMachines = move.move?.machines?.map { machineInfo ->
                                PkMoveMachines(
                                    moveId = moveId ?: -1,
                                    machineNr = machineInfo.machine_number,
                                    versionGroupId = machineInfo.version_group_id ?: -1
                                )
                            } ?: emptyList()
                            Triple(
                                dbMove,
                                moveNames,
                                moveMachines
                            )
                        } else null
                    } else null
                }?.also { (dbMove, moveNames, moveMachines) ->
                    moveListResult.add(dbMove)
                    moveNamesResult.addAll(moveNames)
                    moveMachinesResult.addAll(moveMachines)
                }

                PkMoveVersionGroupDetail(
                    id = move.id.toLong(),
                    levelLearnedAt = move.level,
                    moveId = moveId?.toLong() ?: -1,
                    moveLearnMethod = move.moveLearnMethodName?.name,
                    moveLearnMethodId = move.move_learn_method_id,
                    versionGroupId = move.version_group_id,
                    pokemonId = pokemonId
                )
            }
        }.awaitAll()
        moveVersionVersionGroupDetails.addAll(versionGroupDetails)

        return@coroutineScope MoveWrapper(
            moveListResult.toList(),
            moveNamesResult.toList(),
            moveMachinesResult.toList(),
            moveVersionVersionGroupDetails.toList()
        )
    }

    private fun getSpeciesInfo(
        pokemonId: Int?,
        allSpeciesInfo: PokemonDetail1Query.Specy?,
    ): Pair<PkSpecieInfo, List<PkNames>> {

        //create the data object we will insert into database
        val speciesInfo = PkSpecieInfo(
            id = allSpeciesInfo?.id ?: -1,
            pokemonId = pokemonId ?: -1,
            isLegendary = allSpeciesInfo?.is_legendary ?: false,
            isMythical = allSpeciesInfo?.is_mythical ?: false,
            order = allSpeciesInfo?.order ?: -1,
            evolutionChainId = allSpeciesInfo?.evolution_chain_id,
            name = allSpeciesInfo?.name ?: "error",
            genderRate = allSpeciesInfo?.gender_rate,
            captureRate = allSpeciesInfo?.capture_rate,
            baseHappiness = allSpeciesInfo?.base_happiness
        )
        // create a list of all species names
        val speciesNames = allSpeciesInfo?.allNames?.map {
            PkNames(
                speciesId = allSpeciesInfo.id,
                name = it.name,
                genus = it.genus,
                languageId = it.language_id ?: -1
            )
        } ?: emptyList()
        return Pair(speciesInfo, speciesNames)
    }

    private fun getAllAbilityDetails(
        newAbilities: MutableList<PokemonDetail2Query.Ability>,
    ): Pair<List<PkAbilityInfo>, Triple<List<PkAbilityName>, List<PkAbilityEffectText>, List<PkAbilityFlavorText>>> {
        val nameList = mutableListOf<PkAbilityName>()
        val effectTextList = mutableListOf<PkAbilityEffectText>()
        val flavorTextList = mutableListOf<PkAbilityFlavorText>()

        val deferredResults = newAbilities.map { ability ->

            val names = ability.ability?.nameTranslated?.map {
                PkAbilityName(
                    abilityId = ability.ability_id ?: -1,
                    name = it.name,
                    languageId = it.language_id ?: -1
                )
            } ?: emptyList()
            nameList.addAll(names)

            // flavor texts
            val flavorEntry = ability.ability?.shortText?.map { text ->
                PkAbilityFlavorText(
                    abilityId = text.ability_id ?: -1,
                    effectTextShort = text.flavor_text,
                    languageId = text.language_id ?: -1,
                    versionGroupId = text.version_group_id ?: -1
                )
            } ?: emptyList()
            flavorTextList.addAll(flavorEntry)

            // effect texts
            val effectEntry = ability.ability?.longText?.map { text ->
                PkAbilityEffectText(
                    abilityId = text.ability_id ?: -1,
                    effectTextLong = text.effect,
                    languageId = text.language_id ?: -1,
                )
            } ?: emptyList()
            effectTextList.addAll(effectEntry)

            // Ability
            PkAbilityInfo(
                id = ability.ability_id ?: -1,
                name = ability.ability?.name ?: "error",
            )
        }
        return Pair(
            deferredResults,
            Triple(nameList, effectTextList, flavorTextList)
        )
    }

    private suspend fun getPokedexTexts(
        specieData: PokemonDetail2Query.Specy?,
    ): List<PokemonDexEntries> =
        coroutineScope {
            val deferredResults = specieData?.pokedexTexts?.map { entry ->
                async {
                    PokemonDexEntries(
                        id = entry.id.toLong(),
                        speciesId = specieData.id,
                        text = entry.text,
                        versionGroupId = entry.version_id ?: -1,
                        languageId = entry.language_id ?: -1
                    )
                }

            }?.awaitAll() ?: emptyList()
            return@coroutineScope deferredResults
        }

    private suspend fun mapFormInfo(
        formInfo: List<PokemonDetail3Query.Node>?,
        specieId: Int
    ): List<PkForms> = coroutineScope {
        val deferredForms = formInfo?.map { formInfo ->
            async {
                val sprites = formInfo.pokemon_v2_pokemonformsprites_aggregate.nodes
                val spritesJson = Gson().toJson(sprites)
                PkForms(
                    formId = formInfo.id,
                    speciesId = specieId,
                    defaultName = formInfo.name,
                    name = formInfo.pokemon_v2_pokemonformnames.firstOrNull()?.pokemon_name
                        ?: "Error",
                    pokemonId = formInfo.pokemon_id ?: -1,
                    formOrder = formInfo.form_order ?: -1,
                    isBattleOnly = formInfo.is_battle_only,
                    order = formInfo.order ?: -1,
                    isMega = formInfo.is_mega,
                    isDefault = formInfo.is_default,
                    sprites = spritesJson
                )
            }
        }?.awaitAll() ?: emptyList()
        return@coroutineScope deferredForms
    }

    private fun getEvolutionDetails(
        part3: PokemonDetail3Query.Data,
        pokemonId: Int?,
        speciesId: Int?
    ): Pair<PkEvolutionChain, List<PkEvolutionDetails>>? {
        if (pokemonId != speciesId) return null
        val chain = part3.pokemon_v2_evolutionchain.firstOrNull() ?: return null
        val evolutionChainForDb = PkEvolutionChain(id = chain.id)
        val evolutionDetails = chain.pokemon_v2_pokemonspecies.map { species ->
            val speciesIdNew = species.id
            val evolvesToSpeciesId = chain.pokemon_v2_pokemonspecies.find { it.evolves_from_species_id == speciesIdNew }?.id
            PkEvolutionDetails(
                name = species.name,
                evolutionChainId = species.evolution_chain_id ?: -1,
                evolvesFromSpeciesId = species.evolves_from_species_id,
                minLevel = species.pokemon_v2_pokemonevolutions.firstOrNull()?.min_level,
                speciesId = species.id,
                evolvesToSpeciesId = evolvesToSpeciesId
            )
        }
        return Pair(evolutionChainForDb, evolutionDetails)
    }
}