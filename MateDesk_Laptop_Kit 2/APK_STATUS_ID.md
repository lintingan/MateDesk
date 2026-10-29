# Status build APK MateDesk

Tidak ada file `.apk` di paket sumber ini. Ini disengaja agar tidak memberikan file palsu yang tidak dapat dipasang.

Paket build tersedia dalam dua bentuk:
- lokal: `Build_MateDesk_APK.command` pada Mac yang memiliki Android Studio/SDK;
- cloud: `.github/workflows/android-apk.yml` pada GitHub Actions.

Build belum dapat diverifikasi hingga salah satu jalur tersebut benar-benar berjalan sukses. Setelah GitHub Actions sukses, unduh artifact `MateDesk-debug-APK`, ekstrak, dan instal `app-debug.apk` pada perangkat Android yang kompatibel.
