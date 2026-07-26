package com.echoease.app.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class Screen {
    @Serializable
    object Onboarding : Screen()
    
    @Serializable
    object Auth : Screen()

    @Serializable
    object BuildingSelection : Screen()
    
    @Serializable
    data class RoomSelection(val buildingId: String) : Screen()
    
    @Serializable
    object Home : Screen()

    @Serializable
    object Dashboard : Screen()

    @Serializable
    object Profile : Screen()

    @Serializable
    object Admin : Screen()
}
