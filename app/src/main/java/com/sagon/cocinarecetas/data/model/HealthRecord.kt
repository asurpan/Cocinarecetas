package com.sagon.cocinarecetas.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "health_records")
@Serializable
data class HealthRecord(
    @PrimaryKey
    val date: Long, // Start of day in millis
    val weight: Float? = null,
    val caloriesConsumed: Int = 0,
    val proteinConsumed: Float = 0f,
    val carbsConsumed: Float = 0f,
    val fatConsumed: Float = 0f
)
