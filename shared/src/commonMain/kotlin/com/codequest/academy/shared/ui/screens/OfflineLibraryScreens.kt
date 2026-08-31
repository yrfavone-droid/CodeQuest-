package com.codequest.academy.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codequest.academy.shared.data.LibraryBookmark
import com.codequest.academy.shared.data.LibraryKind
import com.codequest.academy.shared.data.LibraryReadingState
import com.codequest.academy.shared.data.OfflineLibraryResource
import com.codequest.academy.shared.data.ProgressRepository
import com.codequest.academy.shared.documents.DocumentActionResult
import com.codequest.academy.shared.documents.OfflineDocumentActions
import com.codequest.academy.shared.ui.components.PrimaryButton
import com.codequest.academy.shared.ui.components.SecondaryButton
import com.codequest.academy.shared.ui.navigation.Navigation
import com.codequest.academy.shared.ui.navigation.Screen
import com.codequest.academy.shared.ui.theme.AppTypography
import com.codequest.academy.shared.ui.theme.DisplayStyle
import com.codequest.academy.shared.ui.theme.Theme
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun LibraryHomeScreen(repository: ProgressRepository, navigation: Navigation) {
    val books = repository.libraryResources(LibraryKind.BOOK)
    val files = repository.libraryResources(LibraryKind.INTENSIVE_FILE)
    val states = repository.readingStates()
    LibraryPage("OFFLINE LIBRARY", "Your Nous technical library", "25 verified PDFs are stored locally on this device. Opening and saving documents never requires a network connection.") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            StatCard("BOOKS", books.size.toString(), "150 pages each", Modifier.weight(1f))
            StatCard("INTENSIVE FILES", files.size.toString(), "50 pages each", Modifier.weight(1f))
            StatCard("TOTAL PAGES", (books.sumOf { it.pageCount } + files.sumOf { it.pageCount }).toString(), "verified locally", Modifier.weight(1f))
            StatCard("READING", states.size.toString(), "documents resumed", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryButton("Browse books", { navigation.navigateTo(Screen.Books) })
            SecondaryButton("Browse intensive files", { navigation.navigateTo(Screen.IntensiveFiles) })
            SecondaryButton("Search library", { navigation.navigateTo(Screen.Search) })
        }
        ReadingNotice()
    }
}

@Composable
fun LibraryListScreen(
    title: String,
    kind: LibraryKind?,
    repository: ProgressRepository,
    actions: OfflineDocumentActions
) {
    val resources = repository.libraryResources(kind)
    LibraryPage("OFFLINE LIBRARY", title, if (kind == LibraryKind.BOOK) "Five 150-page core books, packaged and verified for local reading." else if (kind == LibraryKind.INTENSIVE_FILE) "Twenty optional 50-page Intensive Files, packaged and verified for local reading." else "All verified local PDFs in the Nous AI Academy library.") {
        resources.forEach { resource -> ResourceCard(resource, repository, actions) }
    }
}

@Composable
fun SearchLibraryScreen(repository: ProgressRepository, actions: OfflineDocumentActions) {
    var query by remember { mutableStateOf("") }
    val matches = repository.libraryResources().filter { resource ->
        query.isBlank() || resource.title.contains(query, ignoreCase = true) || resource.subtitle.contains(query, ignoreCase = true)
    }
    LibraryPage("LOCAL SEARCH", "Search the library", "Search titles and descriptions stored locally. Once opened, use your PDF viewer’s search for the complete document text.") {
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text("Search titles and descriptions") }, singleLine = true)
        Text("${matches.size} local ${if (matches.size == 1) "result" else "results"}", style = AppTypography.caption, color = Theme.colors.textMuted)
        matches.forEach { ResourceCard(it, repository, actions) }
    }
}

@Composable
fun ReadingProgressScreen(repository: ProgressRepository, actions: OfflineDocumentActions) {
    val states = repository.readingStates()
    LibraryPage("LOCAL PROGRESS", "Reading progress", "Last-opened pages are private, saved on this device, and never sent to a server.") {
        if (states.isEmpty()) EmptyPanel("No documents opened yet", "Open a book or Intensive File to begin tracking its last-opened page.")
        states.forEach { state -> ResourceCard(state.resource, repository, actions, state) }
    }
}

@Composable
fun BookmarksScreen(repository: ProgressRepository, actions: OfflineDocumentActions) {
    val resources = repository.libraryResources().associateBy { it.id }
    val bookmarks = repository.bookmarks()
    LibraryPage("LOCAL BOOKMARKS", "Bookmarks", "Bookmarks remain local to this device and are retained independently of the packaged PDF files.") {
        if (bookmarks.isEmpty()) EmptyPanel("No bookmarks yet", "Open a local PDF and choose Bookmark page to save a return point.")
        bookmarks.forEach { bookmark ->
            val resource = resources[bookmark.resourceId]
            if (resource != null) BookmarkCard(bookmark, resource, repository, actions)
        }
    }
}

