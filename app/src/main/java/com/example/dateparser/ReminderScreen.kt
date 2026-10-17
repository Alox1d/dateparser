package com.example.dateparser

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.github.alox1d.dateparser.sample.parseReminderDraft
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.TimeZone

private data class SavedReminder(
    val title: String,
    val startsAt: Instant
)

private val dateFormat = DateTimeFormatter.ofPattern("EEE, MMM d yyyy · HH:mm")

@Composable
internal fun ReminderScreen() {
    val context = LocalContext.current
    val zone = remember { TimeZone.currentSystemDefault() }
    val zoneId = remember { ZoneId.of(zone.id) }
    var input by remember { mutableStateOf("Buy milk tomorrow at 7pm") }
    var title by remember { mutableStateOf("") }
    var chosenTime by remember { mutableStateOf<LocalDateTime?>(null) }
    var parsedInstant by remember { mutableStateOf<Instant?>(null) }
    var edited by remember { mutableStateOf(false) }
    var previewVisible by remember { mutableStateOf(false) }
    var explanation by remember { mutableStateOf("") }
    val saved = remember { mutableStateListOf<SavedReminder>() }

    fun preview() {
        val result = parseReminderDraft(input, Clock.System.now(), zone)
        previewVisible = true
        edited = false
        parsedInstant = null
        if (result == null || result.endsAt != null) {
            title = input.trim()
            chosenTime = null
            explanation = if (result?.endsAt != null) {
                "This example saves one time, not a range. Enter a single time or select it manually."
            } else {
                "The date or time is unclear. Try 7pm, 19:00, or choose a date and time."
            }
        } else {
            // Never treat a date-only phrase as a reminder title.
            title = result.title
            parsedInstant = result.startsAt
            chosenTime = LocalDateTime.ofInstant(
                java.time.Instant.parse(result.startsAt.toString()), zoneId
            )
            explanation = "Recognized offline. Review the title and time before adding."
        }
    }

    val resolved: Instant? = chosenTime?.let { local ->
        if (!edited) parsedInstant
        else zoneId.rules.getValidOffsets(local).singleOrNull()?.let { offset ->
            Instant.parse(local.toInstant(offset).toString())
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().imePadding()
            .semantics { testTagsAsResourceId = true }
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Reminders", style = MaterialTheme.typography.headlineMedium)
                Text("Natural-language dates, parsed entirely offline",
                    style = MaterialTheme.typography.bodyMedium)
            }
        }
        item {
            OutlinedTextField(
                value = input, onValueChange = { input = it },
                label = { Text("Describe your reminder") },
                placeholder = { Text("Buy milk tomorrow at 7pm") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2, maxLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { preview() })
            )
        }
        item {
            Button(onClick = { preview() }, modifier = Modifier.fillMaxWidth()) {
                Text("Preview reminder")
            }
        }
        if (previewVisible) {
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Check before saving", style = MaterialTheme.typography.titleLarge)
                        Text(explanation, style = MaterialTheme.typography.bodyMedium)
                        OutlinedTextField(
                            value = title, onValueChange = { title = it },
                            label = { Text("Reminder title") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Text(
                            chosenTime?.format(dateFormat) ?: "No date selected",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text("Timezone: " + zone.id, style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                val initial = chosenTime ?: LocalDateTime.now(zoneId).plusDays(1)
                                DatePickerDialog(context, { _, y, month, day ->
                                    chosenTime = LocalDateTime.of(
                                        y, month + 1, day, initial.hour, initial.minute
                                    )
                                    edited = true
                                }, initial.year, initial.monthValue - 1, initial.dayOfMonth).show()
                            }) { Text("Edit date") }
                            OutlinedButton(enabled = chosenTime != null, onClick = {
                                val initial = chosenTime ?: return@OutlinedButton
                                TimePickerDialog(context, { _, hour, minute ->
                                    chosenTime = initial.withHour(hour).withMinute(minute)
                                    edited = true
                                }, initial.hour, initial.minute, true).show()
                            }) { Text("Edit time") }
                        }
                        if (chosenTime != null && resolved == null) {
                            Text(
                                if (edited) "Selected local time is invalid or overlaps a DST transition."
                                else "Choose a date and time to continue.",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Button(
                            enabled = title.isNotBlank() && resolved != null,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                val instant = resolved ?: return@Button
                                saved.add(0, SavedReminder(title.trim(), instant))
                                previewVisible = false
                                input = ""
                            }
                        ) { Text("Add reminder") }
                    }
                }
            }
        }
        item {
            Text("Saved reminders (" + saved.size + ")",
                style = MaterialTheme.typography.titleLarge)
        }
        if (saved.isEmpty()) {
            item {
                Text("Your confirmed reminders will appear here.",
                    style = MaterialTheme.typography.bodyMedium)
            }
        }
        items(saved) { reminder ->
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(reminder.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        LocalDateTime.ofInstant(
                            java.time.Instant.parse(reminder.startsAt.toString()), zoneId
                        ).format(dateFormat) + " · " + zone.id,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
