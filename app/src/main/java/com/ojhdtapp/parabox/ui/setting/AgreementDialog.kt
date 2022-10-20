package com.ojhdtapp.parabox.ui.setting

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ojhdtapp.parabox.R

@Composable
fun AgreementDialog(
    modifier: Modifier = Modifier,
    showDialog: Boolean,
    icon: @Composable () -> Unit,
    title: String,
    contentResId: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text(text = "同意并接受")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(text = "拒绝")
                }
            },
            icon = icon,
            title = { Text(text = title) },
            text = {
                Column(modifier = Modifier
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState())) {
                    SelectionContainer() {
                        Text(text = stringResource(id = contentResId))
                    }
                }
            },
        )
    }
}