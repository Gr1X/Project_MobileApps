package com.example.project_mobileapps.features.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.project_mobileapps.ui.components.ActionGrid
import com.example.project_mobileapps.ui.components.GreetingSection
import com.example.project_mobileapps.ui.components.RecentAppointmentsSection
import com.example.project_mobileapps.ui.components.UpcomingAppointmentCard

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onDoctorClick: (String) -> Unit,
    onAppointmentClick: () -> Unit,
    onNewsClick: () -> Unit
) {
    // Column utama yang bisa di-scroll agar semua komponen muat
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // 1. Menampilkan komponen sapaan
        GreetingSection(name = uiState.userName)

        // 2. Menampilkan komponen grid ikon
        ActionGrid(onActionClick = { actionTitle ->
            when (actionTitle) {
                "Appointment" -> onAppointmentClick()
                "News" -> onNewsClick()
            }
        })

        // 3. Menampilkan janji temu jika ada
        uiState.upcomingAppointment?.let { appointment ->
            UpcomingAppointmentCard(appointment = appointment)
        }

        // 4. Menampilkan daftar dokter
        RecentAppointmentsSection(
            doctors = uiState.recentDoctors,
            onDoctorClick = onDoctorClick
        )
    }
}