package io.github.alox1d.dateparser.sample

import io.github.alox1d.dateparser.DateOrder
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun main(args: Array<String>) {
    val reference = Instant.parse(args.getOrNull(2) ?: "2026-07-03T10:15:00Z")
    println("DateParser Kotlin 0.1.1 | published KMP consumer")
    println("Reference: $reference")
    if (args.isNotEmpty()) {
        val order = args.getOrNull(1)?.takeUnless { it == "NONE" }?.let(DateOrder::valueOf)
        val zone = TimeZone.of(args.getOrNull(3) ?: "UTC")
        showPreview(args[0], reference, zone, order)
        return
    }
    showPreview("Buy milk, tomorrow from 5PM to 8PM", reference, TimeZone.UTC)
    showPreview("Meet on July 4, 2026 at 5PM", reference, TimeZone.of("Europe/Berlin"))
    showPreview("Meet on 4th July 2026 at 5 pm", reference, TimeZone.of("America/New_York"))
    showPreview("03/04/2026", reference, TimeZone.UTC, DateOrder.MDY)
    showPreview("03/04/2026", reference, TimeZone.UTC, DateOrder.DMY)
    showPreview("03/04/2026", reference, TimeZone.UTC)
    showPreview("every 2 days", reference, TimeZone.UTC)
}

private fun showPreview(text: String, reference: Instant, zone: TimeZone, order: DateOrder? = null) {
    println("\nInput: $text")
    println("Zone: ${zone.id} | Slash order: ${order?.name ?: "not selected"}")
    val draft = parseReminderDraft(text, reference, zone, order)
    if (draft == null) {
        println("Review needed: choose a date order or enter the date manually; recurrence needs a separate flow.")
        return
    }
    println("Title: ${draft.title.ifBlank { "(add a title)" }}")
    println("Start: ${draft.startsAt.toLocalDateTime(zone)} | UTC: ${draft.startsAt}")
    draft.endsAt?.let { println("End: ${it.toLocalDateTime(zone)} | UTC: $it") }
    println("Preview only: confirm or edit before saving.")
}
