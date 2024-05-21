package com.example.pokinfo.data.remote

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.network.okHttpClient
import com.example.pokeinfo.data.graphModel.AbilityDetailQuery
import com.example.pokeinfo.data.graphModel.AllAbilitiesQuery
import com.example.pokeinfo.data.graphModel.AttackDetailsQuery
import com.example.pokeinfo.data.graphModel.AttacksQuery
import com.example.pokeinfo.data.graphModel.LanguageAndVersionNamesQuery
import com.example.pokeinfo.data.graphModel.PokeListQuery
import com.example.pokeinfo.data.graphModel.PokemonDetail1Query
import com.example.pokeinfo.data.graphModel.PokemonDetail2Query
import com.example.pokeinfo.data.graphModel.PokemonDetail3Query
import com.example.pokeinfo.data.graphModel.PokemonWithAbilityQuery
import com.example.pokinfo.BuildConfig
import com.example.pokinfo.data.models.typeInfo.Type
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.concurrent.TimeUnit


private const val BASE_URL = "https://pokeapi.co/api/v2/"
//private const val BASE_URL = BuildConfig.localApiUrl
private const val LOCAL_GRAPH_URL = BuildConfig.localGraphqlUrl

interface PokeApiService {

    @GET("type/{id}")
    suspend fun getTypeInfos(@Path("id")id: Int) : Type

}

class GraphApiServiceImpl(private val apolloClient: ApolloClient) {

    /** Gets a List of every Pokemon with basic Info like weight height and sprites for home fragment  */
    suspend fun sendPokeListQuery(languageId: Int): PokeListQuery.Data? {
        val response = apolloClient.query(PokeListQuery(languageId)).execute()
        return response.data
    }

    /** For Home-Detail Fragment */
    suspend fun firstPokemonQuery(nr: Int, languageId: Int): PokemonDetail1Query.Data? {
        val response = apolloClient.query(PokemonDetail1Query(nr, languageId))
            .execute()
        return response.data
    }

    /** For Home-Detail Fragment */
    suspend fun secondPokemonQuery(nr: Int, languageId: Int): PokemonDetail2Query.Data? {
        val response = apolloClient.query(PokemonDetail2Query(nr, languageId))
            .execute()
        return response.data
    }

    /** For Home-Detail Fragment */
    suspend fun thirdPokemonQuery(speciesId: Int, languageId: Int): PokemonDetail3Query.Data? {
        val response = apolloClient.query(PokemonDetail3Query(speciesId, languageId)).execute()
        return response.data
    }

    /** All Attacks */
    suspend fun sendAttackListQuery(languageId: Int): AttacksQuery.Data? {
        val response = apolloClient.query(AttacksQuery(languageId))
            .execute()
        return response.data
    }

    /** Details for an attack */
    suspend fun sendAttackDetailQuery(moveId: Int, languageId: Int): AttackDetailsQuery.Data? {
        val response = apolloClient.query(AttackDetailsQuery(moveId, languageId))
            .execute()
        return response.data
    }

    /** All Abilities*/
    suspend fun sendAbilitiesListQuery(languageId: Int): AllAbilitiesQuery.Data? {
        val response = apolloClient.query(AllAbilitiesQuery(languageId)).execute()
        return response.data
    }

    /** Ability Details */
    suspend fun sendAbilityDetailQuery(abilityId: Int): AbilityDetailQuery.Data? {
        val response = apolloClient.query(AbilityDetailQuery(abilityId)).execute()
        return response.data
    }

    /** Gets every LanguageName and Version Names (Red, Yellow etc) */
    suspend fun sendLanguageVersionNameQuery(languageId: Int): LanguageAndVersionNamesQuery.Data? {
        val response = apolloClient.query(LanguageAndVersionNamesQuery(languageId)).execute()
        return response.data
    }

    /** Gets a list containing every pokemon who has a specific ability */
    suspend fun sendAbilityPokemonListQuery(abilityId: Int): PokemonWithAbilityQuery.Data? {
        val response = apolloClient.query(PokemonWithAbilityQuery(abilityId)).execute()
        return response.data
    }
}

private val loggingInterceptor = HttpLoggingInterceptor().apply {
    level = HttpLoggingInterceptor.Level.BODY // Zeigt die vollständigen Anfragedetails
}

private val httpClient = OkHttpClient.Builder()
    .addInterceptor(loggingInterceptor)
    .build()

private val okHttpClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(10, TimeUnit.SECONDS)
    .writeTimeout(10, TimeUnit.SECONDS)
    .build()

private val apolloClient = ApolloClient.Builder()
    .serverUrl(LOCAL_GRAPH_URL)
    //.httpEngine(OkHttpEngine)
    .okHttpClient(okHttpClient)
    .addHttpHeader("x-hasura-admin-secret", "pokemon")
    .build()

private val moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

private val retrofit = Retrofit.Builder()
   // .client(httpClient)
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .baseUrl(BASE_URL)
    .build()


object PokeApi {
    val retrofitService: PokeApiService by lazy { retrofit.create(PokeApiService::class.java) }
    val retrofitGraphService: GraphApiServiceImpl by lazy { GraphApiServiceImpl(apolloClient)}
}