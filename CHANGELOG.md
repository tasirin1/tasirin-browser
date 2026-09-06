## [Unreleased]
- **Fix: gunakan `kotlin { compilerOptions }` DSL** -- AGP 9 dengan KGP 2.x (built-in Kotlin) menghapus blok `kotlinOptions`; gunakan `compilerOptions` di blok `kotlin` tingkat atas.
- **Fix: `getLocationOnScreen` pakai `IntArray`** -- Tipe array tidak cocok (FloatArray vs IntArray) untuk `View.getLocationOnScreen()` — parameter wajib IntArray.
- **Initial release** — Browser Android ringan berbasis WebView dengan halaman bookmark utama, mode cursor on-screen untuk navigasi tanpa sentuh (D-pad / tombol), address bar dengan back/forward, bookmark management (tambah/hapus), dan menu (share, refresh, clear cache). minSdk 21, targetSdk 36.
