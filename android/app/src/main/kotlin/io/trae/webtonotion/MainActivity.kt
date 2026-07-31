package io.trae.webtonotion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.trae.webtonotion.ui.navigation.AppNav
import io.trae.webtonotion.ui.theme.WebToNotionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WebToNotionTheme {
                AppNav()
            }
        }
    }
}
