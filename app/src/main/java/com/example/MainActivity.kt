package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.data.firebase.configureAppCheck
import com.example.ui.screens.LexiconAppRoot
import com.example.ui.theme.LexiconTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure Firebase App Check with launch intent BEFORE UI, ViewModels, or Firebase ops
        configureAppCheck(this, intent)

        // STEP 2: Enable edge-to-edge layout and render Lexicon Compose UI
        enableEdgeToEdge()
        setContent {
            LexiconTheme {
                LexiconAppRoot()
            }
        }
    }
}
