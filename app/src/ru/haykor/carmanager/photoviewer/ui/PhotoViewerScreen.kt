package ru.haykor.carmanager.photoviewer.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.filesDir
import io.github.vinceglb.filekit.resolve
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import ru.haykor.carmanager.*
import ru.haykor.carmanager.core.presentation.coroutines.ObserveAsEvents
import ru.haykor.carmanager.core.ui.sharedImage
import ru.haykor.carmanager.photoviewer.component.PhotoViewerComponent
import ru.haykor.carmanager.photoviewer.component.PhotoViewerEvent

@Composable
fun PhotoViewerScreen(component: PhotoViewerComponent, modifier: Modifier = Modifier) {
    val photoPath = component.photoPath
    val context = LocalContext.current

    ObserveAsEvents(component.events) { event ->
        when (event) {
            PhotoViewerEvent.ImageSaved -> Toast.makeText(
                context,
                getString(Res.string.photo_viewer_saved_toast),
                Toast.LENGTH_SHORT
            ).show()

            PhotoViewerEvent.ImageSaveFailed -> Toast.makeText(
                context,
                getString(Res.string.photo_viewer_save_failed_toast),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    PhotoViewerContent(
        photoPath = photoPath,
        onBack = component::back,
        onSave = component::save
    )
}

@Composable
private fun PhotoViewerContent(photoPath: String, onBack: () -> Unit, onSave: () -> Unit) {
    val file = remember(photoPath) { FileKit.filesDir.resolve(photoPath) }
    val zoomState = rememberZoomState()
    var barsVisible by remember { mutableStateOf(true) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AsyncImage(
            model = file,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            onSuccess = { zoomState.setContentSize(it.painter.intrinsicSize) },
            modifier = Modifier
                .sharedImage(key = photoPath)
                .zoomable(
                    zoomState = zoomState,
                    onTap = { barsVisible = !barsVisible } // double-tap zoom is built in
                )
                .align(Alignment.Center)
        )

        AnimatedVisibility(
            visible = barsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(Res.drawable.arrow_back), "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onSave) {
                        Icon(painterResource(Res.drawable.download), "Save")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.4f),
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    }
}
