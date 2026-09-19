# CardMate Android

Pembaca dan pembuat kartu nama untuk Android. OCR memakai ML Kit di perangkat, hasil dapat dikoreksi, disimpan ke Kontak, serta diekspor menjadi PNG dengan QR vCard.

Versi 1.1 memakai foto kamera resolusi penuh (termasuk informasi rotasi), parser berbasis pola dan posisi teks, pemisahan beberapa nomor telepon, serta menampilkan teks OCR mentah untuk pemeriksaan.

## Build

Jalankan `gradle assembleRelease` dengan JDK 17 dan Android SDK 35. APK keluaran berada di `app/build/outputs/apk/release/app-release.apk`.

Workflow GitHub Actions sudah tersedia untuk menghasilkan APK secara otomatis.
