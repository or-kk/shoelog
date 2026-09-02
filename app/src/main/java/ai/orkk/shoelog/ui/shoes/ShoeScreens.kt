@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package ai.orkk.shoelog.ui.shoes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ai.orkk.shoelog.data.repository.ShoeDraft
import ai.orkk.shoelog.data.repository.ShoeRepository
import ai.orkk.shoelog.domain.Exercise
import ai.orkk.shoelog.domain.Shoe
import ai.orkk.shoelog.ui.EmptyState
import ai.orkk.shoelog.ui.ExerciseRow
import ai.orkk.shoelog.ui.LoadingState
import ai.orkk.shoelog.ui.ShoeMileageCard
import ai.orkk.shoelog.ui.formatDistance
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale
import kotlin.math.roundToLong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

val SuggestedBrands = listOf(
    "Nike", "Adidas", "ASICS", "Saucony", "New Balance", "Puma", "HOKA",
    "Brooks", "Mizuno", "On", "Under Armour", "Reebok", "기타",
)

data class ShoeEditorForm(
    val brand: String = "",
    val model: String = "",
    val nickname: String = "",
    val color: String = "",
    val purchaseDate: String = "",
    val startDate: String = "",
    val purchasePriceWon: String = "",
    val listPriceWon: String = "",
    val initialKm: String = "0",
    val targetKm: String = "500",
    val photoUri: String? = null,
    val retired: Boolean = false,
    val defaultShoe: Boolean = false,
) {
    fun validate(): Map<String, String> = buildMap {
        if (brand.isBlank()) put("brand", "브랜드를 입력해 주세요.")
        if (model.isBlank()) put("model", "모델명을 입력해 주세요.")
        val initial = initialKm.toDoubleOrNull()
        if (initial == null || initial < 0) put("initialKm", "0 이상의 거리를 입력해 주세요.")
        val target = targetKm.toDoubleOrNull()
        if (target == null || target <= 0) put("targetKm", "0보다 큰 목표거리를 입력해 주세요.")
        if (purchaseDate.isNotBlank() && runCatching { LocalDate.parse(purchaseDate) }.isFailure) {
            put("purchaseDate", "YYYY-MM-DD 형식으로 입력해 주세요.")
        }
        if (startDate.isNotBlank() && runCatching { LocalDate.parse(startDate) }.isFailure) {
            put("startDate", "YYYY-MM-DD 형식으로 입력해 주세요.")
        }
        if (purchasePriceWon.isNotBlank() && purchasePriceWon.toLongOrNull()?.takeIf { it >= 0 } == null) {
            put("purchasePriceWon", "0 이상의 원화 정수를 입력해 주세요.")
        }
        if (listPriceWon.isNotBlank() && listPriceWon.toLongOrNull()?.takeIf { it >= 0 } == null) {
            put("listPriceWon", "0 이상의 원화 정수를 입력해 주세요.")
        }
    }

    fun toDraft(): ShoeDraft {
        require(validate().isEmpty()) { "입력값을 확인해 주세요." }
        return ShoeDraft(
            brand = brand.trim(),
            model = model.trim(),
            nickname = nickname.trim(),
            color = color.trim(),
            purchaseDate = purchaseDate.takeIf(String::isNotBlank)?.let(LocalDate::parse),
            startDate = startDate.takeIf(String::isNotBlank)?.let(LocalDate::parse),
            purchasePriceWon = purchasePriceWon.takeIf(String::isNotBlank)?.toLong(),
            listPriceWon = listPriceWon.takeIf(String::isNotBlank)?.toLong(),
            initialMeters = (initialKm.toDouble() * 1_000).roundToLong(),
            targetMeters = (targetKm.toDouble() * 1_000).roundToLong(),
            photoUri = photoUri,
            retired = retired,
            defaultShoe = defaultShoe,
        )
    }

    companion object {
        fun from(shoe: Shoe) = ShoeEditorForm(
            brand = shoe.brand,
            model = shoe.model,
            nickname = shoe.nickname,
            color = shoe.color,
            purchaseDate = shoe.purchaseDate?.toString().orEmpty(),
            startDate = shoe.startDate?.toString().orEmpty(),
            purchasePriceWon = shoe.purchasePriceWon?.toString().orEmpty(),
            listPriceWon = shoe.listPriceWon?.toString().orEmpty(),
            initialKm = formatEditableKm(shoe.initialMeters),
            targetKm = formatEditableKm(shoe.targetMeters),
            photoUri = shoe.photoUri,
            retired = shoe.retired,
            defaultShoe = shoe.defaultShoe,
        )

        private fun formatEditableKm(meters: Long): String {
            val km = meters / 1_000.0
            return if (km % 1.0 == 0.0) km.toLong().toString() else km.toString()
        }
    }
}

