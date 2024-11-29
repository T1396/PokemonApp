package com.example.pokinfo.data.enums

enum class AttackGenerationFilter(val filterName: String, val genId: Int) {
    FROM_GEN1("Inv. in Gen1", 1),
    FROM_GEN2("Inv. in Gen2", 2),
    FROM_GEN3("Inv. in Gen3", 3),
    FROM_GEN4("Inv. in Gen4", 4),
    FROM_GEN5("Inv. in Gen5", 5),
    FROM_GEN6("Inv. in Gen6", 6),
    FROM_GEN7("Inv. in Gen7", 7),
    FROM_GEN8("Inv. in Gen8", 8),
    FROM_GEN9("Inv. in Gen9", 9)
}

enum class AttackTypeFilter(val filterName: String) {
    PHYSICAL_ATTACKS("Phy. Attacks"),
    SPECIAL_ATTACKS("Sp. Attacks"),
    STATUS_ATTACKS("Status-Attacks"),
}

enum class AbilityGenerationFilter(val filterName: String, val genId: Int) {
    FROM_GEN3("Gen3", 3),
    FROM_GEN4("Gen4", 4),
    FROM_GEN5("Gen5", 5),
    FROM_GEN6("Gen6", 6),
    FROM_GEN7("Gen7", 7),
    FROM_GEN8("Gen8", 8),
    FROM_GEN9("Gen9", 9)
}

enum class PokemonSortSetting(val filterName: String) {
    WEIGHT("Weight"),
    HEIGHT("Height"),
    NAME("Name"),
    STATS("Stat Values")
}

enum class PokemonSortOption {
    ASCENDING, DESCENDING, INACTIVE
}
