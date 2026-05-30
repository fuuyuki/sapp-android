package com.example.sapp.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Composable
fun LanguageSelector(
    currentLanguage: String?, // "en" or "id"
    onLanguageChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = "Change Language",
                // Set to onPrimary if using in a primary-colored TopAppBar
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // English Option
            DropdownMenuItem(
                text = { Text("English") },
                onClick = {
                    onLanguageChange("en")
                    expanded = false
                },
                trailingIcon = {
                    if (currentLanguage == "en") {
                        Icon(Icons.Default.Check, contentDescription = null)
                    }
                }
            )
            // Indonesia Option
            DropdownMenuItem(
                text = { Text("Indonesia") },
                onClick = {
                    onLanguageChange("id")
                    expanded = false
                },
                trailingIcon = {
                    if (currentLanguage == "id") {
                        Icon(Icons.Default.Check, contentDescription = null)
                    }
                }
            )
        }
    }
}