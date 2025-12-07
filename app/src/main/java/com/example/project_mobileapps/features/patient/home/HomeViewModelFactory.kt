// File: features/patient/home/HomeViewModelFactory.kt
package com.example.project_mobileapps.features.patient.home

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.project_mobileapps.data.repo.AuthRepository
import com.example.project_mobileapps.data.repo.DoctorRepository
import com.example.project_mobileapps.di.AppContainer

class HomeViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(
                application = application, // Pass Application ke ViewModel
                doctorRepository = DoctorRepository(),
                authRepository = AuthRepository,
                queueRepository = AppContainer.queueRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}