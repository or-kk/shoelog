package ai.orkk.shoelog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ai.orkk.shoelog.ui.theme.ShoeLogTheme

class PermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShoeLogTheme {
                PermissionsRationaleScreen()
            }
        }
    }
}

@Composable
private fun PermissionsRationaleScreen() {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Health Connect 개인정보 보호",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
            )
            Text("ShoeLog는 러닝화별 누적 주행거리를 계산하기 위해 달리기 운동 세션과 거리만 읽습니다.")
            Text("위치, 경로, 심박수, 걸음 수, 칼로리, 수면 등 다른 건강 정보는 요청하거나 읽지 않습니다.")
            Text("읽은 정보는 이 기기 안에만 저장되며 서버, 광고, 분석 서비스로 전송하지 않습니다.")
            Text("ShoeLog는 Health Connect 원본을 수정하거나 삭제하지 않습니다.")
            Text(
                text = "권한은 Android의 Health Connect 설정에서 언제든 변경하거나 철회할 수 있습니다.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
