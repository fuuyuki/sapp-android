package com.example.sapp.ui

import androidx.camera.core.CameraSelector
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.res.stringResource
import com.example.sapp.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmMedicationScreen(
    onBack: () -> Unit,
    onConfirm: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Initialize CameraController for the Front Camera
    val cameraController = remember {
        LifecycleCameraController(context).apply {
            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
//            bindToLifecycle(lifecycleOwner)
        }
    }

    // Bind the lifecycle properly
    LaunchedEffect(Unit) {
        cameraController.bindToLifecycle(lifecycleOwner)
    }


    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.verify_meds),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            // Large Capture Button
            LargeFloatingActionButton(
                onClick = {
                    // In a production app, you would call cameraController.takePicture here
                    // For this prototype, we trigger the confirmation callback
                    onConfirm()
                },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = "Capture",
                    modifier = Modifier.size(36.dp)
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black) // Standard camera background
        ) {
            // Camera Preview View
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        // Use implementation mode to ensure better compatibility
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        controller = cameraController
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { previewView ->
                    previewView.controller = cameraController
                }
            )

            // Instruction Overlay
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(24.dp)
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = stringResource(R.string.camera_instruction),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Bottom decorative gradient/mask to make the capture button pop
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f))
                        )
                    )
            )
        }
    }
}