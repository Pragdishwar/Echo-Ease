package com.echoease.app.util

object AppConstants {
    /**
     * The time window (in milliseconds) used to group noise flags together for consensus logic.
     * Currently set to 10 minutes.
     */
    const val CONSENSUS_WINDOW_MS = 10 * 60 * 1000L

    /**
     * The mandatory cooldown period (in milliseconds) between noise flags from the same user.
     * Currently set to 5 minutes.
     */
    const val FLAG_RATE_LIMIT_MS = 5 * 60 * 1000L
    
    /**
     * Rolling window for incident history in days.
     */
    const val INCIDENT_HISTORY_DAYS = 30

    /**
     * Set to true to bypass Firebase and use static mock data.
     * Use true for testing UI without real data.
     */
    var USE_MOCK_DATA = false
}
