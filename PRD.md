# PRD — PUSATRACK

**Pusa Coffee Inventory & Financial Tracking System**
Sistem Informasi Manajemen Operasional Berbasis Mobile untuk Pengelolaan Inventori dan Keuangan Pusa Coffee Cabang BTP

|                                                        |                                                                                     |
| ------------------------------------------------------ | ----------------------------------------------------------------------------------- |
| Versi / status                                         | 0.1 — Draft untuk ditinjau tim                                                      |
| Tanggal                                                | 22 November 2026                                                                    |
| Mitra                                                  | Pusa Coffee Cabang BTP                                                              |
| Tim pengusul (3D-TKJ, Politeknik Negeri Ujung Pandang) | Raidah Syahirah Ahmad, Afdal Rizaldy, Natashya Rosario Kabo, Alim Anugrah           |
| Manajer proyek                                         | Iin Karmila Yusri, S.ST., M.Eng., Ph.D.                                             |
| Dosen terkait                                          | Andi Asvin Mahersatillah S, Kom., M.T.; Ahmad Maruf Firman, S.Tr.T, M.Kom.          |
| Durasi / biaya                                         | 5 minggu / Rp 350.000,00 (sponsor: –)                                               |
| Luaran                                                 | Aplikasi mobile Android, beserta backend API dan database pendukung                 |
| Dokumen acuan                                          | RPP (disahkan 19/11/2026), ERD PUSATRACK (Gambar 2), Mockup PUSATRACK v6 (32 layar) |

**Legenda penanda**

- Teks tanpa tanda bersumber dari RPP, ERD, atau mockup v6.
- **[USULAN]** adalah rekomendasi penyusun PRD agar implementasi dapat berjalan. Ini bukan fakta dari dokumen sumber dan boleh diubah tim.
- **[KONFIRMASI K-xx]** adalah keputusan yang belum final. Semua item terdaftar di **Lampiran B**.
- Rujukan layar memakai nomor mockup, contoh `(L03)` untuk layar 03. Daftar lengkap ada di **Lampiran A**.

---

## 1. Ringkasan Produk & Tujuan

### 1.1 Latar belakang dan masalah

Pusa Coffee Cabang BTP menjual minuman kopi dengan empat karyawan yang menjalankannya, 2 sebagai eksekutor lapangan yang menjaga kedai dan 2 sebagai analis, nmaun tidak menutp kemungkinan saling membackup dalam operasional pengelolaan kedai. Aktivitas harian meliputi persiapan alat dan bahan sebelum buka, penjualan dengan pembayaran tunai atau QRIS, pencatatan pengeluaran operasional, dan rekapitulasi transaksi di akhir hari. Pencatatan saat ini masih manual dan memakai Microsoft Excel.

Masalah utama hasil observasi dan wawancara:

1. Pasokan bahan baku terkadang lama diterima. Stok dapat habis sebelum pasokan berikutnya tiba dan operasional kedai berhenti beberapa hari.
2. Pencatatan pengeluaran operasional dan rekapitulasi penjualan dilakukan terpisah, sehingga data kurang terstruktur.

### 1.2 Solusi

PUSATRACK adalah aplikasi mobile Android yang mengintegrasikan pencatatan penjualan, inventori, monitoring lead time pengiriman bahan, pengeluaran, rekap keuangan, riwayat aktivitas, dan laporan yang dapat diekspor ke Excel. Setiap karyawan memakai akun pribadi.

### 1.3 Tujuan

| #   | Tujuan                                                                                                    | Terkait masalah |
| --- | --------------------------------------------------------------------------------------------------------- | --------------- |
| T1  | Karyawan mengetahui kondisi stok bahan dan mendapat peringatan sebelum stok habis                         | 1               |
| T2  | Lead time pengiriman bahan tercatat dan dapat dievaluasi dari riwayat                                     | 1               |
| T3  | Penjualan, pengeluaran, dan HPP berada dalam satu sistem sehingga laba harian dan periodik dapat dilihat  | 2               |
| T4  | Setiap aktivitas dapat ditelusuri ke karyawan tertentu dan tidak dapat diubah atau dihapus lewat aplikasi | 2               |
| T5  | Data dapat dikeluarkan sebagai file `.xlsx` dan dibagikan ke aplikasi lain, misalnya WhatsApp             | 2               |

### 1.4 Indikator keberhasilan (dari RPP)

| No  | Indikator                      | Kriteria ketercapaian                                                                                                        |
| --- | ------------------------------ | ---------------------------------------------------------------------------------------------------------------------------- |
| 1   | Aplikasi                       | Tersedia dan dapat digunakan langsung oleh mitra                                                                             |
| 2   | Autentikasi pengguna           | Ada akun personal berisi username dan password untuk mengakses layanan                                                       |
| 3   | Dashboard operasional          | Dapat diakses pengguna                                                                                                       |
| 4   | Pencatatan penjualan           | Dapat diakses pengguna                                                                                                       |
| 5   | Manajemen inventori            | Tambah, ubah, lihat bahan beserta riwayat perubahan; stok bertambah sesuai jumlah diterima; dapat dimonitor dan diaudit CRUD |
| 6   | Pencatatan lead time           | Catatan lead time dan rata-rata waktu tunggu dapat dimonitor                                                                 |
| 7   | Pengeluaran dan rekap keuangan | Pengeluaran operasional dapat dicatat; rekap ditampilkan dari data yang dimasukkan                                           |
| 8   | Activity History               | Mencatat identitas akun, jenis aktivitas, tanggal, waktu; hanya-baca                                                         |
| 9   | Ekspor laporan                 | Laporan `.xlsx`; opsi simpan atau bagikan lewat aplikasi lain                                                                |
| 10  | Penyimpanan data               | Data operasional tersimpan di database dan dapat ditampilkan kembali                                                         |

### 1.5 Pemangku kepentingan

- **Pengguna akhir:** 4 karyawan Pusa Coffee Cabang BTP.
- **Mitra / penerima serah terima:** Pusa Coffee Cabang BTP.
- **Pelaksana:** 4 mahasiswa tim pengusul.
- **Pembimbing dan penilai:** manajer proyek dan dua dosen terkait. Mata kuliah terkait: Aplikasi Mobile dan Keamanan Informasi.

---

## 2. Ruang Lingkup, Batasan & Arsitektur Monorepo

### 2.1 Dalam ruang lingkup

Aplikasi Android PUSATRACK dengan backend CI4 + MySQL yang mencakup:

1. Autentikasi pengguna (username dan password per karyawan)
2. Dashboard operasional dan saldo kas awal
3. Kasir / pencatatan penjualan (Tunai dan QRIS)
4. Riwayat transaksi dan Activity History (hanya-baca)
5. Notifikasi operasional (stok menipis dan pesanan)
6. Manajemen inventori, tambah stok, dan pemeriksaan fisik (stok opname)
7. Pesanan stok dan monitoring lead time
8. Pencatatan pengeluaran
9. Rekap keuangan
10. Laporan dan ekspor `.xlsx`
11. Produk dan resep (perhitungan HPP)

### 2.2 Di luar ruang lingkup (tetap, sesuai RPP)

- Integrasi langsung dengan sistem pembayaran QRIS.
- Sensor atau perangkat pengukur stok otomatis.
- Pelacakan lokasi kendaraan pengiriman secara real-time.
- Pengelolaan penggajian.
- Pengelolaan pemasaran.

### 2.3 Batasan perilaku sistem (dari RPP)

- Data penjualan, pengeluaran, dan persediaan berasal dari input pengguna.
- Lead time dihitung dari waktu pemesanan dan konfirmasi penerimaan oleh pengguna.
- Perhitungan laba bergantung pada data penjualan, HPP, dan pengeluaran yang dimasukkan.
- Setiap karyawan hanya mengakses aplikasi dengan akunnya sendiri.
- Riwayat aktivitas hanya dapat dilihat, tidak dapat diubah atau dihapus lewat aplikasi.
- Platform: Android.

### 2.4 Struktur monorepo

```
/                      (root repository)
├── PRD.md             dokumen ini
├── database/          skema MySQL, seed, dokumentasi ERD
├── backend-api/       CodeIgniter 4 (REST API)
└── android-app/       Kotlin, Views/XML, Fragment
```

Isi tiap folder:

- `database/`: (sumber ERD).
- `backend-api/`: struktur standar CI4.
- `android-app/`: struktur standar proyek Android Studio.

### 2.5 Pembagian kerja dan aturan kolaborasi

- `database/` dan `backend-api/` dikerjakan terpisah dari `android-app/` agar perubahan tidak saling menimpa.

Aturan kerja:

- Satu PR hanya menyentuh satu folder utama, kecuali perubahan kontrak.
- Penamaan branch `db/…`, `api/…`, `app/…`.
- Folder yang sedang dikerjakan orang lain tidak diedit tanpa kesepakatan.
- File rahasia (`.env`, kunci layanan Firebase, keystore) tidak pernah di-commit; sediakan `.env.example`.
- Android bekerja terhadap contoh sampai API siap.

### 2.6 Arsitektur teknis

