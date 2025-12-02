package com.example.project_mobileapps.features.patient.home

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.project_mobileapps.data.repo.AuthRepository
import com.example.project_mobileapps.data.repo.DoctorRepository
import com.example.project_mobileapps.di.AppContainer
/**
 * Factory class untuk membuat instance [HomeViewModel].
 * Ini diperlukan karena [HomeViewModel] memiliki dependensi (repository)
 * yang perlu di-pass saat konstruksi.
 */
class HomeViewModelFactory(private val application: Application) : ViewModelProvider.Factory { // Terima Application
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(
                application = application, // Pass Application
                doctorRepository = DoctorRepository(),
                authRepository = AuthRepository,
                queueRepository = AppContainer.queueRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}