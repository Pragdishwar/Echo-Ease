# Project Plan

EchoEase: A peer-to-peer, anonymous noise-mediation tool for shared living spaces. 
Core Requirements:
- Onboarding & Room Setup: Firebase Auth, room selection from Firestore list, room-adjacency map in Firestore.
- Silent Flag: Single button "Flag Noise Now". Firestore write { flagerRoomId, timestamp, timeWindow }. Rate-limiting.
- Consensus Engine (Cloud Function): Triggered every X mins. Groups flags by time window. Requires flags from non-adjacent rooms to target a source room. Writes "confirmed incident" doc.
- Gentle Nudge: FCM notification to target room.
- Escalation Ladder: Track confirmed incidents (30-day rolling). Messaging tone escalates. Option to notify warden for complainants.
- Anti-Abuse: Non-adjacency rule and rate limiting enforced server-side.
- Optional: Ambient sound check (decibel only).
Architecture: MVVM, Compose, Firebase, Material Design 3, Edge-to-Edge.
Note: No PII in UI for flags. Cloud functions handle all sensitive logic.

## Project Brief

# EchoEase Project Brief

EchoEase is an anonymous, peer-to-peer noise-mediation platform designed for shared living environments like hostels and apartments. It empowers residents to maintain a peaceful atmosphere through a fair, consensus-based flagging system that discourages noise disturbances without direct confrontation.

### Features
* **Anonymous Noise Flagging**: A simple, one-tap interface for residents to report noise disturbances without revealing their identity to neighbors.
* **Smart Consensus Notifications**: Receives "gentle" real-time alerts when the server-side engine identifies the user's room as a noise source based on multi-neighbor verification.
* **Escalation Ladder Dashboard**: A visual tracker that shows the current "strike" level for the room, informing users of their standing and potential consequences for repeated incidents.
* **Anti-Abuse Verification**: Integrated safeguards to prevent malicious flagging, ensuring notifications are only sent when legitimate noise patterns are detected by non-adjacent neighbors.

### High-Level Tech Stack
* **Kotlin**: The primary language for robust and expressive Android development.
* **Jetpack Compose**: A modern toolkit for building a responsive, Material Design 3-compliant user interface.
* **Jetpack Navigation 3**: A state-driven navigation framework to manage the app's flow and deep-linking.
* **Compose Material Adaptive**: Provides a flexible layout system that automatically adjusts to different screen sizes and form factors.
* **Kotlin Coroutines**: Manages asynchronous tasks and background processing efficiently.
* **Firebase (Auth, Firestore, Cloud Functions & FCM)**: Powers the server-side consensus engine, user authentication, and delivers real-time notifications.

## Implementation Steps

### Task_1_Initial_Setup_Auth: Configure Firebase (Auth, Firestore, FCM) dependencies and implement the Onboarding flow, including Firebase Authentication and Room Selection from a Firestore-backed list.
- **Status:** COMPLETED
- **Updates:** - Configured Firebase (Auth, Firestore, FCM) dependencies.
- **Acceptance Criteria:**
  - Firebase dependencies added to build scripts
  - Firebase Auth integration successful
  - Room list fetched from Firestore
  - Onboarding UI follows M3 guidelines
  - Build passes

### Task_2_Flagging_And_Consensus_Logic: Implement the 'Flag Noise Now' feature with rate-limiting and Firestore write logic. Define the Firestore schema for flags and room adjacency.
- **Status:** COMPLETED
- **Updates:** - Created Home Screen with "Flag Noise Now" button.
- **Acceptance Criteria:**
  - 'Flag Noise Now' button functional
  - Firestore write includes roomId and timestamp
  - Rate-limiting prevents excessive flagging
  - Adjacency map structure initialized in Firestore
  - App does not crash

### Task_3_Dashboard_And_Notifications: Develop the Escalation Ladder Dashboard to display strike levels and incident history. Integrate FCM to handle 'Gentle Nudge' notifications.
- **Status:** COMPLETED
- **Updates:** - Developed Escalation Ladder Dashboard visualizing 30-day strike levels.
- **Acceptance Criteria:**
  - Dashboard visualizes 30-day rolling strike level
  - FCM notifications displayed correctly
  - Escalation tone matches strike level
  - Ambient sound check (decibel) stub/implementation

### Task_4_Branding_Refinement_Verify: Apply Material 3 vibrant color scheme, implement Edge-to-Edge display, create an adaptive app icon, and perform final stability and requirement verification.
- **Status:** COMPLETED
- **Updates:** - Implemented adaptive navigation using NavigationSuiteScaffold (BottomBar for phones, Rail for tablets).
- Refined Edge-to-Edge display by removing manual status bar overrides.
- Centralized business logic constants (cooldowns, time windows) in AppConstants.
- Verified Material 3 branding and vibrancy across all screens.
- Final stability check passed; app is functionally complete and production-ready.
- **Acceptance Criteria:**
  - Material 3 vibrant theme applied
  - Full Edge-to-Edge display implemented
  - Adaptive app icon matches branding
  - Critical UI issues reported/fixed
  - Final verification of all features without crashes
- **Duration:** N/A

