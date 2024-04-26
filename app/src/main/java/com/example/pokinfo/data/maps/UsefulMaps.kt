package com.example.pokinfo.data.maps

import com.example.pokinfo.R

// maps the origin key names to readable Names to display it later
val readableNames = mapOf(
    "sprites" to "Default",
    "dream_world" to "Dream World",
    "home" to "Home",
    "official-artwork" to "Official Artwork",
    "showdown" to "Showdown",
    "red-blue" to "Gen1 Red-Blue",
    "yellow" to "Gen1 Yellow",
    "gold" to "Gen2 Gold",
    "silver" to "Gen2 Silver",
    "crystal" to "Gen2 Crystal",
    "ruby-sapphire" to "Gen3 Ruby-Sapphire",
    "emerald" to "Gen3 Emerald",
    "firered-leafgreen" to "Gen3 Firered-Leafgreen",
    "diamond-pearl" to "Gen4 Diamond-Pearl",
    "heartgold-soulsilver" to "Gen4 Heartgold-Soulsilver",
    "platinum" to "Gen4 Platinum",
    "black-white" to "Gen5 Black-White",
    "animated" to "Gen5 Black-White animated",
    "omegaruby-alphasapphire" to "Gen6 OmegaRuby - Alphasapphire",
    "x-y" to "Gen6 X - Y",
    "ultra-sun-ultra-moon" to "Gen7 Ultrasun-Ultramoon",
)

val imageTypeName = mapOf(
    "front_shiny" to "Shiny Front",
    "front_female" to "Female Front",
    "front_default" to "Default Front",
    "front_shiny_female" to "Shiny female Front",
    "back_shiny" to "Shiny back",
    "back_female" to "Female back",
    "back_default" to "Default back",
    "back_shiny_female" to "Female shiny back",
    "back_gray" to "Back gray",
    "back_transparent" to "Back transparent",
    "back_shiny_transparent" to "Back shiny transparent",
    "front_gray" to "Front gray",
    "front_transparent" to "Front transparent",
    "front_shiny_transparent" to "Front shiny transparent"
)

val versionGroupNames = mapOf(
    1 to "Red / Blue",
    2 to "Yellow",
    3 to "Gold / Silver",
    4 to "Crystal",
    5 to "Ruby / Sapphire",
    6 to "Emerald",
    7 to "Firered / Leafgreen",
    8 to "Diamond / Pearl",
    9 to "Platinum",
    10 to "Heartgold / Soulsilver",
    11 to "Black / White",
    12 to "Collosseum",
    13 to "XD (Gale of Darkness)",
    14 to "Black / White 2",
    15 to "X / Y",
    16 to "Omega Ruby / Alpha Sapphire",
    17 to "Sun / Moon",
    18 to "Ultra Sun / Moon",
    19 to "Lets go Pikachu/Eevee",
    20 to "Sword/Shield",
    21 to "The Isle of Armor",
    22 to "The Crown Tundra",
    23 to "Brilliant Diamond / Shining Pearl",
    24 to "Legends-Arceus",
    25 to "Scarlet / Violet",
    26 to "The Teal Mask",
    27 to "The Indigo Disk",
)

//endregion

val languageMap = mapOf(
    1 to "ja-Hrkt",
    2 to "roomaji",
    3 to "ko",
    4 to "zh-Hant",
    5 to "fr",
    6 to "de",
    7 to "es",
    8 to "it",
    9 to "en",
    11 to "ja",
    12 to "zh-Hans"
)

