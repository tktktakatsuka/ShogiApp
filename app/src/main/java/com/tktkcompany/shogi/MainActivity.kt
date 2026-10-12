package com.tktkcompany.shogi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tktkcompany.shogi.ui.ShogiScreen
import com.tktkcompany.shogi.ui.theme.ShogiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShogiTheme {
                ShogiScreen()
            }
        }
    }
}
