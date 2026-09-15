package com.sagon.cocinarecetas

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.junit.Test
import java.io.File

class RecipeExtractionTest {
    @Test
    fun testInspectPdf() {
        val pdfFile = File("D:\\AA RESPALDO COPIAR EN 2 DISCOS\\libros de recetas\\Karlos.Arguinano.1069.Recetas.Esp.PDF{www.cadiztorrent.es}.pdf")
        if (!pdfFile.exists()) {
            println("File not found at: ${pdfFile.absolutePath}")
            return
        }
        
        PDDocument.load(pdfFile).use { document ->
            val stripper = PDFTextStripper()
            // Inspect first 5 pages to see the structure
            stripper.startPage = 1
            stripper.endPage = 10
            val text = stripper.getText(document)
            println("--- PDF CONTENT START ---")
            println(text)
            println("--- PDF CONTENT END ---")
        }
    }
}
