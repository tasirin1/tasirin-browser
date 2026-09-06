#!/usr/bin/env python3
"""Guard cepat struktur repo + whitespace untuk Tasirin Browser."""
import os, re, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
errors = []

def ok(msg):
    print(f"[*] {msg}: OK")

def check_required(path):
    if not os.path.exists(os.path.join(ROOT, path)):
        errors.append(f"File wajib tidak ada: {path}")

# 1) Struktur penting
for p in ["app/src/main/AndroidManifest.xml",
          "app/src/main/java/com/tasirin/browser/MainActivity.kt",
          "app/src/main/java/com/tasirin/browser/ui/CursorController.kt",
          "app/src/main/java/com/tasirin/browser/data/BookmarkRepository.kt",
          "app/build.gradle.kts", "gradle/wrapper/gradle-wrapper.jar",
          "gradlew", "CHANGELOG.md", "README.md", "docs/index.html"]:
    check_required(p)

# 2) Bahasa wajib Inggris di string resource
res_dir = os.path.join(ROOT, "app/src/main/res/values")
for fn in ("strings.xml", "styles.xml", "colors.xml"):
    f = os.path.join(res_dir, fn)
    if os.path.exists(f):
        content = open(f, encoding="utf-8").read().lower()
        banned = ["beranda", "pengaturan", "tentang", "unduh", "berbagi", "tambah", "hapus", "cari", "buka", "batal", "keluar", "segarkan", "pesan", "tampilkan"]
        for w in banned:
            if w in content:
                errors.append(f"Kata bahasa Indonesia di resource {fn}: '{w}'")

# 3) Whitespace / trailing whitespace
for root_dir, _, files in os.walk(ROOT):
    if "/.git" in root_dir or "/build" in root_dir: continue
    for fn in files:
        if fn.endswith((".kt", ".kts", ".yml", ".yaml", ".py", ".xml", ".md", ".html", ".toml", ".properties")):
            p = os.path.join(root_dir, fn)
            try:
                lines = open(p, encoding="utf-8").read().splitlines()
                for i, line in enumerate(lines, 1):
                    if line.rstrip() != line:
                        errors.append(f"Trailing whitespace: {os.path.relpath(p, ROOT)}:{i}")
            except Exception:
                pass

if errors:
    for e in errors:
        print(f"[FAIL] {e}")
    print(f"HASIL: {len(errors)} problem")
    sys.exit(1)
print("HASIL: SEMUA SEHAT")
