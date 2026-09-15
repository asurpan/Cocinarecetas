package com.sagon.cocinarecetas.ui.screens

import android.util.Log
import android.webkit.WebView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.drawWithContent
import com.sagon.cocinarecetas.data.remote.FirestoreService
import com.sagon.cocinarecetas.util.SoundUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json

@Composable
fun WelcomeScreen(
    skipDisclaimer: Boolean = false,
    onStartClick: () -> Unit
) {
    val context = LocalContext.current
    var showDisclaimer by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "¡Bienvenido a CocinaREcetas!",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.ExtraBold
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        // Olla con agua y burbujas
        OllaAnimation()

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Tu recetario inteligente personal, organizado para que cocines como un profesional.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Temporizador para el auto-salto automático (incrementado a 12 segundos para lectura pausada y tranquila)
        LaunchedEffect(Unit) {
            delay(12000) // Cambiado a 12 segundos totales para dar máxima comodidad al usuario
            if (!showDisclaimer) {
                // Ejecutamos la misma acción del botón de forma automática
                if (skipDisclaimer) {
                    onStartClick()
                } else {
                    showDisclaimer = true
                }
            }
        }

        // Animación de destello infinito (Shimmer effect) para llamar la atención del botón
        val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
        val shimmerOffset by infiniteTransition.animateFloat(
            initialValue = -500f,
            targetValue = 1000f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmerOffset"
        )

        Button(
            onClick = {
                SoundUtil.playBeep()
                if (skipDisclaimer) {
                    onStartClick()
                } else {
                    showDisclaimer = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .drawWithContent {
                    drawContent()
                    // Dibujamos el destello plateado por encima del botón
                    val brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.4f),
                            Color.White.copy(alpha = 0.1f),
                            Color.Transparent
                        ),
                        start = Offset(shimmerOffset, 0f),
                        end = Offset(shimmerOffset + 150f, size.height)
                    )
                    drawRect(brush = brush)
                },
            shape = MaterialTheme.shapes.medium
        ) {
            Text("EMPEZAR A COCINAR", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Firma del autor en la parte inferior con estilo elegante
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Desarrollado por JOSEMAN",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = "appsaiber@gmail.com",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                fontWeight = FontWeight.Medium
            )
        }
    }

    var showOnboarding by remember { mutableStateOf(false) }
    var onboardingStep by remember { mutableIntStateOf(0) }

    val onboardingMessages = listOf(
        "🍳 ¡Bienvenido! Descubre 1.453 recetas magistrales diseñadas para tu salud.",
        "📊 Controla tu nutrición: Registra calorías y macronutrientes de forma sencilla.",
        "⚖️ Evolución de peso: Visualiza tu progreso con gráficos claros y motivadores.",
        "🛒 Lista inteligente: Genera tu lista de la compra y compártela por WhatsApp."
    )

    if (showOnboarding) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("¿Cómo funciona?", fontWeight = FontWeight.Black) },
            text = {
                Text(onboardingMessages[onboardingStep])
            },
            confirmButton = {
                TextButton(onClick = {
                    if (onboardingStep < onboardingMessages.size - 1) {
                        onboardingStep++
                    } else {
                        showOnboarding = false
                        onStartClick()
                    }
                }) {
                    Text(if (onboardingStep < onboardingMessages.size - 1) "SIGUIENTE" else "¡EMPEZAR!")
                }
            }
        )
    }

    if (showDisclaimer) {
        AlertDialog(
            onDismissRequest = { },
            confirmButton = {
                TextButton(onClick = {
                    SoundUtil.playBeep()
                    showDisclaimer = false
                    showOnboarding = true
                }) {
                    Text("ACEPTO")
                }
            },
            title = { Text("Aviso Importante") },
            text = {
                Text(
                    "Esta aplicación es una herramienta de organización personal y recetario privado. " +
                    "El desarrollador no se hace responsable del uso de la información ni de los resultados culinarios. " +
                    "Para contacto o soporte: appsaiber@gmail.com. ¡Cocina con precaución y disfruta!\n\n" +
                    "Al continuar, confirmas que has leído y aceptas nuestra Política de Privacidad."
                )
            },
            dismissButton = {
                TextButton(onClick = {
                    val browserIntent = android.content.Intent(
                        android.content.Intent.ACTION_VIEW, 
                        android.net.Uri.parse("https://appsaiber.github.io/CocinaREcetas/privacy.html")
                    )
                    context.startActivity(browserIntent)
                }) {
                    Text("VER PRIVACIDAD")
                }
            }
        )
    }
}

