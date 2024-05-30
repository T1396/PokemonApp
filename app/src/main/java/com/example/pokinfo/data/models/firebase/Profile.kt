package com.example.pokinfo.data.models.firebase

import com.google.firebase.Timestamp

data class Profile(
    val userId: String = "",
    val emailAddress: String = ""
)

data class PublicProfile(
    val userId: String = "",
    val username: String = "",
    val profilePicture: String = "",
    val teamsCount: Int = 0,
    val likeCount: Int = 0,
    val registrationDate: Timestamp = Timestamp.now(),
) {

    fun toHashMap(): Map<String, Any?> {
        return hashMapOf(
            "userId" to userId,
            "username" to username,
            "profilePicture" to profilePicture,
            "teamsCount" to teamsCount,
            "likeCount" to likeCount,
            "registrationDate" to registrationDate.toDate()
        )
    }

    companion object {
        @JvmStatic
        fun fromMap(map: Map<String, Any?>?): PublicProfile? {
            if (map.isNullOrEmpty()) return null

            return PublicProfile(
                userId = map["userId"] as? String ?: "",
                username = map["username"] as? String ?: "?",
                profilePicture = map["profilePicture"] as? String ?: "",
                teamsCount = (map["teamsCount"] as? Number)?.toInt() ?: 0,
                likeCount = (map["likeCount"] as? Number)?.toInt() ?: 0,
                registrationDate = ((map["registrationDate"] as? Timestamp)?.let {
                    Timestamp(it.seconds, it.nanoseconds)
                }) ?: Timestamp(0, 0)
            )
        }
    }
}