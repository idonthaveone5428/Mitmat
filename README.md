<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# Mitmat (মিটমাট) — Split bills. Stay even.

A modern, offline-first Android expense-splitting application built with Kotlin and Jetpack Compose. Designed for friends, flatmates, and travel groups to easily track expenses, split bills (equally, by percentage, or custom exact shares), and simplify settlements.

### Key Features
- **Group Expense Management:** Create groups with custom currencies and pastel accent themes.
- **Flexible Splitting:** Supports equal splits, exact amount splits, percentages, and multiple payers per bill.
- **Smart Debt Simplification:** Computes minimum required cash transactions so everyone stays even with fewest transfers.
- **Settlement Tracking:** Record partial and full settlements between participants.
- **Activity Feed & Statistics:** Real-time chronological activity feed and financial dashboard.
- **Theme & Appearance:** Pastel Material 3 surfaces, Ash dark mode, and AMOLED true-black theme options.
- **Local Data Persistence & Backups:** Offline Room database with JSON export/import backup capabilities.

## Run Locally

**Prerequisites:**  [Android Studio](https://developer.android.com/studio)


1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Allow Android Studio to fix any incompatibilities as it imports the project.
4. Create a file named `.env` in the project directory and set `GEMINI_API_KEY` in that file to your Gemini API key (see `.env.example` for an example)
5. Remove this line from the app's `build.gradle.kts` file: `signingConfig = signingConfigs.getByName("debugConfig")`
6. Run the app on an emulator or physical device
7. If you have already published your app in AI Studio, please [request upload key reset](https://support.google.com/googleplay/android-developer/answer/9842756#zippy=%2Crequest-an-upload-key-reset) in Google Play Console.
