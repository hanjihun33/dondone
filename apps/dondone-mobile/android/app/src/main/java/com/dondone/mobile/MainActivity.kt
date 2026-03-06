package com.dondone.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.dondone.mobile.app.DonDoneApp
import com.dondone.mobile.app.session.DemoSessionViewModel
import com.dondone.mobile.core.designsystem.DonDoneTheme

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<DemoSessionViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DonDoneTheme {
                DonDoneApp(viewModel = viewModel)
            }
        }
    }
}

