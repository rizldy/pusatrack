# DESIGN.md — PUSATRACK (Android UI/UX Specification — v7)

Dokumen panduan desain dan implementasi antarmuka **PUSATRACK (Pusa Coffee Inventory & Financial Tracking System)** untuk aplikasi Android berbasis **Kotlin, Views/XML, dan Fragment** (Material Design 3), diselaraskan dengan **Mockup PUSATRACK v7 (39 layar, 125 hotspot)** dan `PRD.md`.

> **Catatan Token Warna:** Warna inti brand, teks, kontainer, dan permukaan di bawah sudah mengikuti nilai eksak dari mockup v6/v7 (`README.md`). Untuk 15 paint style lengkap (termasuk nilai hex eksak warna semantik kuning/merah/hijau sukses), bagian bertanda `[VERIFIKASI code.js]` dapat dicocokkan langsung ke deklarasi variabel di `pusatrack-figma-plugin/code.js`.

## 1. Prinsip Desain & Identitas Visual

* **Platform & Acuan Kanvas:** Android (Kotlin + Views/XML + Fragment, bukan Jetpack Compose), Material 3 (`Theme.Material3.Light.NoActionBar`), acuan desain `390×800 dp`.
* **Orientasi Layar:** Dikunci pada **Portrait** (`android:screenOrientation="portrait"`).
* **Gaya Visual (v6/v7):**
  * **Latar semua layar putih bersih (`#FFFFFF`)**.
  * **Hijau tua forest (`#17402F`)** sebagai warna utama, dipadukan kontainer tonal (`#E3EEE6`).
  * **Kartu putih dengan shadow lembut tanpa garis tepi (borderless)** — Drop Shadow blur `18`, opasitas `7,5%`.
  * **Tipografi modern:** Menggunakan **Plus Jakarta Sans** (tebal untuk judul; Fraunces tidak digunakan lagi). Fallback: **Inter**.
* **Bahasa & Format Lokalisasi:**
  * Bahasa: **Bahasa Indonesia**.
  * Zona waktu: **WITA (`Asia/Makassar`)**.
  * Format tanggal: `Rab, 18 Nov 2026`.
  * Format mata uang: `Rp 360.000` (pemisah ribuan titik, tanpa desimal untuk tampilan umum).
* **Aksesibilitas Operasional:**
  * Target sentuh minimum **`48dp` × `48dp`** untuk tombol, stepper, chip, dan item daftar.
  * Kontras teks tinggi (`#14231B` di atas `#FFFFFF`).
  * Label teks jelas mendampingi ikon pada navigasi bawah dan tombol aksi kritis.

---

## 2. Design Tokens (Android `res/values/`)

### 2.1 Palet Warna (`res/values/colors.xml`)

```xml
<resources>
    <!-- Brand & Primary — Eksak v6/v7 -->
    <color name="pusa_green_primary">#17402F</color>      <!-- Forest Green utama -->
    <color name="pusa_green_container">#E3EEE6</color>    <!-- Kontainer tonal / tombol sekunder -->
    <color name="pusa_on_primary">#FFFFFF</color>
    <color name="pusa_on_primary_container">#17402F</color>

    <!-- Surface & Background — Eksak v6/v7 -->
    <color name="bg_screen">#FFFFFF</color>               <!-- Latar semua layar putih bersih -->
    <color name="surface_card">#FFFFFF</color>            <!-- Kartu putih tanpa border -->
    <color name="surface_input">#F2F5F3</color>           <!-- Isian tonal abu-hijau tanpa border [VERIFIKASI code.js] -->
    <color name="shadow_card">#1314231B</color>           <!-- Shadow kartu ~7.5% opacity -->
    <color name="shadow_primary">#3317402F</color>        <!-- Shadow hijau untuk tombol utama -->

    <!-- Typography / Neutral — Eksak v6/v7 -->
    <color name="text_primary">#14231B</color>            <!-- Teks utama -->
    <color name="text_muted">#69776F</color>              <!-- Teks redup / sub-label -->

    <!-- Semantic Colors [VERIFIKASI code.js] -->
    <color name="semantic_success">#16A34A</color>
    <color name="semantic_success_bg">#DCFCE7</color>

    <color name="semantic_warning">#D97706</color>
    <color name="semantic_warning_bg">#FEF3C7</color>
    <color name="semantic_warning_border">#FDE68A</color>

    <color name="semantic_danger">#DC2626</color>
    <color name="semantic_danger_bg">#FEE2E2</color>
    <color name="semantic_danger_border">#FECACA</color>
</resources>
```

