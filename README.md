# Lexicon

Lexicon is a minimalist Android app for intermediate-to-advanced learners of American English. It helps learners build practical vocabulary through short, contextual lessons focused on modern usage.

The current app version is **3.0** (version code **3**).

## Features

- Daily slang, idioms, phrasal verbs, and workplace expressions
- Example dialogues, etymology, and US English audio pronunciation
- Interactive quizzes, practice sessions, and personalized learning goals
- Adaptive learning paths and phrase lookup powered by Firebase AI Logic

## Tech stack

- Kotlin and Jetpack Compose
- Firebase Authentication and Cloud Firestore
- Firebase AI Logic and Firebase App Check

## Getting started

### Requirements

- Android Studio with Android SDK 36.1.1
- JDK and Gradle versions compatible with the Android Gradle Plugin 9.1.1 (the project uses Gradle 9.3.1)
- A Firebase project for the cloud-backed features

### Configure Firebase

1. Register an Android app in your Firebase project with application ID `com.aistudio.lexicon.k4m9qx`.
2. Download its `google-services.json` and place it in `app/google-services.json`.
3. Enable Firebase Authentication, Cloud Firestore, Firebase AI Logic, and App Check in the Firebase console.
4. Configure App Check for your environment. Debug builds use the Firebase debug provider; production builds expect a string resource named `recaptcha_site_key` with your reCAPTCHA site key.

Gemini requests use Firebase AI Logic with App Check; no Gemini API key is required in `.env` (see [`.env.example`](.env.example)). Keep project-specific configuration and credentials out of source control.

### Build and run

Open the repository in Android Studio, allow Gradle sync to finish, then run the `app` configuration on an emulator or Android device (Android 7.0 / API 24 or newer).

The repository contains Gradle wrapper configuration but not the wrapper scripts. If you have Gradle 9.3.1 installed locally, you can build and run unit tests from the project root:

```sh
gradle :app:assembleDebug
gradle :app:testDebugUnitTest
```

## Project layout

- `app/src/main/java/com/example/ui/` — Compose screens, components, and theme
- `app/src/main/java/com/example/domain/` — learning-path and quiz logic
- `app/src/main/java/com/example/data/` — phrase catalog, audio, and Firebase data access
- `firestore.rules` — Firestore security rules
- `firestore.test.js` — Firestore rules tests
