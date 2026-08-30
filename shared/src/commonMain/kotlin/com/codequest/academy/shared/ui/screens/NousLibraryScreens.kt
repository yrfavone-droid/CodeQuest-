package com.codequest.academy.shared.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import codequestacademy.shared.generated.resources.*
import com.codequest.academy.shared.data.*
import com.codequest.academy.shared.ui.components.PrimaryButton
import com.codequest.academy.shared.ui.components.SecondaryButton
import com.codequest.academy.shared.ui.navigation.Navigation
import com.codequest.academy.shared.ui.navigation.Screen
import com.codequest.academy.shared.ui.theme.AppTypography
import com.codequest.academy.shared.ui.theme.DisplayStyle
import com.codequest.academy.shared.ui.theme.Theme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
@Composable
fun NousLibraryScreen(kind: LibraryKind, navigation: Navigation, repository: ProgressRepository, documentHandler: AcademyDocumentHandler) {
    val items = remember(kind) { if (kind == LibraryKind.BOOK) repository.getNousBooks() else repository.getNousIntensiveFiles() }
    val title = if (kind == LibraryKind.BOOK) "Books" else "Intensive Files"
    val subtitle = if (kind == LibraryKind.BOOK) "Five core books, packaged locally for uninterrupted reading." else "Twenty optional technical files for focused, offline study."
    var actionMessage by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().background(Theme.colors.appBackground).verticalScroll(rememberScrollState()).padding(40.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        Text("NOUS AI ACADEMY", style = AppTypography.caption.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp), color = Theme.colors.brandPrimary)
        Text(title, style = DisplayStyle.copy(color = Theme.colors.textPrimary))
        Text(subtitle, style = AppTypography.body1, color = Theme.colors.textSecondary)
        actionMessage?.let { Text(it, style = AppTypography.body2, color = Theme.colors.textSecondary) }
        items.chunked(if (kind == LibraryKind.BOOK) 3 else 4).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                row.forEach { item ->
                    val readerState = repository.getReaderState(item.id)
                    NousDocumentCard(item, readerState, Modifier.weight(1f), onRead = { navigation.navigateTo(Screen.NousReader(item.id)) }, onDownload = {
                        val result = documentHandler.downloadPdf(item)
                        actionMessage = result.message
                    })
                }
                repeat((if (kind == LibraryKind.BOOK) 3 else 4) - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun NousDocumentCard(item: AcademyLibraryItem, readerState: ReaderState, modifier: Modifier, onRead: () -> Unit, onDownload: () -> Unit) {
    val readingProgress = if (readerState.page <= 1) 0 else (((readerState.page - 1).toFloat() / (item.pageCount - 1).coerceAtLeast(1)) * 100).toInt().coerceIn(0, 100)
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(Theme.colors.surfacePrimary).border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(14.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Image(painterResource(coverPainter(item.coverResource)), "Cover preview for ${item.title}", Modifier.fillMaxWidth().height(if (item.kind == LibraryKind.BOOK) 210.dp else 140.dp).clip(RoundedCornerShape(9.dp)), contentScale = ContentScale.Crop)
        Text(item.id, style = AppTypography.caption.copy(fontWeight = FontWeight.Bold), color = Theme.colors.brandPrimary)
        Text(item.title, style = AppTypography.body1.copy(fontWeight = FontWeight.SemiBold), color = Theme.colors.textPrimary, maxLines = 3)
        Text(if (item.kind == LibraryKind.BOOK) "Core Book · ${item.pageCount} pages" else "Intensive File · ${item.pageCount} pages", style = AppTypography.caption, color = Theme.colors.textSecondary)
        Text("$readingProgress% read", style = AppTypography.caption.copy(fontWeight = FontWeight.Bold), color = Theme.colors.textMuted)
        if (item.kind == LibraryKind.BOOK) PrimaryButton("Continue reading", onClick = onRead) else PrimaryButton("Open file", onClick = onRead)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { SecondaryButton(if (item.kind == LibraryKind.BOOK) "Open book" else "Read", onClick = onRead); SecondaryButton("Download", onClick = onDownload) }
    }
}

@Composable
fun NousReaderScreen(item: AcademyLibraryItem, navigation: Navigation, repository: ProgressRepository, documentHandler: AcademyDocumentHandler, reader: OfflinePdfReader) {
    var state by remember(item.id) { mutableStateOf(repository.getReaderState(item.id)) }
    var bookmarks by remember(item.id) { mutableStateOf(repository.getReaderBookmarks(item.id)) }
    var query by remember { mutableStateOf("") }
    val pageCount = remember(item.id) { reader.pageCount(item).coerceAtLeast(item.pageCount) }
    val image = remember(item.id, state.page, state.zoom) { reader.renderPage(item, state.page, state.zoom) }
    val searchResults = remember(item.id, query) { if (query.trim().length >= 2) reader.searchPages(item, query) else emptyList() }
    fun save(next: ReaderState) { state = next; repository.saveReaderState(item.id, next.page, next.zoom) }
    Column(Modifier.fillMaxSize().background(Theme.colors.appBackground).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            SecondaryButton("Back", onClick = navigation::pop)
            Column(Modifier.weight(1f).padding(horizontal = 16.dp)) { Text(item.title, style = AppTypography.h3, color = Theme.colors.textPrimary); Text("Offline PDF · Page ${state.page} of $pageCount", style = AppTypography.caption, color = Theme.colors.textSecondary) }
            SecondaryButton(if (bookmarks.contains(state.page)) "Bookmarked" else "Bookmark", onClick = { repository.toggleReaderBookmark(item.id, state.page); bookmarks = repository.getReaderBookmarks(item.id) })
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            SecondaryButton("‹ Previous", enabled = state.page > 1, onClick = { save(state.copy(page = state.page - 1)) })
            SecondaryButton("Next ›", enabled = state.page < pageCount, onClick = { save(state.copy(page = state.page + 1)) })
            SecondaryButton("−", enabled = state.zoom > .7f, onClick = { save(state.copy(zoom = state.zoom - .1f)) })
            Text("${(state.zoom * 100).toInt()}%", style = AppTypography.caption, color = Theme.colors.textSecondary)
            SecondaryButton("+", enabled = state.zoom < 2.5f, onClick = { save(state.copy(zoom = state.zoom + .1f)) })
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search this PDF") },
                singleLine = true,
                modifier = Modifier.width(220.dp)
            )
            SecondaryButton("Open externally", onClick = { documentHandler.openPdf(item) })
        }
        if (searchResults.isNotEmpty()) Text("Matches: ${searchResults.joinToString { "page $it" }}", style = AppTypography.caption, color = Theme.colors.brandPrimary)
        if (reader.tableOfContents(item).isNotEmpty()) Text("Contents: ${reader.tableOfContents(item).take(6).joinToString(" · ")}", style = AppTypography.caption, color = Theme.colors.textMuted)
        Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(12.dp)).background(Color.White).verticalScroll(rememberScrollState()), contentAlignment = Alignment.TopCenter) {
            if (image != null) Image(image, "${item.title}, page ${state.page}", Modifier.fillMaxWidth().padding(16.dp), contentScale = ContentScale.Fit)
            else Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text("This page could not be rendered locally.", style = AppTypography.body1, color = Theme.colors.textPrimary); Spacer(Modifier.height(10.dp)); PrimaryButton("Open PDF", onClick = { documentHandler.openPdf(item) }) }
        }
    }
}

