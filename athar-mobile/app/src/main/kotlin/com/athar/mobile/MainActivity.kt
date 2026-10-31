package com.athar.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.athar.mobile.ui.AtharApp
import com.athar.mobile.ui.AtharTheme

class MainActivity : ComponentActivity() {
    private val vm: AtharViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by vm.state.collectAsStateWithLifecycle()
            AtharTheme {
                AtharApp(
                    state = state,
                    onQueryChange = vm::onQueryChange,
                    onLookup = vm::lookup,
                    onAnalyze = vm::analyzeLecture,
                    onDemo = vm::loadDemo
                )
            }
        }
    }
}
