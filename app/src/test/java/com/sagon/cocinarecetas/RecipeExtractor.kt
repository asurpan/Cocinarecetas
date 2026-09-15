package com.sagon.cocinarecetas

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import java.io.File

object RecipeExtractor {
    @JvmStatic
    fun main(args: Array<String>) {
        val logFile = File("C:\\Users\\Jose\\AndroidStudioProjects\\CocinaREcetas\\app\\build\\parsing_summary.txt")
        val pdfFile = File("D:\\AA RESPALDO COPIAR EN 2 DISCOS\\libros de recetas\\Karlos.Arguinano.1069.Recetas.Esp.PDF{www.cadiztorrent.es}.pdf")
        
        if (!pdfFile.exists()) {
            logFile.writeText("PDF file not found.")
            return
        }
        
        PDDocument.load(pdfFile).use { document ->
            val stripper = PDFTextStripper()
            val fullText = stripper.getText(document)
            
            // Find all recipe headers
            // Pattern matches start of line, optional spaces, digits/dots, dash, title
            val headerPattern = """(?m)^\s*([\d.]+)\s*[–-]\s*([^\n\r]+)""".toRegex()
            val matches = headerPattern.findAll(fullText).toList()
            
            val sb = java.lang.StringBuilder()
            sb.appendLine("Total header matches found: ${matches.size}")
            
            val parsedRecipes = mutableListOf<ParsedRecipe>()
            
            for (i in matches.indices) {
                val match = matches[i]
                val numStr = match.groupValues[1].replace(".", "")
                val id = numStr.toIntOrNull() ?: continue
                val title = match.groupValues[2].trim()
                
                val startPos = match.range.last + 1
                val endPos = if (i < matches.size - 1) matches[i + 1].range.first else fullText.length
                
                val recipeContent = fullText.substring(startPos, endPos)
                
                // Parse ingredients and instructions
                val ingIndex = recipeContent.indexOf("Ingredientes")
                val elabIndex = recipeContent.indexOf("Elaboración:")
                
                var ingredientsList = listOf<String>()
                var instructionsList = listOf<String>()
                
                if (ingIndex != -1 && elabIndex != -1 && elabIndex > ingIndex) {
                    val ingText = recipeContent.substring(ingIndex, elabIndex)
                    ingredientsList = parseIngredients(ingText)
                    
                    val elabText = recipeContent.substring(elabIndex + "Elaboración:".length)
                    instructionsList = parseInstructions(elabText)
                } else if (elabIndex != -1) {
                    // No ingredients header or weird format, but has Elaboración
                    val elabText = recipeContent.substring(elabIndex + "Elaboración:".length)
                    instructionsList = parseInstructions(elabText)
                } else {
                    // Fallback if no Elaboración found, use the whole text cleaned up
                    instructionsList = parseInstructions(recipeContent)
                }
                
                val category = determineCategory(id, title)
                
                parsedRecipes.add(ParsedRecipe(id, title, ingredientsList, instructionsList, category))
            }
            
            sb.appendLine("Successfully segmented recipes: ${parsedRecipes.size}")
            
            // Generate the JSON file
            val jsonFile = File("C:\\Users\\Jose\\AndroidStudioProjects\\CocinaREcetas\\app\\src\\main\\assets\\recipes.json")
            jsonFile.parentFile?.mkdirs()
            
            val jsonSb = java.lang.StringBuilder()
            jsonSb.append("[\n")
            for (index in parsedRecipes.indices) {
                val r = parsedRecipes[index]
                jsonSb.append("  {\n")
                jsonSb.append("    \"title\": \"${escapeJson(r.title)}\",\n")
                jsonSb.append("    \"ingredients\": [\n")
                r.ingredients.forEachIndexed { i, ing ->
                    jsonSb.append("      \"${escapeJson(ing)}\"${if (i < r.ingredients.size - 1) "," else ""}\n")
                }
                jsonSb.append("    ],\n")
                jsonSb.append("    \"instructions\": [\n")
                r.instructions.forEachIndexed { i, inst ->
                    jsonSb.append("      \"${escapeJson(inst)}\"${if (i < r.instructions.size - 1) "," else ""}\n")
                }
                jsonSb.append("    ],\n")
                jsonSb.append("    \"category\": \"${escapeJson(r.category)}\",\n")
                jsonSb.append("    \"healthTags\": [],\n")
                jsonSb.append("    \"isFavorite\": false,\n")
                jsonSb.append("    \"notes\": \"\"\n")
                jsonSb.append("  }${if (index < parsedRecipes.size - 1) "," else ""}\n")
            }
            jsonSb.append("]\n")
            
            jsonFile.writeText(jsonSb.toString(), Charsets.UTF_8)
            sb.appendLine("JSON file written to: ${jsonFile.absolutePath} with ${parsedRecipes.size} recipes.")
            
            logFile.writeText(sb.toString())
        }
    }
    
