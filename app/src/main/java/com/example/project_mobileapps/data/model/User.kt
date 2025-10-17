package com.example.project_mobileapps.data.model

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

enum class Gender {
    PRIA,
    WANITA
}


enum class Role {
    PASIEN,
    DOKTER,
    ADMIN
}

data class User(
    val uid: String,
    val name: String,
    val email: String,
    val password: String? = null,
    val role: Role,
    val phoneNumber: String = "N/A",
    val gender: Gender = Gender.PRIA,
    val dateOfBirth: String = "N/A",
    val profilePictureUrl: String = ""
)