package com.example.project_mobileapps.features.patient.home

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
class HomeViewModelFactory : ViewModelProvider.Factory {
    /**
     * Membuat instance ViewModel baru.
     * @param modelClass Class dari ViewModel yang akan dibuat.
     * @return Instance [HomeViewModel] yang baru.
     * @throws IllegalArgumentException jika [modelClass] bukan [HomeViewModel].
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(
                doctorRepository = DoctorRepository(),
                authRepository = AuthRepository,
                queueRepository = AppContainer.queueRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}