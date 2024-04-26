package com.example.pokinfo.data.models

import com.google.firebase.Timestamp

data class Profile(
    val username: String = "",
    val emailAddress: String = "",
    val registrationDate: Timestamp,
    val profilePicture: String = ""
)