@OptIn(ExperimentalResourceApi::class)
private fun coverPainter(key: String) = when (key) {
    "nous_book_01" -> Res.drawable.nous_book_01; "nous_book_02" -> Res.drawable.nous_book_02; "nous_book_03" -> Res.drawable.nous_book_03; "nous_book_04" -> Res.drawable.nous_book_04; "nous_book_05" -> Res.drawable.nous_book_05
    "nous_file_01" -> Res.drawable.nous_file_01; "nous_file_02" -> Res.drawable.nous_file_02; "nous_file_03" -> Res.drawable.nous_file_03; "nous_file_04" -> Res.drawable.nous_file_04; "nous_file_05" -> Res.drawable.nous_file_05
    "nous_file_06" -> Res.drawable.nous_file_06; "nous_file_07" -> Res.drawable.nous_file_07; "nous_file_08" -> Res.drawable.nous_file_08; "nous_file_09" -> Res.drawable.nous_file_09; "nous_file_10" -> Res.drawable.nous_file_10
    "nous_file_11" -> Res.drawable.nous_file_11; "nous_file_12" -> Res.drawable.nous_file_12; "nous_file_13" -> Res.drawable.nous_file_13; "nous_file_14" -> Res.drawable.nous_file_14; "nous_file_15" -> Res.drawable.nous_file_15
    "nous_file_16" -> Res.drawable.nous_file_16; "nous_file_17" -> Res.drawable.nous_file_17; "nous_file_18" -> Res.drawable.nous_file_18; "nous_file_19" -> Res.drawable.nous_file_19; else -> Res.drawable.nous_file_20
}
