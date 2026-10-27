package ru.haykor.carmanager.feature.serviceEditor.ui

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import java.text.DateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Date
import java.util.Locale
import ru.haykor.carmanager.R
import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.core.domain.ServiceCategoryId
import ru.haykor.carmanager.core.domain.model.Car
import ru.haykor.carmanager.core.domain.model.FieldError
import ru.haykor.carmanager.core.domain.model.RecordField
import ru.haykor.carmanager.core.domain.model.ServiceCategory
import ru.haykor.carmanager.core.domain.model.ServiceRecordFormInput
import ru.haykor.carmanager.core.presentation.ui.FormTextField
import ru.haykor.carmanager.feature.serviceEditor.component.EditorMode
import ru.haykor.carmanager.feature.serviceEditor.component.ServiceEditorComponent
import ru.haykor.carmanager.feature.serviceEditor.component.ServiceEditorIntent
import ru.haykor.carmanager.feature.serviceEditor.component.ServiceEditorState
import ru.haykor.carmanager.feature.serviceEditor.component.ServiceEditorState.Form
import ru.haykor.carmanager.ui.theme.AppTheme

// region composables

@Composable
fun ServiceEditorScreen(component: ServiceEditorComponent, modifier: Modifier = Modifier) {
    val state by component.state.subscribeAsState()

    ServiceEditorContent(
        state = state,
        editorMode = component.editorMode,
        onIntent = component::accept,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ServiceEditorContent(
    state: ServiceEditorState,
    editorMode: EditorMode,
    onIntent: (ServiceEditorIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val isNew = editorMode is EditorMode.Create

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (isNew) {
                                R.string.create_service_record_title
                            } else {
                                R.string.edit_service_record_title
                            }
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onIntent(ServiceEditorIntent.BackClicked) }) {
                        Icon(
                            painter = painterResource(R.drawable.chevron_left),
                            contentDescription = stringResource(R.string.go_back)
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            if (state is Form) {
                SaveBar(
                    isSaving = state.isSaving,
                    onClick = { onIntent(ServiceEditorIntent.SaveClicked) }
                )
            }
        },
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { padding ->
        when (state) {
            ServiceEditorState.Loading -> LoadingContent(Modifier.padding(padding))

            ServiceEditorState.NotFound -> NotFoundContent(
                onBackClick = { onIntent(ServiceEditorIntent.BackClicked) },
                modifier = Modifier.padding(padding)
            )

            is Form -> ServiceEditorForm(
                form = state,
                onIntent = onIntent,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun ServiceEditorForm(form: Form, onIntent: (ServiceEditorIntent) -> Unit, modifier: Modifier = Modifier) {
    val input = form.input

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        FormSection(stringResource(R.string.service_editor_section_car)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.service_editor_car),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${form.car.brand} ${form.car.model}",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        FormSection(stringResource(R.string.service_editor_section_record)) {
            CategoryPicker(
                selectedName = form.categories.first { it.id == input.categoryId }.name,
                categories = form.categories,
                enabled = !form.isSaving,
                onSelect = { onIntent(ServiceEditorIntent.CategoryPicked(it)) },
                modifier = Modifier.fillMaxWidth()
            )

            DatePickerField(
                date = input.date,
                enabled = !form.isSaving,
                onDatePicked = { onIntent(ServiceEditorIntent.DatePicked(it)) }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ServiceField(
                    field = RecordField.AT_MILEAGE,
                    value = input.atMileage,
                    form = form,
                    onIntent = onIntent,
                    suffix = stringResource(R.string.unit_km),
                    inputFilter = ::digitsOnly,
                    keyboardOptions = NumberKeyboard,
                    modifier = Modifier.weight(1f)
                )
                ServiceField(
                    field = RecordField.PRICE,
                    value = input.price,
                    form = form,
                    onIntent = onIntent,
                    hint = stringResource(R.string.hint_optional),
                    inputFilter = ::digitsOnly,
                    keyboardOptions = NumberKeyboard,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        FormSection(stringResource(R.string.service_editor_section_note)) {
            ServiceField(
                field = RecordField.NOTE,
                value = input.note,
                form = form,
                onIntent = onIntent,
                singleLine = false,
                minLines = 1,
                maxLines = 6,
                hint = stringResource(R.string.hint_optional),
                keyboardOptions = NoteKeyboard
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun FormSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
        )
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
        }
    }
}

@Composable
private fun ServiceField(
    field: RecordField,
    value: String,
    form: Form,
    onIntent: (ServiceEditorIntent) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    hint: String? = null,
    suffix: String? = null,
    inputFilter: (String) -> String = { it },
    keyboardOptions: KeyboardOptions = TextKeyboard
) {
    FormTextField(
        value = value,
        onValueChange = { onIntent(ServiceEditorIntent.FieldChanged(field, inputFilter(it))) },
        onFocusLost = { onIntent(ServiceEditorIntent.FieldBlurred(field)) },
        label = field.label(),
        error = form.errors[field]?.message(field),
        hint = hint,
        suffix = suffix,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        enabled = !form.isSaving,
        keyboardOptions = keyboardOptions,
        modifier = modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryPicker(
    selectedName: String,
    categories: List<ServiceCategory>,
    enabled: Boolean,
    onSelect: (ServiceCategoryId) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = enabled && it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            enabled = enabled,
            shape = MaterialTheme.shapes.medium,
            label = { Text(stringResource(R.string.service_editor_category)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = {
                        onSelect(category.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerField(date: Long, enabled: Boolean, onDatePicked: (Long) -> Unit) {
    var showDatePicker by remember { mutableStateOf(false) }
    val formattedDate = remember(date) {
        DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(Date(date))
    }

    OutlinedTextField(
        value = formattedDate,
        onValueChange = {},
        readOnly = true,
        singleLine = true,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        label = { Text(stringResource(R.string.service_editor_date)) },
        trailingIcon = {
            IconButton(onClick = { showDatePicker = true }, enabled = enabled) {
                Icon(
                    painter = painterResource(R.drawable.calendar),
                    contentDescription = stringResource(R.string.service_editor_date)
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { showDatePicker = true }
    )

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = date.toDatePickerMillis())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { onDatePicked(it.fromDatePickerMillis()) }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun NotFoundContent(onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.service_editor_unavailable),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.service_editor_unavailable_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        TextButton(onClick = onBackClick) {
            Text(stringResource(R.string.go_back))
        }
    }
}

@Composable
private fun SaveBar(isSaving: Boolean, onClick: () -> Unit) {
    Surface(tonalElevation = 3.dp) {
        Button(
            onClick = onClick,
            enabled = !isSaving,
            shapes = ButtonDefaults.shapes(),
            contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight),
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .height(52.dp)
        ) {
            if (isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(stringResource(R.string.save))
            }
        }
    }
}

// endregion

// region utils

private val TextKeyboard = KeyboardOptions(
    capitalization = KeyboardCapitalization.Sentences,
    imeAction = ImeAction.Next
)

private val NumberKeyboard = KeyboardOptions(
    keyboardType = KeyboardType.Number,
    imeAction = ImeAction.Next
)

private val NoteKeyboard = KeyboardOptions(
    capitalization = KeyboardCapitalization.Sentences,
    imeAction = ImeAction.Default
)

private fun digitsOnly(value: String): String = value.filter(Char::isDigit)

private fun Long.toDatePickerMillis(): Long = Instant.ofEpochMilli(this)
    .atZone(ZoneId.systemDefault())
    .toLocalDate()
    .atStartOfDay(ZoneOffset.UTC)
    .toInstant()
    .toEpochMilli()

private fun Long.fromDatePickerMillis(): Long = Instant.ofEpochMilli(this)
    .atZone(ZoneOffset.UTC)
    .toLocalDate()
    .atStartOfDay(ZoneId.systemDefault())
    .toInstant()
    .toEpochMilli()

@Composable
private fun RecordField.label(): String = stringResource(
    when (this) {
        RecordField.AT_MILEAGE -> R.string.field_mileage
        RecordField.NOTE -> R.string.field_note
        RecordField.PRICE -> R.string.service_editor_price
    }
)

@Composable
private fun FieldError.message(field: RecordField): String = when (this) {
    FieldError.Required -> stringResource(R.string.error_required)

    FieldError.Invalid -> when (field) {
        RecordField.AT_MILEAGE -> stringResource(R.string.error_invalid_mileage)
        RecordField.PRICE -> stringResource(R.string.error_invalid_price)
        RecordField.NOTE -> stringResource(R.string.error_invalid)
    }

    is FieldError.TooLong -> stringResource(R.string.error_too_long, maxLength)
}

// endregion

// region previews

@Preview(name = "Create service record", showBackground = true)
@Preview(name = "Create service record · dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ServiceEditorPreview() {
    val carId = CarId("car-preview")
    val categoryId = ServiceCategoryId("category-preview")
    AppTheme {
        ServiceEditorContent(
            state = Form(
                input = ServiceRecordFormInput(
                    carId = carId,
                    categoryId = categoryId,
                    atMileage = "120000",
                    price = "1500",
                    date = System.currentTimeMillis()
                ),
                car = Car(
                    id = carId,
                    brand = "Toyota",
                    model = "Camry",
                    year = 2018,
                    note = null,
                    vin = null,
                    plateCountry = null,
                    plate = null,
                    mileage = 120000,
                    photoUri = null,
                    createdAt = 0,
                    updatedAt = 0
                ),
                categories = listOf(
                    ServiceCategory(categoryId, "Maintenance")
                )
            ),
            editorMode = EditorMode.Create(carId),
            onIntent = {}
        )
    }
}
