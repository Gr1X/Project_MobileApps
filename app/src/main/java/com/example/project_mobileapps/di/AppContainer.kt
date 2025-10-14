package com.example.project_mobileapps.di

import com.example.project_mobileapps.data.repo.DummyQueueRepository
import com.example.project_mobileapps.data.repo.QueueRepository

object AppContainer {

    // Di sinilah kita memilih sumber listrik: Genset (Dummy) atau PLN (Firebase)
    val queueRepository: QueueRepository = DummyQueueRepository

    // Nanti setelah UTS, Anda tinggal ganti baris di atas menjadi:
    // val queueRepository: QueueRepository = FirebaseQueueRepository()
}