package com.example.checklist.ui.screens.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.res.stringResource
import com.example.checklist.R
import kotlinx.coroutines.delay

@Composable
fun TextFieldDialog(
    title: String,
    label: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    initialText: String = ""
) {
    var fieldValue by remember(initialText) {
        mutableStateOf(
            TextFieldValue(
                text = initialText,
                selection = androidx.compose.ui.text.TextRange(initialText.length)
            )
        )
    }

    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current


    LaunchedEffect(Unit) {
        // Let the dialog compose first
        delay(100)
        focusRequester.requestFocus()
        keyboard?.show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = fieldValue,
                onValueChange = { fieldValue = it},
                label = { Text(label) },
                singleLine = true,
                modifier = Modifier.focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (fieldValue.text.isNotBlank()) onConfirm(fieldValue.text)
                    }
                )
            )
        },
        confirmButton = { TextButton(onClick = { if (fieldValue.text.isNotBlank()) onConfirm(fieldValue.text) }) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
fun SectionHeader(title: String, onClick: (() -> Unit)? = null) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 8.dp)
    )
    HorizontalDivider()
    Spacer(Modifier.height(8.dp))
}