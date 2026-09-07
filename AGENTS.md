# Panduan pengelolaan repo (untuk AI)

Baca file ini SEBELUM mengubah, memperbaiki, atau mengelola repository ini.

## Struktur repository

```
.
├── .github/workflows/build.yml       # CI: CHANGELOG guard → build → lint+test → release
├── .github/workflows/codeql.yml      # CodeQL: analisis keamanan statis Java/Kotlin
├── .github/workflows/gitleaks.yml    # Deteksi secret ter-commit
├── app/
│   ├── build.gradle.kts              # minSdk 21 / targetSdk 36, compileSdk 36
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/tasirin/browser/
│       │   ├── App.kt                # Application singleton
│       │   ├── MainActivity.kt       # WebView + bookmarks + cursor mode
│       │   ├── data/
│       │   │   ├── Bookmark.kt       # Model bookmark
│       │ │   └── BookmarkRepository.kt  # Persistensi bookmark (SharedPreferences)
│       │   └── ui/
│       │       ├── BookmarkAdapter.kt   # RecyclerView adapter bookmarks
│       │       └── CursorController.kt  # Cursor on-screen (D-pad / tombol)
│       └── res/
│           ├── layout/activity_main.xml
│           ├── layout/item_bookmark.xml
│           ├── layout/dialog_add_bookmark.xml
│           └── values/strings.xml + styles.xml
├── docs/index.html                   # WEBSITE GitHub Pages
├── CHANGELOG.md
├── README.md / README.en.md
└── gradle/libs.versions.toml
```

## Arsitektur ringkas

- **MainActivity** = WebView + halaman bookmark + toolbar (back, forward, URL input,
  cursor mode toggle, bookmark star, menu).
- **CursorController** = overlay on-screen + tombol D-pad untuk navigasi tanpa sentuh.
- **BookmarkRepository** = persistensi JSON via SharedPreferences.

## Aturan pengembangan

1. **Build resmi via CI** — jangan build lokal untuk rilis.
2. **UI Bahasa Inggris** (default `values/strings.xml`). Komentar & commit Bahasa Indonesia.
3. **Gaya commit**: `type(scope): deskripsi` — feat, fix, ui, perf, refactor, docs, chore.
4. **Jangan ubah `versionName`/`versionCode` manual** — CI yang mengatur.
5. **minSdk 21** — semua fitur harus punya fallback Android 5.
6. **targetSdk 36** / compileSdk 36.
7. **Changelog wajib** untuk perubahan `app/src/main`.
8. **Lint & unit test hijau** sebelum merge.
9. **Secrets**: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
10. **Keystore fingerprint**: `c2785a618082683755eeae867e0a2e01f450b1fd448859d1ec21cf854c5713d1`.