```
┌───────────────────────────────┐        REST/JSON        ┌──────────────────────────┐      ┌───────┐
│ android-app                   │ ──────────────────────► │ backend-api (CI4)        │ ───► │ MySQL │
│ Kotlin, Views/XML, Fragment   │ ◄────────────────────── │ auth, validasi, aturan   │ ◄─── │       │
│ tema putih bersih + hijau tua │                         │ bisnis, laporan          │      └───────┘
└──────┬────────────────────────┘                         └────────────┬─────────────┘
       │ Android Intent                                                │
       ├─ kamera / galeri (foto bahan)                                 ▼
       ├─ WhatsApp (file laporan .xlsx)                          Firebase Cloud Messaging (notifikasi)
```

| Komponen                                     | Peran                                                                                                        | Catatan konsistensi                                                                                               |
| -------------------------------------------- | ------------------------------------------------------------------------------------------------------------ | ----------------------------------------------------------------------------------------------------------------- |
| **MySQL** (Laragon untuk pengembangan lokal) | Satu-satunya penyimpan data operasional (pengguna, bahan, stok, transaksi, riwayat)                          | Jangan menduplikasi data operasional ke layanan lain                                                              |
| **CI4 (`backend-api/`)**                     | Autentikasi, validasi, seluruh aturan bisnis (pengurangan stok, HPP, lead time, rekap), pencatatan aktivitas | Android tidak menghitung hasil final; hasil final selalu dari server. Android boleh menghitung pratinjau tampilan |
| **Android (`android-app/`)**                 | Antarmuka, input, tampilan, pemanggilan API, integrasi Intent                                                | Android tidak terhubung langsung ke MySQL                                                                         |
| **Penyimpanan internal perangkat**           | Sesi login, cache sementara file ekspor, serta foto bahan                                                    |
|                                              |
| **Firebase**                                 | sebagai layanan pendukung (notifikasi).                                                                      |
|                                              |
| **Android Intent**                           | Kamera/galeri, kirim pesan ke WhatsApp, bagikan file                                                         | Wajib ada fallback bila WhatsApp tidak terpasang                                                                  |

### 2.7 Titik integrasi yang harus dipastikan konsisten

| ID   | Topik                                   | kesepakatan                                                                          |
| ---- | --------------------------------------- | ------------------------------------------------------------------------------------ |
| K-01 | Lokasi hosting backend (hosting online) | Backend CI4 + MySQL dipakai di lingkungan pengembangan/pengujian tim terlebih dahulu |

| K-01 | Lokasi hosting backend (hosting online / lokal) | menggunakan lokal server tim dengan base URL yang disepakati bersama.
daring jika diperlukan. |
| K-02 | Mekanisme sesi login |Login menggunakan username dan password akun personal karyawan (is_active) yang diverifikasi
di backend CI4 tanpa token rumit; aplikasi menyimpan identitas sesi pengguna aktif (user_id, username, full_name) untuk keperluan pencatatan aktivitas.
| K-03 | Firebase sebagai penyedia notifikasi, | mengirimkan notifikasi stock menipis|
| K-04 | Sumber kebenaran skema: `database/` (SQL) atau migrasi CI4 | penyimpanan data utama |
| K-05 | Penyimpanan foto bahan: unggah ke server atau lokal perangkat | Foto dokumentasi stok bahan (image_url pada tabel ingredients) dikelola lewat penyimpanan yang
konsisten dengan alur CI4/aplikasi agar tautan/path-nya seragam dan tidak membingungkan saat data dibaca antarperangkat|
| K-06 | Tempat pembuatan file `.xlsx`: server (CI4) atau perangkat | Tempat pembuatan file laporan .xlsx ditetapkan pada satu sisi saja yaitu pada backend CI4|
| K-07 | Perlu mode offline atau tidak | Menentukan arsitektur cache dan sinkronisasi. |

---

## 3. User Roles & Alur Autentikasi

### 3.1 Role

RPP hanya mendefinisikan satu jenis pengguna: **Karyawan** (4 akun pribadi). Tidak ada role pemilik/admin di RPP maupun mockup.

### 3.2 Matriks akses (asumsi sementara)

| Fitur                                                                | Karyawan                                          |
| -------------------------------------------------------------------- | ------------------------------------------------- |
| Login/logout, Dashboard, Kasir                                       | Ya                                                |
| Inventori (lihat, tambah/edit bahan, tambah stok, pemeriksaan fisik) | Ya                                                |
| Pesanan stok dan konfirmasi penerimaan                               | Ya                                                |
| Catat pengeluaran, rekap keuangan, laporan/ekspor                    | Ya                                                |
| Produk dan resep (lihat dan edit harga/resep)                        | Ya                                                |
| Activity History                                                     | Lihat saja (tidak ada ubah/hapus untuk siapa pun) |
| Kelola akun karyawan                                                 | Ya                                                |

sudah pasti: edit harga/resep dan penyesuaian stok boleh oleh semua karyawan.

### 3.3 Alur login

1. Pengguna membuka aplikasi. Bila belum ada sesi valid, tampil layar Login (L01).
2. Pengguna mengisi username dan password, lalu menekan **button login**.
3. Server memeriksa username, mencocokkan password dengan `password_hash`, dan memastikan `is_active = true`.
4. Bila valid, server membuat sesi/token dan mengembalikan profil singkat (`user_id`, `username`, `full_name`).
5. Aplikasi menyimpan sesi di penyimpanan internal dan membuka Beranda (L02) dengan sapaan memakai `full_name`.
6. Setiap request berikutnya membawa kredensial sesi. Server mencatat `user_id` pada setiap aktivitas yang tersimpan.
7. **Keluar** (menu Beranda) menghapus sesi di perangkat dan, bila relevan, mencabutnya di server.

### 3.4 Aturan autentikasi

- Akun personal per karyawan; **tidak ada PIN bersama**.
- Pesan gagal login bersifat generik dan tidak menyebut apakah username atau password yang salah.
- Password disimpan sebagai hash (`password_hash`), tidak pernah disimpan atau dikirim ulang dalam bentuk asli.
- Akun dengan `is_active = false` tidak dapat login.
- Layar login menampilkan "Lupa password? Minta tim untuk mereset akunmu". Reset dan pembuatan akun dilakukan manual oleh tim lewat aplikasi.
- Auidit data dicatat di Activity History atau
- Batasi percobaan login berulang (rate limiting) dan simpan sesi di penyimpanan terenkripsi (EncryptedSharedPreferences).

---

## 4. Spesifikasi Fitur & User Stories per Modul

Format tiap modul: **User story → Input → Proses → Output / Acceptance Criteria (AC)**.

### M1 — Autentikasi (L01)

- **US-1.1** Sebagai karyawan, saya ingin masuk dengan akun pribadi agar semua aktivitas saya tercatat atas nama saya.
- **Input:** `username`, `password`.
- **Proses:** validasi tidak kosong, verifikasi hash, cek `is_active`, buat sesi (Bagian 3.3).
- **Output / AC:**
  - Berhasil masuk, pengguna diarahkan ke Beranda dan disapa dengan nama.
  - Kredensial salah, muncul pesan generik dan sesi tidak dibuat.
  - Ada tombol tampil/sembunyi password.
  - Tombol **Keluar** tersedia di menu Beranda dan mengakhiri sesi.

### M2 — Dashboard Operasional & Saldo Kas Awal (L02)

- **US-2.1** Sebagai karyawan, saya ingin melihat ringkasan operasional hari ini agar tahu kondisi kedai sekilas.
- **Proses:** server mengagregasi data hari berjalan dari penjualan, pengeluaran, bahan, dan pesanan.
- **Output / AC:**
  - Kartu **penjualan hari ini**: total Rp, jumlah transaksi, rincian Tunai dan QRIS, plus tombol **Buka Kasir**.
  - Kartu **pengeluaran hari ini** (total dan jumlah), **transaksi terakhir** (kode, jam, nominal), **perlu distok** (jumlah bahan), **pesanan berjalan** (kode dan lama berjalan).
  - Banner peringatan stok menipis dengan tombol **Lihat**.
  - **Menu lainnya:** Keuangan, Laporan, Pengeluaran, Produk, Cek Fisik, Keluar. Ikon lonceng membuka Notifikasi dengan badge jumlah belum dibaca.
  - Navigasi bawah 5 tab: Beranda, Inventori, **Kasir** (tengah, menonjol), Pesanan, Riwayat.

### M3 — Kasir / Pencatatan Penjualan (L03–L06)

- **US-3.1** Sebagai karyawan, saya ingin memilih menu dan jumlah lalu menyelesaikan pembayaran agar penjualan tercatat.
- **US-3.2** Sebagai karyawan, saya ingin memeriksa kembali pesanan sebelum disimpan agar tidak salah catat.
- **Input:** daftar produk dan `quantity` (tap kartu menambah, stepper +/−), `payment_method` (Tunai/QRIS), `amount_received` (untuk Tunai).
- **Proses:**
  1. Aplikasi menampilkan produk aktif beserta harga tetap (tanpa diskon). Harga dan total tidak dikirim dari klien; server mengambilnya dari data produk.
  2. Layar Pembayaran: metode Tunai atau QRIS. Untuk Tunai ada kolom uang diterima (diisi manual) dan tombol cepat (Rp.10.000), kembalian dihitung otomatis (jika ada). QRIS hanya dicatat manual sesuai nominla inputan.
  3. **Periksa & Konfirmasi** membuka dialog ringkasan: pesanan, total, metode, diterima, kembalian, dan peringatan bahwa setelah dikonfirmasi transaksi tidak dapat diubah atau dibatalkan. Tombol: **Ya, Konfirmasi Transaksi** dan **Kembali dan periksa lagi**.
  4. Saat dikonfirmasi, server dalam satu transaksi database: menyimpan `sales` dan `sale_details` (dengan snapshot harga jual dan HPP), mengurangi stok bahan sesuai resep × jumlah, menulis `stock_movements` (jenis penjualan, `stock_before`, `stock_after`), menulis `activity_history`, dan mengisi `confirmed_at`.
