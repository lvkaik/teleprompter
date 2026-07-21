package com.yourname.teleprompter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.yourname.teleprompter.ui.scripts.ScriptListScreen
import com.yourname.teleprompter.ui.theme.TeleprompterTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TeleprompterTheme {
                ScriptListScreen()
            }
        }
    }
}