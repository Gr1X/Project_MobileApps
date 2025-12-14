// File: utils/PdfGenerator.kt
package com.example.project_mobileapps.utils

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.project_mobileapps.data.model.QueueItem
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfGenerator {

    fun generateAndOpenMedicalReport(
        context: Context,
        patientName: String,
        data: QueueItem
    ) {
        val pdfDocument = PdfDocument()

        // 1. Setup Halaman A4
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Ukuran A4 dalam point (1/72 inch)
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        // --- WARNA & FONT ---
        val primaryColor = Color.rgb(41, 121, 255) // Biru Professional
        val blackColor = Color.BLACK
        val grayColor = Color.DKGRAY

        // 2. HEADER (KOP SURAT)
        paint.color = primaryColor
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 24f
        canvas.drawText("KLINIK SEHAT BERSAMA", 40f, 60f, paint)

        paint.color = blackColor
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 12f
        canvas.drawText("Jl. Kesehatan No. 123, Jakarta Pusat", 40f, 85f, paint)
        canvas.drawText("Telp: (021) 555-9999 | Email: info@kliniksehat.com", 40f, 100f, paint)

        // Garis Pembatas
        paint.strokeWidth = 2f
        paint.color = grayColor
        canvas.drawLine(40f, 120f, 555f, 120f, paint)

        // 3. JUDUL DOKUMEN
        paint.color = blackColor
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 18f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("RESUME MEDIS & RESEP", 595f / 2, 160f, paint)

        // 4. INFO PASIEN (Kiri) & DOKTER (Kanan)
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        val dateStr = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(Date())

        var yPos = 200f
        val leftCol = 40f
        val rightCol = 350f

        // Kiri
        canvas.drawText("Nama Pasien : $patientName", leftCol, yPos, paint)
        canvas.drawText("No. Antrian : ${data.queueNumber}", leftCol, yPos + 20, paint)

        // Kanan
        canvas.drawText("Tanggal : $dateStr", rightCol, yPos, paint)
        canvas.drawText("Dokter  : dr. Budi Santoso", rightCol, yPos + 20, paint)

        // 5. ISI REKAM MEDIS (Kotak Data)
        yPos += 60f

        // Helper function untuk menggambar section
        fun drawSection(title: String, content: String, startY: Float): Float {
            var currentY = startY

            // Judul Section
            paint.color = primaryColor
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(title, leftCol, currentY, paint)
            currentY += 20f

            // Isi
            paint.color = blackColor
            paint.typeface = Typeface.DEFAULT

            // Simple text wrapping (memecah teks panjang)
            val maxCharsPerLine = 85
            val words = content.split(" ")
            var line = ""
            for (word in words) {
                if ((line + word).length > maxCharsPerLine) {
                    canvas.drawText(line, leftCol, currentY, paint)
                    currentY += 15f
                    line = "$word "
                } else {
                    line += "$word "
                }
            }
            canvas.drawText(line, leftCol, currentY, paint)

            return currentY + 30f // Jarak antar section
        }

        // TANDA VITAL
        val vitalInfo = "Tensi: ${data.bloodPressure} mmHg | Suhu: ${data.temperature}°C | Berat: ${data.weightKg} kg"
        yPos = drawSection("TANDA VITAL (Objective)", vitalInfo, yPos)

        // DIAGNOSA
        yPos = drawSection("DIAGNOSA (Assessment)", data.diagnosis, yPos)

        // RESEP OBAT (Penting!)
        yPos = drawSection("RESEP OBAT (Plan)", data.prescription, yPos)

        // CATATAN
        if (data.doctorNotes.isNotEmpty()) {
            yPos = drawSection("CATATAN TAMBAHAN", data.doctorNotes, yPos)
        }

        // 6. FOOTER & TANDA TANGAN
        yPos += 40f
        paint.color = blackColor
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Jakarta, $dateStr", 555f, yPos, paint)
        yPos += 60f
        canvas.drawText("( dr. Budi Santoso )", 555f, yPos, paint)

        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 10f
        paint.color = Color.LTGRAY
        canvas.drawText("Dokumen ini dicetak otomatis oleh Aplikasi KlinIQ.", 595f / 2, 800f, paint)

        pdfDocument.finishPage(page)

        // 7. SIMPAN & BUKA FILE
        saveAndOpenPdf(context, pdfDocument, "MedicalRecord_${data.queueNumber}.pdf")
    }

    private fun saveAndOpenPdf(context: Context, document: PdfDocument, fileName: String) {
        try {
            // Simpan di folder Downloads/KlinIQ
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "KlinIQ")
            if (!dir.exists()) dir.mkdirs()

            val file = File(dir, fileName)
            document.writeTo(FileOutputStream(file))
            document.close()

            Toast.makeText(context, "PDF Disimpan: ${file.absolutePath}", Toast.LENGTH_LONG).show()

            // Intent Membuka PDF
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, "application/pdf")
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NO_HISTORY

            val chooser = Intent.createChooser(intent, "Buka Laporan Medis")
            context.startActivity(chooser)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal membuat PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}