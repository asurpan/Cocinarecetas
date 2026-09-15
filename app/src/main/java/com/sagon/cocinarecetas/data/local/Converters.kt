package com.sagon.cocinarecetas.data.local

import androidx.room.TypeConverter
import com.sagon.cocinarecetas.data.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    private val jsonInstance = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @TypeConverter
    fun fromList(list: List<String>): String = jsonInstance.encodeToString(list)
    @TypeConverter
    fun toList(json: String): List<String> = jsonInstance.decodeFromString(json)

    @TypeConverter
    fun fromMenuMeta(v: MenuMeta): String = jsonInstance.encodeToString(v)
    @TypeConverter
    fun toMenuMeta(j: String): MenuMeta = jsonInstance.decodeFromString(j)

    @TypeConverter
    fun fromNutritionProfile(v: NutritionProfile): String = jsonInstance.encodeToString(v)
    @TypeConverter
    fun toNutritionProfile(j: String): NutritionProfile = jsonInstance.decodeFromString(j)

    @TypeConverter
    fun fromNutritionPerServing(v: NutritionPerServing): String = jsonInstance.encodeToString(v)
    @TypeConverter
    fun toNutritionPerServing(j: String): NutritionPerServing = jsonInstance.decodeFromString(j)

    @TypeConverter
    fun fromDietary(v: Dietary): String = jsonInstance.encodeToString(v)
    @TypeConverter
    fun toDietary(j: String): Dietary = jsonInstance.decodeFromString(j)

    @TypeConverter
    fun fromMealSuitability(v: MealSuitability): String = jsonInstance.encodeToString(v)
    @TypeConverter
    fun toMealSuitability(j: String): MealSuitability = jsonInstance.decodeFromString(j)

    @TypeConverter
    fun fromGoals(v: Goals): String = jsonInstance.encodeToString(v)
    @TypeConverter
    fun toGoals(j: String): Goals = jsonInstance.decodeFromString(j)

    @TypeConverter
    fun fromNutritionQuality(v: NutritionQuality): String = jsonInstance.encodeToString(v)
    @TypeConverter
    fun toNutritionQuality(j: String): NutritionQuality = jsonInstance.decodeFromString(j)

    @TypeConverter
    fun fromMenuConstraints(v: MenuConstraints): String = jsonInstance.encodeToString(v)
    @TypeConverter
    fun toMenuConstraints(j: String): MenuConstraints = jsonInstance.decodeFromString(j)

    @TypeConverter
    fun fromNutrition(v: Nutrition): String = jsonInstance.encodeToString(v)
    @TypeConverter
    fun toNutrition(j: String): Nutrition = jsonInstance.decodeFromString(j)

    @TypeConverter
    fun fromPortionScaling(v: PortionScaling): String = jsonInstance.encodeToString(v)
    @TypeConverter
    fun toPortionScaling(j: String): PortionScaling = jsonInstance.decodeFromString(j)

    @TypeConverter
    fun fromMenuEngine(v: MenuEngine): String = jsonInstance.encodeToString(v)
    @TypeConverter
    fun toMenuEngine(j: String): MenuEngine = jsonInstance.decodeFromString(j)

    @TypeConverter
    fun fromMenuRules(v: MenuRules): String = jsonInstance.encodeToString(v)
    @TypeConverter
    fun toMenuRules(j: String): MenuRules = jsonInstance.decodeFromString(j)
}
