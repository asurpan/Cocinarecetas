package com.sagon.cocinarecetas.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val weight: Float = 70f,
    val height: Float = 170f,
    val age: Int = 30,
    val gender: String = "Otro", // "Hombre", "Mujer", "Otro"
    val activityLevel: String = "Moderada", // "Sedentaria", "Ligera", "Moderada", "Alta", "Atleta"
    val goal: String = "Mantenimiento", // "Perder peso", "Mantenimiento", "Ganar músculo"
    val dailyCalorieTarget: Int = 2000,
    val dailyProteinTarget: Int = 100,
    val isSetupComplete: Boolean = false
)
