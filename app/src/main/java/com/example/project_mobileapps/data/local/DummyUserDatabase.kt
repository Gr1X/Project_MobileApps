// File: data/local/DummyUserDatabase.kt
package com.example.project_mobileapps.data.local

import com.example.project_mobileapps.data.model.Gender
import com.example.project_mobileapps.data.model.Role
import com.example.project_mobileapps.data.model.User

object DummyUserDatabase {
    val users = mutableListOf(
        User(
            uid = "pasien01",
            name = "Budi Santoso",
            email = "pasien@gmail.com",
            password = "pasien",
            role = Role.PASIEN,
            phoneNumber = "081234567890",
            gender = Gender.PRIA,
            dateOfBirth = "1990-05-15",
            profilePictureUrl = ""
        ),
        User(
            uid = "pasien02",
            name = "Citra Lestari",
            email = "citra@gmail.com",
            password = "citra",
            role = Role.PASIEN,
            phoneNumber = "087654321098",
            gender = Gender.WANITA,
            dateOfBirth = "1995-11-20",
            profilePictureUrl = ""
        ),
        User(
            uid = "dokter01",
            name = "Dr. Budi Santoso",
            email = "dokter@gmail.com",
            password = "dokter",
            role = Role.DOKTER
        ),
        User(
            uid = "admin01",
            name = "Admin Klinik",
            email = "admin@gmail.com",
            password = "admin",
            role = Role.ADMIN
        )
    )
}