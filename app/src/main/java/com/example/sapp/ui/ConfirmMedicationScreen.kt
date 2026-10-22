package com.example.sapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sapp.R
import com.example.sapp.ui.components.AppCard
import com.example.sapp.ui.components.AppTopBar
import com.example.sapp.ui.components.EmptyState
import com.example.sapp.ui.components.GradientHero
import com.example.sapp.ui.components.ScreenContainer

/**
 * Due-dose entry point. It resolves the medication that is due now and opens
 * [com.example.sapp.verification.MouthVerificationActivity] on an explicit tap,
 * so the camera never starts silently. The actual CameraX + MediaPipe flow
 * lives in that activity.
 */
@Composable
fun ConfirmMedicationScreen(
    duePillName: String?,
    onBack: () -> Unit,
    onStartVerification: (String) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(
                title = stringResource(R.string.verify_meds),
                onBack = onBack
            )
        }
    ) { padding ->
        ScreenContainer(
            modifier = Modifier.padding(padding),
            maxWidth = 620.dp
        ) {
            GradientHero(
                title = stringResource(R.string.mouth_verification_title),
                subtitle = if (duePillName != null) {
                    stringResource(R.string.confirm_meds_intro)
                } else {
                    stringResource(R.string.confirm_meds_no_due)
                },
                icon = Icons.Default.CameraAlt
            )

            AppCard {
                if (duePillName != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = stringResource(R.string.confirm_meds_due, duePillName),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.confirm_meds_intro),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { onStartVerification(duePillName) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.size(8.dp))
                            Text(stringResource(R.string.start_scanning))
                        }
                    }
                } else {
                    EmptyState(
                        icon = Icons.Default.CameraAlt,
                        title = stringResource(R.string.confirm_meds_no_due),
                        message = stringResource(R.string.no_schedules_message)
                    )
                }
            }
        }
    }
}
