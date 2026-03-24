package com.audioquran

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.audioquran.ui.screens.HomeScreen
import com.audioquran.ui.theme.AudioQuranTheme

/**
 * Main activity for the Audio Quran app.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            AudioQuranTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val viewModel: com.audioquran.ui.MainViewModel = viewModel()
                    HomeScreen(viewModel = viewModel)
                }
            }
        }
    }
}
