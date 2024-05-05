package com.example.pokinfo.data.local


import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.pokinfo.data.local.converter.LongListConverter
import com.example.pokinfo.data.local.converter.PkTypeConverter
import com.example.pokinfo.data.local.converter.StatInfosConverter
import com.example.pokinfo.data.local.converter.StatValueConverter
import com.example.pokinfo.data.local.converter.StringListConverter
import com.example.pokinfo.data.local.converter.TimeConverter
import com.example.pokinfo.data.models.database.pokemon.PokemonForList
import com.example.pokinfo.data.models.database.versionAndLanguageNames.LanguageNames
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
import com.example.pokinfo.data.models.database.pokemon.Pokemon
import com.example.pokinfo.data.models.database.pokemon.PokemonDexEntries
import com.example.pokinfo.data.models.database.pokemon.PokemonInsertStatus
import com.example.pokinfo.data.models.database.type.DamageRelation
import com.example.pokinfo.data.models.database.type.GameIndex
import com.example.pokinfo.data.models.database.type.MoveWithType
import com.example.pokinfo.data.models.database.type.PokemonType
import com.example.pokinfo.data.models.database.type.PokemonWithThisType
import com.example.pokinfo.data.models.database.type.PokemonTypeName
import com.example.pokinfo.data.models.database.versionAndLanguageNames.VersionNames


@Database(
    entities = [
        PokemonInsertStatus::class,
        PokemonForList::class,
        PokemonType::class,
        MoveWithType::class,
        DamageRelation::class,
        GameIndex::class,
        PokemonTypeName::class,
        PokemonWithThisType::class,
        Pokemon::class,
        PkNames::class,
        PkSpecieInfos::class,
        PokemonDexEntries::class,
        PkMoves::class,
        PkMoveNames::class,
        PkMove::class,
        PkMoveVersionGroupDetail::class,
        PkAbilityInfo::class,
        PkAbilitiesToJoin::class,
        PkAbilityFlavorText::class,
        PkAbilityEffectText::class,
        PkAbilityName::class,
        LanguageNames::class,
        VersionNames::class,
        PkMoveMachines::class,
        PkForms::class
    ],
    version = 34
)
@TypeConverters(
    StringListConverter::class,
    PkTypeConverter::class,
    StatInfosConverter::class,
    LongListConverter::class,
    TimeConverter::class,
    StatValueConverter::class
)
abstract class PokeDatabase : RoomDatabase() {

    abstract val pokeDao: PokeDatabaseDao
    abstract val pokeTypeDao: PokemonTypeDao

    companion object {
        private lateinit var dbInstance: PokeDatabase

        fun getDatabase(context: Context): PokeDatabase {
            synchronized(this) {
                if (!this::dbInstance.isInitialized) {
                    dbInstance = Room.databaseBuilder(
                        context.applicationContext,
                        PokeDatabase::class.java,
                        "poke_database"
                    ).fallbackToDestructiveMigration().build()
                }
                return dbInstance
            }
        }
    }
}