package com.example.sapp.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.sapp.R
import com.example.sapp.update.UpdateState
import java.util.Locale

@Composable
fun UpdateDialog(
    state: UpdateState,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onRetry: () -> Unit
) {
    if (state is UpdateState.Idle) return

    AlertDialog(
        onDismissRequest = { if (state !is UpdateState.Downloading) onDismiss() },
        title = { Text(titleFor(state)) },
        text = {
            when (state) {
                UpdateState.Checking -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.update_checking))
                }

                is UpdateState.UpToDate -> Text(
                    stringResource(R.string.update_up_to_date, state.versionName)
                )

                is UpdateState.Available -> Text(
                    stringResource(
                        R.string.update_available_message,
                        state.release.versionName ?: "?",
                        state.currentVersionName
                    )
                )

                is UpdateState.Downloading -> Column {
                    Text(
                        stringResource(
                            R.string.update_downloading,
                            (state.progress * 100).toInt()
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    if (state.total > 0) {
                        LinearProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (state.total > 0) {
                            "${formatBytes(state.downloaded)} / ${formatBytes(state.total)}"
                        } else {
                            formatBytes(state.downloaded)
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                is UpdateState.ReadyToInstall -> Text(
                    stringResource(R.string.update_ready_message)
                )

                is UpdateState.Failed -> Text(state.message)

                UpdateState.Idle -> Unit
            }
        },
        confirmButton = {
            when (state) {
                is UpdateState.Available -> TextButton(onClick = onDownload) {
                    Text(stringResource(R.string.update_download))
                }

                is UpdateState.ReadyToInstall -> TextButton(onClick = onInstall) {
                    Text(stringResource(R.string.update_install))
                }

                is UpdateState.Failed -> if (state.canRetry) {
                    TextButton(onClick = onRetry) {
                        Text(stringResource(R.string.update_retry))
                    }
                }

                is UpdateState.UpToDate -> TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.close))
                }

                else -> Unit
            }
        },
        dismissButton = {
            when (state) {
                is UpdateState.Downloading -> TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }

                is UpdateState.Available,
                is UpdateState.ReadyToInstall,
                is UpdateState.Failed -> TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.update_later))
                }

                UpdateState.Checking -> TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }

                else -> Unit
            }
        }
    )
}

@Composable
private fun titleFor(state: UpdateState): String = when (state) {
    is UpdateState.UpToDate -> stringResource(R.string.update_up_to_date_title)
    is UpdateState.Available -> stringResource(R.string.update_available_title)
    is UpdateState.ReadyToInstall -> stringResource(R.string.update_ready_title)
    is UpdateState.Failed -> stringResource(R.string.update_failed_title)
    else -> stringResource(R.string.update_title)
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 MB"
    val megabytes = bytes / (1024.0 * 1024.0)
    return String.format(Locale.getDefault(), "%.1f MB", megabytes)
}
