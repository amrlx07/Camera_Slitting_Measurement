package com.xmrled.cameraslittingmeasurement

import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.hypot

@Composable
fun CameraScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // State hasil deteksi dari Analyzer
    var detectedCircle by remember { mutableStateOf<DetectedCircle?>(null) }

    // State ukuran layar preview
    var viewWidth by remember { mutableIntStateOf(1) }
    var viewHeight by remember { mutableIntStateOf(1) }

    // State validasi: Apakah objek masuk dalam lingkaran target?
    var isObjectInsideTarget by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {

        // 1. KAMERA PREVIEW
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }

                val cameraExecutor = Executors.newSingleThreadExecutor()
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build()
                    preview.setSurfaceProvider(previewView.surfaceProvider)

                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .apply {
                            setAnalyzer(cameraExecutor, CircleAnalyzer { circle ->
                                detectedCircle = circle
                            })
                        }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalyzer
                        )
                    } catch (e: Exception) {
                        Log.e("CameraScreen", "Gagal bind kamera", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                viewWidth = view.width
                viewHeight = view.height
            }
        )

        // 2. LAYER GAMBAR (CANVAS)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val screenCenterX = size.width / 2f
            val screenCenterY = size.height / 2f

            // --- A. GAMBAR ZONA TARGET (Lingkaran Statis) ---
            // Radius target (misal 40% dari lebar layar)
            val targetRadius = size.width * 0.40f

            // Warna berubah: Putih (Standar) -> Hijau (Pas)
            val targetColor = if (isObjectInsideTarget) Color.Green else Color.White.copy(alpha = 0.5f)
            val targetStroke = if (isObjectInsideTarget) 10f else 5f

            drawCircle(
                color = targetColor,
                radius = targetRadius,
                center = Offset(screenCenterX, screenCenterY),
                style = Stroke(width = targetStroke)
            )
            // ------------------------------------------------

            // --- B. GAMBAR HASIL DETEKSI (Jika Ada) ---
            detectedCircle?.let { circle ->
                if (circle.sourceWidth > 0 && circle.sourceHeight > 0) {
                    // Hitung Skala
                    val scaleX = size.width / circle.sourceWidth
                    val scaleY = size.height / circle.sourceHeight
                    val scale = maxOf(scaleX, scaleY)

                    // Transformasi Koordinat
                    val sourceCenterX = circle.sourceWidth / 2f
                    val sourceCenterY = circle.sourceHeight / 2f

                    val finalX = (circle.x - sourceCenterX) * scale + screenCenterX
                    val finalY = (circle.y - sourceCenterY) * scale + screenCenterY
                    val finalRadius = circle.radius * scale

                    // --- LOGIKA FILTER: APAKAH DI DALAM TARGET? ---
                    // Hitung jarak titik pusat objek ke titik pusat layar
                    val distanceFromCenter = hypot(finalX - screenCenterX, finalY - screenCenterY)

                    // Ambang batas toleransi (misal: harus dalam radius 150 piksel dari tengah)
                    val allowedDistance = targetRadius * 0.5 // Toleransi 50% dari radius target

                    // Cek kondisi
                    val isInside = distanceFromCenter < allowedDistance

                    // Update state ke UI (gunakan side effect yg aman nanti, tapi ini oke buat canvas)
                    isObjectInsideTarget = isInside

                    // Hanya gambar lingkaran hijau JIKA masuk target
                    if (isInside) {
                        drawCircle(
                            color = Color.Green,
                            radius = finalRadius,
                            center = Offset(finalX, finalY),
                            style = Stroke(width = 8f)
                        )
                        drawCircle(
                            color = Color.Red,
                            radius = 10f,
                            center = Offset(finalX, finalY)
                        )
                    }
                }
            } ?: run {
                isObjectInsideTarget = false // Reset kalau tidak ada deteksi
            }
        }

        // 3. UI INFO CARD (Bagian Bawah)
        // Hanya muncul jika objek terdeteksi DAN valid (masuk target)
        if (isObjectInsideTarget && detectedCircle != null) {
            InfoCard(
                circle = detectedCircle!!,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 50.dp, start = 16.dp, end = 16.dp) // Jarak dari bawah
            )
        } else {
            // Teks instruksi jika belum pas
            Text(
                text = "Paskan objek di dalam lingkaran",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 16.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            )
        }
    }
}

// --- KOMPONEN KARTU INFORMASI ---
@Composable
fun InfoCard(circle: DetectedCircle, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF222222)), // Warna Gelap Elegan
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ikon / Indikator Status
            Text(
                text = "✅",
                fontSize = 32.sp,
                modifier = Modifier.padding(end = 16.dp)
            )

            // Kolom Data
            Column {
                Text(
                    text = "DETEKSI BERHASIL",
                    color = Color.Green,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Data Teknis
                Text(
                    text = "Radius Piksel: %.1f px".format(circle.radius),
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    text = "Posisi: X=${circle.x.toInt()}, Y=${circle.y.toInt()}",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}