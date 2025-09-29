package com.example.project_mobileapps.data.repo

import com.example.project_mobileapps.data.model.Booking
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import com.google.firebase.Timestamp

class BookingRepository {
    private val bookingsCollection = Firebase.firestore.collection("bookings")

    suspend fun createBooking(booking: Booking): Boolean {
        return try {
            bookingsCollection.add(booking).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getUserBookings(userId: String): List<Booking> {
        return try {
            val snapshot = bookingsCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()
            snapshot.toObjects(Booking::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getTodaysBookingsForDoctor(doctorId: String): List<Booking> {
        // Tentukan rentang waktu "hari ini" dari jam 00:00 hingga 23:59
        val now = Calendar.getInstance()
        now.set(Calendar.HOUR_OF_DAY, 0); now.set(Calendar.MINUTE, 0); now.set(Calendar.SECOND, 0)
        val startOfDay = Timestamp(now.time)

        now.add(Calendar.DAY_OF_YEAR, 1)
        val endOfDay = Timestamp(now.time)

        return try {
            val snapshot = bookingsCollection
                .whereEqualTo("doctorId", doctorId)
                .whereGreaterThanOrEqualTo("appointmentTimestamp", startOfDay)
                .whereLessThan("appointmentTimestamp", endOfDay)
                .get().await()

            snapshot.toObjects(Booking::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getUpcomingBookingForUser(userId: String): Booking? {
        return try {
            val snapshot = bookingsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "Akan Datang")
                .whereGreaterThanOrEqualTo("appointmentTimestamp", Timestamp.now())
                .orderBy("appointmentTimestamp", com.google.firebase.firestore.Query.Direction.ASCENDING) // Urutkan dari yang paling dekat
                .limit(1)
                .get()
                .await()

            snapshot.documents.firstOrNull()?.toObject(Booking::class.java)
        } catch (e: Exception) {
            null
        }
    }
}