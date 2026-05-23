package com.moncake94.pos

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.moncake94.pos.ui.PosApp
import com.moncake94.pos.ui.PosViewModel
import com.moncake94.pos.ui.theme.MoncakeTheme

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<PosViewModel> {
        val app = application as MoncakePosApp
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PosViewModel(app.repository, app.printerService, app.backupRepository) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 31) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN),
                10
            )
        }
        setContent {
            MoncakeTheme {
                PosApp(viewModel)
            }
        }
    }
}