### 2.2 Tipografi (`res/font/` — Plus Jakarta Sans)

Pasang family font **Plus Jakarta Sans** (`Regular`, `Medium`, `SemiBold`, `Bold`, `ExtraBold`) di `res/font/` dengan fallback **Inter**.

| Peran (10 Text Styles Acuan v6/v7) | Ukuran `[VERIFIKASI code.js]` | Weight | Pemakaian |
| :--- | :--- | :--- | :--- |
| `Display / Hero Nominal` | `26sp–28sp` | Bold | Total Penjualan Hari Ini, Estimasi Laba Bersih, Rata-rata Lead Time |
| `Headline` | `20sp–22sp` | Bold | Judul halaman, sapaan `full_name` di Beranda |
| `TitleMedium` | `16sp` | Bold / SemiBold | Judul kartu, nama menu Kasir, judul dialog |
| `TitleSmall` | `14sp` | SemiBold | Sub-judul seksi, judul baris daftar |
| `BodyMedium` | `14sp` | Regular | Teks utama, deskripsi aktivitas, isi form |
| `BodySmall` | `12sp` | Regular | Keterangan, tanggal/jam WITA, konversi satuan |
| `LabelLarge` | `14sp` | SemiBold | Teks tombol pill utama & sekunder |
| `LabelMedium` | `12sp` | SemiBold | Status chip, filter chip, label navigasi bawah |
| `LabelSmall` | `11sp` | Medium | Badge notifikasi, label mikro |
| `Code / Tabular` | `13sp` | Medium | Kode `TRX-xxxx`, `PSN-xxx`, nominal berkolom |

### 2.3 Spasi, Radius & Elevasi (`res/values/dimens.xml`)

```xml
<resources>
    <dimen name="spacing_xs">4dp</dimen>
    <dimen name="spacing_sm">8dp</dimen>
    <dimen name="spacing_md">12dp</dimen>
    <dimen name="spacing_lg">16dp</dimen>
    <dimen name="spacing_xl">24dp</dimen>

    <dimen name="radius_input">12dp</dimen>
    <dimen name="radius_card">16dp</dimen>
    <dimen name="radius_pill">999dp</dimen>

    <!-- Shadow lembut kartu (setara blur 18, opacity 7.5%) -->
    <dimen name="card_elevation">6dp</dimen>
    <dimen name="card_stroke_width">0dp</dimen>
    <dimen name="touch_target_min">48dp</dimen>
    <dimen name="bottom_nav_height">68dp</dimen>
</resources>
```

---

## 3. Arsitektur Navigasi & Shell Aplikasi

### 3.1 Alur Masuk & Sesi

* **Belum Login → `L01` Login** (tanpa Bottom Navigation).
* **Sudah Login → `MainActivity`** yang memuat `NavHostFragment` dan **Bottom Navigation 5 Tab**.

### 3.2 Bottom Navigation (5 Tab Utama)

Bar navigasi berlatar **putih dengan shadow lembut** (tanpa border atas yang keras).

| Posisi | Label Tab | Layar Root | Catatan Visual (v6/v7) |
| :--- | :--- | :--- | :--- |
| 1 | **Beranda** | `L02` | Tab default setelah login |
| 2 | **Inventori** | `L10` | Daftar & filter bahan baku |
| 3 (Tengah) | **Kasir** | `L03` | **Tombol bundar hijau tua (`#17402F`), besar & menonjol** di tengah bar |
| 4 | **Pesanan** | `L17` | Lacak pesanan stok (layout timeline) |
| 5 | **Riwayat** | `L07` | Activity History hanya-baca |

