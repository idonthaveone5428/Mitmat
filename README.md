

# Mitmat (মিটমাট) — Split bills. Stay even.

A modern, offline-first Android expense-splitting application built with Kotlin and Jetpack Compose. Designed for friends, flatmates, and travel groups to easily track expenses, split bills (equally, by percentage, or custom exact shares), and simplify settlements.

## Automated Builds (GitHub Actions)

This repository includes a GitHub Actions workflow in `.github/workflows/build.yml` that automatically builds and tests the app:

1. **Automatic Builds**: Runs unit tests and builds the APK automatically on every push or pull request to `main` / `master`.
2. **Manual Builds**: Go to the **Actions** tab on GitHub, select **Build Mitmat Android App**, click **Run workflow**, and choose between:
   - `debug` (Builds installable APK)
   - `bundle` (Builds Android App Bundle `.aab`)
3. **Download APK**: Once the workflow finishes, scroll down to the **Artifacts** section on the workflow run page to download `mitmat-debug-apk`.

## Features
- **Group Expense Management:** Custom group currencies and color themes.
- **Selective Backups:** Choose specific groups or all groups to export/import via JSON backup.
- **Trash Bin & Archive:** 7-day auto-delete trash bin with instant restore or permanent delete, plus read-only archived groups.
- **AMOLED True-Black & Themes:** Ash dark background, cream light mode, and true black AMOLED mode.
- **Smart Settlements:** Computes minimum required transfers to settle balances.