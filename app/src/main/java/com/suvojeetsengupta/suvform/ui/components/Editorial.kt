package com.suvojeetsengupta.suvform.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suvojeetsengupta.suvform.ui.theme.Fraunces
import com.suvojeetsengupta.suvform.ui.theme.Mono
import com.suvojeetsengupta.suvform.ui.theme.SuvTheme

/** White editorial card with hairline border. */
@Composable
fun SuvCard(
    modifier: Modifier = Modifier,
    radius: Int = 18,
    border: Boolean = true,
    container: Color = SuvTheme.colors.card,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(radius.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(container, shape)
            .then(if (border) Modifier.border(1.dp, SuvTheme.colors.line2, shape) else Modifier)
            .padding(contentPadding),
    ) { content() }
}

/** Mono uppercase section label, optionally with a leading hairline tick. */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = SuvTheme.colors.muted,
    tick: Boolean = false,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (tick) {
            Box(Modifier.width(16.dp).height(1.dp).background(SuvTheme.colors.accent))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text.uppercase(),
            fontFamily = Mono,
            fontSize = 11.sp,
            letterSpacing = 1.2.sp,
            fontWeight = FontWeight.Medium,
            color = color,
        )
    }
}

/** Mono meta text used for dates, counts, codes. */
@Composable
fun MonoMeta(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = SuvTheme.colors.muted,
    size: Int = 11,
) {
    Text(
        text = text,
        modifier = modifier,
        fontFamily = Mono,
        fontSize = size.sp,
        letterSpacing = 0.2.sp,
        color = color,
    )
}

enum class ChipVariant { Outline, SolidInk, Accent, Live }

@Composable
fun SuvChip(
    text: String,
    variant: ChipVariant = ChipVariant.Outline,
    modifier: Modifier = Modifier,
) {
    val c = SuvTheme.colors
    val shape = RoundedCornerShape(100.dp)
    val (bg, fg, borderColor) = when (variant) {
        ChipVariant.Outline -> Triple(c.card, c.ink, c.line)
        ChipVariant.SolidInk -> Triple(c.feature, c.onFeature, c.feature)
        ChipVariant.Accent -> Triple(c.accentSoft, c.accentDeep, Color.Transparent)
        ChipVariant.Live -> Triple(c.okSoft, c.ok, Color.Transparent)
    }
    Row(
        modifier = modifier
            .clip(shape)
            .background(bg, shape)
            .then(if (borderColor != Color.Transparent) Modifier.border(1.dp, borderColor, shape) else Modifier)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (variant == ChipVariant.Live) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(c.ok))
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = text,
            fontFamily = if (variant == ChipVariant.Live) Mono else MaterialTheme.typography.labelMedium.fontFamily,
            fontSize = 11.sp,
            fontWeight = if (variant == ChipVariant.Outline) FontWeight.Medium else FontWeight.SemiBold,
            color = fg,
        )
    }
}

/** Serif-italic glyph tile used as a form/section icon. */
@Composable
fun GlyphIcon(
    letter: String,
    modifier: Modifier = Modifier,
    size: Int = 44,
    radius: Int = 12,
    container: Color = SuvTheme.colors.paper2,
    content: Color = SuvTheme.colors.ink,
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape(radius.dp))
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letter,
            fontFamily = Fraunces,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Medium,
            fontSize = (size * 0.4).sp,
            color = content,
        )
    }
}

enum class ButtonVariant { Ink, Accent, Ghost }

/** Full-width editorial button. */
@Composable
fun SuvButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Ink,
    enabled: Boolean = true,
    leading: ImageVector? = null,
    height: Int = 52,
    radius: Int = 14,
) {
    val c = SuvTheme.colors
    val (bg, fg) = when (variant) {
        ButtonVariant.Ink -> c.feature to c.onFeature
        ButtonVariant.Accent -> c.accent to c.onAccent
        ButtonVariant.Ghost -> Color.Transparent to c.ink
    }
    val shape = RoundedCornerShape(radius.dp)
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        color = if (enabled) bg else bg.copy(alpha = 0.4f),
        border = if (variant == ButtonVariant.Ghost) BorderStroke(1.dp, c.ink) else null,
        modifier = modifier.fillMaxWidth().height(height.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                Icon(leading, null, tint = fg, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
            }
            Text(text, color = fg, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
}
