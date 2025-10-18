package com.example.project_mobileapps.di

import com.example.project_mobileapps.data.repo.DummyQueueRepository
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
    /**
     * Menyediakan instance singleton dari [QueueRepository].
     * Saat ini, container ini diatur untuk menyediakan [DummyQueueRepository],
     * yang ideal untuk pengembangan dan demonstrasi UI tanpa memerlukan backend sungguhan.
     */
    val queueRepository: QueueRepository = DummyQueueRepository
}