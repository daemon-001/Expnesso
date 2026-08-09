# Expnesso

Expnesso is a modern Android application built with Kotlin and Jetpack Compose for managing and sharing group expenses effortlessly. It helps you track shared expenses, calculate who owes whom, and keep your group finances organized.

## Features

- **Google Sign-In**: Secure and fast authentication using Firebase Auth.
- **Group Sessions**: Create or join expense-sharing sessions (groups) with a unique invite code or QR code scanning.
- **Expense Tracking**: Add transactions, specifying who paid and how the expense is split among members.
- **Debt Calculation**: Automatically calculates balances to determine who owes whom.
- **Beautiful UI**: Designed with Material Design 3 and built entirely using Jetpack Compose.
- **Visual Insights**: View your expenses using interactive charts.
- **Animations**: Engaging UI animations using Lottie.

## Tech Stack & Libraries

- **Kotlin**: Primary programming language.
- **Jetpack Compose**: Modern toolkit for building native Android UI.
- **Navigation Compose**: Type-safe routing and navigation.
- **Firebase**:
  - **Authentication**: For Google Sign-In.
  - **Firestore**: Real-time NoSQL cloud database for syncing sessions and transactions.
  - **Analytics**: To measure user interactions.
- **Coil**: For loading and displaying images efficiently.
- **Lottie Compose**: For rendering After Effects animations natively.
- **Vico Charts**: For displaying expense charts and data visualization.
- **Play Services Code Scanner & ZXing**: For QR code scanning to join sessions easily.
- **Coroutines**: For asynchronous programming.

## Project Structure

- `data`: Contains data models (`User`, `Session`, `Transaction`), repositories (`FirestoreRepository`), and local data sources.
- `domain`: Contains business logic and use cases.
- `ui`: Contains Jetpack Compose screens categorized by features (dashboard, expense, login, session, etc.) and the theme definitions.
- `utils`: Utility classes and extensions.
- `di`: Dependency Injection setup.

## Getting Started

### Prerequisites

- Android Studio (Latest version recommended)
- Java 11 or higher
- A Firebase project

### Setup Instructions

1. **Clone the repository:**
   ```bash
   git clone <repository-url>
   ```
2. **Open in Android Studio:** Open the `Expnesso` folder in Android Studio.
3. **Firebase Configuration:**
   - Go to the [Firebase Console](https://console.firebase.google.com/).
   - Create a new Android project or use an existing one.
   - Register your app with the application ID `com.daemon.expnesso`.
   - Download the `google-services.json` file.
   - Place the `google-services.json` file in the `app/` directory of the project.
   - Enable **Google Sign-In** in the Firebase Authentication settings.
   - Enable **Firestore Database** in test mode or with appropriate security rules.
4. **Build and Run:** Sync the Gradle files and run the application on an emulator or physical device running Android 9.0 (API level 28) or higher.
