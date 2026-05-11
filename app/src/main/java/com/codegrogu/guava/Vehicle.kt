package com.codegrogu.guava.model

import com.google.firebase.Timestamp

data class Vehicle(
    val vehicleId:         String     = "",
    val licensePlate:      String     = "",
    val initialKm:         Int        = 0,
    val conditionImageUrl: String     = "",
    val checkedInByUid:    String     = "",
    val checkedInByName:   String     = "",
    val checkInTimestamp:  Timestamp? = null
)