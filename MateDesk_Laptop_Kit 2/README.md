# MateDesk Pro v0.3.0

MateDesk adalah desktop environment virtual bergaya PC untuk tablet Android/HarmonyOS yang kompatibel. Antarmuka berjalan dalam WebView; aplikasi eksternal tetap diluncurkan dan dikelola oleh sistem Android.

## Isi paket

- `MateDesk_Prototype_v0.2.html` — preview untuk mencoba di laptop.
- `android-project/` — proyek Android Studio (Kotlin + WebView).
- `.github/workflows/android-apk.yml` — build APK otomatis di GitHub Actions.
- `Build_MateDesk_APK.command` — build lokal di macOS jika Android Studio, JDK 17, Android SDK Platform 35, Build-Tools 35.0.0, dan koneksi internet tersedia.
- `Jalankan_MateDesk_Preview.command` — jalankan preview lokal di browser.

## Coba desktop di MacBook

1. Ekstrak ZIP.
2. Jalankan `Jalankan_MateDesk_Preview.command` (klik kanan → Open bila macOS memblokirnya), atau buka HTML langsung di browser.
3. Browser preview tidak memiliki izin untuk mendeteksi atau membuka aplikasi Android di tablet.

## Cara mendapatkan APK dari GitHub Actions

Workflow sudah disertakan di `.github/workflows/android-apk.yml`. Untuk menggunakannya:

1. Buat repository GitHub.
2. Salin **isi** folder `MateDesk_Laptop_Kit` ke root repository, termasuk folder tersembunyi `.github`.
3. Commit dan push source code ke GitHub. Kamu dapat menggunakan GitHub Desktop agar lebih mudah mengunggah folder beserta subfolder.
4. Buka tab **Actions** di repository. Pilih **Build MateDesk APK**.
5. Jika belum berjalan otomatis, klik **Run workflow**.
6. Buka hasil run yang berhasil. Di bagian **Artifacts**, unduh `MateDesk-debug-APK.zip`.
7. Ekstrak ZIP artefak; di dalamnya ada `app-debug.apk`. Pindahkan APK ke MatePad lalu pasang setelah mengizinkan instalasi dari sumber tersebut.

GitHub Actions memerlukan akses internet untuk mengunduh Gradle dan komponen SDK. Debug APK ditandatangani dengan debug key otomatis untuk pengujian; jangan gunakan sebagai distribusi produksi.

## Build APK secara lokal di MacBook

1. Pasang Android Studio dan JDK 17.
2. Dari Android Studio SDK Manager, pasang Android SDK Platform 35 dan Build-Tools 35.0.0.
3. Jalankan `Build_MateDesk_APK.command`, atau dari Terminal:

   ```bash
   cd android-project
   chmod +x gradlew
   ./gradlew --no-daemon :app:assembleDebug
   ```

APK debug akan berada di `android-project/app/build/outputs/apk/debug/app-debug.apk`.

## Fitur dan batasan

- Desktop responsif, Start menu, pencarian, taskbar, jendela internal, Notes, Calculator, File Manager, tema, wallpaper, dan posisi ikon tersimpan.
- Di dalam APK, jembatan native mencoba menampilkan aplikasi yang dapat diluncurkan melalui Package Manager, memakai ikon aplikasi asli, membuka aplikasi via Intent, memilih berkas, serta menyimpan catatan sebagai berkas.
- Fitur jendela bebas eksternal bersifat eksperimental. Android/HarmonyOS bisa mengabaikan `ActivityOptions.setLaunchBounds()` dan membuka aplikasi secara biasa. MateDesk tidak mengubah APK aplikasi lain.
- Kompatibilitas APK di HarmonyOS bergantung pada versi sistem/perangkat dan harus diuji pada MatePad yang sebenarnya.

## Status APK

APK **belum disertakan di dalam ZIP ini**, karena lingkungan pembuat paket tidak memiliki Android SDK/Gradle dan tidak dapat mengakses internet untuk mengunduhnya. Workflow GitHub Actions disertakan agar build dapat dilakukan di runner yang mempunyai SDK dan koneksi internet; build baru bisa dinyatakan sukses setelah workflow selesai dan artefak APK tersedia.
