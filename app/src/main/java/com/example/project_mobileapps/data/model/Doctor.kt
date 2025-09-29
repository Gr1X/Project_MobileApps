package com.example.project_mobileapps.data.model

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Doctor(
    val id: String = "",
    val name: String = "",
    val specialization: String = "",
    val photoUrl: String = ""
)