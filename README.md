# moncake94 POS

Native Android POS/cashier app for moncake94, built with Kotlin, Jetpack Compose, Room SQLite, and direct ESC/POS thermal printer support.

## Stack

- Native Android Kotlin
- Jetpack Compose Material 3
- Room SQLite untuk offline-first database
- ESC/POS text command untuk printer thermal 58mm
- Direct Bluetooth SPP printing dari dalam aplikasi
- USB OTG dan WiFi/LAN disiapkan dalam arsitektur printer

## Cara menjalankan

1. Buka folder ini di Android Studio.
2. Pastikan Android SDK tersedia dan gunakan JDK 17.
3. Sync Gradle.
4. Jalankan module `app` ke emulator atau HP Android.

Catatan: repository ini belum menyertakan Gradle wrapper jar, jadi Android Studio/Gradle lokal dibutuhkan untuk build pertama.

## Alur kasir

1. Buka tab `Kasir`.
2. Cari produk atau pilih kategori.
3. Tap produk.
4. Jika produk punya variasi, pilih opsi variasi dulu.
5. Buka `Lihat Cart / Checkout`.
6. Atur qty dengan tombol `+` dan `-`.
7. Masukkan nominal bayar.
8. Tap `Simpan + Print` untuk menyimpan transaksi dan mencetak struk.

## Menambah produk dan variasi

1. Buka tab `Produk`.
2. Tap `Tambah Produk`.
3. Isi nama produk, kategori, dan harga dasar.
4. Untuk produk dengan variasi, aktifkan `Produk punya variasi`.
5. Isi nama variasi, misalnya `Size`, `Variant`, atau `Flavor`.
6. Tambahkan opsi variasi, misalnya `Slice`, `Whole`, `Chocolate`, beserta harga masing-masing.
7. Tap `Simpan`.

Kategori dan produk contoh dapat diedit atau dihapus dari aplikasi.

## Printer langsung dari aplikasi

Kode printer dipisahkan dari UI:

- `app/src/main/java/com/moncake94/pos/printer/PrinterService.kt`
- `app/src/main/java/com/moncake94/pos/printer/EscPosReceiptBuilder.kt`
- `app/src/main/java/com/moncake94/pos/printer/BluetoothPrinterManager.kt`
- `app/src/main/java/com/moncake94/pos/printer/PrinterSettingsRepository.kt`
- `app/src/main/java/com/moncake94/pos/printer/UsbPrinterManager.kt`
- `app/src/main/java/com/moncake94/pos/printer/WifiPrinterManager.kt`
- `app/src/main/java/com/moncake94/pos/ui/printer/PrinterSetupScreen.kt`

Checkout dan riwayat transaksi hanya memanggil `PrinterService`, bukan kode Bluetooth/ESC/POS langsung.

## Cara pair dan pilih printer

1. Pair printer thermal Bluetooth dari pengaturan Bluetooth Android.
2. Buka tab `Printer`.
3. Berikan izin Bluetooth saat diminta.
4. Tap `Cari Printer Bluetooth/USB`.
5. Pilih printer dari daftar untuk menyimpan default printer.
6. Tap `Test Print 58mm`.

Setelah default printer disimpan, kasir cukup tap `Simpan + Print` dari checkout atau `Reprint` dari detail riwayat transaksi.

## Format struk 58mm

Receipt builder memakai lebar aman 32 karakter untuk printer 58mm. Header berisi:

```text
moncake94
Jl. Danau Limboto Blok C-1 no.5, RT.13/RW.5, Pejompongan, Bend. Hilir, Kecamatan Tanah Abang, Kota Jakarta Pusat, Daerah Khusus Ibukota Jakarta 10210
IG: @moncake94
WA: 087877530387
```

Alamat panjang otomatis dibungkus agar tidak kepotong. Test print menyertakan:

```text
Marmer Cake - Whole
Bolu Tape
```

## Database lokal

Database Room berada di:

- `app/src/main/java/com/moncake94/pos/data/AppDatabase.kt`
- `app/src/main/java/com/moncake94/pos/data/Entities.kt`
- `app/src/main/java/com/moncake94/pos/data/PosDao.kt`
- `app/src/main/java/com/moncake94/pos/data/PosRepository.kt`

Data disimpan lokal di SQLite Android dengan nama database `moncake94_pos.db`, sehingga app tetap berjalan tanpa internet dan data tetap ada setelah app ditutup.

## Batasan versi awal

- Bluetooth adalah prioritas utama dan sudah memakai koneksi langsung RFCOMM/SPP.
- Android 12+ meminta `BLUETOOTH_CONNECT` dan `BLUETOOTH_SCAN`.
- USB OTG disiapkan dan dapat mencetak jika perangkat memberi izin USB dan printer memakai USB printer class.
- WiFi/LAN disiapkan melalui socket port `9100`.
- Beberapa printer murah punya variasi ESC/POS berbeda; jika ada karakter tidak sesuai, charset/command bisa disesuaikan di `EscPosReceiptBuilder`.
