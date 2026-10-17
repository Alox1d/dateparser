package com.example.dateparser

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.github.alox1d.dateparser.DateParser
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private data class Reminder(val title: String, val instant: Instant)
private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { ReminderScreen() } }
    }
}

@Composable private fun ReminderScreen() {
    val context = LocalContext.current
    val parser = remember { DateParser() }
    var input by remember { mutableStateOf("Buy milk tomorrow at 7pm") }
    var title by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<LocalDateTime?>(null) }
    var preview by remember { mutableStateOf(false) }
    var warning by remember { mutableStateOf("") }
    val reminders = remember { mutableStateListOf<Reminder>() }
    val zone = remember { ZoneId.systemDefault() }
    fun recognize() {
        val match = parser.parse(input, Clock.System.now(), TimeZone.currentSystemDefault()).singleOrNull()
        if (match == null || match.recurrence != null) {
            warning = "Date/time is ambiguous or unsupported. Specify e.g. tomorrow at 7pm."
            title = input.trim()
            selected = null
        } else {
            title = match.remainingText.trim().trim(',', ' ', '-', '—').ifEmpty { input }
            selected = LocalDateTime.ofInstant(java.time.Instant.parse(match.instant.toString()), zone)
            warning = if (match.remainingText.contains(Regex("\\bat\\s+\\d+\\b"))) "Review the unrecognized time" else "Review before adding"
        }
        preview = true
    }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("DateParser · Reminders", style = MaterialTheme.typography.headlineSmall)
        Text("Offline natural-language date preview", style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(input, { input = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Reminder") },
            singleLine = false, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { recognize() }))
        Button(onClick = { recognize() }) { Text("Preview reminder") }
        if (preview) {
            HorizontalDivider()
            Text("Confirm the result", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(title, { title = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Editable title") })
            Text(warning)
            val date = selected
            Text(date?.format(formatter) ?: "Date not resolved")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    val initial = date ?: LocalDateTime.now().plusDays(1)
                    DatePickerDialog(context, { _, y, m, d -> selected = (selected ?: initial).withYear(y).withMonth(m + 1).withDayOfMonth(d) },
                        initial.year, initial.monthValue - 1, initial.dayOfMonth).show()
                }) { Text("Edit date") }
                OutlinedButton(onClick = {
                    val initial = date ?: LocalDateTime.now()
                    TimePickerDialog(context, { _, h, m -> selected = (selected ?: initial).withHour(h).withMinute(m) },
                        initial.hour, initial.minute, true).show()
                }) { Text("Edit time") }
            }
            Button(enabled = title.isNotBlank() && date != null, onClick = {
                val confirmed = selected ?: return@Button
                reminders.add(0, Reminder(title.trim(), Instant.parse(confirmed.atZone(zone).toInstant().toString())))
                preview = false
                input = ""
            }) { Text("Add reminder") }
        }
        HorizontalDivider()
        Text("Saved in this session (${reminders.size})", style = MaterialTheme.typography.titleMedium)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(reminders) { reminder ->
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(reminder.title, style = MaterialTheme.typography.titleMedium)
                        Text(LocalDateTime.ofInstant(java.time.Instant.parse(reminder.instant.toString()), zone).format(formatter))
                    }
                }
            }
        }
    }
}
