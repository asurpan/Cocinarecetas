package com.sagon.cocinarecetas.data.local

import androidx.room.*
import com.sagon.cocinarecetas.data.model.Recipe
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {
    @Query("SELECT * FROM recipes WHERE isDeletedLocally = 0 ORDER BY isFavorite DESC, title ASC")
    fun getAllRecipes(): Flow<List<Recipe>>

    @Query("""
        SELECT * FROM recipes 
        WHERE isDeletedLocally = 0
        AND (:searchQuery IS NOT NULL OR :category IS NOT NULL)
        ORDER BY isFavorite DESC, title ASC
    """)
    fun searchRecipes(searchQuery: String, category: String = ""): Flow<List<Recipe>>

    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun getRecipeById(id: Int): Recipe?

    @Query("""
        SELECT * FROM recipes 
        WHERE isDeletedLocally = 0
        AND (:category = '' OR category LIKE :category)
        ORDER BY RANDOM() LIMIT :limit
    """)
    suspend fun getRandomRecipesSample(limit: Int, category: String = ""): List<Recipe>

    @Query("""
        SELECT * FROM recipes 
        WHERE isDeletedLocally = 0
        AND category IN (:categories)
        ORDER BY RANDOM() LIMIT :limit
    """)
    suspend fun getRandomRecipesByCategories(categories: List<String>, limit: Int): List<Recipe>

    @Query("SELECT * FROM recipes WHERE isFavorite = 1 AND isDeletedLocally = 0")
    fun getFavoriteRecipes(): Flow<List<Recipe>>

    @Query("DELETE FROM recipes")
    suspend fun deleteAllRecipes()

    @Query("SELECT COUNT(*) FROM recipes")
    suspend fun getRecipeCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(recipes: List<Recipe>)

    @Query("UPDATE recipes SET isDeletedLocally = 0")
    suspend fun restoreAllHiddenRecipes()

    @Update
    suspend fun updateRecipe(recipe: Recipe)
}
