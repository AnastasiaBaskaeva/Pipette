package com.baskaeva.pipette.presentation.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baskaeva.pipette.R
import com.baskaeva.pipette.domain.ColorItem
import com.baskaeva.pipette.presentation.theme.CardDark
import com.baskaeva.pipette.presentation.theme.OnSurface

private val FavouriteRed = Color(0xFFE91E63)

private fun copyHexToClipboard(context: Context, hex: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("HEX Color", hex))
    Toast.makeText(context, "Скопировано: $hex", Toast.LENGTH_SHORT).show()
}

@Composable
private fun ColorCard(
    colorItem: ColorItem,
    swatchSize: Dp,
    swatchCorner: Dp,
    hexFontSize: Int,
    onSwatchClick: () -> Unit,
    subtitle: String?,
    actionIcon: ImageVector,
    actionIconTint: Color,
    actionDescription: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = Color(colorItem.rgb)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardDark)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Плашка цвета
        Box(
            modifier = Modifier
                .size(swatchSize)
                .clip(RoundedCornerShape(swatchCorner))
                .background(color)
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(swatchCorner))
                .clickable { onSwatchClick() }
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = colorItem.hex.uppercase(),
                color = Color.White,
                fontSize = hexFontSize.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = subtitle, color = OnSurface, fontSize = 11.sp)
            }
        }

        IconButton(onClick = onAction) {
            Icon(
                imageVector = actionIcon,
                contentDescription = actionDescription,
                tint = actionIconTint,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}


/**
 * Карточка для ResultPhotoScreen при 3 цветах (крупная плашка, HEX крупно).
 * Плашка → копировать HEX. Сердце → добавить/убрать из избранного.
 */
@Composable
fun ColorItemExpanded(
    colorItem: ColorItem,
    isFavourited: Boolean,
    onToggleFavourite: (ColorItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    ColorCard(
        colorItem = colorItem,
        swatchSize = 72.dp,
        swatchCorner = 12.dp,
        hexFontSize = 20,
        onSwatchClick = { copyHexToClipboard(context, colorItem.hex) },
        subtitle = stringResource(R.string.copy_colour),
        actionIcon = if (isFavourited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
        actionIconTint = if (isFavourited) FavouriteRed else Color.White.copy(alpha = 0.7f),
        actionDescription = if (isFavourited) stringResource(R.string.fav_delete)
                            else stringResource(R.string.fav_add),
        onAction = { onToggleFavourite(colorItem) },
        modifier = modifier
    )
}

/**
 * Карточка для ResultPhotoScreen при 6–15 цветах (компактная).
 * Плашка → копировать HEX. Сердце → добавить/убрать из избранного.
 */
@Composable
fun ColorItemCompact(
    colorItem: ColorItem,
    isFavourited: Boolean,
    onToggleFavourite: (ColorItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val color = Color(colorItem.rgb)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardDark)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color)
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                .clickable { copyHexToClipboard(context, colorItem.hex) }
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = colorItem.hex.uppercase(),
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = { onToggleFavourite(colorItem) },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = if (isFavourited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (isFavourited) stringResource(R.string.fav_delete)
                                    else stringResource(R.string.fav_add),
                tint = if (isFavourited) FavouriteRed else Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Карточка для FavouritesScreen
 * Плашка → копировать HEX. Корзина → удалить из избранного.
 */
@Composable
fun FavouriteColorItem(
    colorItem: ColorItem,
    onDelete: (ColorItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    ColorCard(
        colorItem = colorItem,
        swatchSize = 72.dp,
        swatchCorner = 12.dp,
        hexFontSize = 20,
        onSwatchClick = { copyHexToClipboard(context, colorItem.hex) },
        subtitle = stringResource(R.string.copy_colour),
        actionIcon = Icons.Default.Delete,
        actionIconTint = Color.White.copy(alpha = 0.5f),
        actionDescription = stringResource(R.string.fav_delete),
        onAction = { onDelete(colorItem) },
        modifier = modifier
    )
}