### 3.3 Akses Fitur Sekunder (dari Beranda `L02`)

* **Top Bar Beranda:** Sapaan `full_name` + ikon **Lonceng Notifikasi** dengan *badge* jumlah belum dibaca → `L09`.
* **Grid "Menu Lainnya" di Beranda (7 Menu di v7):**
  1. **Keuangan** → `L23` Rekap Keuangan
  2. **Laporan** → `L26` Laporan dan Ekspor
  3. **Pengeluaran** → `L24` Catat Pengeluaran
  4. **Produk** → `L29` Produk dan Resep
  5. **Cek Fisik** → `L15` Pemeriksaan Fisik (Stok Opname)
  6. **Akun** *(Baru di v7)* → `L33` Manajemen Akun
  7. **Keluar** → Dialog konfirmasi keluar, hapus sesi lokal, kembali ke `L01`

> **Aturan Visibilitas Bottom Nav:** Bottom Navigation hanya tampil pada 5 layar root (`L02`, `L03`, `L07`, `L10`, `L17`). Pada semua layar detail, form, pembayaran, manajemen akun, dan ekspor, sembunyikan Bottom Navigation dan tampilkan **tombol kembali bundar putih bershadow** di kiri atas.

---

## 4. Pustaka Komponen Reusable (Views/XML — Gaya v6/v7)

1. **Kartu Putih Borderless (`MaterialCardView`)**
   * `cardBackgroundColor="@color/surface_card"`, `strokeWidth="0dp"`, `cardCornerRadius="@dimen/radius_card"`, `cardElevation="@dimen/card_elevation"`, shadow lembut (`outlineAmbientShadowColor` & `outlineSpotShadowColor` bernuansa `#14231B` rendah opasitas di Android 9+).
2. **Kolom Isian Tonal (`TextInputLayout` / Custom Field)**
   * Latar berisi tonal abu-hijau (`surface_input`) **tanpa border** pada keadaan normal, label berada di dalam kolom.
   * Saat fokus/aktif, tampilkan **cincin (stroke) hijau tua (`#17402F`)**.
3. **Tombol Aksi (Pill & Bundar)**
   * **Tombol Utama:** Bentuk **pill** (`radius_pill`), latar `#17402F`, teks `#FFFFFF`, dengan **shadow berwarna hijau**.
   * **Tombol Sekunder:** Bentuk **pill**, latar tonal `#E3EEE6`, teks `#17402F`, tanpa border.
   * **Tombol Destruktif / Koreksi:** Bentuk **pill**, latar merah (`semantic_danger`), teks `#FFFFFF` (untuk *Sesuaikan Stok* `L16` dan *Nonaktifkan Akun* `L39`).
   * **Tombol Kembali (Back):** **Bundar putih (`#FFFFFF`) dengan shadow lembut**, ukuran minimal `44dp–48dp`.
4. **Chip Filter & Segmented Control**
   * State **terpilih:** latar **hijau tua penuh (`#17402F`)**, teks putih.
   * State **tidak terpilih:** latar tonal/putih bershadow lembut, teks `#14231B` atau `#69776F`.
5. **Quantity Stepper (`layout_stepper.xml`)**
   * Tombol `−`, angka di tengah, tombol `+` dengan area sentuh ≥ `48dp`. Dipakai di Kasir (`L03`), Pemeriksaan Fisik (`L15`), dan Buat Pesanan (`L19`).
6. **Status Chip / Badge**
   * **Aman / Sesuai / Selesai / Aktif:** teks hijau, latar hijau lembut.
   * **Menipis / Dalam Perjalanan:** teks amber, latar amber lembut.
   * **Habis / Selisih / Nonaktif:** teks merah, latar merah lembut.
