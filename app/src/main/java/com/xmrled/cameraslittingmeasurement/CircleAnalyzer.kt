package com.xmrled.cameraslittingmeasurement

import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

// --- PERUBAHAN 1: Tambahkan info ukuran sumber gambar ---
data class DetectedCircle(
    val x: Float, val y: Float, val radius: Float,
    val sourceWidth: Int, val sourceHeight: Int // <-- Tambahan penting!
)
// -------------------------------------------------------

class CircleAnalyzer(
    private val onCircleDetected: (DetectedCircle?) -> Unit
) : ImageAnalysis.Analyzer {

    override fun analyze(image: ImageProxy) {
        var matRgb: Mat? = null
        var matGray: Mat? = null
        var matInverted: Mat? = null
        var matBlurred: Mat? = null
        var circlesMat: Mat? = null

        try {
            val bitmap = image.toBitmap()
            if (bitmap == null) {
                onCircleDetected(null)
                return
            }

            // --- PERUBAHAN 2: Simpan ukuran gambar analisis ---
            val analyzeWidth = bitmap.width
            val analyzeHeight = bitmap.height
            // -------------------------------------------------

            matRgb = Mat()
            Utils.bitmapToMat(bitmap, matRgb)

            matGray = Mat()
            Imgproc.cvtColor(matRgb, matGray, Imgproc.COLOR_RGB2GRAY)

            matInverted = Mat()
            Core.bitwise_not(matGray, matInverted) // Inversi untuk objek hitam

            matBlurred = Mat()
            Imgproc.GaussianBlur(matInverted, matBlurred, Size(9.0, 9.0), 2.0, 2.0)

            circlesMat = Mat()
            Imgproc.HoughCircles(
                matBlurred, circlesMat, Imgproc.HOUGH_GRADIENT, 1.0, 100.0,
                100.0,
                // Saya naikkan sedikit jadi 40.0 biar lebih stabil (gak gampang loncat)
                40.0,
                50, 1000
            )

            var result: DetectedCircle? = null
            if (!circlesMat.empty() && circlesMat.cols() > 0) {
                val data = circlesMat.get(0, 0)
                if (data != null && data.size >= 3) {
                    // --- PERUBAHAN 3: Masukkan ukuran gambar ke dalam hasil ---
                    result = DetectedCircle(
                        x = data[0].toFloat(),
                        y = data[1].toFloat(),
                        radius = data[2].toFloat(),
                        sourceWidth = analyzeWidth,
                        sourceHeight = analyzeHeight
                    )
                    // ----------------------------------------------------------
                    Log.d("CircleAnalyzer", "Dapat di: ${data[0]},${data[1]} pada gambar ukuran $analyzeWidth x $analyzeHeight")
                }
            }

            onCircleDetected(result)

        } catch (e: Exception) {
            Log.e("CircleAnalyzer", "Error: ${e.message}")
            onCircleDetected(null)
        } finally {
            matRgb?.release()
            matGray?.release()
            matInverted?.release()
            matBlurred?.release()
            circlesMat?.release()
            image.close()
        }
    }
}