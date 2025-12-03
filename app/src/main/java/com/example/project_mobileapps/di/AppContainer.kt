package com.example.project_mobileapps.di

import com.example.project_mobileapps.data.repo.DoctorRepository
import com.example.project_mobileapps.data.repo.FirestoreQueueRepository
import com.example.project_mobileapps.data.repo.MealPlanRepository
import com.example.project_mobileapps.data.repo.QueueRepository

/**
 * Object singleton yang berfungsi sebagai container Dependency Injection (DI) sederhana.
 * Pola ini dikenal sebagai Service Locator. Tujuannya adalah untuk menyediakan instance
 * repository yang konsisten (singleton) ke seluruh aplikasi.
 *
 * Dengan memusatkan pembuatan instance di sini, kita memudahkan pengelolaan dependensi
 * dan mempermudah penggantian implementasi di masa depan (misalnya, dari Dummy ke implementasi network).
 */
object AppContainer {
    const val CLINIC_ID = "VmdA8nFNc05tBCDaF0ip"

    val queueRepository: QueueRepository = FirestoreQueueRepository
    val doctorRepository: DoctorRepository = DoctorRepository()
    val mealPlanRepository = MealPlanRepository()
}