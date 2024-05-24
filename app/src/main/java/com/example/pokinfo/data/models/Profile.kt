package com.example.pokinfo.data.models

import com.example.pokinfo.data.models.firebase.TeamPokemon
import com.google.firebase.Timestamp

data class Profile(
    val username: String = "",
    val emailAddress: String = "",
    val registrationDate: Timestamp = Timestamp.now(),
    val profilePicture: String = "",
    val ownedTeams: List<String> = emptyList(),  // Liste der IDs der Teams, die der Benutzer besitzt
    val accessibleTeams: List<String> = emptyList()  // Liste der IDs der Teams, auf die der Benutzer Zugriff hat
)

data class PublicProfile(
    val userId: String = "",
    val username: String = "",
    val profilePicture: String = ""
) {
    companion object {
        @JvmStatic
        fun fromMap(map: Map<String, Any?>): PublicProfile? {
            if (map.isEmpty()) return null

            return PublicProfile(
                userId = map["userId"] as? String ?: "",
                username = map["username"] as? String ?: "?",
                profilePicture = map["profilePicture"] as? String ?: ""
            )
        }
    }


}