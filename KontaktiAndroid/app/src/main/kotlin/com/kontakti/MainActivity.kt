package com.kontakti

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    // TODO: wire NavHost and top-level navigation graph
                    // Import screens are reachable via:
                    //   ImportContactsScreen(onBack = { /* navController.popBackStack() */ })
                    //   GmailImportScreen(onBack = { /* navController.popBackStack() */ })
                }
            }
        }
    }
}
