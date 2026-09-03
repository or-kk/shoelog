package ai.orkk.shoelog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ai.orkk.shoelog.ui.ShoeLogApp
import ai.orkk.shoelog.ui.Routes
import ai.orkk.shoelog.ui.theme.ShoeLogTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as ShoeLogApplication).container
        setContent {
            ShoeLogTheme {
                ShoeLogApp(
                    container = container,
                    initialExerciseId = intent.getStringExtra(EXTRA_EXERCISE_ID),
                    initialRoute = if (intent.getBooleanExtra(EXTRA_OPEN_SHOE_MANAGEMENT, false)) {
                        Routes.SHOES
                    } else {
                        Routes.HOME
                    },
                )
            }
        }
    }

    companion object {
        const val EXTRA_EXERCISE_ID = "ai.orkk.shoelog.extra.EXERCISE_ID"
        const val EXTRA_OPEN_SHOE_MANAGEMENT = "ai.orkk.shoelog.extra.OPEN_SHOE_MANAGEMENT"
    }
}
