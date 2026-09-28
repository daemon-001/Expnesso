# 💸 Expnesso

<p align="center">
  <img src="icon.png" alt="Expnesso Logo" width="200"/>
</p>

Expnesso is a modern Android application built with Kotlin and Jetpack Compose for managing and sharing group expenses effortlessly. It helps you track shared expenses, calculate who owes whom, and keep your group finances organized.

## ✨ Features

- **Google Sign-In**: Secure and fast authentication using Firebase Auth.
- **Group Sessions**: Create or join expense-sharing sessions (groups) with a unique invite code or QR code scanning.
- **Expense Tracking**: Add transactions, specifying who paid and how the expense is split among members.
- **Debt Calculation**: Automatically calculates balances to determine who owes whom.
- **Beautiful UI**: Designed with Material Design 3 and built entirely using Jetpack Compose.
- **Visual Insights**: View your expenses using interactive charts.
- **Animations**: Engaging UI animations using Lottie.

## 📸 Screenshots

<p align="center">
  <img src="visuals/txz.png" width="100%"/>
</p>

## 🛠 Tech Stack & Libraries

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
- **Coroutines & Flow**: For asynchronous programming and reactive data streams.
- **Version Catalog**: Gradle dependency management using `libs.versions.toml`.

## 🏗 Architecture & Project Structure

The project follows a **Clean Architecture** approach combined with the **MVVM (Model-View-ViewModel)** design pattern to ensure separation of concerns, scalability, and testability.

- `app/src/main/java/com/daemon/expnesso/`:
  - `data/`: Contains data models (`User`, `Session`, `Transaction`), repositories (`FirestoreRepository`), and local data sources.
  - `domain/`: Contains business logic and use cases.
  - `ui/`: Contains Jetpack Compose screens categorized by features (dashboard, expense, login, session, etc.) and the theme definitions.
  - `navigation/`: Handles app-wide routing and screen transitions using Compose Navigation.
  - `utils/`: Utility classes, helper functions, and Kotlin extensions.
  - `di/`: Dependency Injection setup for providing repositories, use cases, and external dependencies.

## 🚀 Getting Started

### Prerequisites

- **Android Studio**: Latest recommended version.
- **Java**: JDK 11 or higher.
- **Firebase Account**: To set up backend services.

### Setup Instructions

1. **Clone the repository:**
   ```bash
   git clone <repository-url>
   ```

2. **Open in Android Studio:** 
   Launch Android Studio and select `Open an existing project`. Navigate to the cloned `Expnesso` folder.

3. **Firebase Configuration:**
   - Go to the [Firebase Console](https://console.firebase.google.com/).
   - Create a new Android project or use an existing one.
   - Register your app with the application ID `com.daemon.expnesso`.
   - Download the `google-services.json` file.
   - Place the `google-services.json` file in the `app/` directory of the project.
   - Enable **Google Sign-In** in the Firebase Authentication settings.
   - Enable **Firestore Database** in test mode or with appropriate security rules.
   - *For a detailed guide, please refer to [firebase_setup.md](firebase_setup.md) included in this repository.*

4. **Build and Run:**
   - Sync the Gradle files (`Sync Project with Gradle Files`).
   - Run the application on an emulator or a physical device running Android 9.0 (API level 28) or higher.

## 🤝 Contribution

Contributions are always welcome! If you'd like to improve the app, please feel free to fork the repository, make your changes, and submit a pull request. 

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is open-source and available under the [MIT License](LICENSE).