@Composable
private fun LibraryPage(eyebrow: String, title: String, description: String, body: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().background(Theme.colors.appBackground).verticalScroll(rememberScrollState()).padding(horizontal = 48.dp, vertical = 42.dp), verticalArrangement = Arrangement.spacedBy(17.dp)) {
        Text(eyebrow, style = AppTypography.caption.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp), color = Theme.colors.brandPrimary)
        Text(title, style = DisplayStyle.copy(color = Theme.colors.textPrimary))
        Text(description, style = AppTypography.body1, color = Theme.colors.textSecondary, modifier = Modifier.fillMaxWidth(.82f))
        Spacer(Modifier.height(4.dp))
        body()
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StatCard(label: String, value: String, detail: String, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(Theme.colors.surfacePrimary).border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(14.dp)).padding(19.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, style = AppTypography.caption.copy(fontWeight = FontWeight.Bold, letterSpacing = .8.sp), color = Theme.colors.textMuted)
        Text(value, style = AppTypography.h1.copy(fontWeight = FontWeight.Bold), color = Theme.colors.brandPrimary)
        Text(detail, style = AppTypography.caption, color = Theme.colors.textSecondary)
    }
}

@Composable
private fun ResourceCard(resource: OfflineLibraryResource, repository: ProgressRepository, actions: OfflineDocumentActions, suppliedState: LibraryReadingState? = null) {
    var pageText by remember(resource.id) { mutableStateOf((suppliedState ?: repository.readingState(resource))?.currentPage?.toString() ?: "1") }
    var message by remember(resource.id) { mutableStateOf("") }
    val savedState = suppliedState ?: repository.readingState(resource)
    val availability = actions.availability(resource)
    val kindLabel = if (resource.kind == LibraryKind.BOOK) "BOOK" else "INTENSIVE FILE"
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Theme.colors.surfacePrimary).border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(15.dp)).padding(21.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${resource.id} · $kindLabel", style = AppTypography.caption.copy(fontWeight = FontWeight.Bold, letterSpacing = .7.sp), color = Theme.colors.brandPrimary)
                Text(resource.title, style = AppTypography.h2.copy(fontWeight = FontWeight.SemiBold), color = Theme.colors.textPrimary)
                Text(resource.subtitle, style = AppTypography.body2, color = Theme.colors.textSecondary)
            }
            Text("${resource.pageCount} PAGES", style = AppTypography.caption.copy(fontWeight = FontWeight.Bold), color = Theme.colors.textMuted)
        }
        Text(if (availability.success) "● Verified and available offline" else "● ${availability.message}", style = AppTypography.caption, color = if (availability.success) Theme.colors.success else Theme.colors.error)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(pageText, { pageText = it.filter(Char::isDigit).take(4) }, Modifier.width(128.dp), label = { Text("Page") }, singleLine = true)
            Text("Last opened: page ${savedState?.currentPage ?: 1}", style = AppTypography.caption, color = Theme.colors.textSecondary)
            PrimaryButton("Open PDF", {
                message = savePage(repository, resource, pageText)
                if (message.startsWith("Saved")) message = actions.open(resource).message
            }, enabled = availability.success)
            SecondaryButton("Download / Save", { message = actions.saveCopy(resource).message }, enabled = availability.success)
            SecondaryButton("Bookmark page", { message = bookmarkPage(repository, resource, pageText) }, enabled = availability.success)
        }
        if (message.isNotBlank()) Text(message, style = AppTypography.caption, color = if (message.startsWith("Saved") || message.startsWith("Opened")) Theme.colors.success else Theme.colors.textSecondary)
    }
}

@Composable
private fun BookmarkCard(bookmark: LibraryBookmark, resource: OfflineLibraryResource, repository: ProgressRepository, actions: OfflineDocumentActions) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Theme.colors.surfacePrimary).border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(14.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("${resource.id} · PAGE ${bookmark.page}", style = AppTypography.caption.copy(fontWeight = FontWeight.Bold), color = Theme.colors.brandPrimary)
        Text(resource.title, style = AppTypography.h2, color = Theme.colors.textPrimary)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton("Open PDF", { repository.saveReadingPage(resource, bookmark.page); actions.open(resource) })
            SecondaryButton("Save a copy", { actions.saveCopy(resource) })
        }
    }
}

@Composable
private fun ReadingNotice() = Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Theme.colors.brandSoft).border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(14.dp)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
    Text("OFFLINE GUARANTEE", style = AppTypography.caption.copy(fontWeight = FontWeight.Bold, letterSpacing = .8.sp), color = Theme.colors.brandPrimary)
    Text("PDFs are validated with their bundled SHA-256 checksums before opening or saving. If a file is missing or corrupted, the app stops the action and reports the error.", style = AppTypography.body2, color = Theme.colors.textSecondary)
}

@Composable
private fun EmptyPanel(title: String, description: String) = Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Theme.colors.surfacePrimary).border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(14.dp)).padding(25.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Text(title, style = AppTypography.h2, color = Theme.colors.textPrimary)
    Text(description, style = AppTypography.body1, color = Theme.colors.textSecondary)
}

private fun savePage(repository: ProgressRepository, resource: OfflineLibraryResource, text: String): String {
    val page = text.toIntOrNull()
    if (page == null || page !in 1..resource.pageCount) return "Choose a page from 1 to ${resource.pageCount}."
    return if (repository.saveReadingPage(resource, page)) "Saved page $page locally." else "No local account is active."
}

private fun bookmarkPage(repository: ProgressRepository, resource: OfflineLibraryResource, text: String): String {
    val page = text.toIntOrNull()
    if (page == null || page !in 1..resource.pageCount) return "Choose a page from 1 to ${resource.pageCount}."
    return if (repository.addBookmark(resource, page) != null) "Saved a bookmark for page $page locally." else "No local account is active."
}
