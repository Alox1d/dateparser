package com.example.dateparser

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import io.github.alox1d.dateparser.DateParser
import kotlinx.datetime.TimeZone

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val parser = DateParser()
        val examples = listOf("купить через 5 часов", "meeting tomorrow at 17:30")
        val results = examples.joinToString("\n\n") { text ->
            val matches = parser.parse(text, timeZone = TimeZone.currentSystemDefault())
            text + "\n" + matches.joinToString("\n") { it.targetDate.toString() }
        }
        setContentView(TextView(this).apply { text = results; setPadding(32, 48, 32, 32) })
    }
}
