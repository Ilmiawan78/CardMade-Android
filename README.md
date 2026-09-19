# CardMate Android

MVP pembaca dan pembuat kartu nama untuk Android. OCR memakai ML Kit di perangkat, hasil dapat dikoreksi, disimpan ke Kontak, serta diekspor menjadi PNG dengan QR vCard.

## Build

Jalankan `gradle assembleRelease` dengan JDK 17 dan Android SDK 35. APK keluaran berada di `app/build/outputs/apk/release/app-release.apk`.

Workflow GitHub Actions sudah tersedia untuk menghasilkan APK secara otomatis.