- **Output / AC:**
  - Keranjang kosong, menampilkan daftar produk yang dijual **sesuai daftar tabel produk**.
  - Layar **Transaksi berhasil**: kode `TRX-xxxx`, total, metode, daftar "Stok bahan berkurang otomatis" (jumlah dan sisa), catatan transaksi terkunci, tombol **Transaksi Baru** dan **Lihat Detail Transaksi**.
  - Proses bersifat atomik; bila gagal, tidak ada stok yang berubah sebagian.
  - Setelah konfirmasi, bila stok bahan ≤ batas minimum, peringatan muncul (M5, M6).
  - Tabel `sales` mencatat sesuai dengan konsep dalam erd.

### M4 — Riwayat Transaksi & Activity History (L07–L08)

- **US-4.1** Sebagai karyawan, saya ingin melihat riwayat aktivitas dan detail transaksi agar semua bisa ditelusuri.
- **US-4.2** Sebagai karyawan, saya ingin riwayat tidak bisa diubah atau dihapus agar data dapat dipercaya.
- **Input:** filter jenis (Semua, Penjualan, Stok, Pesanan, Biaya), tanggal; RPP juga meminta filter pengguna, rentang tanggal, sorting, dan pagination.
- **Proses:** server mengambil `activity_history` dengan filter, urutan, dan halaman.
- **Output / AC:**
  - Satu daftar aktivitas hanya-baca. Tiap item: nama pengguna dan jenis aktivitas (Aktivitas yang dicatat meliputi penambahan atau perubahan stok, pencatatan penjualan, pencatatan pengeluaran, serta konfirmasi penerimaan bahan baku. Setiap riwayat dilengkapi dengan informasi pengguna, jenis aktivitas, tanggal, dan waktu pelaksanaan.), informasi detail tambahan (nominal/jumlah, jam, kode/metode) disesuaikan dengan tipe aktivitas tercatat.
  - Banner "Hanya baca: catatan tidak bisa diubah atau dihapus".
  - fitur penghapus otomatis akan disediakan oleh codingan sistem.
  - Mengetuk item penjualan membuka **Detail Transaksi** (L08): pesanan, total, metode, diterima, kembalian, bahan terpakai sesuai resep, catatan transaksi terkunci.
  - Tidak ada tombol atau endpoint ubah/hapus untuk riwayat dan transaksi terkonfirmasi.
  - Setiap catatan memuat `user_id`, jenis, tanggal, waktu. Jenis yang dicatat: tambah/ubah stok, penjualan, pengeluaran, konfirmasi penerimaan (RPP), ditambah pesanan dibuat dan penyesuaian stok (tampil di mockup).
  - Urutan bawaan: terbaru di atas.
  - dapat melakukan shoting berdasarkan tanggal
  - Pencatatan sesuai dengan aktivitas yang terekam dalam aplikasi, baik itu dari inputan manual pengguna, maupun otomasisasi dalam sistem.

### M5 — Notifikasi (L09)

- **US-5.1** Sebagai karyawan, saya ingin diberi tahu saat stok menipis atau pesanan belum diterima agar bisa bertindak lebih awal.
- **Input:** data stok (tanpa input pengguna); aksi **Tandai dibaca**.
- **Proses:** sistem mengevaluasi `current_stock ≤ minimum_stock` dan status pesanan, lalu menampilkan di aplikasi dan mengirim push ke perangkat karyawan.
- **Output / AC:**
  - Daftar notifikasi: stok menipis (sisa dan batas minimum).
  - Notifikasi push tetap muncul meski aplikasi ditutup; mengetuknya membuka aplikasi.
  - Tombol **Tandai dibaca** dan badge lonceng di Beranda.
  - Mekanisme push menggunakan sistem firebase.

### M6 — Manajemen Inventori, Tambah Stok & Pemeriksaan Fisik (L10–L16)

**6a. Daftar dan detail bahan (L10–L11)**