7. **Dialog Konfirmasi ("Periksa Kembali" & Keamanan)**
   * `L05` Konfirmasi Transaksi (peringatan amber, transaksi terkunci permanen).
   * `L16` Sesuaikan Stok (peringatan merah, daftar selisih dari → menjadi, tombol merah).
   * `L20` Periksa Pesanan (rincian + total + catatan stok baru bertambah saat penerimaan dikonfirmasi).
   * `L25` Periksa Pengeluaran (metode, waktu, baris, total, catatan terkunci).
   * `L38` Verifikasi Password *(Baru di v7)* (konfirmasi password akun aktif sebelum aksi sensitif akun).
   * `L39` Nonaktifkan Akun *(Baru di v7)* (konfirmasi nonaktifkan akun karyawan agar tidak bisa login).
8. **State Views Wajib di Setiap Fragment**
   * **Loading:** Indikator loading asinkron (UI tidak membeku).
   * **Empty State:** Ikon netral + teks informatif.
   * **Error State:** Pesan galat jaringan/validasi yang jelas + tombol **Coba Lagi**.

---

## 5. Spesifikasi 39 Layar (`L01–L39`)

### Modul M1 & M2 — Autentikasi & Beranda (`L01–L02`)

* **`L01` Login:** Logo & identitas PUSATRACK, kolom tonal `username` & `password` (dengan toggle lihat/sembunyi password), tombol pill hijau **Login**, teks bantuan *"Lupa password? Minta tim untuk mereset akunmu"*. Pesan error login wajib generik.
* **`L02` Beranda:** Sapaan `full_name` + lonceng notifikasi ber-badge (`L09`); banner peringatan stok menipis + tombol **Lihat**; kartu hero **Penjualan Hari Ini** (total Rp, jumlah transaksi, rincian Tunai & QRIS, tombol **Buka Kasir**); kartu ringkasan **Pengeluaran Hari Ini**, **Transaksi Terakhir**, **Perlu Distok**, **Pesanan Berjalan**; grid **Menu Lainnya** (Keuangan, Laporan, Pengeluaran, Produk, Cek Fisik, **Akun**, Keluar).

### Modul M3 — Kasir / Penjualan (`L03–L06`)

* **`L03` Kasir:** Dua kartu menu besar (data contoh asli: **Kopi Susu** & **Strawberry**, masing-masing `Rp 10.000`). Tap kartu menambah qty + stepper `+/−`. Bar bawah menampilkan total item, total Rp, dan tombol **Lanjut ke Pembayaran**.
* **`L04` Pembayaran:** Segmented control **Tunai / QRIS** (terpilih = hijau penuh). Untuk Tunai: input uang diterima, tombol nominal cepat (`Rp 10.000`, dll.), pratinjau kembalian otomatis. Untuk QRIS: catat manual sesuai nominal. Tombol **Periksa & Konfirmasi**.
* **`L05` Dialog Konfirmasi Transaksi:** Ringkasan item, total, metode, uang diterima, kembalian, peringatan transaksi tidak dapat diubah/dibatalkan, tombol **Ya, Konfirmasi Transaksi** dan **Kembali dan periksa lagi**.
* **`L06` Transaksi Berhasil:** Ikon sukses, kode `TRX-xxxx`, total & metode, daftar **Stok bahan berkurang otomatis** (terpakai & sisa), keterangan transaksi terkunci, tombol **Transaksi Baru** dan **Lihat Detail Transaksi**.

### Modul M4 & M5 — Riwayat & Notifikasi (`L07–L09`)

