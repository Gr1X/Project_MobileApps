# KliniQ: Aplikasi Antrian Praktik Dokter

## 1. Deskripsi Aplikasi

**KliniQ** adalah sebuah aplikasi mobile Android yang dirancang untuk memodernisasi dan menyederhanakan sistem antrian di klinik dokter. Aplikasi ini dibangun untuk memenuhi tugas Ujian Tengah Semester (UTS) mata kuliah IF570L (LAB) Mobile Application Programming.

Tujuan utama dari KliniQ adalah menggantikan sistem antrian manual yang konvensional dengan solusi digital yang efisien dan transparan. Aplikasi ini melayani tiga peran pengguna utama dengan fungsionalitas yang spesifik untuk masing-masing peran:

### Fitur Utama

**Untuk Pasien:**
* **Registrasi & Login:** Pasien dapat membuat akun dan masuk ke dalam aplikasi.
* **Pengambilan Antrian Online:** Mengambil nomor antrian secara digital tanpa harus datang langsung ke klinik.
* **Pemantauan Real-Time:** Melihat posisi antrian saat ini dan estimasi waktu tunggu secara langsung dari ponsel.
* **Riwayat Kunjungan:** Mengakses kembali riwayat kunjungan sebelumnya untuk melihat detail keluhan dan tanggal.
* **Notifikasi:** Menerima pemberitahuan saat nomor antrian akan segera dipanggil.
* **Pembatalan:** Membatalkan janji temu jika berhalangan hadir.

**Untuk Dokter:**
* **Manajemen Antrian:** Memanggil pasien berikutnya, menandai konsultasi selesai, dan melihat daftar antrian harian.
* **Kontrol Praktik:** Mengatur status praktik (Buka/Tutup) secara manual.
* **Dasbor Harian:** Melihat ringkasan jumlah pasien yang telah dilayani dalam sehari.

**Untuk Admin / Asisten Dokter:**
* **Manajemen Penuh:** Mengelola seluruh alur antrian, termasuk menambah atau menghapus antrian secara manual.
* **Pengelolaan Jadwal:** Mengatur dan memperbarui jadwal praktik dokter (hari dan jam).
* **Laporan & Analitik:** Melihat laporan statistik pasien (harian, mingguan, bulanan) untuk analisis.

## 2. Teknologi yang Digunakan

* **Bahasa:** Kotlin
* **UI Framework:** Jetpack Compose
* **Arsitektur:** Model-View-ViewModel (MVVM)
* **Navigasi:** Jetpack Navigation Compose
* **Asynchronous:** Kotlin Coroutines & Flow
* **Manajemen State:** `StateFlow` dan `collectAsState`
* **Pola Desain:** Repository Pattern, Dependency Injection (manual via `AppContainer`)

## 3. Anggota Kelompok

* [**Gregorius Frederico**] - [**00000078998**]
* [**Vilbert Jhovan**] - [**00000077251**]
* [**Keisen Susanto**] - [**00000078136**]
