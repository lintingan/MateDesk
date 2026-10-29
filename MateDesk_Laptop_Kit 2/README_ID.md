# MateDesk Pro v0.2 — Paket untuk dicoba di laptop

Paket ini menyediakan dua cara mencoba MateDesk:

1. **Preview laptop (paling cepat):** membuka prototipe desktop HTML di browser MacBook. Ini bukan APK dan tidak bisa mendeteksi atau meluncurkan aplikasi Android di laptop.
2. **Proyek Android Studio:** source Kotlin + WebView yang dapat dibangun menjadi APK setelah Android Studio, JDK, Android SDK, dan koneksi internet untuk dependensi tersedia.

## A. Coba sekarang di MacBook tanpa Android Studio

1. Ekstrak `MateDesk_Laptop_Kit.zip`.
2. Buka folder hasil ekstrak.
3. Klik dua kali `Jalankan_MateDesk_Preview.command`.
4. Browser akan terbuka pada alamat lokal `http://127.0.0.1:8765/MateDesk_Prototype_v0.2.html`.
5. Untuk menghentikan server, tekan `Control + C` pada jendela Terminal yang terbuka.

Jika macOS menolak menjalankan file `.command`, klik kanan file itu → **Open**. Alternatifnya, buka `MateDesk_Prototype_v0.2.html` langsung di Safari/Chrome/Firefox.

Preview berisi desktop interaktif, Start menu, jendela internal, Notes, Calculator, File Manager berbasis pemilih berkas browser, Settings, tema, dan wallpaper. Data lokal tersimpan di browser yang digunakan.

## B. Build APK melalui Android Studio di MacBook

### Persyaratan
- Android Studio.
- JDK 17 atau JDK yang dibundel dengan Android Studio.
- Android SDK Platform 35 dan Android Build-Tools 35.0.0 (pasang melalui Android Studio → Settings → Languages & Frameworks → Android SDK).
- Koneksi internet saat pertama kali mengunduh Gradle/Android Gradle Plugin.

### Cara build
1. Buka Android Studio.
2. Pilih **Open** dan arahkan ke folder `android-project` di dalam paket ini.
3. Jika diminta mengunduh SDK atau plugin Gradle, setujui setelah memeriksa versinya.
4. Pilih **Build → Build Bundle(s) / APK(s) → Build APK(s)**, atau jalankan Terminal dari folder `android-project`:

   ```bash
   ./gradlew :app:assembleDebug
   ```

5. APK debug yang dihasilkan berada di:

   `android-project/app/build/outputs/apk/debug/app-debug.apk`

Ada skrip `Build_MateDesk_APK.command` untuk memeriksa SDK lalu menjalankan build. Skrip `gradlew` di paket ini merupakan bootstrap ringan yang mengunduh Gradle 8.9 bila belum ada; skrip ini bukan JAR Gradle Wrapper resmi.

## C. Apa yang benar-benar berfungsi?

- Preview HTML diuji di Chromium: desktop tampil, Start menu bisa dibuka, Notes bisa dibuka, jendela bisa dimaksimalkan/dipulihkan/ditutup, dan tidak ditemukan error JavaScript pada alur uji dasar.
- Android source mengemas HTML di WebView dan menyediakan jembatan Kotlin untuk daftar aplikasi Android yang terlihat oleh package manager, ikon, peluncuran aplikasi, pemilihan berkas, penyimpanan teks, dan percobaan meminta bounds jendela.
- APK **belum dibangun di lingkungan pembuat paket ini** karena Android SDK dan Gradle tidak tersedia di sana. Android source harus di-build di MacBook menggunakan Android Studio sebelum dipasang.
- Perilaku aplikasi eksternal sebagai jendela bebas bergantung pada perangkat/HarmonyOS. `setLaunchBounds()` hanya memberi petunjuk; sistem dapat mengabaikannya. Aplikasi eksternal tidak menjadi jendela virtual internal MateDesk secara otomatis.

## D. Catatan keamanan

Aplikasi eksternal dibuka menggunakan Android Intent. MateDesk tidak memodifikasi APK pihak ketiga, tidak menggunakan root, dan tidak menginjeksikan kode ke aplikasi lain. File Manager hanya bekerja dengan file yang dipilih oleh pengguna.
