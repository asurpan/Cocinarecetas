package com.sagon.cocinarecetas

import com.sagon.cocinarecetas.data.model.*
import org.junit.Assert.*
import org.junit.Test

class RecipeLogicTest {

    @Test
    fun testNutritionalBalanceCalculation() {
        // Simulamos platos con calorías específicas
        val b = Recipe(title = "B", nutrition = Nutrition(perServing = NutritionValues(kcal = 200.0, protein_g = 10.0)))
        val l = Recipe(title = "L", nutrition = Nutrition(perServing = NutritionValues(kcal = 600.0, protein_g = 30.0)))
        val d = Recipe(title = "D", nutrition = Nutrition(perServing = NutritionValues(kcal = 400.0, protein_g = 20.0)))

        // Calculamos el total
        val totalKcal = (b.nutrition.perServing.kcal ?: 0.0) + 
                        (l.nutrition.perServing.kcal ?: 0.0) + 
                        (d.nutrition.perServing.kcal ?: 0.0)
        
        val totalProt = (b.nutrition.perServing.protein_g ?: 0.0) + 
                        (l.nutrition.perServing.protein_g ?: 0.0) + 
                        (d.nutrition.perServing.protein_g ?: 0.0)

        assertEquals("Las calorías deben sumar 1200", 1200.0, totalKcal, 0.1)
        assertEquals("Las proteínas deben sumar 60", 60.0, totalProt, 0.1)
    }

    @Test
    fun testSalsasFilterIntegrity() {
        // Simulamos una lista de recetas mezclada
        val recipes = listOf(
            Recipe(title = "Salsa Brava", category = "salsas"),
            Recipe(title = "Ensalada César", category = "ensaladas"),
            Recipe(title = "Mayonesa Casera", category = "salsas")
        )

        // Aplicamos el filtro de categoría "salsas" (tal como lo hace el ViewModel)
        val filtered = recipes.filter { it.category.lowercase() == "salsas" }

        assertTrue("Solo deben quedar 2 recetas", filtered.size == 2)
        assertTrue("No debe haber ninguna ensalada", filtered.none { it.category.lowercase() == "ensaladas" })
        assertTrue("El título debe contener Salsa o Mayonesa", filtered.all { it.title.contains("Salsa") || it.title.contains("Mayonesa") })
    }

    @Test
    fun testBreakfastWildcardsProperties() {
        // Verificamos que los comodines que creamos tengan los campos necesarios para no romper el menú
        val wildcard = Recipe(
            title = "Tostada", 
            category = "DESAYUNO", 
            nutrition = Nutrition(perServing = NutritionValues(kcal = 250.0, protein_g = 8.0)),
            mealSuitability = MealSuitability(breakfast = true)
        )

        assertNotNull("Debe tener objeto nutrición", wildcard.nutrition.perServing.kcal)
        assertTrue("Debe ser apto para desayuno", wildcard.mealSuitability.breakfast)
        assertFalse("No debe ser un plato principal pesado", wildcard.isMainDish)
    }
}