val typeColorMap = mapOf(
    1 to Triple(R.color.type_colour_normal, R.drawable.pokemon_type_icon_normal, R.color.text_colour_normal),
    2 to Triple(R.color.type_colour_fighting, R.drawable.pokemon_type_icon_fighting, R.color.text_colour_fighting),
    3 to Triple(R.color.type_colour_flying, R.drawable.pokemon_type_icon_flying, R.color.text_colour_flying),
    4 to Triple(R.color.type_colour_poison, R.drawable.pokemon_type_icon_poison, R.color.text_colour_poison),
    5 to Triple(R.color.type_colour_ground, R.drawable.pokemon_type_icon_ground, R.color.text_colour_ground),
    6 to Triple(R.color.type_colour_rock, R.drawable.pokemon_type_icon_rock, R.color.text_colour_rock),
    7 to Triple(R.color.type_colour_bug, R.drawable.pokemon_type_icon_bug, R.color.text_colour_bug),
    8 to Triple(R.color.type_colour_ghost, R.drawable.pokemon_type_icon_ghost, R.color.text_colour_ghost),
    9 to Triple(R.color.type_colour_steel, R.drawable.pokemon_type_icon_steel, R.color.text_colour_steel),
    10 to Triple(R.color.type_colour_fire, R.drawable.pokemon_type_icon_fire, R.color.text_colour_fire),
    11 to Triple(R.color.type_colour_water, R.drawable.pokemon_type_icon_water, R.color.text_colour_water),
    12 to Triple(R.color.type_colour_grass, R.drawable.pokemon_type_icon_grass, R.color.text_colour_grass),
    13 to Triple(R.color.type_colour_electric, R.drawable.pokemon_type_icon_electric, R.color.text_colour_electric),
    14 to Triple(R.color.type_colour_psychic, R.drawable.pokemon_type_icon_psychic, R.color.text_colour_psychic),
    15 to Triple(R.color.type_colour_ice, R.drawable.pokemon_type_icon_ice, R.color.text_colour_ice),
    16 to Triple(R.color.type_colour_dragon, R.drawable.pokemon_type_icon_dragon, R.color.text_colour_dragon),
    17 to Triple(R.color.type_colour_dark, R.drawable.pokemon_type_icon_dark, R.color.text_colour_dark),
    18 to Triple(R.color.type_colour_fairy, R.drawable.pokemon_type_icon_fairy, R.color.text_colour_fairy),
    10001 to Triple(R.color.type_colour_unknown, R.drawable.pokemon_type_icon_unknown, R.color.text_colour_unknown),
    10002 to Triple(R.color.type_colour_unknown, R.drawable.pokemon_type_icon_unknown, R.color.text_colour_unknown)
)

val versionGroupMap = mapOf(
    1 to "red-blue" to 1, // red-blue has versionNr1 and is from gen1
    2 to "yellow" to 1,
    3 to "gold-silver" to 2, // gold-silver has versionNr 3 and is from gen2
    4 to "crystal" to 2,
    5 to "ruby-sapphire" to 3,
    6 to "emerald" to 3,
    7 to "firered-leafgreen" to 3,
    8 to "diamond-pearl" to 4,
    9 to "platinum" to 4,
    10 to "heartgold-soulsilver" to 4,
    11 to "black-white" to 5,
    12 to "collosseum" to 3,
    13 to "xd" to 3,
    14 to "black-2-white-2" to 5,
    15 to "x-y" to 6,
    16 to "omega-ruby-alpha-sapphire" to 6,
    17 to "sun-moon" to 7,
    18 to "ultra-sun-ultra-moon" to 7,
    19 to "lets-go-pikachu-lets-go-eevee" to 7,
    20 to "sword-shield" to 8,
    21 to "the-isle-of-armor" to 8,
    22 to "the-crown-tundra" to 8,
    23 to "brilliant-diamond-and-shining-pearl" to 8,
    24 to "legends-arceus" to 8,
    25 to "scarlet-violet" to 9,
    26 to "the-teal-mask" to 9,
    27 to "the-indigo-disk" to 9
)

val versionGroupIdMap = mapOf(
    "red-blue" to 1,
    "yellow" to 2,
    "gold-silver" to 3,
    "crystal" to 4,
    "ruby-sapphire" to 5,
    "emerald" to 6,
    "firered-leafgreen" to 7,
    "diamond-pearl" to 8,
    "platinum" to 9,
    "heartgold-soulsilver" to 10,
    "black-white" to 11,
    "colosseum" to 12,
    "xd" to 13,
    "black-2-white-2" to 14,
    "x-y" to 15,
    "omega-ruby-alpha-sapphire" to 16,
    "sun-moon" to 17,
    "ultra-sun-ultra-moon" to 18,
    "lets-go-pikachu-lets-go-eevee" to 19,
    "sword-shield" to 20,
    "the-isle-of-armor" to 21,
    "the-crown-tundra" to 22,
    "brilliant-diamond-and-shining-pearl" to 23,
    "legends-arceus" to 24,
    "scarlet-violet" to 25,
    "the-teal-mask" to 26,
    "the-indigo-disk" to 27
)