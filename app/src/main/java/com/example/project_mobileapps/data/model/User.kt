package com.example.project_mobileapps.data.model

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@IgnoreExtraProperties
data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val profilePhotoUrl: String = "",

    @ServerTimestamp
    val createdAt: Date? = null
)