data class ShoeEditorUiState(
    val form: ShoeEditorForm = ShoeEditorForm(),
    val errors: Map<String, String> = emptyMap(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val savedShoeId: Long? = null,
    val message: String? = null,
)

class ShoeEditorViewModel(
    private val repository: ShoeRepository,
    private val shoeId: Long?,
) : ViewModel() {
    private val _state = MutableStateFlow(ShoeEditorUiState(isLoading = shoeId != null))
    val state: StateFlow<ShoeEditorUiState> = _state.asStateFlow()

    init {
        if (shoeId != null) {
            viewModelScope.launch {
                val shoe = repository.observeShoe(shoeId).filterNotNull().first()
                _state.value = ShoeEditorUiState(form = ShoeEditorForm.from(shoe))
            }
        }
    }

    fun updateForm(form: ShoeEditorForm) {
        _state.value = _state.value.copy(form = form, errors = emptyMap(), message = null)
    }

    fun setPhotoUri(uri: String?) = updateForm(_state.value.form.copy(photoUri = uri))

    fun save() {
        val form = _state.value.form
        val errors = form.validate()
        if (errors.isNotEmpty()) {
            _state.value = _state.value.copy(errors = errors, message = "입력값을 확인해 주세요.")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, message = null)
            runCatching { repository.save(shoeId, form.toDraft()) }
                .onSuccess { _state.value = _state.value.copy(isSaving = false, savedShoeId = it) }
                .onFailure { _state.value = _state.value.copy(isSaving = false, message = it.message ?: "저장하지 못했습니다.") }
        }
    }

    companion object {
        fun factory(repository: ShoeRepository, shoeId: Long?): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ShoeEditorViewModel(repository, shoeId) as T
            }
    }
}

@Composable
fun ShoeListScreen(
    shoes: List<Shoe>,
    includeRetired: Boolean,
    onIncludeRetiredChange: (Boolean) -> Unit,
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("러닝화") }, actions = { TextButton(onClick = onAdd) { Text("추가") } }) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("은퇴한 신발 포함")
                    Switch(checked = includeRetired, onCheckedChange = onIncludeRetiredChange)
                }
            }
            if (shoes.isEmpty()) {
                item { EmptyState("등록된 러닝화가 없습니다", "러닝화를 추가해 마일리지를 관리하세요.", "러닝화 추가", onAdd) }
            } else {
                items(shoes, key = { it.id }) { ShoeMileageCard(it, onClick = { onOpen(it.id) }) }
            }
        }
    }
}

