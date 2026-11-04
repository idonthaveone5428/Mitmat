package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainAppScaffold
import com.example.ui.theme.MitmatTheme
import com.example.ui.viewmodel.MitmatViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MitmatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val preferences by viewModel.preferences.collectAsStateWithLifecycle()

            MitmatTheme(
                appearanceMode = preferences.appearanceMode,
                isAmoled = preferences.isAmoled,
                designTheme = preferences.designTheme
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainAppScaffold(viewModel = viewModel)
                }
            }
        }
    }
}
