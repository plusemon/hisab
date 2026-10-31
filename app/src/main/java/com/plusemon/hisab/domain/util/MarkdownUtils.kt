package com.plusemon.hisab.domain.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

object MarkdownUtils {

    fun parseMarkdown(input: String): AnnotatedString {
        if (input.isBlank()) return AnnotatedString("")

        return buildAnnotatedString {
            val lines = input.lines()
            lines.forEachIndexed { index, line ->
                var trimmedLine = line.trim()

                var isHeader = false
                if (trimmedLine.startsWith("#")) {
                    isHeader = true
                    trimmedLine = trimmedLine.dropWhile { it == '#' }.trim()
                }

                if (trimmedLine.startsWith("* ") || trimmedLine.startsWith("- ")) {
                    trimmedLine = "• " + trimmedLine.substring(2)
                }

                if (isHeader) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        appendFormattedLine(trimmedLine)
                    }
                } else {
                    appendFormattedLine(trimmedLine)
                }

                if (index < lines.size - 1) {
                    append("\n")
                }
            }
        }
    }

    private fun AnnotatedString.Builder.appendFormattedLine(line: String) {
        // Convert markdown links [label](url) -> label
        val linkCleanedLine = line.replace(Regex("\\[([^\\]]+)]\\([^)]+\\)"), "$1")
        val regex = Regex("\\*\\*(.*?)\\*\\*|__(.*?)__")
        var lastIndex = 0

        for (match in regex.findAll(linkCleanedLine)) {
            if (match.range.first > lastIndex) {
                append(linkCleanedLine.substring(lastIndex, match.range.first))
            }

            val boldText = match.groups[1]?.value ?: match.groups[2]?.value ?: ""
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(boldText)
            }

            lastIndex = match.range.last + 1
        }

        if (lastIndex < linkCleanedLine.length) {
            append(linkCleanedLine.substring(lastIndex))
        }
    }

    fun deduplicateReleaseNotes(notes: String): String {
        if (notes.isBlank()) return notes
        val lines = notes.lines()
        val distinctLines = mutableListOf<String>()
        for (rawLine in lines) {
            // Skip auto-generated "Full Changelog" lines
            if (rawLine.contains("Full Changelog", ignoreCase = true) && rawLine.contains("http", ignoreCase = true)) {
                continue
            }
            // Strip trailing "in https://github.com/..." from GitHub auto-generated PR lines and raw URLs
            val cleanedLine = rawLine
                .replace(Regex("\\s+in\\s+https?://\\S+"), "")
                .replace(Regex("\\[([^\\]]+)]\\([^)]+\\)"), "$1")
                .replace(Regex("https?://\\S+"), "")
                .trimEnd()

            val trimmed = cleanedLine.trim()
            if (trimmed.isEmpty() || trimmed == "## What's Changed" || trimmed == "### What's Changed") {
                if (trimmed.isEmpty() && distinctLines.isNotEmpty() && distinctLines.last().isNotBlank()) {
                    distinctLines.add("")
                }
            } else if (distinctLines.none { it.trim() == trimmed }) {
                distinctLines.add(cleanedLine)
            }
        }
        return distinctLines.joinToString("\n").trim()
    }
}
