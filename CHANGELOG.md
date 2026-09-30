# Changelog

All notable changes to this project will be documented in this file.

## v1.0.3 (10031)
September 30, 2026

### Bug Fixes & Optimizations
- **Release Mode Crashes Fixed**: 
  - Fixed Google Sign-In `10 (DEVELOPER_ERROR)` by preserving the Web Client ID string from the resource shrinker.
  - Fixed Firestore crash (`Could not deserialize object`) by ensuring model classes are retained during R8 obfuscation.
  - Fixed ML Kit Code Scanner `NullPointerException` during Dashboard initialization by keeping internal package boundaries safe from aggressive R8 optimizations.

## v1.0.2 (10021)
September 30, 2026

### Key Improvements & Polish
- **Dashboard & Books**: Polished empty dashboard state with onboarding actions, added quick "Create Book" and "Join Book" buttons in the Books tab, and streamlined header layout.
- **Google Sign-In & Onboarding**: Enhanced Google sign-in failure diagnostics with status code details and added a styled Google user name prompt dialog.
- **Build & Optimization**: Enabled release code shrinking and resource minification, and updated Android Gradle Plugin.

## v1.0.1 (10011)
September 28, 2026

### Key Features
- **Google Sign-In**: Integrated secure authentication using Firebase Auth.
- **Group Sessions**: Added functionality to create or join expense-sharing sessions using unique invite codes or QR code scanning.
- **Expense Tracking**: Implemented adding transactions, specifying who paid and how the expense is split among members.
- **Debt Calculation**: Implemented an automated balance calculator to determine who owes whom.
- **Visual Insights**: Integrated Vico Charts for displaying interactive expense charts and data visualization.
- **Modern UI**: Designed with Material Design 3, built entirely using Jetpack Compose, and enhanced with Lottie animations.
- **Cloud Syncing**: Added Firebase Firestore for real-time syncing of sessions and transactions.

### Bug Fixes
- **General Stability**: Addressed minor layout issues and improved visual consistency in Jetpack Compose UI.



