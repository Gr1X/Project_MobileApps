package com.example.project_mobileapps.di

import com.example.project_mobileapps.data.repo.DummyQueueRepository
import com.example.project_mobileapps.data.repo.QueueRepository

object AppContainer {
    val queueRepository: QueueRepository = DummyQueueRepository
}