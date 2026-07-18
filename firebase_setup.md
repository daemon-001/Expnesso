# Expnesso - Complete Firebase Setup Guide

This guide will walk you through the entire process of setting up Firebase for Expnesso from start to finish, ensuring Google Sign-In and Cloud Firestore work perfectly together.

> [!WARNING]
> **Database Wipe Required**
> If you are updating from a previous version of Expnesso, the database structure has fundamentally changed to support advanced bill-splitting and user profiles. 
> You MUST go to the Firebase Console -> Firestore Database, and delete the `sessions` and `transactions` collections manually before running this app to prevent crashes from old data structures.

## 1. Get Your SHA-1 Fingerprint
For Google Sign-In to work on Android, Google needs to verify your app's signature. 
During development, your computer signs the app using a debug key. We extracted this earlier.

* **Your Debug SHA-1 Key:** `D6:15:C7:45:D4:50:6C:8A:FC:E7:B8:E7:42:46:F7:C3:57:6D:**:**`

*(Note: When you are ready to publish to the Play Store, you will need to add the Play Store's release SHA-1 key as well).*

## 2. Configure Firebase Android App
1. Go to the [Firebase Console](https://console.firebase.google.com).
2. Open your project (`expnesso`).
3. Click the **Gear Icon ⚙️** next to "Project Overview" and select **Project settings**.
4. Scroll down to the **"Your apps"** section. 
   * *If you haven't added an app yet, click the Android icon to add one with the package name `com.daemon.expnesso`.*
5. Under your Android app, click **Add fingerprint**.
6. Paste the SHA-1 key from Step 1 and click **Save**.

## 3. Enable Google Sign-In (Authentication)
1. In the left menu under **Build**, click on **Authentication**.
2. Click the **Get Started** button (if you haven't enabled it yet).
3. Go to the **Sign-in method** tab.
4. Click **Add new provider** and select **Google**.
5. Toggle the **Enable** switch.
6. Provide a project support email (select your email from the dropdown).
7. Click **Save**.

## 4. Setup Cloud Firestore (Database)
1. In the left menu under **Build**, click on **Firestore Database**.
2. Click **Create database**.
3. Choose a physical location for your database and click **Next**.
4. Select **Start in test mode** (or production mode) and click **Create**.
5. Once the database dashboard loads, click on the **Rules** tab.
6. Replace all the code in the editor with the following rules to ensure only logged-in users can access data:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      // Only allow read and write if the user is authenticated
      allow read, write: if request.auth != null;
    }
  }
}
```
7. Click **Publish**.

## 5. Download and Place google-services.json
This file acts as the bridge between your app and your Firebase project. It contains all API keys and Web Client IDs (which are generated when you enabled Google Sign In).

1. Go back to **Project settings** (⚙️).
2. Scroll down to your Android app and click the **google-services.json** download button.
3. Move the downloaded file into your Android project. It **must** be placed in the `app/` directory (e.g., `Expnesso/app/google-services.json`).
4. *Important: If you are replacing an old file, verify the new file has data inside the `"oauth_client"` array. If it's empty, you missed step 3.*

## 6. Run the App
1. Clean your project in Android Studio (`Build > Clean Project`).
2. Run the app (`Shift + F10`).
3. Google Sign-In, profile syncing, and split-expense book creation will now work smoothly!
