## [Unreleased]
- **Fix: gunakan `kotlin { compilerOptions }` DSL** -- AGP 9 dengan KGP 2.x (built-in Kotlin) menghapus blok `kotlinOptions`; gunakan `compilerOptions` di blok `kotlin` tingkat atas.
- **Fix: `getLocationOnScreen` pakai `IntArray`** -- Tipe array tidak cocok (FloatArray vs IntArray) untuk `View.getLocationOnScreen()` — parameter wajib IntArray.
- **Fix: uji normalizeUrl menerima spasi literal** -- Assertion test sebelumnya hanya mencari encoded (`+` / `%20`), padahal helper tidak memanggil `Uri.encode`.
- **Fix: gunakan `OnBackPressedCallback` alih-alih `onKeyDown(KEYCODE_BACK)`** -- targetSdk 36 memaksa predictive back; lint gagal pada `onKeyDown` untuk tombol back. Pindah ke `OnBackPressedDispatcher` dengan callback, sedangkan `onKeyDown` hanya menangani D-pad/cursor mode.
- **Initial release** — Browser Android ringan berbasis WebView dengan halaman bookmark utama, mode cursor on-screen untuk navigasi tanpa sentuh (D-pad / tombol), address bar dengan back/forward, bookmark management (tambah/hapus), dan menu (share, refresh, clear cache). minSdk 21, targetSdk 36.