* **`L07` Riwayat (Activity History):** Banner *"Hanya baca: catatan tidak bisa diubah atau dihapus"*; chip filter jenis (**Semua**, **Penjualan**, **Stok**, **Pesanan**, **Biaya**), filter tanggal/pengguna, sorting terbaru di atas, pagination. Item penjualan bisa diketuk → `L08`. Tanpa tombol ubah/hapus.
* **`L08` Detail Transaksi:** Hanya-baca. Menampilkan kode `TRX-xxxx`, waktu WITA, karyawan, daftar pesanan, total, metode, diterima, kembalian, rincian bahan terpakai sesuai resep, catatan terkunci.
* **`L09` Notifikasi:** Daftar peringatan stok menipis (sisa & batas minimum) serta status pesanan; tombol **Tandai dibaca**; terintegrasi push notification FCM.

### Modul M6 — Inventori & Pemeriksaan Fisik (`L10–L16`)

* **`L10` Inventori:** Pencarian + chip filter **Semua / Aman / Menipis / Habis** dengan jumlah; kartu bahan (contoh: Bubuk Strawberry, Bubuk Kopi, Susu, Cup) menampilkan foto, nama, stok fisik, status chip, batas minimum; FAB **Tambah Bahan** → `L13`.
* **`L11` Detail Bahan:** Foto bahan, stok saat ini, harga kemasan & per satuan, batas minimum, jenis pengadaan, riwayat pergerakan stok, tombol **Edit** (`L14`) dan **Tambah Stok** (`L12`, untuk bahan *Beli langsung*). **Tidak ada tombol Kurangi Stok di layar ini.**
* **`L12` Tambah Stok (Beli Langsung):** Tempat beli (opsional), tanggal & jam, jumlah kemasan, harga per kemasan, pratinjau pertambahan stok & stok jadi, catatan tidak dihitung sebagai pengeluaran (memperbarui harga bahan/HPP).
* **`L13` Tambah Bahan:** Picker foto (kamera/galeri), nama, satuan pembelian, satuan penggunaan, isi per kemasan, harga kemasan, **batas minimum dalam satuan pembelian** (mis. `3 kotak`, dengan konversi ke `ml`/`g` di bawahnya), cara pengadaan (*Dipasok pusat* / *Beli langsung*), stok awal + pratinjau konversi.
* **`L14` Edit Bahan:** Mengubah atribut bahan tanpa mengubah stok (keterangan: gunakan *Tambah Stok* atau *Pemeriksaan Fisik*).
* **`L15` Pemeriksaan Fisik (Satu-satunya jalan mengurangi stok manual):** Per bahan tampil stok sistem, stepper hitung **kemasan utuh** (kotak/pak/bungkus) + toggle **"Ada kemasan terbuka?"** (tanpa input ml/gram manual), badge **Sesuai / Selisih** otomatis, alasan (default *"Selisih pemeriksaan fisik"*) & deskripsi opsional.
* **`L16` Dialog Sesuaikan Stok:** Daftar bahan yang selisih saja (dari → menjadi, selisih), alasan, peringatan permanen, tombol pill merah **Sesuaikan Stok** + **Kembali**.

### Modul M7 — Pesanan Stok & Lead Time (`L17–L22`)

* **`L17` Lacak Pesanan Stok (Layout Timeline Vertikal):**
  * Header/notifikasi tetap di atas.
  * Kartu statistik **Lead Time** (Rata-rata, Tercepat, Terlama dari pesanan diterima).
  * Kartu **Sedang Dalam Perjalanan** (`PSN-xxx`) memakai **timeline vertikal** (*Dipesan → Dalam perjalanan → Diterima*) dengan ikon status di sisi kanan, lama berjalan dibanding rata-rata, dan tombol **Konfirmasi Penerimaan** (`L18`).
  * Seksi **Riwayat Terbaru** (kode, tanggal terima, lead time).
  * Tombol **Buat Pesanan** (`L19`) tetap di kanan bawah.
