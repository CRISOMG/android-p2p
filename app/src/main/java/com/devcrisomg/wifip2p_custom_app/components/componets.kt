package com.devcrisomg.wifip2p_custom_app.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun CustomButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    text: String = "Placeholder Text",
    containerColor: Color = Color(0xffff7f50),
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    contentPadding: PaddingValues = PaddingValues(4.dp),
    elevation: ButtonElevation = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier.wrapContentSize(),
        contentPadding = contentPadding,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor, contentColor = contentColor
        ),
        shape = MaterialTheme.shapes.extraSmall,
        elevation = elevation,
        border = border, // Usa el borde personalizado
        enabled = enabled, // Controla si el botón está habilitado
        // Add rounded corners (shape defined in your theme)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.wrapContentWidth(),
            maxLines = 1,
            overflow = TextOverflow.Clip,
            color = Color.Black
        )
    }
}