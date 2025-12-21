package com.xmrled.cameraslittingmeasurement

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.xmrled.cameraslittingmeasurement.ui.theme.CameraSlittingMeasurementTheme
import org.opencv.android.OpenCVLoader

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //cek opencv
        val isOpenCVLoaded = OpenCVLoader.initDebug()
        val statusText = if (isOpenCVLoaded) "OpenCV loaded successfully" else "OpenCV failed to load"

        if (isOpenCVLoaded) Log.d("TEST_OPENCV", "successfully load library!")

        enableEdgeToEdge()
        setContent {
            CameraScreen()
            }
        }
    }


@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CameraSlittingMeasurementTheme {
        Greeting("Android")
    }
}