@Composable
fun OllaAnimation() {
    val context = LocalContext.current
    var time by remember { mutableStateOf(0f) }
    
    // Transición de color suave
    val colorStart by animateColorAsState(
        targetValue = if (time >= 8f) Color(0xFFE53935) else Color(0xFF03A9F4),
        animationSpec = tween(durationMillis = 3500, easing = FastOutSlowInEasing),
        label = "colorStart"
    )
    val colorEnd by animateColorAsState(
        targetValue = if (time >= 8f) Color(0xFFB71C1C) else Color(0xFF0288D1),
        animationSpec = tween(durationMillis = 3500, easing = FastOutSlowInEasing),
        label = "colorEnd"
    )
    
    DisposableEffect(Unit) {
        onDispose {
            SoundUtil.stopBoilingSound()
        }
    }

    LaunchedEffect(Unit) {
        SoundUtil.playBoilingSound(context)
        val startTime = System.currentTimeMillis()
        while (true) {
            val currentTimeMillis = System.currentTimeMillis()
            time = (currentTimeMillis - startTime) / 1000f
            kotlinx.coroutines.delay(16)
        }
    }

    val waterLevel = (time / 4f).coerceAtMost(1f)
    
    Canvas(modifier = Modifier.size(240.dp).padding(20.dp)) {
        val width = size.width
        val height = size.height
        
        val leftX = width * 0.2f
        val rightX = width * 0.8f
        
        // --- DIBUJAR FUEGO DEBAJO DE LA OLLA ---
        val firePath = Path().apply {
            val startY = height * 0.98f
            moveTo(leftX + 20f, startY)
            var currentX = leftX + 20f
            val endX = rightX - 20f
            val flameWidth = (endX - currentX) / 5f
            
            for (i in 0..4) {
                val flameHeight = 30f + kotlin.math.abs(kotlin.math.sin(time * 10f + i)) * 40f
                quadraticTo(
                    currentX + flameWidth / 2f, startY - flameHeight,
                    currentX + flameWidth, startY
                )
                currentX += flameWidth
            }
        }
        val fireGradient = Brush.verticalGradient(
            colors = listOf(Color(0xFFFFD54F), Color(0xFFFF6F00)),
            startY = height * 0.85f,
            endY = height
        )
        drawPath(firePath, brush = fireGradient)

        val ollaColor = Color(0xFF455A64)
        
        // --- DIBUJAR CUERPO DE LA OLLA ---
        val pathOlla = Path().apply {
            moveTo(leftX, height * 0.25f)
            lineTo(leftX, height * 0.85f)
            quadraticTo(leftX, height * 0.95f, width * 0.3f, height * 0.95f)
            lineTo(width * 0.7f, height * 0.95f)
            quadraticTo(rightX, height * 0.95f, rightX, height * 0.85f)
            lineTo(rightX, height * 0.25f)
        }
        drawPath(pathOlla, color = ollaColor, style = Stroke(width = 12f, cap = StrokeCap.Round))
        
        // --- BORDE SUPERIOR (RIM) PARA QUE NO PAREZCA UNA TUBERÍA ---
        drawOval(
            color = ollaColor,
            topLeft = Offset(leftX - 6f, height * 0.22f),
            size = Size(rightX - leftX + 12f, 20f),
            style = Stroke(width = 8f)
        )
        
        // Asas de la olla
        val handleSize = 50f
        drawArc(
            color = Color.Gray,
            startAngle = 90f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(leftX - handleSize, height * 0.4f),
            size = Size(handleSize, handleSize * 1.5f),
            style = Stroke(8f, cap = StrokeCap.Round)
        )
        drawArc(
            color = Color.Gray,
            startAngle = -90f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(rightX, height * 0.4f),
            size = Size(handleSize, handleSize * 1.5f),
            style = Stroke(8f, cap = StrokeCap.Round)
        )

        // --- AGUA CON DEGRADADO (FADE/FUSION) ---
        if (waterLevel > 0.01f) {
            val waterHeight = height * 0.65f * waterLevel
            val waterTopY = height * 0.9f - waterHeight
            
            val waterPath = Path().apply {
                moveTo(leftX + 6f, height * 0.9f)
                lineTo(leftX + 6f, waterTopY)
                for (x in (leftX + 6f).toInt()..(rightX - 6f).toInt() step 5) {
                    val variation = kotlin.math.sin(x * 0.05f + time * 5f) * 8f
                    lineTo(x.toFloat(), waterTopY + variation)
                }
                lineTo(rightX - 6f, height * 0.9f)
                close()
            }
            
            val waterBrush = Brush.verticalGradient(
                colors = listOf(colorStart.copy(alpha = 0.7f), colorEnd.copy(alpha = 0.5f)),
                startY = waterTopY,
                endY = height * 0.95f
            )
            drawPath(waterPath, brush = waterBrush)
        }

        // --- BURBUJAS ---
        if (waterLevel > 0.3f) {
            val bubbleCount = 8
            for (i in 0 until bubbleCount) {
                val offset = (time + i * 0.5f) % 2f
                val bX = width * (0.35f + (i * 0.04f))
                val bY = height * 0.85f - (offset * height * 0.4f)
                
                if (bY > height * 0.9f - (height * 0.7f * waterLevel) && bX > leftX && bX < rightX) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.4f),
                        radius = 6f + (i % 3) * 2f,
                        center = Offset(bX, bY)
                    )
                }
            }
        }
    }
}
