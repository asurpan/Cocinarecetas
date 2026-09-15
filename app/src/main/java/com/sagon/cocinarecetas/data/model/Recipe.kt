package com.sagon.cocinarecetas.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Entity(
    tableName = "recipes",
    indices = [
        Index(value = ["title"], unique = true), // Único por título: Clave para evitar duplicados reales
        Index(value = ["category"])
    ]
)
@Serializable
data class Recipe(
    @PrimaryKey(autoGenerate = true)
    @Transient
    val id: Int = 0,
    @SerialName("id")
    val originalId: String = "",
    val title: String = "",
    val ingredients: List<String> = emptyList(),
    val instructions: List<String> = emptyList(),
    val category: String = "",
    val healthTags: List<String> = emptyList(),
    val isFavorite: Boolean = false,
    val isDeletedLocally: Boolean = false,
    val notes: String = "",
    val cookingTime: String = "20-40 min",
    val imageUrl: String? = null,
    val isMainDish: Boolean = true,
    val servings: Int? = null,
    val menuMeta: MenuMeta = MenuMeta(),
    val nutritionProfile: NutritionProfile = NutritionProfile(),
    val nutritionPerServing: NutritionPerServing = NutritionPerServing(),
    val dietary: Dietary = Dietary(),
    val mealSuitability: MealSuitability = MealSuitability(),
    val goals: Goals = Goals(),
    val nutritionQuality: NutritionQuality = NutritionQuality(),
    val menuConstraints: MenuConstraints = MenuConstraints(),
    val nutrition: Nutrition = Nutrition(),
    val portionScaling: PortionScaling = PortionScaling(),
    val menuEngine: MenuEngine = MenuEngine(),
    val menuRules: MenuRules = MenuRules()
)

@Serializable
data class MenuMeta(
    val role: String = "",
    val mealTypes: List<String> = emptyList(),
    val isStandaloneMeal: Boolean = false,
    val isSuitableForBreakfast: Boolean = false,
    val isSuitableForLunch: Boolean = false,
    val isSuitableForDinner: Boolean = false,
    val isSuitableForSnack: Boolean = false,
    val isSuitableForDessert: Boolean = false,
    val isSuitableForAperitif: Boolean = false,
    val selectionPriority: String = ""
)

@Serializable
data class MealSuitability(
    val breakfast: Boolean = false,
    val morningSnack: Boolean = false,
    val lunch: Boolean = false,
    val afternoonSnack: Boolean = false,
    val dinner: Boolean = false,
    val dessert: Boolean = false,
    val accompaniment: Boolean = false,
    val aperitif: Boolean = false,
    val standaloneMeal: Boolean = false,
    val side: Boolean = false,
    val sauce: Boolean = false
)

@Serializable
data class NutritionProfile(
    val tags: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
)

@Serializable
data class NutritionPerServing(
    val servings_used_for_estimate: Int? = null,
    val coverage: Double? = null,
    val kcal: Double? = null,
    val protein_g: Double? = null,
    val carbohydrate_g: Double? = null,
    val fat_g: Double? = null,
    val fiber_g: Double? = null,
    val sugars_g: Double? = null,
    val saturated_fat_g: Double? = null,
    val sodium_mg: Double? = null,
    val status: String = ""
)

@Serializable
data class Dietary(
    val vegetarian: Boolean? = null,
    val vegan: Boolean? = null,
    val containsGluten: Boolean? = null,
    val containsDairy: Boolean? = null,
    val containsEgg: Boolean? = null,
    val containsFish: Boolean? = null,
    val containsShellfish: Boolean? = null,
    val containsNuts: Boolean? = null,
    val containsAddedSugar: Boolean? = null,
    val glutenFree: Boolean? = null,
    val dairyFree: Boolean? = null,
    val eggFree: Boolean? = null,
    val containsMeat: Boolean? = null
)

@Serializable
data class Goals(
    val healthy: Boolean? = null,
    val weightLoss: Boolean? = null,
    val muscleGain: Boolean? = null,
    val diabeticFriendly: Boolean? = null,
    val highProtein: Boolean? = null,
    val highFiber: Boolean? = null,
    val lowCarb: Boolean? = null,
    val lowSodium: Boolean? = null
)

@Serializable
data class NutritionQuality(
    val calorieClass: String = "",
    val proteinClass: String = "",
    val estimationCoverage: Double? = null,
    val reliability: String = "",
    val usableForAutomaticMenuBalancing: Boolean = false
)

@Serializable
data class MenuConstraints(
    val canBeBreakfast: Boolean = false,
    val canBeSnack: Boolean = false,
    val canBeMainLunch: Boolean = false,
    val canBeMainDinner: Boolean = false,
    val canBeDessert: Boolean = false,
    val canBeAccompaniment: Boolean = false,
    val canBeAperitif: Boolean = false,
    val mustNotBeUsedAsStandaloneMeal: Boolean = false,
    val maxOccurrencesPerWeek: Int = 2
)

@Serializable
data class NutritionValues(
    val kcal: Double? = null,
    val protein_g: Double? = null,
    val carbohydrate_g: Double? = null,
    val fat_g: Double? = null,
    val fiber_g: Double? = null,
    val sugars_g: Double? = null,
    val saturated_fat_g: Double? = null,
    val sodium_mg: Double? = null
)

@Serializable
data class NutritionEstimate(
    val status: String = "",
    val coverage: Double? = null,
    val reliability: String = "",
    val laboratoryVerified: Boolean = false
)

@Serializable
data class Nutrition(
    val perServing: NutritionValues = NutritionValues(),
    val per100g: NutritionValues = NutritionValues(),
    val estimate: NutritionEstimate = NutritionEstimate()
)

@Serializable
data class PortionScaling(
    val supported: Boolean = false,
    val baseServings: Int = 4,
    val defaultMultiplier: Double = 1.0,
    val allowedMultiplierRange: List<Double> = emptyList(),
    val method: String = "",
    val fieldsScaled: List<String> = emptyList(),
    val formula: String = "",
    val warning: String = ""
)

@Serializable
data class MenuEngine(
    val primaryRole: String = "",
    val mealSlots: List<String> = emptyList(),
    val standaloneMeal: Boolean = false,
    val componentOnly: Boolean = false,
    val nutritionReliableEnoughForBalancing: Boolean = false,
    val maxWeeklyOccurrences: Int = 2
)

@Serializable
data class MenuRules(
    val canBeBreakfast: Boolean = false,
    val canBeMainMeal: Boolean = false,
    val canBeSnack: Boolean = false,
    val canBeDessert: Boolean = false,
    val canBeSide: Boolean = false,
    val canBeSauce: Boolean = false,
    val mustNotBeUsedAsStandaloneMeal: Boolean = false,
    val requiresPairingForBalancedMeal: Boolean = false
)
