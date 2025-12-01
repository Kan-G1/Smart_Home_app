# Smart Home Android App

This is a modern, feature-rich Android application designed for controlling and monitoring a variety of smart home devices. The app is built with Kotlin and leverages Firebase for backend services, providing a seamless and real-time user experience.

## Core Features

- **Robust User Authentication:** Secure sign-up and login functionality using Firebase Authentication.
- **Dynamic Dashboard:** A central hub that provides an at-a-glance summary of devices, real-time total power consumption, and quick navigation.
- **Device Control Grid:** A beautifully organized screen displaying all registered smart devices (like lights and cameras) in a grid, conveniently grouped by room.
- **Real-time Updates:** Utilizes Firebase Firestore to ensure all device states and information are updated in real-time across the app.
- **Power Usage Simulation:** A dynamic power meter on the dashboard that calculates and visualizes the total wattage of all active devices.
- **Geofencing:** Implements Google's Location services to define a home area, with a static map on the dashboard for visual confirmation.
- **Modern & Consistent UI:** A complete, professional user interface built with Material Design components, ensuring a clean and intuitive experience across all screens.
- **In-App Editing:** Features like "tap-to-edit" for the user's display name provide a modern and seamless user experience.

## Technologies Used

- **Language:** Kotlin
- **Architecture:** MVVM-like pattern
- **UI:** Android XML with Material Design Components (`CardView`, `RecyclerView`, `ConstraintLayout`, `TextInputLayout`, `FloatingActionButton`)
- **Backend:**
    - **Firebase Authentication:** For user management.
    - **Firebase Firestore:** As the real-time NoSQL database for user and device data.
- **Location & Maps:**
    - **Google Play Services for Location:** For Geofencing capabilities.
    - **Google Maps Static API:** To display a lightweight and dynamic map on the dashboard.
- **Asynchronous Programming:** Kotlin Coroutines for background tasks like network requests.

## Project Setup

To build and run this project, you will need to perform the following steps:

1.  **Clone the Repository:**
    ```bash
    git clone <your-repository-url>
    ```

2.  **Firebase Setup:**
    - Create a new project in the [Firebase Console](https://console.firebase.google.com/).
    - Add an Android app to your Firebase project with the package name `com.example.smarthomeapp`.
    - Download the generated `google-services.json` file and place it in the `app/` directory of this project.
    - In the Firebase Console, enable **Authentication** with the **Email/Password** sign-in method.
    - Enable the **Firestore Database** and accept the default security rules for now.

3.  **Google Maps API Key:**
    - In the [Google Cloud Console](https://console.cloud.google.com/), select the same project that Firebase created for you.
    - Go to **APIs & Services > Library** and ensure the following APIs are enabled:
        - **Maps Static API**
        - **Geocoding API**
    - Go to **APIs & Services > Credentials** and find the API key that was created for you (or create a new one).
    - Create a file named `local.properties` in the root directory of the project.
    - Add your API key to the `local.properties` file like this:
      ```properties
      MAPS_API_KEY=YOUR_API_KEY_HERE
      ```

4.  **Build and Run:**
    - Open the project in Android Studio.
    - Let Gradle sync the dependencies.
    - Build and run the app on an emulator or a physical device.

