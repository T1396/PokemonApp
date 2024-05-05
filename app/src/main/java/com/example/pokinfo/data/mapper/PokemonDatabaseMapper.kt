package com.example.pokinfo.data.mapper

import com.example.pokeinfo.data.graphModel.FormQuery
import com.example.pokeinfo.data.graphModel.PokemonDetail1Query
import com.example.pokeinfo.data.graphModel.PokemonDetail2Query
import com.example.pokinfo.data.PokemonDataWrapper
import com.example.pokinfo.data.Repository
import com.example.pokinfo.data.models.database.pokemon.PkAbilitiesToJoin
import com.example.pokinfo.data.models.database.pokemon.PkAbilityEffectText
import com.example.pokinfo.data.models.database.pokemon.PkAbilityFlavorText
import com.example.pokinfo.data.models.database.pokemon.PkAbilityInfo
import com.example.pokinfo.data.models.database.pokemon.PkAbilityName
import com.example.pokinfo.data.models.database.pokemon.PkForms
import com.example.pokinfo.data.models.database.pokemon.PkMove
import com.example.pokinfo.data.models.database.pokemon.PkMoveMachines
import com.example.pokinfo.data.models.database.pokemon.PkMoveNames
import com.example.pokinfo.data.models.database.pokemon.PkMoveVersionGroupDetail
import com.example.pokinfo.data.models.database.pokemon.PkMoves
import com.example.pokinfo.data.models.database.pokemon.PkNames
import com.example.pokinfo.data.models.database.pokemon.PkSpecieInfos
import com.example.pokinfo.data.models.database.pokemon.PkStatInfos
import com.example.pokinfo.data.models.database.pokemon.PkType
import com.example.pokinfo.data.models.database.pokemon.Pokemon
import com.example.pokinfo.data.models.database.pokemon.PokemonData
import com.example.pokinfo.data.models.database.pokemon.PokemonDexEntries
import com.google.gson.Gson
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class PokemonDatabaseMapper(private val repository: Repository) {

    data class MoveWrapper(
        val moves: List<PkMove>,
        val moveNames: List<PkMoveNames>,
        val moveMachines: List<PkMoveMachines>,
        val versionGroupDetails: List<PkMoveVersionGroupDetail>
    )

    private val moveIdSet = mutableSetOf<Long>()


    private suspend fun createPokemonDbEntry(
        part1: PokemonDetail1Query.Pokemon,
        part3: FormQuery.Data,
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
        var displayName = part3.pokemon_v2_pokemonform_aggregate.nodes.find { it.pokemon_id == pokemonId }?.pokemon_v2_pokemonformnames
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
                synchronized(alreadyAddedMoveIdSet) { // Korrekter Synchronisierungsblock
                    if (!alreadyAddedMoveIdSet.contains(moveId)) {
                        // Überprüfen Sie, ob die Bewegung bereits in der Datenbank existiert
                        val moveExistsInDB = allExistingMoves.any { it.id.toInt() == moveId }

                        if (!moveExistsInDB) {
                            alreadyAddedMoveIdSet.add(moveId)

                            // Erstellen Sie die Instanzen von dbMove, moveNames und moveMachines basierend auf Ihren Daten
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

                            // Ergebnisse für spätere Verwendung außerhalb des synchronisierten Blocks speichern
                            Triple(
                                dbMove,
                                moveNames,
                                moveMachines
                            ) // Return values to add to lists outside of synchronized block
                        } else null // Wenn die Bewegung bereits hinzugefügt wurde, nichts zurückgeben
                    } else null // Wenn die ID bereits in alreadyAddedMoveIdSet ist, nichts zurückgeben
                }?.also { (dbMove, moveNames, moveMachines) ->
                    // Fügen Sie die Details außerhalb des synchronisierten Blocks hinzu
                    moveListResult.add(dbMove)
                    moveNamesResult.addAll(moveNames)
                    moveMachinesResult.addAll(moveMachines)
                }

                // Erstellen Sie das Detail-Objekt, unabhängig von der Synchronisation
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
        }.awaitAll() // Warten auf die Vollendung aller asynchronen Vorgänge
        moveVersionVersionGroupDetails.addAll(versionGroupDetails)

        return@coroutineScope MoveWrapper(
            moveListResult.toList(), // Convert to list to avoid any mutable access after function return
            moveNamesResult.toList(),
            moveMachinesResult.toList(),
            moveVersionVersionGroupDetails.toList()
        )
    }

    private fun getSpeciesInfo(
        pokemonId: Int?,
        allSpeciesInfo: PokemonDetail1Query.Specy?,
    ): Pair<PkSpecieInfos, List<PkNames>> {

        // checks if the pokemon evolves from any other species to show the evolution chain
        val evolutionChain = allSpeciesInfo?.evolutions

        val evolvesFromId = allSpeciesInfo?.evolves_from_species_id
        val evolvesToIdList = evolutionChain?.species?.nodes?.map {
            it.id.toLong()
        }?.filter { it != allSpeciesInfo.id.toLong() && it != evolvesFromId?.toLong() }
            ?: emptyList()

        //create the data object we will insert into database
        val speciesInfos = PkSpecieInfos(
            id = allSpeciesInfo?.id ?: -1,
            pokemonId = pokemonId ?: -1,
            isLegendary = allSpeciesInfo?.is_legendary ?: false,
            isMythical = allSpeciesInfo?.is_mythical ?: false,
            order = allSpeciesInfo?.order ?: -1,
            evolvesFromSpeciesId = evolvesFromId,
            evolvesToSpeciesIds = evolvesToIdList,
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
        return Pair(speciesInfos, speciesNames)
    }

    private fun getAllAbilityDetails(
        newAbilities: MutableList<PokemonDetail2Query.Ability>,
    ): Pair<List<PkAbilityInfo>, Triple<List<PkAbilityName>, List<PkAbilityEffectText>, List<PkAbilityFlavorText>>> {
            val nameList = mutableListOf<PkAbilityName>()
            val effectTextList = mutableListOf<PkAbilityEffectText>()
            val flavorTextList = mutableListOf<PkAbilityFlavorText>()

            val deferredResults = newAbilities.map { ability ->
                    // Ihre Logik hier, um die Daten für eine einzelne Fähigkeit zu verarbeiten
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
            //return
            return Pair(
                deferredResults,
                Triple(nameList, effectTextList, flavorTextList)
            )
        }

    private suspend fun getPokedexTexts(
        specyData: PokemonDetail2Query.Specy?,
    ): List<PokemonDexEntries> =
        coroutineScope {
            val deferredResults = specyData?.pokedexTexts?.map { entry ->
                async {
                    PokemonDexEntries(
                        id = entry.id.toLong(),
                        speciesId = specyData.id,
                        text = entry.text,
                        versionGroupId = entry.version_id ?: -1,
                        languageId = entry.language_id ?: -1
                    )
                }

            }?.awaitAll() ?: emptyList()
            return@coroutineScope deferredResults
        }

    private suspend fun mapFormInfos(
        formInfos: List<FormQuery.Node>?,
        specieId: Int
    ): List<PkForms> = coroutineScope {
        val deferredForms = formInfos?.map { formInfo ->
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


    suspend fun savePokemonDetailsIntoDatabase(
        pokemonDataWrapper: PokemonDataWrapper,
        languageId: Int,
        onSaved: (Boolean) -> Unit,
    ) {

        val part1 = pokemonDataWrapper.data1?.pokemon?.first()
        val part2 = pokemonDataWrapper.data2
        val part3 = pokemonDataWrapper.data3

        val gson = Gson()
        val sprites = part1?.sprites
        val spritesJson = gson.toJson(sprites)

        val pokemonId = part1?.id
        val allSpeciesInfo = part1?.specy
        val type1 = part1?.typeInfos?.firstOrNull()
        val type2 = part1?.typeInfos?.getOrNull(1)
        val abilities = part2?.pokemon?.firstOrNull()?.abilities
        val formInfos = part3?.pokemon_v2_pokemonform_aggregate?.nodes


        if (part1 != null && part2 != null && part3 != null) {

            //
            val allExistingAbilities = repository.getAbilitiesFromDB()
            val newAbilities = mutableListOf<PokemonDetail2Query.Ability>()
            abilities?.forEach { ability ->
                if (!allExistingAbilities.any { it.id == ability.ability_id }) {
                    newAbilities.add(ability)
                }
            }
            val (abilityInfos, namesAndTexts) = getAllAbilityDetails(newAbilities)
            val (abilityNames, effectTexts, flavorTexts) = namesAndTexts

            // create a db entry with some basic infos like sprites types and other small things
            val pokeDbEntry = createPokemonDbEntry(part1, part3, pokemonId ?: -1, languageId, type1, type2, spritesJson)
            // species info and names
            val (speciesInfo, specieNames) = getSpeciesInfo(
                pokemonId,
                allSpeciesInfo,
            )

            val formList = mapFormInfos(formInfos, speciesInfo.id)

            val pokedexData = part2.pokemon.firstOrNull()?.specy
            val pokedexEntries = getPokedexTexts(pokedexData)

            // get all moves that exists in the database to exclude those to not insert it more than 1 time in to database
            val allExistingMoves = repository.getAllMovesFromDB()
            val moveData = processPokemonMoves(
                part2.pokemon.firstOrNull()?.moves ?: emptyList(),
                allExistingMoves,
                pokemonId ?: -1,
            )

            // creates a list of every move id the pokemon can learn
            val pkMoves = PkMoves(
                pokemonId = pokemonId ?: -1,
                moveIds = moveIdSet.toList().sortedBy { it }
            )
            // join list / table holds info which pokemon have which ability
            val joinList = abilities?.map {
                PkAbilitiesToJoin(
                    pokemonId = pokemonId ?: -1,
                    abilityId = it.ability_id ?: -1,
                    slot = it.slot
                )
            } ?: emptyList()
            // put all chunks into one Data type to give it to the repo

            val pokemonData = PokemonData(
                pokemon = pokeDbEntry,
                abilityInfoList = abilityInfos,
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
                formData = formList
            )


            repository.insertPokemonDataIntoDB(pokemonData) { success ->
                onSaved(success)
            }


        }
    }

}