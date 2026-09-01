package ai.orkk.shoelog.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.permission.HealthPermission
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ai.orkk.shoelog.AppContainer
import ai.orkk.shoelog.data.health.HealthConnectAvailability
import ai.orkk.shoelog.data.repository.SyncResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val availability: HealthConnectAvailability = HealthConnectAvailability.AVAILABLE,
    val basePermissionsGranted: Boolean = false,
    val historyAvailable: Boolean = false,
    val historyGranted: Boolean = false,
    val notificationsGranted: Boolean = false,
    val autoAssignDefault: Boolean = false,
    val sampleMode: Boolean = false,
    val isSyncing: Boolean = false,
    val message: String? = null,
)

class SettingsViewModel(private val container: AppContainer) : ViewModel() {
    private val operation = MutableStateFlow(OperationState())

    val state = combine(container.settingsRepository.settings, operation) { settings, operation ->
        SettingsUiState(
            availability = container.healthConnectDataSource.availability(),
            basePermissionsGranted = operation.grantedPermissions.containsAll(container.healthConnectDataSource.requiredPermissions),
            historyAvailable = container.healthConnectDataSource.historyFeatureAvailable(),
            historyGranted = HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY in operation.grantedPermissions,
            notificationsGranted = operation.notificationsGranted,
            autoAssignDefault = settings.autoAssignDefault,
            sampleMode = settings.sampleMode,
            isSyncing = operation.syncing,
            message = operation.message,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    init {
        operation.value = operation.value.copy(notificationsGranted = container.notificationManager.canNotify())
        refreshPermissions()
    }

    fun refreshPermissions(granted: Set<String>? = null) {
        viewModelScope.launch {
            val permissions = granted ?: runCatching { container.healthConnectDataSource.grantedPermissions() }.getOrDefault(emptySet())
            operation.value = operation.value.copy(grantedPermissions = permissions)
            container.settingsRepository.setHistoryRequested(
                HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY in permissions,
            )
        }
    }

    fun setAutoAssign(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setAutoAssignDefault(enabled) }
    }

    fun refreshNotificationPermission() {
        operation.value = operation.value.copy(notificationsGranted = container.notificationManager.canNotify())
    }

    fun setSampleMode(enabled: Boolean) {
        viewModelScope.launch {
            runCatching {
                if (enabled) container.sampleDataRepository.install() else container.sampleDataRepository.clear()
                container.settingsRepository.setSampleMode(enabled)
            }.onSuccess {
                operation.value = operation.value.copy(message = if (enabled) "가상 샘플 데이터를 추가했습니다." else "샘플 데이터만 제거했습니다.")
            }.onFailure {
                operation.value = operation.value.copy(message = "샘플 데이터를 변경하지 못했습니다.")
            }
        }
    }

    fun sync() {
        viewModelScope.launch {
            operation.value = operation.value.copy(syncing = true, message = null)
            val message = when (val result = container.syncRepository.sync()) {
                is SyncResult.Success -> "동기화 완료 · 새 달리기 ${result.newExerciseIds.size}개"
                is SyncResult.PermissionRequired -> "운동과 거리 읽기 권한을 허용해 주세요."
                SyncResult.UpdateRequired -> "Health Connect를 업데이트해 주세요."
                SyncResult.Unsupported -> "이 기기에서는 Health Connect를 지원하지 않습니다."
                is SyncResult.Failed -> result.userMessage
            }
            operation.value = operation.value.copy(syncing = false, message = message)
        }
    }

    private data class OperationState(
        val grantedPermissions: Set<String> = emptySet(),
        val notificationsGranted: Boolean = false,
        val syncing: Boolean = false,
        val message: String? = null,
    )

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = SettingsViewModel(container) as T
        }
    }
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onRequestPermissions: () -> Unit,
    onRequestHistory: () -> Unit,
    onSync: () -> Unit,
    onAutoAssignChange: (Boolean) -> Unit,
    onSampleModeChange: (Boolean) -> Unit,
    onRequestNotifications: () -> Unit = {},
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("settings_list"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("설정", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        }
        item {
            SettingsCard("Health Connect 권한") {
                Text(
                    when (state.availability) {
                        HealthConnectAvailability.AVAILABLE -> if (state.basePermissionsGranted) "운동 세션과 거리 읽기 권한이 허용되었습니다." else "달리기와 거리를 가져오려면 권한이 필요합니다."
                        HealthConnectAvailability.UPDATE_REQUIRED -> "Health Connect 설치 또는 업데이트가 필요합니다."
                        HealthConnectAvailability.UNSUPPORTED -> "이 기기에서는 Health Connect를 사용할 수 없습니다."
                    },
                )
                Text("위치, 경로, 심박수, 수면은 읽지 않습니다.", style = MaterialTheme.typography.bodySmall)
                Button(
                    onClick = onRequestPermissions,
                    enabled = state.availability == HealthConnectAvailability.AVAILABLE && !state.basePermissionsGranted,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (state.basePermissionsGranted) "권한 허용됨" else "권한 요청") }
            }
        }
        item {
            SettingsCard("삼성헬스 연결 안내") {
                Text("Samsung Health → 설정 → Health Connect에서 운동과 거리 공유를 켜 주세요.")
                Text("워치 기록이 표시되기까지 동기화 지연이 생길 수 있습니다.", style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            SettingsCard("동기화") {
                OutlinedButton(
                    onClick = onSync,
                    enabled = !state.isSyncing && state.basePermissionsGranted,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (state.isSyncing) "동기화 중" else "지금 동기화") }
                state.message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
        }
        item {
            SettingsCard("러닝화 자동 배정") {
                SettingSwitch(
                    label = "새 달리기에 기본 신발 자동 배정",
                    checked = state.autoAssignDefault,
                    onCheckedChange = onAutoAssignChange,
                )
                Text("자동 배정된 기록도 달리기 화면에서 변경할 수 있습니다.", style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            SettingsCard("과거 기록 가져오기") {
                Text("기본 범위는 권한을 처음 허용한 시점 기준 최근 30일입니다.")
                Button(
                    onClick = onRequestHistory,
                    enabled = state.historyAvailable && !state.historyGranted,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (state.historyGranted) "과거 기록 권한 허용됨" else "30일 이전 기록 권한 요청") }
            }
        }
        item {
            SettingsCard("미배정 달리기 알림") {
                Text("앱 동기화 중 새 미배정 달리기를 발견하면 알려줍니다.")
                Button(
                    onClick = onRequestNotifications,
                    enabled = !state.notificationsGranted,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (state.notificationsGranted) "알림 허용됨" else "알림 허용") }
            }
        }
        item {
            SettingsCard("샘플 데이터") {
                SettingSwitch("가상 데이터로 화면 체험", state.sampleMode, onSampleModeChange)
                Text("실제 건강 기록이나 개인 정보는 샘플에 포함되지 않습니다.", style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            SettingsCard("개인정보 보호") {
                Text("운동과 러닝화 정보는 기기 내부에만 저장합니다.")
                Text("서버, 계정, 광고, 네트워크 전송 기능이 없습니다.")
                Text("Health Connect 원본 데이터는 읽기만 하며 수정하거나 삭제하지 않습니다.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
    HorizontalDivider()
}
