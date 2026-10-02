package com.example.core.model

data class ParsedImportItem(
    val type: SourceType,
    val url: String
)

data class ParsedImportSummary(
    val items: List<ParsedImportItem>,
    val playlistCount: Int,
    val singleVideoCount: Int,
    val profileCount: Int,
    val invalidLinesCount: Int,
    val duplicateCount: Int
) {
    val totalValidCount: Int get() = items.size
}
