package com.example.project_mobileapps.data.local

import com.example.project_mobileapps.data.model.Role
import com.example.project_mobileapps.data.model.User

object DummyUserDatabase {
    val users = listOf(
        // Data Pasien
        User(
            uid = "pasien01",
            name = "pasien",
            email = "pasien@gmail.com",
            password = "pasien",
            role = Role.PASIEN
        ),
        // Data Dokter
        User(
            uid = "dokter01",
            name = "dokter",
            email = "dokter@gmail.com",
            password = "dokter",
            role = Role.DOKTER
        ),
        // Data Admin
        User(
            uid = "admin01",
            name = "Admin",
            email = "admin@gmail.com",
            password = "admin",
            role = Role.ADMIN
        )
    )
}