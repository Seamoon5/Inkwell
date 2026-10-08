package com.seamoon5.inkwell

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.seamoon5.inkwell.data.NoteDatabase
import com.seamoon5.inkwell.data.NoteRepository
import com.seamoon5.inkwell.ui.screens.NoteEditorScreen
import com.seamoon5.inkwell.ui.screens.NoteListScreen
import com.seamoon5.inkwell.ui.theme.InkwellTheme
import com.seamoon5.inkwell.ui.viewmodel.NoteViewModel
import com.seamoon5.inkwell.ui.viewmodel.NoteViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = NoteDatabase.getDatabase(applicationContext)
        val repository = NoteRepository(database.noteDao())

        setContent {
            InkwellTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    InkwellApp(repository = repository)
                }
            }
        }
    }
}

@Composable
fun InkwellApp(repository: NoteRepository) {
    val navController = rememberNavController()
    val viewModel: NoteViewModel = viewModel(factory = NoteViewModelFactory(repository))

    NavHost(navController = navController, startDestination = "list") {
        composable("list") {
            NoteListScreen(
                viewModel = viewModel,
                onNoteClick = { id -> navController.navigate("editor/$id") },
                onNewNote = { navController.navigate("editor/0") }
            )
        }
        composable(
            route = "editor/{noteId}",
            arguments = listOf(navArgument("noteId") { type = NavType.LongType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong("noteId") ?: 0L
            NoteEditorScreen(
                viewModel = viewModel,
                noteId = noteId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