* **`L18` Konfirmasi Penerimaan:** Detail `PSN-xxx`, picker tanggal & jam terima (tidak boleh < tanggal pesan), durasi pengiriman otomatis (mis. `2 hari 26 menit`), checkbox wajib kesesuaian, tombol **Konfirmasi Barang Sampai**.
* **`L19` Buat Pesanan:** Khusus bahan yang **dipasok pusat** (Bubuk Kopi, Bubuk Strawberry, Cup). Pemasok default *"Pusat Pusa Coffee"*, tanggal/jam pesan, stepper jumlah kemasan + **Tambah Bahan**, estimasi total, tombol **Periksa & Simpan**.
* **`L20` Dialog Periksa Pesanan:** Rincian item, harga kemasan, total, catatan stok bertambah setelah dikonfirmasi terima, tombol **Ya, Simpan Pesanan**.
* **`L21` Pesanan Dibuat:** Layar sukses `PSN-xxx`, tombol **Kirim ke WhatsApp** (`L22`) dan **Nanti Saja**.
* **`L22` Pesan WhatsApp:** Pratinjau teks template pesanan otomatis (**tanpa harga**), tombol **Buka di WhatsApp** lewat Android Intent (pilih tujuan manual di WA; wajib fallback chooser bila WA tidak terpasang).

### Modul M8, M9 & M10 — Pengeluaran, Rekap Keuangan & Laporan (`L23–L28`)

* **`L23` Rekap Keuangan:** Filter **Harian / Mingguan / Bulanan** + tanggal acuan; kartu hero **Estimasi Laba Bersih** (nominal + %); grafik penjualan per hari (rata-rata & hari tertinggi); rincian Penjualan, HPP, Laba Kotor, Pengeluaran Operasional; rincian metode bayar (Tunai vs QRIS); informasi Saldo Kas Awal & perubahan kas terpisah dari laba; tombol cepat **Catat Pengeluaran** (`L24`).
* **`L24` Catat Pengeluaran:** Segmented metode bayar **Tunai / QRIS** (satu metode per catatan), tanggal & jam, beberapa baris sekaligus (dropdown kategori, nominal Rp, keterangan opsional) + tombol **Tambah Baris**, total otomatis, tombol **Periksa & Simpan**.
* **`L25` Dialog Konfirmasi Pengeluaran:** Metode, waktu, rincian baris, total, peringatan catatan terkunci, tombol **Ya, Simpan Pengeluaran**.
* **`L26` Laporan dan Ekspor:** Pilihan periode (**Hari ini / 7 hari / Bulan ini / Kustom**) + rentang tanggal; checklist 8 pilihan sheet (bawaan tercentang: *Penjualan produk*, *Penggunaan bahan baku*, *Lead time pengadaan*, *Rekap keuangan*); tombol **Buat Laporan (.xlsx)**.
* **`L27` Ekspor Selesai:** Info nama file (`Laporan_PUSATRACK_...xlsx`), jumlah sheet, ukuran file, tombol **Simpan** dan **Bagikan** (`L28`).
* **`L28` Android Sharesheet:** Intent `ACTION_SEND` menggunakan `FileProvider` untuk berbagi `.xlsx` ke WhatsApp, Gmail, Drive, dll.

### Modul M11 — Produk & Resep (`L29–L32`)

* **`L29` Produk dan Resep:** Daftar kartu produk (nama, harga jual, perkiraan HPP, margin Rp & %, status aktif); FAB **Tambah Produk** (`L32`). Ketuk baris → `L30`.
  * *Data uji acuan:* **Kopi Susu** (`18 g` bubuk kopi + `150 ml` susu + `1` cup → HPP `Rp 5.676`); **Strawberry** (`15 g` bubuk strawberry + `150 ml` susu + `1` cup → HPP `Rp 6.066`).
* **`L30` Detail Resep (Hanya Baca):** Harga jual, perkiraan HPP, margin, status aktif, komposisi bahan per 1 porsi (read-only), tombol **Edit Resep** (`L31`).
* **`L31` Edit Resep:** Ubah nama, harga jual, switch **Aktif**, tambah/ubah/hapus komposisi bahan; pratinjau HPP & margin berubah real-time; tombol **Simpan Perubahan**.
* **`L32` Tambah Produk:** Form produk baru + komposisi resep (tanpa bahan = HPP `Rp 0`), tombol **Simpan Produk**.