    private fun parseIngredients(text: String): List<String> {
        // Split by the bullet character •
        val parts = text.split("•")
        val result = mutableListOf<String>()
        for (i in 1 until parts.size) {
            val ing = parts[i].trim()
                .replace(Regex("""\s+"""), " ") // clean up spaces/newlines
            if (ing.isNotBlank()) {
                // Remove trailing dots or page footer references if any leaked
                val cleaned = cleanLine(ing)
                if (cleaned.isNotBlank() && !cleaned.startsWith("file://")) {
                    result.add(cleaned)
                }
            }
        }
        if (result.isEmpty()) {
            // Fallback: lines that look like ingredients
            text.lines().map { it.trim() }.forEach { line ->
                if (line.isNotBlank() && !line.contains("Ingredientes") && !line.startsWith("file://")) {
                    val cleaned = cleanLine(line)
                    if (cleaned.isNotBlank()) result.add(cleaned)
                }
            }
        }
        return result
    }
    
    private fun parseInstructions(text: String): List<String> {
        val result = mutableListOf<String>()
        val lines = text.lines()
        var currentStep = java.lang.StringBuilder()
        
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isBlank()) continue
            if (trimmed.startsWith("file://")) continue
            // Ignore headers like section names that leak into pages
            if (trimmed == "Carnes" || trimmed == "Postres" || trimmed == "Ensaladas" || 
                trimmed == "Verduras y hortalizas" || trimmed == "Arroces" || trimmed == "Pastas" || 
                trimmed == "Sopas y cremas" || trimmed == "Pescados y mariscos" || trimmed == "Recetas básicas") {
                continue
            }
            
            // If the line is short and uppercase, it might be a subheader or header leaking, ignore if it doesn't add value
            if (trimmed.length < 30 && trimmed == trimmed.uppercase() && !trimmed.any { it.isDigit() }) {
                continue
            }
            
            currentStep.append(trimmed).append(" ")
            
            // If line ends with a period, let's treat it as a completed instruction step or sentence
            if (trimmed.endsWith(".") || trimmed.endsWith(". ")) {
                val stepText = currentStep.toString().trim().replace(Regex("""\s+"""), " ")
                if (stepText.isNotBlank()) {
                    result.add(stepText)
                }
                currentStep = java.lang.StringBuilder()
            }
        }
        
        // Add remaining text if any
        val remaining = currentStep.toString().trim().replace(Regex("""\s+"""), " ")
        if (remaining.isNotBlank()) {
            result.add(remaining)
        }
        
        return if (result.isEmpty()) listOf("Ver elaboración en el libro original.") else result
    }
    
    private fun cleanLine(line: String): String {
        // Remove trailing or leaking URLs or page numbers
        var s = line
        val fileIdx = s.indexOf("file://")
        if (fileIdx != -1) {
            s = s.substring(0, fileIdx).trim()
        }
        return s
    }
    
    private fun determineCategory(id: Int, title: String): String {
        return when (id) {
            in 1..15 -> if (id == 2 || id == 5) "Sopas" else "Aperitivos"
            in 16..25 -> "Postres"
            in 26..95 -> "Ensaladas"
            in 96..260 -> "Verduras"
            in 261..290 -> "Arroces"
            in 291..350 -> "Pastas"
            in 351..400 -> "Legumbres"
            in 401..460 -> "Sopas"
            in 461..495 -> "Aperitivos" // Huevos
            in 496..695 -> "Carnes"
            in 696..895 -> "Pescados"
            else -> "Postres" // 896..1069
        }
    }
    
    private fun escapeJson(text: String): String {
        return text.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
    
    data class ParsedRecipe(
        val id: Int,
        val title: String,
        val ingredients: List<String>,
        val instructions: List<String>,
        val category: String
    )
}
