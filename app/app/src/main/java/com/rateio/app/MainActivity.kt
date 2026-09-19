package com.rateio.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

// Placeholder de bootstrap do módulo :app — só confirma que Compose/navegação/DI wiring
// compilam sobre :domain e :data. A tela "Seus grupos" de verdade (RF41) é escopo da T8.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RateioApp()
        }
    }
}

@Composable
private fun RateioApp() {
    MaterialTheme {
        Scaffold { innerPadding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Text(text = "Rateio")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RateioAppPreview() {
    RateioApp()
}
