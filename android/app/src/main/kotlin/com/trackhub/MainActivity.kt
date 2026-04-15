package com.trackhub

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.trackhub.ui.AppNavGraph
import com.trackhub.ui.theme.TrackHubTheme

/**
 * FragmentActivity (not ComponentActivity) so BiometricPrompt can attach.
 * Compose still works inside FragmentActivity via setContent().
 */
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TrackHubTheme {
                AppNavGraph()
            }
        }
    }
}
