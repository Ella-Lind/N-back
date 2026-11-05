package com.example.nback

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nback.ui.NBackGameApp
import com.example.nback.ui.theme.NBackTheme
import com.example.nback.viewmodel.NBackViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NBackTheme {
                val viewModel: NBackViewModel = viewModel()
                NBackGameApp(viewModel = viewModel)
            }
        }
    }
}