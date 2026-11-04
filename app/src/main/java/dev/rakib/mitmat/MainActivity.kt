package dev.rakib.mitmat

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
import dev.rakib.mitmat.ui.MainAppScaffold
import dev.rakib.mitmat.ui.theme.MitmatTheme
import dev.rakib.mitmat.ui.viewmodel.MitmatViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MitmatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val preferences by viewModel.preferences.collectAsStateWithLifecycle()

            MitmatTheme(
                appearanceMode = preferences.appearanceMode,
                isAmoled = preferences.isAmoled
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainAppScaffold(viewModel = viewModel)
                }
            }
        }
    }
}