### Modul M12 — Manajemen Akun (`L33–L39`) *(Baru di v7)*

> Akses dari **Beranda (`L02`) → Menu Lainnya → Akun**. Seluruh karyawan memiliki akses setara untuk mengelola akun tim sesuai keputusan PRD, dengan verifikasi password untuk aksi sensitif.

* **`L33` Manajemen Akun:** Daftar akun karyawan (`full_name`, `username`, badge status **Aktif / Nonaktif**), info jumlah akun aktif, tombol/FAB **Tambah Akun** (`L36`). Ketuk baris akun → `L34`.
* **`L34` Detail Akun:** Profil lengkap karyawan (`full_name`, `username`, tanggal dibuat `created_at`, status aktif/nonaktif), tombol aksi **Edit Akun** (`L35`), **Reset Password** (`L37`), dan tombol **Nonaktifkan / Aktifkan Akun** (`L39`).
* **`L35` Edit Akun:** Form ubah `full_name` dan/atau `username` dengan kolom isian tonal, tombol pill hijau **Simpan Perubahan** (dapat memicu `L38` verifikasi password jika diperlukan).
* **`L36` Tambah Akun:** Form pembuatan akun karyawan baru (`full_name`, `username`, `password` awal + konfirmasi password dengan toggle lihat/sembunyi), tombol pill hijau **Simpan Akun**.
* **`L37` Reset Password:** Form atur ulang password baru + konfirmasi password (dengan toggle lihat/sembunyi) untuk akun terpilih; memicu `L38` Dialog Verifikasi Password sebelum disimpan agar tidak disalahgunakan.
* **`L38` Dialog Verifikasi Password:** Dialog konfirmasi keamanan yang meminta karyawan yang sedang login memasukkan **password-nya sendiri** sebelum mengeksekusi perubahan sensitif (seperti reset password atau nonaktifkan akun). Tombol: **Verifikasi & Lanjutkan** (hijau) dan **Batal** (tonal).
* **`L39` Dialog Nonaktifkan Akun:** Dialog peringatan bahwa akun yang dinonaktifkan (`is_active = false`) tidak dapat lagi login ke aplikasi, namun seluruh catatan riwayat aktivitas sebelumnya tetap tersimpan (tidak dihapus). Tombol: **Ya, Nonaktifkan** (merah) dan **Batal** (tonal).

---

## 6. Panduan Implementasi Android & Integrasi Intent

* **Font & Ikon:** Bundel font `Plus Jakarta Sans` di `res/font/` dan gunakan *Vector Drawable* Material Symbols/Icons Rounded agar tajam dan ringan.
* **Bayangan Kartu (Borderless Shadow v6/v7):** Gunakan `MaterialCardView` dengan `app:strokeWidth="0dp"`, `app:cardElevation="6dp"`, dan atur `android:outlineSpotShadowColor` / `android:outlineAmbientShadowColor` ke warna gelap dengan alpha rendah (`#2414231B`) agar menyerupai drop shadow lembut Figma (blur 18, opasitas 7,5%) tanpa terlihat kaku.
* **Foto Bahan (`L10–L14`):** Gunakan `ActivityResultContracts.TakePicture` (dengan `FileProvider`) dan `ActivityResultContracts.PickVisualMedia`; muat gambar dengan **Glide** atau **Coil**.
* **WhatsApp Intent (`L22`):** Kirim teks melalui `Intent.ACTION_SEND` bertarget `com.whatsapp` dengan fallback `Intent.createChooser` bila WhatsApp tidak terpasang.
* **Ekspor & Sharesheet (`L27–L28`):** Unduh file `.xlsx` dari backend CI4 ke cache internal aplikasi, lalu bagikan melalui `FileProvider` (`res/xml/file_paths.xml`) ke Android Sharesheet.