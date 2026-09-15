package com.sagon.cocinarecetas.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.sagon.cocinarecetas.data.model.Recipe
import kotlinx.coroutines.tasks.await
import android.util.Log

class FirestoreService {
    private val db = FirebaseFirestore.getInstance()
    private val recipesCollection = db.collection("recipes")
    private val metaCollection = db.collection("meta")

    /**
     * Obtiene un mapa de ID -> Hash de todas las recetas en la nube
     * Esto permite saber qué recetas han cambiado sin descargarlas todas.
     */
    suspend fun getRecipeManifest(): Map<String, String> {
        return try {
            val doc = metaCollection.document("recipes_manifest").get().await()
            if (doc.exists()) {
                doc.data?.mapValues { it.value.toString() } ?: emptyMap()
            } else emptyMap()
        } catch (e: Exception) {
            Log.e("Firestore", "Error al obtener manifiesto", e)
            emptyMap()
        }
    }

    /**
     * Actualiza el manifiesto en la nube tras una subida.
     */
    suspend fun updateManifest(manifest: Map<String, String>) {
        try {
            metaCollection.document("recipes_manifest").set(manifest).await()
            // También actualizamos la versión global
            metaCollection.document("sync_info").set(mapOf("version" to System.currentTimeMillis())).await()
        } catch (e: Exception) {
            Log.e("Firestore", "Error al actualizar manifiesto", e)
        }
    }

    /**
     * Sube una receta individual a Firestore.
     */
    suspend fun uploadSingleRecipe(recipe: Recipe): Boolean {
        return try {
            // Usamos el título como ID único para evitar duplicados en la nube
            val docId = recipe.title.uppercase().trim().hashCode().toString()
            recipesCollection.document(docId).set(recipe).await()
            Log.d("Firestore", "Receta '${recipe.title}' subida con éxito.")
            true
        } catch (e: Exception) {
            Log.e("Firestore", "Error al subir receta individual", e)
            false
        }
    }

    /**
     * Borra todas las recetas actuales de Firestore de forma segura.
     */
    suspend fun wipeCloudRecipesCollection() {
        try {
            Log.d("Firestore", "Iniciando purga completa de la colección 'recipes' en la nube...")
            val snapshot = recipesCollection.get().await()
            val batch = db.batch()
            snapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            batch.commit().await()
            
            // Actualizamos la versión meta
            metaCollection.document("sync_info").set(mapOf("version" to System.currentTimeMillis())).await()
            Log.d("Firestore", "Purga de recetas en la nube completada con éxito.")
        } catch (e: Exception) {
            Log.e("Firestore", "Error purgando recetas en Firestore", e)
        }
    }

    /**
     * Descarga solo las recetas si la versión de la nube es superior a la local.
     * Spark Limit: 50k lecturas/día. No queremos bajar 1100 recetas cada vez.
     */
    suspend fun getCloudRecipesIfUpdated(localVersion: Long): List<Recipe>? {
        return try {
            val syncDoc = metaCollection.document("sync_info").get().await()
            val remoteVersion = syncDoc.getLong("version") ?: 0L
            
            if (remoteVersion > localVersion) {
                Log.d("Firestore", "Nueva versión detectada. Descargando...")
                val snapshot = recipesCollection.get().await()
                snapshot.toObjects(Recipe::class.java)
            } else {
                Log.d("Firestore", "El recetario local ya está al día.")
                null
            }
        } catch (e: Exception) {
            Log.e("Firestore", "Error al sincronizar con la nube", e)
            null
        }
    }
}
