# Echo-Ease 🏢🔊

**Echo-Ease** is a modern, decentralized noise complaint management and verification platform built for hostels, dormitories, and shared apartment complexes. It eliminates the friction of traditional noise disputes by allowing residents to anonymously report noise disturbances with verifiable audio evidence, utilizing a consensus-driven backend to automatically validate and escalate recurring issues.

## 🌟 Key Features

*   **Real-Time Decibel Visualizer:** A stunning, liquid-smooth, and accurate live audio waveform visualizer built natively in Jetpack Compose that reacts dynamically to ambient room noise, calibrated specifically for mobile microphone hardware.
*   **Audio Proof Attachments:** Users can record and securely upload short 5-second audio snippets directly to the cloud (Supabase Storage) to serve as verifiable proof of severe noise disturbances.
*   **Consensus Verification Logic:** Instead of immediately penalizing a room for a single complaint, the system requires a *consensus*. Multiple flags from adjacent rooms within a specific time window are required to automatically upgrade a "Flag" into a "Confirmed Incident."
*   **Automated Warden Escalation:** Repeated confirmed incidents for a single room over time will automatically flag the room for Administrative or Warden intervention, keeping management out of minor squabbles while highlighting serious offenders.
*   **Dual-Role Architecture:**
    *   **Resident Portal:** Submit flags, view your personal flagging history (and whether they were confirmed), and track the status of your complaints.
    *   **Admin Dashboard:** Oversee all building incidents, listen to audio proofs directly from the dashboard, and manage escalated rooms.
*   **Built with Modern Tech:** 100% Kotlin, Jetpack Compose UI, Coroutines/Flow for asynchronous data, and Supabase PostgREST for backend database communication.

## 🛠 Tech Stack

*   **Language:** Kotlin
*   **UI Toolkit:** Jetpack Compose (Material Design 3)
*   **Asynchronous:** Kotlin Coroutines & StateFlow
*   **Backend / Database:** [Supabase](https://supabase.com) (PostgreSQL)
*   **Storage:** Supabase Buckets (for `.m4a` audio evidence files)
*   **Authentication:** Simulated (Simple UID-based login for demonstration)

## 🚀 Getting Started

### Prerequisites
*   Android Studio (Iguana or newer recommended)
*   A Supabase Project

### Supabase Setup
1. Create a new Supabase project.
2. Navigate to your project settings and locate your `URL` and `Anon Key`.
3. Update the `SupabaseClient.kt` file in the Android project with your credentials.
4. **Database Schema:** Execute the provided `schema.sql` file in your Supabase SQL Editor. This will automatically generate the required tables (`building_config`, `rooms`, `users`, `flags`, `confirmed_incidents`) and set up the necessary Row Level Security (RLS) policies.
5. **Storage Bucket:** Ensure an audio storage bucket named `audio_proofs` is created in Supabase with public read access and authenticated insert access.

### Running the App
1. Clone the repository.
2. Open the project in Android Studio.
3. Sync Gradle.
4. Build and run on a physical device (highly recommended over an emulator for accurate microphone hardware access).

## 📱 Screenshots

*(Add your screenshots here)*
*   **Home Screen / Decibel Meter**
*   **Incident History**
*   **Admin Dashboard**

## 🏗 Architecture & Design Decisions

*   **Real-time Audio Processing:** The app uses Android's raw `AudioRecord` API reading in micro-chunks of 1024 frames (yielding ~43 FPS) to calculate Root Mean Square (RMS) decibel data natively, avoiding third-party audio visualizer libraries.
*   **Data Models:** The repository strictly maps network data classes via `kotlinx.serialization` to isolate backend schema changes from the UI State.
*   **Simulated Backend:** To remain fully serverless, the "Consensus Logic" is triggered client-side by the `RoomRepository` immediately after a flag is pushed, rather than relying on an external Node/Python server or Supabase Edge Functions.

## 📜 License

This project is licensed under the MIT License - see the LICENSE file for details.
