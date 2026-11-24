package ru.haykor.carmanager.feature.serviceEditor.ui

import android.content.res.Configuration
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import io.github.vinceglb.filekit.*
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ru.haykor.carmanager.*
import ru.haykor.carmanager.core.designsystem.CmSectionHeader
import ru.haykor.carmanager.core.designsystem.FormTextField
import ru.haykor.carmanager.core.designsystem.UnsavedChangesDialog
import ru.haykor.carmanager.core.designsystem.cmSegmentShape
import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.core.domain.ServiceCategoryId
import ru.haykor.carmanager.core.domain.ServiceRecordId
import ru.haykor.carmanager.core.domain.model.*
import ru.haykor.carmanager.core.presentation.coroutines.ObserveAsEvents
import ru.haykor.carmanager.core.ui.CarCard
import ru.haykor.carmanager.core.ui.theme.AppTheme
import ru.haykor.carmanager.core.ui.toUiText
import ru.haykor.carmanager.feature.serviceEditor.component.*
import ru.haykor.carmanager.feature.serviceEditor.component.ServiceEditorState.Form
import java.text.DateFormat
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.*

// region composables

@Composable
fun ServiceEditorScreen(component: ServiceEditorComponent, modifier: Modifier = Modifier) {
    val state by component.state.subscribeAsState()

    val context = LocalContext.current
    ObserveAsEvents(component.events) { event ->
        when (event) {
            is ServiceEditorEvent.StorageError -> {
                Toast.makeText(context, getString(event.error.toUiText()), Toast.LENGTH_SHORT).show()
            }

            is ServiceEditorEvent.DeleteFailed -> {
                Toast.makeText(context, getString(event.error.toUiText()), Toast.LENGTH_SHORT).show()
            }

            is ServiceEditorEvent.PhotoImportFailed -> {
                Toast.makeText(context, getString(event.error.toUiText()), Toast.LENGTH_SHORT).show()
            }
        }
    }

    ServiceEditorContent(
        state = state,
        editorMode = component.editorMode,
        onIntent = component::accept,
        modifier = modifier
    )
}

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
                                Res.string.create_service_record_title
                            } else {
                                Res.string.edit_service_record_title
                            }
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onIntent(ServiceEditorIntent.BackClicked) }) {
                        Icon(
                            painter = painterResource(Res.drawable.chevron_left),
                            contentDescription = stringResource(Res.string.go_back)
                        )
                    }
                },
                actions = {
                    if (editorMode is EditorMode.Edit) {
                        IconButton(onClick = { onIntent(ServiceEditorIntent.DeleteClicked) }) {
                            Icon(
                                painter = painterResource(Res.drawable.delete),
                                contentDescription = null
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            if (state is Form) {
                SaveBar(
                    isDirty = state.isDirty,
                    isBusy = state.isBusy,
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

    ConfirmationDialog(
        conflict = form.mileageConflict,
        onConfirmUpdate = { onIntent(ServiceEditorIntent.UpdateCarMileageClicked) },
        onConfirmKeep = { onIntent(ServiceEditorIntent.KeepCarMileageClicked) },
        onDismiss = { onIntent(ServiceEditorIntent.DismissSaveClicked) }
    )

    if (form.isExitDialogVisible) {
        UnsavedChangesDialog(
            onDismiss = { onIntent(ServiceEditorIntent.DialogDismissed) },
            onSave = { onIntent(ServiceEditorIntent.DialogConfirmClicked) },
            onDiscard = { onIntent(ServiceEditorIntent.DialogIgnoreClicked) }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column {
            CmSectionHeader(stringResource(Res.string.service_editor_section_car))
            CarCard(
                car = form.car,
                onClick = {},
                shape = cmSegmentShape(0, 1)
            )
        }

        Spacer(Modifier.height(8.dp))
        FormSection(stringResource(Res.string.service_editor_section_record)) {
            CategoryPicker(
                selectedName = form.categories.first { it.id == input.categoryId }.name,
                categories = form.categories,
                enabled = !form.isBusy,
                onSelect = { onIntent(ServiceEditorIntent.CategoryPicked(it)) },
                modifier = Modifier.fillMaxWidth()
            )

            DatePickerField(
                date = input.date,
                enabled = !form.isBusy,
                onDatePicked = { onIntent(ServiceEditorIntent.DatePicked(it)) }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ServiceField(
                    field = RecordField.AT_MILEAGE,
                    value = input.atMileage,
                    form = form,
                    onIntent = onIntent,
                    suffix = stringResource(Res.string.unit_km),
                    inputFilter = ::digitsOnly,
                    keyboardOptions = NumberKeyboard,
                    modifier = Modifier.weight(1f)
                )
                ServiceField(
                    field = RecordField.PRICE,
                    value = input.price,
                    form = form,
                    onIntent = onIntent,
                    hint = stringResource(Res.string.hint_optional),
                    inputFilter = ::digitsOnly,
                    keyboardOptions = NumberKeyboard,
                    modifier = Modifier.weight(1f)
                )
            }
            PhotoPicker(
                photoPath = form.input.photoUri,
                onImageClicked = { onIntent(ServiceEditorIntent.ImageClicked) },
                onPicked = { image ->
                    onIntent(ServiceEditorIntent.ImagePicked(image?.path))
                },
                onRemoveClicked = { onIntent(ServiceEditorIntent.ImageUnpinned) }
            )
        }

        Spacer(Modifier.height(8.dp))
        FormSection(stringResource(Res.string.service_editor_section_note)) {
            ServiceField(
                field = RecordField.NOTE,
                value = input.note,
                form = form,
                onIntent = onIntent,
                singleLine = false,
                minLines = 1,
                maxLines = 6,
                hint = stringResource(Res.string.hint_optional),
                keyboardOptions = NoteKeyboard
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

/**
 * Passes [io.github.vinceglb.filekit.PlatformFile].`path` to the component
 */
@Composable
private fun PhotoPicker(
    photoPath: String?,
    onImageClicked: () -> Unit,
    onPicked: (PlatformFile?) -> Unit,
    onRemoveClicked: () -> Unit
) {
    val launcher = rememberFilePickerLauncher(
        type = FileKitType.Image,
        onError = { failure -> Log.d("PhotoPicker", "Picker failed: ${failure.message}") }
    ) { image ->
        // image is a PlatformFile, or null when the user cancelled
        Log.d("PhotoPicker", "Picked ${image?.path}")
        onPicked(image)
    }

    val tileSize = 80.dp
    val badgeSize = 28.dp
    val overhang = badgeSize / 2 // how far the badge sticks out past the tile corner

    // Outer box is larger than the tile so the badge stays inside its bounds (and stays tappable)
    Box(Modifier.size(tileSize + overhang)) {
        // The tile itself (clipped)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(top = overhang, end = overhang)
                .fillMaxSize()
                .clip(ListItemDefaults.shapes().shape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable(
                    onClick = if (photoPath != null) {
                        { onImageClicked() }
                    } else {
                        { launcher.launch() }
                    }
                )
        ) {
            if (photoPath != null) {
                val file = FileKit.filesDir.resolve(photoPath)
                AsyncImage(
                    model = file,
                    contentDescription = file.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
            } else {
                Icon(
                    painter = painterResource(Res.drawable.plus),
                    contentDescription = "Add photo"
                )
            }
        }

        // Remove badge, centered on the tile's top-right corner
        if (photoPath != null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(badgeSize)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    .clip(CircleShape)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = "Remove photo",
                        onClick = onRemoveClicked
                    )
            ) {
                Icon(
                    painter = painterResource(Res.drawable.close),
                    contentDescription = "Remove photo",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun ConfirmationDialog(
    conflict: MileageConflict?,
    onConfirmUpdate: () -> Unit,
    onConfirmKeep: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    conflict?.let { conflict ->
        val numberFormat = remember { NumberFormat.getIntegerInstance() }
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(stringResource(Res.string.mileage_conflict_title))
            },
            text = {
                Text(
                    stringResource(
                        Res.string.mileage_conflict_text,
                        numberFormat.format(conflict.carMileage),
                        numberFormat.format(conflict.recordMileage)
                    )
                )
            },
            confirmButton = {
                Row {
                    TextButton(
                        onClick = onConfirmKeep
                    ) {
                        Text(stringResource(Res.string.keep), color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(
                        onClick = onConfirmUpdate
                    ) {
                        Text(stringResource(Res.string.update), color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(Res.string.cancel))
                }
            },
            modifier = modifier

        )
    }
}

@Composable
private fun FormSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        CmSectionHeader(title)
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
        enabled = !form.isBusy,
        keyboardOptions = keyboardOptions,
        modifier = modifier.fillMaxWidth()
    )
}

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
            label = { Text(stringResource(Res.string.service_editor_category)) },
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
        label = { Text(stringResource(Res.string.service_editor_date)) },
        trailingIcon = {
            IconButton(onClick = { showDatePicker = true }, enabled = enabled) {
                Icon(
                    painter = painterResource(Res.drawable.calendar_month),
                    contentDescription = stringResource(Res.string.service_editor_date)
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
                    Text(stringResource(Res.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(Res.string.cancel))
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
            text = stringResource(Res.string.service_editor_unavailable),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.service_editor_unavailable_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        TextButton(onClick = onBackClick) {
            Text(stringResource(Res.string.go_back))
        }
    }
}

@Composable
private fun SaveBar(isDirty: Boolean, isBusy: Boolean, onClick: () -> Unit) {
    Surface(tonalElevation = 3.dp) {
        Button(
            onClick = onClick,
            enabled = !isBusy && isDirty,
            shapes = ButtonDefaults.shapes(),
            contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight),
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .height(52.dp)
        ) {
            if (isBusy) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(stringResource(Res.string.save))
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
        RecordField.AT_MILEAGE -> Res.string.field_mileage
        RecordField.NOTE -> Res.string.field_note
        RecordField.PRICE -> Res.string.service_editor_price
    }
)

@Composable
private fun RecordFieldError.message(field: RecordField): String = when (this) {
    Required -> stringResource(Res.string.error_required)

    Invalid -> when (field) {
        RecordField.AT_MILEAGE -> stringResource(Res.string.error_invalid_mileage)
        RecordField.PRICE -> stringResource(Res.string.error_invalid_price)
        RecordField.NOTE -> stringResource(Res.string.error_invalid)
    }

    is TooLong -> stringResource(Res.string.error_too_long, maxLength)

    is MileageOutOfSequence -> toText()
}

@Composable
private fun MileageOutOfSequence.toText(): String {
    val nf = remember { NumberFormat.getIntegerInstance() }
    return when {
        min != null && max != null -> stringResource(Res.string.error_mileage_between, nf.format(min), nf.format(max))
        min != null -> stringResource(Res.string.error_mileage_min, nf.format(min))
        max != null -> stringResource(Res.string.error_mileage_max, nf.format(max))
        else -> stringResource(Res.string.error_mileage_invalid)
    }
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

@Preview(name = "Unsaved changes", showBackground = true)
@Preview(name = "Unsaved changes · dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ServiceEditorExitDialogPreview() {
    val carId = CarId("car-preview")
    val categoryId = ServiceCategoryId("category-preview")
    val date = 1_750_000_000_000L
    AppTheme {
        ServiceEditorContent(
            state = Form(
                initialInput = ServiceRecordFormInput(
                    carId = carId,
                    categoryId = categoryId,
                    atMileage = "120000",
                    price = "1500",
                    date = date
                ),
                input = ServiceRecordFormInput(
                    carId = carId,
                    categoryId = categoryId,
                    atMileage = "121000",
                    price = "1500",
                    date = date
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
                ),
                isExitDialogVisible = true
            ),
            editorMode = EditorMode.Create(carId),
            onIntent = {}
        )
    }
}

@Preview(name = "Mileage mismatch", showBackground = true)
@Preview(name = "Mileage mismatch · dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ServiceEditorDialogPreview() = AppTheme {
    val carId = CarId("car-preview")
    val categoryId = ServiceCategoryId("category-preview")
    ServiceEditorContent(
        state = Form(
            input = ServiceRecordFormInput(
                carId = carId,
                categoryId = categoryId,
                atMileage = "110000",
                price = "1500",
                date = System.currentTimeMillis()
            ),
            mileageConflict = MileageConflict(120_000L, 110_000L),
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

@Preview(name = "Edit service record", showBackground = true)
@Preview(name = "Edit service record · dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ServiceEditorEditPreview() {
    val carId = CarId("car-id")
    val recordId = ServiceRecordId("record-id")
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
            editorMode = EditorMode.Edit(recordId),
            onIntent = {}
        )
    }
}
