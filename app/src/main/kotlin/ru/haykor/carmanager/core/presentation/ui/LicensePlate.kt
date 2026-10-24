package ru.haykor.carmanager.core.presentation.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Real plates are white with black characters regardless of the app theme. */
@Composable
fun LicensePlate(plate: String, modifier: Modifier = Modifier.Companion) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color.White,
        contentColor = Color.Black,
        border = BorderStroke(1.5.dp, Color.Black),
        modifier = modifier
    ) {
        Text(
            text = plate,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