- **US-6.1** Sebagai karyawan, saya ingin melihat stok semua bahan dan statusnya agar tahu mana yang perlu dipesan.
- **Output / AC:**
  - Pencarian dan chip filter Semua / Aman / Menipis / Habis dengan jumlah.
  - Kartu bahan: gambar, nama, stok (satuan pakai data fisik, misal angka sesuai stock real`), badge status, batas minimum.
  - Status: **Aman**, **Menipis** (stok ≤ minimum), **Habis** (stok = 0).
  - Detail bahan: stok saat ini, harga per kemasan dan per satuan, batas minimum, riwayat pergerakan stok (jenis, referensi, tanggal, jam, pengguna, ± jumlah, stok setelahnya), tombol **Edit** dan **Tambah stok**.
  - Foto bahan lewat kamera/galeri.

**6b. Tambah dan edit bahan (L13–L14)**

- **Input:** nama bahan, satuan pembelian (mis. kotak), satuan penggunaan (mis. ml), isi per kemasan, harga per kemasan, batas minimum (dalam kemasan), cara pengadaan (**Dipasok ke pusat** atau **Beli langsung**), jumlah kemasan stok awal (tambah bahan).
- **Proses:** simpan bahan. Stok awal dicatat sebagai pergerakan stok `OPENING_STOCK` dan muncul di Riwayat. Edit tidak mengubah stok ("Gunakan Tambah stok atau Pemeriksaan Fisik").
- **AC:** pratinjau stok awal (`5 × 920 ml = 4.600 ml`); perubahan harga hanya memengaruhi transaksi berikutnya (transaksi lama memakai snapshot).
- **Konfirmasi:** K-12 (kolom dan perilaku cara pengadaan; tombol Tambah stok tampil untuk Beli langsung, bahan Dipasok pusat lewat M7, disimpulkan dari mockup), K-19.
- **[USULAN]** Beri peringatan bila satuan atau isi per kemasan diubah setelah ada pergerakan stok.

**6c. Tambah stok / beli langsung (L12)**

- **US-6.2** Sebagai karyawan, saya ingin mencatat bahan yang dibeli langsung di toko agar stok dan harga bahan terbarui.
- **Input:** tempat beli (opsional), tanggal dan jam, jumlah kemasan, harga per kemasan.
- **Proses:** tambah `current_stock`, tulis `stock_movements` (`DIRECT_PURCHASE`), perbarui harga bahan untuk HPP, tulis `activity_history`.
- **AC:** pratinjau `+3 × 920 ml = +2.760 ml` dan stok jadi; catatan "Tidak dihitung sebagai pengeluaran, harga dipakai memperbarui harga bahan (HPP)"; masuk riwayat pergerakan.

**6d. Pemeriksaan fisik / stok opname (L15–L16)**

- **US-6.3** Sebagai karyawan, saya ingin mencocokkan stok sistem dengan stok fisik dan menyesuaikannya bila berbeda.
- **Input:** per bahan, jumlah kemasan utuh yang dihitung (stepper) dan toggle **Ada kemasan terbuka?**; alasan (bawaan "Selisih pemeriksaan fisik"); deskripsi opsional.
- **Proses:** bila toggle aktif, sisa kemasan terbuka di sistem dipertahankan; bila nonaktif, sisa terbuka dianggap tidak ada. Stok fisik = kemasan utuh × isi per kemasan + (sisa sistem bila toggle aktif). Bahan dengan selisih ditandai **Selisih**, lainnya **Sesuai**. Saat **Sesuaikan Stok** dikonfirmasi: simpan `stock_counts` (header), tulis `stock_movements` (`ADJUSTMENT`, `reason`) hanya untuk bahan yang selisih, tulis `activity_history`.
- **AC:**
  - Dialog menampilkan jumlah bahan berubah, nilai dari→menjadi, selisih, alasan, dan peringatan bahwa perubahan tercatat di riwayat dan tidak dapat dibatalkan.
  - Tombol **Sesuaikan Stok** berwarna merah (aksi koreksi); tombol **Kembali** tersedia.
  - Contoh mockup: Susu UHT sistem `2 kotak + 300 ml`, hitung 2 kotak tanpa kemasan terbuka, selisih `−300 ml`.

### M7 — Pesanan Stok & Monitoring Lead Time (L17–L22)

- **US-7.1** Sebagai karyawan, saya ingin membuat pesanan bahan ke pusat dan mengirimkannya lewat WhatsApp.
- **US-7.2** Sebagai karyawan, saya ingin menekan konfirmasi saat barang tiba agar lead time tercatat dan stok bertambah otomatis.
- **US-7.3** Sebagai karyawan, saya ingin melihat rata-rata lead time agar bisa menentukan kapan memesan.

**7a. Lacak pesanan (L17)**

- Statistik: rata-rata lead time, tercepat, terlama, dari jumlah pesanan yang sudah diterima.
- Kartu **Sedang dalam perjalanan** (kode `PSN-xxx`, pemasok, tahap Dipesan → Dalam perjalanan → Diterima, lama berjalan dibanding rata-rata, ringkasan bahan) dengan tombol **Konfirmasi penerimaan**.
- **Riwayat terbaru** (kode, tanggal terima, lead time) dan tombol **Buat Pesanan**.
- Penentuan tahap "Dalam perjalanan" otomatis atau aksi pengguna. **[KONFIRMASI K-21]**

**7b. Buat pesanan dan kirim WhatsApp (L19–L22)**

- **Input:** pemasok (bawaan "Pusat Pusa Coffee"), tanggal dan jam pesan (bawaan sekarang), bahan yang dipasok pusat beserta jumlah kemasan (stepper), tombol **Tambah bahan**.
- **Proses:** total pesanan dihitung dari harga per kemasan; **Periksa & Simpan** membuka dialog ringkasan (rincian, harga, total, catatan "stok baru bertambah setelah penerimaan dikonfirmasi"); setelah **Ya, Simpan Pesanan**: simpan `purchase_orders` dan `purchase_order_details` (snapshot ukuran dan harga kemasan), tulis `activity_history`. Layar **Pesanan dibuat** menawarkan **Kirim ke WhatsApp**, lalu layar **Pesan WhatsApp** menampilkan teks otomatis yang masih bisa diubah di WhatsApp, dikirim lewat Android Intent.
- **Template pesan (dari mockup):** "Halo, saya {nama} dari Pusa Coffee BTP. Pesanan stok {PSN-xxx} ({tanggal}): 1. {bahan}: {jumlah} {kemasan} … Mohon konfirmasi ketersediaan dan estimasi pengiriman. Terima kasih." **Harga tidak dicantumkan.**
- **AC:** minimal 1 bahan dengan jumlah ≥ 1; hanya bahan Dipasok pusat yang dapat dipesan; tombol **Nanti saja** tersedia; bila WhatsApp tidak terpasang, tampil pilihan aplikasi lain.
- **Konfirmasi:** K-23 (nomor tujuan WhatsApp tetap atau dipilih manual di WhatsApp).

**7c. Konfirmasi penerimaan / "Sampai" (L18)**

- **Input:** tanggal dan jam terima (bawaan sekarang, dapat diedit), centang "Jumlah dan ukuran kemasan sudah sesuai pesanan".
- **Proses:** isi `received_at`; hitung lead time = `received_at − order_date`; untuk tiap bahan tulis `stock_movements` (`PURCHASE`, jumlah × isi per kemasan) dan tambah `current_stock`; perbarui harga bahan (HPP) dari harga pesanan; ubah status pesanan; tulis `activity_history`.
- **AC:**
  - Durasi pengiriman tampil otomatis, contoh `2 hari 26 menit`.
  - Tanggal terima tidak boleh sebelum tanggal pesan.
  - Pesanan yang sudah diterima tidak dapat dikonfirmasi ulang.
  - Pesanan masuk ke Riwayat terbaru dan ikut menghitung rata-rata, tercepat, terlama.
  - RPP: stok diperbarui sesuai jumlah yang benar-benar diterima, tetapi mockup hanya punya centang "sesuai pesanan" tanpa input jumlah aktual. **[KONFIRMASI K-14]**
  - Cara memperbarui harga bahan (harga terakhir atau rata-rata). **[KONFIRMASI K-19]**

### M8 — Pencatatan Pengeluaran (L24–L25)

- **US-8.1** Sebagai karyawan, saya ingin mencatat pengeluaran operasional (mis. es batu, konsumsi) agar tidak terpisah dari penjualan.
- **Input:** metode (Tunai/QRIS), tanggal dan jam (bawaan sekarang), satu atau lebih baris berisi kategori (dropdown), nominal, keterangan (opsional); tombol **Tambah baris**.
- **Proses:** dialog **Periksa kembali pengeluaran** (metode, waktu, daftar baris, total, peringatan "pastikan nominal dan metode benar; catatan tidak bisa diubah"); setelah **Ya, Simpan Pengeluaran** simpan `expenses` dan tulis `activity_history`.
- **AC:**
  - Minimal 1 baris; kategori wajib; nominal > 0.
  - Total dihitung otomatis dan ditampilkan.
  - Dapat dibuka dari menu Beranda dan dari tombol di Rekap Keuangan.
  - Setelah disimpan, catatan terkunci.
  - Pengelompokan baris yang disimpan bersamaan. **[KONFIRMASI K-16]**
  - Daftar dan pengelolaan kategori pengeluaran (tidak ada layar kelola kategori). **[KONFIRMASI K-24]**

### M9 — Rekap Keuangan (L23)

- **US-9.1** Sebagai karyawan, saya ingin melihat penjualan, HPP, pengeluaran, dan estimasi laba per periode.
- **Input:** periode **Harian / Mingguan / Bulanan** (plus tanggal acuan).
- **Proses:** server menghitung dari transaksi terkonfirmasi: `laba kotor = penjualan − HPP` dan `estimasi laba bersih = laba kotor − pengeluaran operasional` (Bagian 4.12).
- **Output / AC:**
  - Kartu **Estimasi laba bersih** beserta persen dari penjualan.
  - Grafik penjualan per hari (rata-rata dan hari tertinggi).
  - Rincian: Penjualan, HPP, Laba kotor, Pengeluaran operasional.
  - Penjualan per metode bayar (Tunai vs QRIS).
  - Tombol cepat ke Catat Pengeluaran.
  - **Saldo kas awal dan perubahan kas ditampilkan terpisah dari perhitungan laba** (RPP); belum ada di mockup. **[KONFIRMASI K-11]**
  - Kebutuhan rekap per shift (RPP menyebut shift berbeda, tetapi ERD tidak punya konsep shift). **[KONFIRMASI K-26]**
  - Contoh angka mockup untuk uji: penjualan 7.200.000; HPP 4.250.000; laba kotor 2.950.000; pengeluaran 1.350.000; estimasi laba bersih 1.600.000 (22,2%).

### M10 — Laporan & Ekspor Data (L26–L28)

- **US-10.1** Sebagai karyawan, saya ingin mengekspor laporan periode tertentu ke Excel dan membagikannya, misalnya lewat WhatsApp.
- **Input:** periode (Hari ini / 7 hari / Bulan ini / Kustom), rentang tanggal, pilihan isi laporan (tiap pilihan menjadi satu sheet).
- **Isi laporan yang dapat dipilih:** Penjualan produk, Penggunaan bahan baku, Pergerakan stok, Pembelian bahan, Lead time pengadaan, Penyesuaian stok, Pengeluaran operasional, Rekap keuangan.
- **Proses:** menghasilkan file `.xlsx` dengan satu sheet per pilihan. Lokasi pembuatan file. **[KONFIRMASI K-06]**
- **Output / AC:**
  - Layar **Laporan siap**: nama file (mis. `Laporan_PUSATRACK_1–18Nov2026.xlsx`), jumlah sheet, ukuran, tombol **Simpan** dan **Bagikan**.
  - **Bagikan** membuka Android Sharesheet (WhatsApp, Gmail, Drive, Salin) lewat Intent dengan akses file yang aman (`FileProvider`).
  - Minimal 1 isi laporan dipilih; tanggal awal ≤ tanggal akhir.
  - File dapat dibuka di Microsoft Excel (RPP menyebut Excel dipakai untuk memeriksa hasil ekspor).
  - Pilihan bawaan mockup: Penjualan produk, Penggunaan bahan baku, Lead time pengadaan, Rekap keuangan.

### M11 — Produk & Resep (L29–L32)

- **US-11.1** Sebagai karyawan, saya ingin mengelola produk dan resepnya agar stok berkurang dan HPP terhitung otomatis.
- **Input:** nama produk, harga jual, status aktif, komposisi bahan per 1 porsi (bahan dan jumlah dalam satuan penggunaan).
- **Proses:** simpan `products` dan `product_recipes`. Perkiraan HPP = Σ (jumlah × harga per satuan penggunaan bahan). Margin = harga jual − HPP.
- **AC:**
  - Daftar produk: nama, harga, perkiraan HPP, margin (Rp dan %).
  - **Detail resep hanya-baca**; ada tombol **Edit Resep** agar tidak berubah tanpa sengaja.
  - Pratinjau HPP dan margin langsung berubah saat komposisi diedit; produk baru tanpa bahan menampilkan HPP Rp 0.
  - Produk dinonaktifkan lewat toggle Aktif (tidak ada hapus di mockup) dan tidak tampil di Kasir.
  - Perubahan harga dan resep hanya memengaruhi transaksi berikutnya; transaksi lama memakai snapshot HPP.
  - Contoh uji (L30): Kopi Susu harga Rp 10.000, resep 18 g Bubuk Kopi + 150 ml Susu UHT + 1 Cup Plastik, HPP Rp 5.676, margin Rp 4.324 (43,2%).
  - Hak edit harga/resep. **[KONFIRMASI K-08]**; produk tanpa resep. **[KONFIRMASI K-20]**; harga bahan. **[KONFIRMASI K-19]**

### 4.12 Aturan bisnis lintas modul

| ID   | Aturan                                                                                                                                                                                                                                                                  |
| ---- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| R-01 | Stok disimpan dalam **satuan penggunaan** (g, ml, pcs); pembelian dalam **kemasan**. Stok kemasan = stok ÷ isi per kemasan; sisa = stok mod isi per kemasan.                                                                                                            |
| R-02 | Penjualan mengurangi stok = jumlah resep × `quantity` produk, per bahan, dalam satu transaksi database.                                                                                                                                                                 |
| R-03 | Transaksi menyimpan snapshot `selling_price` dan `hpp` per item. Perubahan harga dan resep tidak mengubah transaksi lama.                                                                                                                                               |
| R-04 | Lead time = `received_at − order_date`. Rata-rata, tercepat, terlama dihitung dari pesanan berstatus diterima.                                                                                                                                                          |
| R-05 | Laba kotor = Σ (subtotal) − Σ (`hpp_snapshot` × qty). Estimasi laba bersih = laba kotor − Σ pengeluaran operasional. Pembelian bahan tidak dihitung sebagai pengeluaran (HPP berbasis resep). Saldo kas awal dan perubahan kas dipisah dari laba. **[KONFIRMASI K-11]** |
| R-06 | Transaksi terkonfirmasi, pengeluaran tersimpan, pergerakan stok, dan riwayat bersifat **tidak dapat diubah atau dihapus** lewat aplikasi/API.                                                                                                                           |
| R-07 | Setiap aksi yang mengubah data menulis `activity_history` dengan `user_id`, jenis, deskripsi, tanggal dan waktu.                                                                                                                                                        |
| R-08 | Setiap pergerakan stok menyimpan `stock_before`, `stock_after`, jenis, sumber (`source_type`, `source_id`), dan pengguna.                                                                                                                                               |
| R-09 | Zona waktu **WITA (Asia/Makassar)**; tanggal tampil `Rab, 18 Nov 2026`; mata uang `Rp 360.000`. **[USULAN]** set zona waktu yang sama di CI4, MySQL, dan app.                                                                                                           |
| R-10 | Perubahan data sensitif melalui dialog **Periksa kembali** sebelum disimpan.                                                                                                                                                                                            |

---

## 5. Kebutuhan Data (ERD), Validasi & Kontrak API

### 5.1 Entitas dan kolom (dibaca dari ERD Gambar 2)

> Verifikasi ulang ke file sumber ERD karena dibaca dari gambar. PK = primary key, FK = foreign key, UK = unique.

| Tabel                    | Kolom                                                                                                                                                                                                  |
| ------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `USERS`                  | `user_id` PK, `username` UK, `password_hash`, `full_name`, `created_at`, `is_active`                                                                                                                   |
| `EXPENSE_CATEGORIES`     | `category_id` PK, `category_name`                                                                                                                                                                      |
| `INGREDIENTS`            | `ingredient_id` PK, `ingredient_name`, `purchase_unit`, `package_size`, `usage_unit`, `current_stock`, `minimum_stock`, `unit_cost`, `is_active`                                                       |
| `STOCK_MOVEMENTS`        | `movement_id` PK, `ingredient_id` FK, `user_id` FK, `activity_id` FK, `movement_type`, `quantity`, `stock_before`, `stock_after`, `source_type`, `source_id`, `description`, `reason`, `movement_date` |
| `STOCK_COUNTS`           | `count_id` PK, `user_id` FK, `count_date`, `notes`, `status`                                                                                                                                           |
| `PRODUCTS`               | `product_id` PK, `product_name`, `selling_price`, `is_active`                                                                                                                                          |
| `PRODUCT_RECIPES`        | `recipe_id` PK, `product_id` FK, `ingredient_id` FK, `quantity_required`                                                                                                                               |
| `PURCHASE_ORDERS`        | `order_id` PK, `user_id` FK, `supplier_name`, `order_date`, `received_at`, `status`                                                                                                                    |
| `PURCHASE_ORDER_DETAILS` | `order_detail_id` PK, `order_id` FK, `ingredient_id` FK, `quantity`, `package_size_snapshot`, `package_price_snapshot`                                                                                 |
| `SALES`                  | `sale_id` PK, `user_id` FK, `sale_number`, `sale_date`, `payment_method`, `total_amount`, `status`, `confirmed_at`                                                                                     |
| `SALE_DETAILS`           | `sale_detail_id` PK, `sale_id` FK, `product_id` FK, `quantity`, `selling_price_snapshot`, `hpp_snapshot`, `subtotal`                                                                                   |
| `EXPENSES`               | `expense_id` PK, `user_id` FK, `category_id` FK, `amount`, `payment_method`, `description`, `expense_date`, `status`                                                                                   |
| `ACTIVITY_HISTORY`       | `activity_id` PK, `user_id` FK, `activity_type`, `description`, `activity_date`                                                                                                                        |

### 5.2 Relasi utama

- `USERS` 1—N `ACTIVITY_HISTORY`, `STOCK_COUNTS`, `STOCK_MOVEMENTS`, `PURCHASE_ORDERS`, `SALES`, `EXPENSES`.
- `EXPENSE_CATEGORIES` 1—N `EXPENSES`.
- `INGREDIENTS` 1—N `STOCK_MOVEMENTS`, `PRODUCT_RECIPES`, `PURCHASE_ORDER_DETAILS`.
- `PRODUCTS` 1—N `PRODUCT_RECIPES`, `SALE_DETAILS`.
- `PURCHASE_ORDERS` 1—N `PURCHASE_ORDER_DETAILS`; `SALES` 1—N `SALE_DETAILS`.
- `ACTIVITY_HISTORY` 1—N `STOCK_MOVEMENTS`.
- `STOCK_MOVEMENTS` menautkan ke dokumen asal lewat `source_type` + `source_id` (penjualan, penerimaan pesanan, pemeriksaan fisik, dll.).

### 5.3 Usulan perubahan skema (celah antara ERD, RPP, dan mockup)

| Kebutuhan                                | Usulan                                                                    | ID   |
| ---------------------------------------- | ------------------------------------------------------------------------- | ---- |
| Saldo kas awal per hari                  | Tabel baru (mis. `CASH_BALANCES`: tanggal, nominal, `user_id`)            | K-11 |
| Cara pengadaan bahan                     | Kolom `procurement_type` di `INGREDIENTS` (`SUPPLIED` / `DIRECT`)         | K-12 |
| Uang diterima dan kembalian              | Kolom `amount_received` (kembalian dihitung) di `SALES`                   | K-13 |
| Jumlah diterima aktual                   | Kolom `received_quantity` di `PURCHASE_ORDER_DETAILS`                     | K-14 |
| Token FCM dan status baca notifikasi     | Tabel baru (mis. `DEVICE_TOKENS`, `NOTIFICATION_READS`), bila FCM dipakai | K-15 |
| Foto bahan                               | Kolom `image_path` di `INGREDIENTS`                                       | K-05 |
| Pengelompokan baris pengeluaran          | Kolom `batch_id`/`activity_id` di `EXPENSES` atau tabel header            | K-16 |
| Harga per kemasan tanpa galat pembulatan | Simpan `package_price` atau presisi desimal `unit_cost` memadai           | K-19 |

### 5.4 Enumerasi (diturunkan dari label mockup; **belum final**, K-17)

- `payment_method`: `CASH` (Tunai), `QRIS`.
- `stock_movements.movement_type`: `SALE`, `PURCHASE`, `DIRECT_PURCHASE`, `OPENING_STOCK`, `ADJUSTMENT`.
- `purchase_orders.status`: `ORDERED`, `RECEIVED` (tahap "Dalam perjalanan" dapat berupa turunan waktu, K-21).
- `sales.status`: `CONFIRMED` (dan `CANCELLED` bila dipakai).
- `stock_counts.status`, `expenses.status`, `activity_type`: nilai perlu ditetapkan.
- Penomoran `TRX-xxxx` dan `PSN-xxx` (urut global atau reset per periode). **[KONFIRMASI K-18]**

### 5.5 Aturan validasi

| Form              | Aturan                                                                                                                 |
| ----------------- | ---------------------------------------------------------------------------------------------------------------------- |
| Login             | `username` dan `password` wajib; akun harus aktif                                                                      |
| Kasir             | Minimal 1 item; `quantity` bilangan bulat ≥ 1; produk harus aktif; Tunai: `amount_received` ≥ total                    |
| Bahan             | Nama wajib dan unik di antara bahan aktif; isi per kemasan > 0; harga per kemasan ≥ 0; batas minimum ≥ 0; satuan wajib |
| Tambah stok       | Jumlah kemasan > 0; harga per kemasan ≥ 0; tanggal tidak di masa depan                                                 |
| Pemeriksaan fisik | Jumlah kemasan ≥ 0; tidak ada perubahan bila tidak ada selisih                                                         |
| Pesanan           | Minimal 1 bahan; jumlah kemasan ≥ 1; hanya bahan Dipasok pusat                                                         |
| Penerimaan        | `received_at` ≥ `order_date`; pesanan belum berstatus diterima; centang kesesuaian wajib                               |
| Pengeluaran       | Minimal 1 baris; kategori wajib; nominal > 0                                                                           |
| Laporan           | Minimal 1 isi laporan; tanggal awal ≤ tanggal akhir                                                                    |
| Produk/resep      | Nama wajib; harga jual > 0; jumlah per bahan > 0; bahan tidak duplikat dalam satu resep                                |

Semua validasi dijalankan di **server** (CI4 Validation). Validasi di app hanya untuk pengalaman pengguna. Uang dan kuantitas memakai tipe `DECIMAL`, bukan float.

### 5.6 Kontrak API **[USULAN]**

> Kontrak ini rancangan awal dari mockup dan ERD, **belum disepakati**. Perubahan dicatat di PRD (Bagian 2.5).

**Konvensi**

- Base path `/api/v1`, JSON UTF-8, waktu ISO 8601 dengan offset `+08:00`.
- Autentikasi: header `Authorization: username` untuk semua endpoint.
- Respons: `{ "status": "success|error", "message": "...", "data": {...}, "meta": {...}, "errors": {...} }`.
- Kode HTTP: 200/201 sukses; 401 tidak terautentikasi; 403 tidak berhak; 404 tidak ditemukan; 409 konflik (mis. pesanan sudah diterima); 422 validasi gagal; 500 galat server.
- Paginasi: `page`, `per_page`; urutan: `sort`, `order`. Meta: `page`, `per_page`, `total`.
- Harga, total, dan HPP selalu dihitung server; klien tidak mengirimnya.

| Metode     | Endpoint                                                                  | Fungsi                                      | Layar        |
| ---------- | ------------------------------------------------------------------------- | ------------------------------------------- | ------------ |
| POST       | `/auth/login`                                                             | Login                                       | L01          |
| POST       | `/auth/logout`                                                            | Akhiri sesi                                 | L02          |
| GET        | `/auth/me`                                                                | Profil pengguna sesi                        | L02          |
| GET        | `/dashboard/summary?date=`                                                | Ringkasan hari                              | L02          |
| GET / POST | `/cash-balances`                                                          | Baca / isi saldo kas awal (K-11)            | L02          |
| GET        | `/products`, `/products/{id}`                                             | Daftar / detail produk dan resep            | L03, L29–L30 |
| POST / PUT | `/products`, `/products/{id}`                                             | Tambah / ubah produk dan resep              | L31–L32      |
| POST       | `/sales`                                                                  | Konfirmasi transaksi                        | L04–L06      |
| GET        | `/sales/{id}`                                                             | Detail transaksi                            | L08          |
| GET        | `/ingredients?status=&q=`                                                 | Daftar bahan                                | L10          |
| GET        | `/ingredients/{id}`                                                       | Detail bahan                                | L11          |
| POST / PUT | `/ingredients`, `/ingredients/{id}`                                       | Tambah / ubah bahan                         | L13–L14      |
| POST       | `/ingredients/{id}/image`                                                 | Unggah foto (bila K-05 = server)            | L13–L14      |
| POST       | `/ingredients/{id}/stock-in`                                              | Tambah stok beli langsung                   | L12          |
| GET        | `/ingredients/{id}/movements`                                             | Riwayat pergerakan stok                     | L11          |
| POST       | `/stock-counts`                                                           | Simpan pemeriksaan fisik dan penyesuaian    | L15–L16      |
| GET        | `/purchase-orders?status=`                                                | Daftar pesanan                              | L17          |
| GET        | `/purchase-orders/lead-time-stats`                                        | Rata-rata, tercepat, terlama                | L17          |
| POST       | `/purchase-orders`                                                        | Buat pesanan                                | L19–L21      |
| GET        | `/purchase-orders/{id}`                                                   | Detail pesanan                              | L17–L18      |
| POST       | `/purchase-orders/{id}/receive`                                           | Konfirmasi penerimaan                       | L18          |
| GET        | `/expense-categories`                                                     | Daftar kategori                             | L24          |
| POST       | `/expenses`                                                               | Simpan satu/lebih baris pengeluaran         | L24–L25      |
| GET        | `/finance/summary?period=&date=`                                          | Rekap keuangan                              | L23          |
| POST       | `/reports/export`                                                         | Buat `.xlsx` (K-06)                         | L26–L27      |
| GET        | `/activity-history?user_id=&type=&from=&to=&sort=&order=&page=&per_page=` | Riwayat (hanya-baca)                        | L07          |
| GET        | `/notifications`                                                          | Daftar notifikasi                           | L09          |
| POST       | `/notifications/read`                                                     | Tandai dibaca (K-15)                        | L09          |
| POST       | `/devices`                                                                | Daftarkan token FCM (hanya bila K-03 = FCM) | –            |

**Immutability:** endpoint `PUT`/`PATCH`/`DELETE` **tidak disediakan** untuk `activity-history`, `sales`, `expenses`, dan `stock_movements`.

### 5.7 Contoh payload

**Login**

```json
// POST /api/v1/auth/login
{ "username": "natashya", "password": "********" }

// 200
{ "status": "success", "message": "Login berhasil",
  "data": { "user": { "user_id": 1, "username": "natashya", "full_name": "Natashya" } } }
```

**Konfirmasi penjualan** (contoh TRX-0015 di mockup)

```json
// POST /api/v1/sales
{ "payment_method": "CASH", "amount_received": 50000,
  "items": [ { "product_id": 1, "quantity": 2 }, { "product_id": 2, "quantity": 1 } ] }

// 201
{ "status": "success", "message": "Transaksi berhasil",
  "data": { "sale_id": 15, "sale_number": "TRX-0015", "payment_method": "CASH",
            "total_amount": 30000, "amount_received": 50000, "change_amount": 20000,
            "confirmed_at": "2026-11-18T09:38:00+08:00",
            "stock_effects": [
              { "ingredient": "Bubuk Kopi", "used": 36, "unit": "g", "stock_after": 17964 },
              { "ingredient": "Bubuk Strawberry", "used": 15, "unit": "g", "stock_after": 2985 },
              { "ingredient": "Susu UHT Full Cream", "used": 450, "unit": "ml", "stock_after": 2140 },
              { "ingredient": "Cup Plastik 12 oz", "used": 3, "unit": "pcs", "stock_after": 57 } ] } }

// 422
{ "status": "error", "message": "Validasi gagal",
  "errors": { "amount_received": "Uang diterima kurang dari total" } }
```

**Konfirmasi penerimaan pesanan** (PSN-014)

```json
// POST /api/v1/purchase-orders/14/receive
{ "received_at": "2026-11-18T09:41:00+08:00", "confirmed_match": true }
// (+ "items": [{ "detail_id": .., "received_quantity": .. }] bila K-14 diterapkan)

// 200
{ "status": "success", "message": "Penerimaan dikonfirmasi",
  "data": { "order_id": 14, "status": "RECEIVED", "lead_time_seconds": 174360,
            "lead_time_label": "2 hari 26 menit" } }

// 409
{ "status": "error", "message": "Pesanan sudah dikonfirmasi diterima" }
```

---

## 6. Kebutuhan Non-Fungsional & Panduan UI/UX

### 6.1 Kebutuhan non-fungsional

| Area            | Kebutuhan                                                                                                                                                                                                                                                        |
| --------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Keamanan        | Password disimpan sebagai hash; semua endpoint selain login wajib autentikasi; validasi dan sanitasi di server; query lewat Query Builder/prepared statement: , kunci Firebase, dan keystore tidak masuk repository;                                             |
| Integritas data | Operasi multi-tabel (penjualan, penerimaan, opname) dalam transaksi database; pembaruan stok aman terhadap penggunaan bersamaan oleh beberapa karyawan (mis. `SELECT … FOR UPDATE` atau pembaruan atomik); foreign key aktif; `DECIMAL` untuk uang dan kuantitas |
| Auditabilitas   | Riwayat hanya-baca;                                                                                                                                                                                                                                              |
|                 |
| Kinerja         | **[USULAN]** respons API untuk operasi normal terasa instan di jaringan lokal (target dapat ditetapkan tim, mis. < 2 detik); layar tidak membeku saat memuat (proses jaringan asinkron dengan indikator loading)                                                 |
| Keandalan       | Cadangan database dan kode berkala (RPP: pencadangan berkala); pesan galat yang jelas bila server tidak terjangkau. Mode offline tidak dirancang. **[KONFIRMASI K-07]**                                                                                          |
| Kompatibilitas  | Android, orientasi portrait. Versi minimum dan perangkat uji. **[KONFIRMASI K-25]**                                                                                                                                                                              |
| Lokalisasi      | Bahasa Indonesia; WITA; format Rupiah dan tanggal sesuai R-09                                                                                                                                                                                                    |
| Keterpeliharaan | Struktur folder monorepo; konvensi penamaan;                                                                                                                                                                                                                     |
|                 |
| Pengujian       | Uji fungsi per modul, navigasi antar-Fragment, autentikasi, penyimpanan data, perhitungan (Bagian 7.5)                                                                                                                                                           |

### 6.2 Panduan UI/UX (mengacu mockup v6)

- **Tema:** putih bersih dan hijau tua, gaya Material 3, tampilan modern dan bersih. Token warna, tipografi, dan spasi final diambil dari file Figma v6.
- **Implementasi:** Kotlin dengan **Views/XML dan Fragment** (bukan Jetpack Compose).
- **Navigasi:** bottom navigation 5 tab (Beranda, Inventori, Kasir di tengah dan menonjol, Pesanan, Riwayat). Fitur lain lewat "Menu lainnya" di Beranda.
- **Warna semantik:** hijau tua untuk aksi utama; kuning/amber untuk peringatan (stok menipis, dialog periksa kembali); merah untuk koreksi/selisih (Sesuaikan Stok, sisa menipis).
- **Pola interaksi:**
  - Dialog **Periksa kembali** sebelum menyimpan penjualan, pengeluaran, pesanan, dan penyesuaian stok.
  - Catatan "terkunci / tidak bisa diubah" pada transaksi dan riwayat.
  - Stepper untuk jumlah; chip untuk filter; badge status bahan.
  - Pratinjau hasil hitung (total, kembalian, stok jadi, HPP) sebelum simpan.
- **Keadaan layar:** sediakan empty state (belum ada transaksi/pesanan), loading, dan error (jaringan, validasi).
- **Aksesibilitas:** target sentuh memadai (≥ 48dp), kontras teks cukup, label jelas pada ikon.
- **Intent:** kamera/galeri (foto bahan), WhatsApp (teks pesanan), Sharesheet (file `.xlsx` lewat `FileProvider`). Siapkan fallback bila aplikasi tujuan tidak ada.
- Peta lengkap 32 layar ada di **Lampiran A**.

---

## 7. Rencana Implementasi Bertahap (Milestone & Jadwal 5 Minggu)

### 7.1 Fase proyek (dari RPP)

| Fase                                      | Pekan | Hari kerja ke- | Keluaran                                                                        |
| ----------------------------------------- | ----- | -------------- | ------------------------------------------------------------------------------- |
| Identifikasi masalah dan pengembangan ide | 1     | 1–3            | Hasil observasi dan wawancara, ide PUSATRACK                                    |
| Deskripsi masalah dan pemetaan            | 1     | 4–5            | Kebutuhan dan batasan sistem                                                    |
| Analisis                                  | 2     | 6–10           | Alur aplikasi, Fragment, UI/UX, rancangan basis data (ERD, mockup v6 sudah ada) |
| Pengembangan sistem                       | 2–4   | 8–20           | Aplikasi, API, database, ekspor                                                 |
| Pengujian, evaluasi, serah terima         | 4–5   | 18–25          | Aplikasi teruji, demonstrasi, serah terima                                      |

Tanggal kalender tiap hari kerja diisi tim setelah tanggal mulai pengembangan ditetapkan.

### 7.2 Milestone pengembangan **[USULAN]** (di dalam rentang RPP)

| MS                        | Hari ke- | Cakupan                                                                                                                                                             | Folder terlibat    | Kriteria selesai                                              |
| ------------------------- | -------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------ | ------------------------------------------------------------- |
| MS-0 Fondasi              | 6–9      | Putuskan item prioritas tinggi Lampiran B; skema database + seed; kerangka CI4 (auth, envelope respons); kerangka Android (tema, navigasi, bottom nav, layar login) | database, api, app | Login end-to-end berjalan di HP                               |
| MS-1 Master data          | 8–12     | Inventori (daftar, detail, tambah/edit bahan, tambah stok) dan Produk/Resep                                                                                         | api, app           | Bahan dan produk bisa dikelola; HPP perkiraan benar           |
| MS-2 Kasir & riwayat      | 11–15    | Dashboard, Kasir (tunai/QRIS, dialog, atomik), Detail Transaksi, Activity History (filter, sorting, pagination)                                                     | api, app           | TRX tercatat, stok berkurang sesuai resep, riwayat hanya-baca |
| MS-3 Pesanan & keuangan   | 14–18    | Pesanan stok, WhatsApp Intent, penerimaan dan lead time, pengeluaran, rekap keuangan, saldo kas awal                                                                | api, app           | Lead time dan rekap sesuai data uji                           |
| MS-4 Laporan & notifikasi | 17–20    | Ekspor `.xlsx` dan Sharesheet, notifikasi stok menipis/pesanan, pemeriksaan fisik, foto bahan                                                                       | api, app           | File terbuka di Excel; notifikasi muncul                      |
| MS-5 Uji & serah terima   | 18–25    | Pengujian bertahap, perbaikan bug, evaluasi bersama pengguna, demonstrasi, serah terima                                                                             | semua              | Indikator RPP 1–10 terpenuhi                                  |

Urutan memperhatikan dependensi: Kasir membutuhkan Produk/Resep dan Inventori; Rekap membutuhkan penjualan, HPP, dan pengeluaran; Laporan membutuhkan semua data sumber. `database/` dan `backend-api/` dapat berjalan paralel dengan `android-app/` memakai contoh respons di Bagian 5.7.

### 7.3 Risiko (dari RPP, ditambah)

| Risiko                                                                                                | Level | Mitigasi                                                                                                               |
| ----------------------------------------------------------------------------------------------------- | ----- | ---------------------------------------------------------------------------------------------------------------------- |
| Permasalahan operasional terlalu beragam                                                              | M     | Klasifikasi berdasarkan urgensi, konfirmasi ke mitra                                                                   |
| Ruang lingkup melebar                                                                                 | M     | Pertahankan batasan Bagian 2.2; prioritaskan fitur utama (inventori, lead time, penjualan/pengeluaran, rekap, laporan) |
| Rancangan alur, Fragment, basis data, atau pembagian MySQL/Firebase/penyimpanan internal belum sesuai | H     | Tinjau rancangan bersama tim sebelum implementasi; selesaikan Lampiran B prioritas tinggi lebih dulu                   |
| Bug dan kendala integrasi Android–API–MySQL–Firebase; kesalahan autentikasi dan pencatatan aktivitas  | H     | Pengembangan bertahap per fitur, uji tiap modul, debugging, pencadangan berkala                                        |
| Fitur belum sesuai rancangan menjelang akhir                                                          | H     | Pengujian bertahap, daftar bug berprioritas, evaluasi bersama pengguna sebelum serah terima                            |
| **[USULAN]** Server lokal tidak terjangkau saat serah terima                                          | H     | Putuskan K-01 sejak awal; uji di jaringan kedai                                                                        |
| **[USULAN]** Kontrak API berubah tanpa diketahui sisi lain                                            | M     | Perubahan kontrak hanya lewat PR yang memperbarui PRD                                                                  |
| **[USULAN]** Rahasia ter-commit ke repository                                                         | M     | `.gitignore` dan `.env.example` per folder                                                                             |
| **[USULAN]** Mockup dan RPP berbeda (Lampiran B)                                                      | M     | Putuskan apakah mockup diperbarui atau skema ditambah                                                                  |

### 7.4 Pemantauan dan evaluasi

Sesuai RPP: diskusi dan koordinasi tim, pemeriksaan progres tiap fitur, pencatatan kendala; setiap tahap dievaluasi terhadap target. **[USULAN]** pertemuan singkat berkala dan papan tugas per modul (M1–M11).

### 7.5 Pengujian dan serah terima

**Data uji** diambil dari angka mockup v6 (data contoh, bukan data produksi; seeder pengembangan boleh memakainya).

| ID    | Skenario                                                                                                   | Hasil yang diharapkan                                                                                                                                                |
| ----- | ---------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| TC-01 | Login valid/salah, akun nonaktif                                                                           | Masuk ke Beranda / pesan generik / ditolak                                                                                                                           |
| TC-02 | Penjualan tunai: Kopi Susu ×2 + Strawberry ×1, diterima Rp 50.000                                          | Total Rp 30.000, kembalian Rp 20.000; stok: Bubuk Kopi −36 g (sisa 17.964), Bubuk Strawberry −15 g (sisa 2.985), Susu UHT −450 ml (sisa 2.140), Cup −3 pcs (sisa 57) |
| TC-03 | HPP Kopi Susu (Rp 120.000 per 1.000 g; Rp 18.500 per 920 ml; Rp 25.000 per 50 pcs)                         | HPP Rp 5.676; margin Rp 4.324 (43,2%)                                                                                                                                |
| TC-04 | Tambah stok Susu UHT +3 kotak × Rp 18.500 (stok 2.140 ml)                                                  | +2.760 ml menjadi 4.900 ml; tidak masuk pengeluaran; harga bahan terbarui                                                                                            |
| TC-05 | Pemeriksaan fisik: Susu UHT hitung 2 kotak tanpa kemasan terbuka; Bubuk Kopi 17 pak dengan kemasan terbuka | Susu −300 ml (selisih), Bubuk Kopi sesuai; dialog "1 bahan berubah"                                                                                                  |
| TC-06 | Pesanan PSN-015: Bubuk Kopi 10 pak × 120.000 + Bubuk Strawberry 6 bungkus × 85.000 + Cup 10 pak × 25.000   | Total Rp 1.960.000 (3 bahan, 26 kemasan); pesan WhatsApp sesuai template tanpa harga                                                                                 |
| TC-07 | Penerimaan PSN-014: dipesan Sen 16 Nov 2026 09:15, diterima 18/11/2026 09:41                               | Lead time 2 hari 26 menit; stok +8.000 g kopi, +3.000 g strawberry, +500 pcs cup; konfirmasi ulang ditolak (409)                                                     |
| TC-08 | Rekap bulanan                                                                                              | Penjualan 7.200.000; HPP 4.250.000; laba kotor 2.950.000; pengeluaran 1.350.000; laba bersih 1.600.000 (22,2%)                                                       |
| TC-09 | Pengeluaran Tunai: Es Batu Rp 25.000 (2 kantong) + Konsumsi Rp 15.000                                      | Total Rp 40.000; tampil di Riwayat dan Rekap                                                                                                                         |
| TC-10 | Ekspor laporan 1–18 Nov (4 sheet), bagikan ke WhatsApp                                                     | File `.xlsx` terbuka di Excel; Sharesheet berfungsi                                                                                                                  |
| TC-11 | Coba ubah/hapus riwayat dan transaksi terkonfirmasi                                                        | Tidak ada UI/endpoint; API menolak                                                                                                                                   |
| TC-12 | Stok bahan turun ≤ batas minimum                                                                           | Peringatan di Beranda, Inventori, dan Notifikasi (push sesuai K-03)                                                                                                  |

**Syarat serah terima:** seluruh indikator Bagian 1.4 terpenuhi, TC-01 s.d. TC-12 lulus, demonstrasi kepada Pusa Coffee Cabang BTP, dan akun empat karyawan telah dibuat.

---

## Lampiran A — Peta layar mockup v6

| No  | Layar                         | Modul |
| --- | ----------------------------- | ----- |
| L01 | Login                         | M1    |
| L02 | Beranda                       | M2    |
| L03 | Kasir                         | M3    |
| L04 | Pembayaran                    | M3    |
| L05 | Dialog Konfirmasi Transaksi   | M3    |
| L06 | Transaksi Berhasil            | M3    |
| L07 | Riwayat                       | M4    |
| L08 | Detail Transaksi              | M4    |
| L09 | Notifikasi                    | M5    |
| L10 | Inventori                     | M6    |
| L11 | Detail Bahan                  | M6    |
| L12 | Tambah Stok                   | M6    |
| L13 | Tambah Bahan                  | M6    |
| L14 | Edit Bahan                    | M6    |
| L15 | Pemeriksaan Fisik             | M6    |
| L16 | Dialog Sesuaikan Stok         | M6    |
| L17 | Lacak Pesanan Stok            | M7    |
| L18 | Konfirmasi Penerimaan         | M7    |
| L19 | Buat Pesanan                  | M7    |
| L20 | Dialog Periksa Pesanan        | M7    |
| L21 | Pesanan Dibuat                | M7    |
| L22 | Pesan WhatsApp                | M7    |
| L23 | Rekap Keuangan                | M9    |
| L24 | Catat Pengeluaran             | M8    |
| L25 | Dialog Konfirmasi Pengeluaran | M8    |
| L26 | Laporan dan Ekspor            | M10   |
| L27 | Ekspor Selesai                | M10   |
| L28 | Android Sharesheet            | M10   |
| L29 | Produk dan Resep              | M11   |
| L30 | Detail Resep (hanya baca)     | M11   |
| L31 | Edit Resep                    | M11   |
| L32 | Tambah Produk                 | M11   |

## Lampiran B — Daftar item yang perlu dikonfirmasi tim

Prioritas **Tinggi** = sebaiknya diputuskan sebelum pengembangan dimulai (sekitar hari ke-8).

| ID   | Topik                                   | Yang perlu diputuskan                                                                                      | Usulan awal                                                        | Prioritas |
| ---- | --------------------------------------- | ---------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------ | --------- |
| K-01 | Hosting backend                         | Laragon di laptop pada Wi-Fi kedai, atau hosting online? Menentukan base URL, HTTPS, dan syarat push FCM   | Putuskan sebelum MS-0; uji di jaringan kedai                       | Tinggi    |
| K-02 | Sesi/token login                        | Jenis token (opaque di database atau JWT), masa berlaku, cara cabut, tempat simpan di perangkat            | Token opaque di database + EncryptedSharedPreferences              | Tinggi    |
| K-03 | Firebase                                | Hanya FCM untuk push, atau juga layanan data? Atau notifikasi tanpa Firebase (polling)?                    | FCM saja; data tetap di MySQL                                      | Tinggi    |
| K-04 | Sumber kebenaran skema                  | `database/` (SQL) atau migrasi CI4?                                                                        | Satu sumber saja; hindari dua-duanya                               | Tinggi    |
| K-05 | Foto bahan                              | Diunggah ke server atau tersimpan lokal? Kolom `image_path`?                                               | Unggah ke server agar tidak hilang saat ganti HP                   | Sedang    |
| K-06 | Pembuatan `.xlsx`                       | Di server (CI4) atau di perangkat?                                                                         | Di server, lalu diunduh dan dibagikan                              | Sedang    |
| K-07 | Offline                                 | Apakah aplikasi harus bisa dipakai tanpa internet?                                                         | Online-only                                                        | Sedang    |
| K-08 | Role dan hak akses                      | Apakah perlu role pemilik/admin? Siapa boleh edit harga/resep, penyesuaian stok? Pembagian PIC tim         | Semua karyawan setara kecuali diputuskan lain                      | Tinggi    |
| K-09 | Akun dan reset password                 | Prosedur membuat 4 akun dan mereset password (manual)                                                      | Seeder dan perintah CLI                                            | Sedang    |
| K-10 | Cakupan aktivitas tercatat              | Apakah login/logout dan edit bahan/produk/resep juga dicatat?                                              | Catat semua perubahan data; login opsional                         | Sedang    |
| K-11 | Saldo kas awal dan perubahan kas        | Tabel/kolom baru, satu per hari atau per shift, siapa mengisi, definisi perubahan kas, tambah ke mockup    | Satu per hari; perubahan kas = penjualan tunai − pengeluaran tunai | Tinggi    |
| K-12 | Cara pengadaan bahan                    | Kolom `procurement_type` dan perilaku: Dipasok pusat (lewat Pesanan) vs Beli langsung (lewat Tambah stok)  | Tambahkan kolom; perilaku sesuai mockup                            | Tinggi    |
| K-13 | Uang diterima dan kembalian             | Tambah kolom `amount_received` di `SALES`                                                                  | Tambahkan; kembalian dihitung                                      | Tinggi    |
| K-14 | Jumlah diterima aktual                  | Tambah input jumlah diterima saat penerimaan, atau cukup centang "sesuai pesanan"                          | Tambah `received_quantity` agar sesuai RPP                         | Tinggi    |
| K-15 | Token FCM dan status baca               | Tabel token perangkat dan penyimpanan status "dibaca"                                                      | Tabel baru bila FCM dipakai                                        | Sedang    |
| K-16 | Pengelompokan pengeluaran               | Cara mengelompokkan beberapa baris pengeluaran yang disimpan bersamaan (muncul sebagai satu entri riwayat) | Kolom `batch_id` atau tabel header                                 | Sedang    |
| K-17 | Enumerasi dan pembatalan                | Nilai final `status`, `movement_type`, `source_type`, `activity_type`; apakah transaksi bisa dibatalkan    | Pakai Bagian 5.4; tanpa pembatalan sesuai mockup                   | Tinggi    |
| K-18 | Penomoran TRX/PSN                       | Urut global atau reset harian/bulanan; panjang digit                                                       | Urut global                                                        | Sedang    |
| K-19 | Harga bahan dan HPP                     | Presisi desimal; harga terakhir atau rata-rata saat penerimaan/pembelian; simpan `package_price`           | Harga terakhir (sesuai mockup); `DECIMAL` berpresisi cukup         | Tinggi    |
| K-20 | Stok tidak cukup / produk tanpa resep   | Blokir penjualan, beri peringatan, atau izinkan stok minus? Boleh jual produk tanpa resep?                 | Peringatan jelas; keputusan bersama mitra                          | Tinggi    |
| K-21 | Status pesanan dan notifikasi terlambat | Apakah "Dalam perjalanan" otomatis atau aksi pengguna? Ambang waktu "belum diterima"                       | Otomatis setelah dipesan; ambang = rata-rata lead time             | Sedang    |
| K-22 | Filter Riwayat                          | Mockup perlu diperbarui agar memuat filter pengguna, rentang tanggal, sorting, pagination (RPP)            | Perbarui mockup sebelum MS-2                                       | Sedang    |
| K-23 | Nomor WhatsApp tujuan                   | Nomor pusat tetap di aplikasi, atau dipilih manual di WhatsApp?                                            | Pilih manual (sesuai mockup)                                       | Sedang    |
| K-24 | Kategori pengeluaran                    | Daftar awal kategori dan siapa yang mengelola (tidak ada layar kelola)                                     | Seed awal: Es Batu, Konsumsi, lainnya sesuai mitra                 | Sedang    |
| K-25 | Versi Android dan perangkat uji         | Minimum SDK dan HP yang dipakai pengujian dan kedai                                                        | Tentukan dari HP karyawan                                          | Sedang    |
| K-26 | Konsep shift                            | Apakah laporan/rekap per shift diperlukan? (RPP menyebut shift berbeda, ERD tidak punya konsep shift)      | Di luar lingkup kecuali diminta mitra                              | Sedang    |
