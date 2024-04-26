package com.example.pokinfo.data.local.converter

import androidx.room.TypeConverter
import com.example.pokinfo.data.models.database.pokemon.StatValues
import com.example.pokinfo.data.models.database.pokemon.PkStatInfos
import com.example.pokinfo.data.models.database.pokemon.PkType
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.lang.reflect.Type
import java.util.Date

class StringListConverter {
    private val moshi = Moshi.Builder().build()
    private val listType: Type = Types.newParameterizedType(List::class.java, String::class.java)

    @TypeConverter
    fun fromString(value: String): List<String>? {
        return moshi.adapter<List<String>>(listType).fromJson(value)
    }

    @TypeConverter
    fun fromList(list: List<String>?): String {
        return moshi.adapter<List<String>>(listType).toJson(list)
    }
}

class LongListConverter {
    private val moshi = Moshi.Builder().build()
    private val listType: Type = Types.newParameterizedType(List::class.java, Long::class.javaObjectType)

    @TypeConverter
    fun fromString(value: String): List<Long>? {
        return moshi.adapter<List<Long>>(listType).fromJson(value)
    }

    @TypeConverter
    fun fromList(list: List<Long>?): String {
        return moshi.adapter<List<Long>>(listType).toJson(list)
    }
}

class PkTypeConverter {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val adapter: JsonAdapter<PkType> = moshi.adapter(PkType::class.java)

    @TypeConverter
    fun fromJson(json: String): PkType? {
        return adapter.fromJson(json)
    }

    @TypeConverter
    fun toJson(pkType: PkType): String {
        return adapter.toJson(pkType)
    }
}

class StatInfosConverter {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val listType: Type = Types.newParameterizedType(List::class.java, PkStatInfos::class.java)
    private val adapter: JsonAdapter<List<PkStatInfos>> = moshi.adapter(listType)

    @TypeConverter
    fun fromJson(json: String): List<PkStatInfos>? {
        return adapter.fromJson(json)
    }

    @TypeConverter
    fun toJson(pkStatInfos: List<PkStatInfos>): String {
        return adapter.toJson(pkStatInfos)
    }


}

class StatValueConverter {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val listType: Type = Types.newParameterizedType(List::class.java, StatValues::class.java)
    private val adapter: JsonAdapter<List<StatValues>> = moshi.adapter(listType)

    @TypeConverter
    fun fromJson(json:String): List<StatValues>? {
        return adapter.fromJson(json)
    }

    @TypeConverter
    fun toJson(statValues: List<StatValues>): String {
        return adapter.toJson(statValues)
    }
}

class TimeConverter {
    @TypeConverter
    fun fromDate(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun toDate(timestamp: Long?): Date? {
        return timestamp?.let { Date(it) }
    }
}


