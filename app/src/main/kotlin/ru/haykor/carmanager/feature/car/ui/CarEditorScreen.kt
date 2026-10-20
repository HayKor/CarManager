package ru.haykor.carmanager.feature.car.ui

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.haykor.carmanager.R
import ru.haykor.carmanager.core.domain.PlateCountry
import ru.haykor.carmanager.core.domain.model.CarFormInput
import ru.haykor.carmanager.core.domain.model.Field
import ru.haykor.carmanager.core.domain.model.FieldError
import ru.haykor.carmanager.core.presentation.ui.FormTextField
import ru.haykor.carmanager.feature.car.component.CarEditorComponent
import ru.haykor.carmanager.feature.car.component.CarEditorEvent
import ru.haykor.carmanager.feature.car.component.CarEditorIntent
import ru.haykor.carmanager.feature.car.component.CarEditorState
import ru.haykor.carmanager.ui.theme.AppTheme

@Composable
fun CarEditorScreen(component: CarEditorComponent, modifier: Modifier = Modifier) {
    val state by component.state.subscribeAsState()

    val context = LocalContext.current
    LaunchedEffect(component.events) {
        component.events.collect { event ->
            when (event) {
                is CarEditorEvent.SaveFailed -> {
                    Toast.makeText(context, event.error.toUiText(), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    CarEditorContent(
        state = state,
        onIntent = component::accept,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CarEditorContent(
    state: CarEditorState,
    onIntent: (CarEditorIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val isNew = (state as? CarEditorState.Form)?.isNew ?: false

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isNew) {
                            stringResource(R.string.create_car_title)
                        } else {
                            stringResource(R.string.edit_car_title)
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onIntent(CarEditorIntent.BackClicked) }) {
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
            if (state is CarEditorState.Form) {
                SaveBar(
                    isSaving = state.isSaving,
                    onClick = { onIntent(CarEditorIntent.SaveClicked) }
                )
            }
        },
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { padding ->
        when (state) {
            // Room answers in milliseconds, a spinner would only flicker
            CarEditorState.Loading -> Unit

            CarEditorState.NotFound -> CarEditorNotFound(
                onBackClick = { onIntent(CarEditorIntent.BackClicked) },
                modifier = Modifier.padding(padding)
            )

            is CarEditorState.Form -> CarEditorForm(
                form = state,
                onIntent = onIntent,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun CarEditorForm(
    form: CarEditorState.Form,
    onIntent: (CarEditorIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val input = form.input
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        SectionHeader(stringResource(R.string.car_editor_section_car))

        CarField(Field.BRAND, input.brand, form, onIntent, keyboardOptions = TextKeyboard)
        CarField(Field.MODEL, input.model, form, onIntent, keyboardOptions = TextKeyboard)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CarField(
                field = Field.YEAR,
                value = input.year,
                form = form,
                onIntent = onIntent,
                inputFilter = ::digitsOnly,
                keyboardOptions = NumberKeyboard,
                modifier = Modifier.weight(1f)
            )
            CarField(
                field = Field.MILEAGE,
                value = input.mileage,
                form = form,
                onIntent = onIntent,
                suffix = stringResource(R.string.unit_km),
                inputFilter = ::digitsOnly,
                keyboardOptions = NumberKeyboard,
                modifier = Modifier.weight(1.4f)
            )
        }

        Spacer(Modifier.height(8.dp))
        SectionHeader(stringResource(R.string.car_editor_section_registration))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PlateCountryPicker(
                selected = input.plateCountry,
                onSelect = { onIntent(CarEditorIntent.PlateCountryChanged(it)) },
                enabled = !form.isSaving,
                modifier = Modifier.width(120.dp)
            )
            CarField(
                field = Field.PLATE,
                value = input.plate,
                form = form,
                onIntent = onIntent,
                hint = stringResource(R.string.hint_optional),
                placeholder = input.plateCountry.example,
                keyboardOptions = CodeKeyboard,
                modifier = Modifier.weight(1f)
            )
        }

        CarField(
            field = Field.VIN,
            value = input.vin,
            form = form,
            onIntent = onIntent,
            hint = stringResource(R.string.hint_vin),
            keyboardOptions = CodeKeyboard.copy(imeAction = ImeAction.Done),
            // Clearing focus fires onFocusLost, so the last field gets validated too
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
        )
    }
}

/** Binds a [FormTextField] to one [Field] of the form: value, error, events. */
@Composable
private fun CarField(
    field: Field,
    value: String,
    form: CarEditorState.Form,
    onIntent: (CarEditorIntent) -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
    placeholder: String? = null,
    suffix: String? = null,
    inputFilter: (String) -> String = { it },
    keyboardOptions: KeyboardOptions = TextKeyboard,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    FormTextField(
        value = value,
        onValueChange = { onIntent(CarEditorIntent.FieldChanged(field, inputFilter(it))) },
        onFocusLost = { onIntent(CarEditorIntent.FieldBlurred(field)) },
        label = field.label(),
        error = form.errors[field]?.message(field, form.input.plateCountry),
        hint = hint,
        placeholder = placeholder,
        suffix = suffix,
        enabled = !form.isSaving,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        modifier = modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlateCountryPicker(
    selected: PlateCountry,
    onSelect: (PlateCountry) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = enabled && it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = "${selected.flag} ${selected.name}",
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            enabled = enabled,
            label = { Text(stringResource(R.string.car_editor_country)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            PlateCountry.entries.forEach { country ->
                DropdownMenuItem(
                    text = { Text(country.displayName()) },
                    leadingIcon = { Text(country.flag) },
                    onClick = {
                        onSelect(country)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun SaveBar(isSaving: Boolean, onClick: () -> Unit) {
    Surface(tonalElevation = 3.dp) {
        Button(
            onClick = onClick,
            enabled = !isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .height(52.dp)
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = LocalContentColor.current
                )
            } else {
                Text(stringResource(R.string.save))
            }
        }
    }
}

@Composable
private fun CarEditorNotFound(onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.car_not_found),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.car_not_found_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onBackClick) {
            Text(stringResource(R.string.go_back))
        }
    }
}

// region Field presentation

private val TextKeyboard = KeyboardOptions(
    capitalization = KeyboardCapitalization.Words,
    imeAction = ImeAction.Next
)

private val NumberKeyboard = KeyboardOptions(
    keyboardType = KeyboardType.Number,
    imeAction = ImeAction.Next
)

private val CodeKeyboard = KeyboardOptions(
    capitalization = KeyboardCapitalization.Characters,
    autoCorrectEnabled = false,
    imeAction = ImeAction.Next
)

private fun digitsOnly(value: String): String = value.filter(Char::isDigit)

@Composable
private fun Field.label(): String = stringResource(
    when (this) {
        Field.BRAND -> R.string.field_brand
        Field.MODEL -> R.string.field_model
        Field.YEAR -> R.string.field_year
        Field.MILEAGE -> R.string.field_mileage
        Field.VIN -> R.string.field_vin
        Field.PLATE -> R.string.field_plate
    }
)

@Composable
private fun FieldError.message(field: Field, plateCountry: PlateCountry): String = when (this) {
    FieldError.Required -> stringResource(R.string.error_required)

    is FieldError.TooLong -> stringResource(R.string.error_too_long, maxLength)

    FieldError.Invalid -> when (field) {
        Field.YEAR -> stringResource(R.string.error_invalid_year)
        Field.MILEAGE -> stringResource(R.string.error_invalid_mileage)
        Field.VIN -> stringResource(R.string.error_invalid_vin)
        Field.PLATE -> stringResource(R.string.error_invalid_plate, plateCountry.example)
        Field.BRAND, Field.MODEL -> stringResource(R.string.error_invalid)
    }
}

/** Regional indicator symbols: "RU" → 🇷🇺 */
private val PlateCountry.flag: String
    get() = buildString {
        name.forEach { appendCodePoint(0x1F1E6 + (it - 'A')) }
    }

private val PlateCountry.example: String
    get() = when (this) {
        PlateCountry.RU -> "А123ВС77"
    }

@Composable
private fun PlateCountry.displayName(): String = stringResource(
    when (this) {
        PlateCountry.RU -> R.string.country_ru
    }
)

// endregion

// region Previews

@Preview(name = "Empty form", showBackground = true)
@Composable
private fun CarEditorEmptyPreview() {
    AppTheme {
        CarEditorContent(state = CarEditorState.Form(), onIntent = {})
    }
}

@Preview(name = "Errors", showBackground = true)
@Preview(name = "Errors · dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CarEditorErrorsPreview() {
    AppTheme {
        CarEditorContent(
            state = CarEditorState.Form(
                input = CarFormInput(brand = "Toyota", year = "1850", mileage = "120000", plate = "Д12ВС"),
                touched = Field.entries.toSet(),
                errors = mapOf(
                    Field.MODEL to FieldError.Required,
                    Field.YEAR to FieldError.Invalid,
                    Field.PLATE to FieldError.Invalid
                )
            ),
            onIntent = {}
        )
    }
}

@Preview(name = "Saving", showBackground = true)
@Composable
private fun CarEditorSavingPreview() {
    AppTheme {
        CarEditorContent(
            state = CarEditorState.Form(
                input = CarFormInput(brand = "Toyota", model = "Camry", year = "2018", mileage = "120000"),
                isSaving = true
            ),
            onIntent = {}
        )
    }
}

@Preview(name = "Not found", showBackground = true)
@Composable
private fun CarEditorNotFoundPreview() {
    AppTheme {
        CarEditorContent(state = CarEditorState.NotFound, onIntent = {})
    }
}

// endregion
