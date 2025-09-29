package com.example.project_mobileapps.data.model

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@IgnoreExtraProperties
data class Booking(
    val id: String = "",
    val doctorId: String = "",
    val doctorName: String = "",
    val userId: String = "",
    val userName: String = "",

    @ServerTimestamp
    val createdAt: Date? = null,
    val appointmentTimestamp: Date? = null,

    val keluhan: String = "",
    val status: String = ""
)