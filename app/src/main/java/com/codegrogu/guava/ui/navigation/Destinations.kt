package com.codegrogu.guava.ui.navigation

import kotlinx.serialization.Serializable

sealed interface Destination {
    @Serializable
    data object Login : Destination

    @Serializable
    data object SignUp : Destination

    @Serializable
    data object MechanicDashboard : Destination

    @Serializable
    data object ManagerDashboard : Destination

    @Serializable
    data object CheckIn : Destination // Added this to fix the AppNavigation error
}