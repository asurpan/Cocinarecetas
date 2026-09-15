package com.sagon.cocinarecetas

import org.junit.Test
import org.junit.Assert.*
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import com.sagon.cocinarecetas.data.model.Recipe

@Serializable
private data class TestRecipeRoot(val recipes: List<Recipe>)

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testJsonParsing() {
        val jsonFile = File("C:/Users/Jose/AndroidStudioProjects/CocinaREcetas/app/src/main/assets/recipes.json")
        assertTrue("JSON file does not exist", jsonFile.exists())
        val jsonString = jsonFile.readText()
        val json = Json { 
            ignoreUnknownKeys = true 
            coerceInputValues = true
            isLenient = true
        }
        try {
            val root = json.decodeFromString<TestRecipeRoot>(jsonString)
            println("Successfully parsed ${root.recipes.size} recipes!")
        } catch (e: Exception) {
            e.printStackTrace()
            fail("Parsing failed with exception: ${e.message}")
        }
    }
}