@Composable
fun ShoeDetailScreen(
    shoe: Shoe?,
    exercises: List<Exercise>,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onRetireToggle: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(shoe?.nickname?.ifBlank { shoe.model } ?: "러닝화 상세") },
                navigationIcon = { TextButton(onClick = onBack) { Text("뒤로") } },
                actions = { TextButton(onClick = onEdit, enabled = shoe != null) { Text("수정") } },
            )
        },
    ) { padding ->
        if (shoe == null) {
            LoadingState(modifier = Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { ShoeMileageCard(shoe, onClick = {}) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("누적 ${formatDistance(shoe.mileage.totalMeters)}", fontWeight = FontWeight.Bold)
                    Text("목표 ${formatDistance(shoe.targetMeters)}")
                    Text("남은 거리 ${formatDistance(shoe.mileage.remainingMeters)}")
                    Text("색상 ${shoe.color.ifBlank { "미입력" }}")
                    Text("사용 시작일 ${shoe.startDate ?: "미입력"}")
                    Text("구매가격 ${formatWon(shoe.purchasePriceWon)}")
                    Text("정가 ${formatWon(shoe.listPriceWon)}")
                }
            }
            item {
                OutlinedButton(onClick = onRetireToggle, modifier = Modifier.fillMaxWidth()) {
                    Text(if (shoe.retired) "다시 활성화" else "은퇴 처리")
                }
            }
            item { Text("이 신발을 착용한 달리기", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            if (exercises.isEmpty()) item { Text("아직 배정된 달리기가 없습니다.") }
            else items(exercises, key = { it.id }) { ExerciseRow(it, onClick = {}) }
        }
    }
}

@Composable
fun ShoeEditorScreen(
    state: ShoeEditorUiState,
    onFormChange: (ShoeEditorForm) -> Unit,
    onPickPhoto: () -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("러닝화 추가·수정") },
                navigationIcon = { TextButton(onClick = onBack) { Text("뒤로") } },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            LoadingState(modifier = Modifier.padding(padding))
            return@Scaffold
        }
        val form = state.form
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).testTag("shoe_editor_list"),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                OutlinedTextField(
                    value = form.brand,
                    onValueChange = { onFormChange(form.copy(brand = it)) },
                    label = { Text("브랜드") },
                    isError = "brand" in state.errors,
                    supportingText = { state.errors["brand"]?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(SuggestedBrands) { brand ->
                        FilterChip(selected = form.brand == brand, onClick = { onFormChange(form.copy(brand = brand)) }, label = { Text(brand) })
                    }
                }
            }
            item { EditorField("모델명", form.model, "model" in state.errors, state.errors["model"]) { onFormChange(form.copy(model = it)) } }
            item { EditorField("별칭", form.nickname) { onFormChange(form.copy(nickname = it)) } }
            item { EditorField("색상", form.color) { onFormChange(form.copy(color = it)) } }
            item { EditorField("구매일 (YYYY-MM-DD)", form.purchaseDate, "purchaseDate" in state.errors, state.errors["purchaseDate"]) { onFormChange(form.copy(purchaseDate = it)) } }
            item { EditorField("사용 시작일 (YYYY-MM-DD)", form.startDate, "startDate" in state.errors, state.errors["startDate"]) { onFormChange(form.copy(startDate = it)) } }
            item {
                EditorField(
                    "구매가격 (원)",
                    form.purchasePriceWon,
                    "purchasePriceWon" in state.errors,
                    state.errors["purchasePriceWon"],
                    KeyboardOptions(keyboardType = KeyboardType.Number),
                ) { onFormChange(form.copy(purchasePriceWon = it)) }
            }
            item {
                EditorField(
                    "정가 (원)",
                    form.listPriceWon,
                    "listPriceWon" in state.errors,
                    state.errors["listPriceWon"],
                    KeyboardOptions(keyboardType = KeyboardType.Number),
                ) { onFormChange(form.copy(listPriceWon = it)) }
            }
            item { EditorField("초기거리 (km)", form.initialKm, "initialKm" in state.errors, state.errors["initialKm"]) { onFormChange(form.copy(initialKm = it)) } }
            item { EditorField("교체 목표거리 (km)", form.targetKm, "targetKm" in state.errors, state.errors["targetKm"]) { onFormChange(form.copy(targetKm = it)) } }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (form.photoUri == null) "신발 사진 없음" else "신발 사진 선택됨")
                    OutlinedButton(onClick = onPickPhoto) { Text("사진 선택") }
                }
            }
            item { ToggleRow("기본 신발", form.defaultShoe) { onFormChange(form.copy(defaultShoe = it)) } }
            state.message?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
            item {
                Button(onClick = onSave, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth()) {
                    Text(if (state.isSaving) "저장 중" else "저장")
                }
            }
        }
    }
}

@Composable
private fun EditorField(
    label: String,
    value: String,
    isError: Boolean = false,
    error: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        isError = isError,
        supportingText = { error?.let { Text(it) } },
        keyboardOptions = keyboardOptions,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
    HorizontalDivider()
}

private fun formatWon(amount: Long?): String = amount
    ?.let { "${NumberFormat.getIntegerInstance(Locale.KOREA).format(it)}원" }
    ?: "미입력"
