package com.kontakti.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kontakti.data.model.RelationshipStrength
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun AvatarComposable(name: String, size: Dp = 40.dp) {
    val initials = buildString {
        val parts = name.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
        if (parts.isNotEmpty()) append(parts[0].first().uppercaseChar())
        if (parts.size > 1) append(parts[1].first().uppercaseChar())
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
    ) {
        Text(
            text = initials.ifEmpty { "?" },
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = (size.value * 0.35f).sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun StrengthBadge(strength: RelationshipStrength) {
    val (bgColor, textColor) = when (strength) {
        RelationshipStrength.cold -> Color(0xFFF4F4F5) to Color(0xFF71717A)
        RelationshipStrength.warm -> Color(0xFFFEF3C7) to Color(0xFFB45309)
        RelationshipStrength.hot  -> Color(0xFFFFEDD5) to Color(0xFFC2410C)
        RelationshipStrength.close -> Color(0xFFDCFCE7) to Color(0xFF15803D)
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = bgColor,
        modifier = Modifier.wrapContentWidth()
    ) {
        Text(
            text = strength.label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun EmptyState(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        icon()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun RelativeDateText(dateString: String, modifier: Modifier = Modifier) {
    val label = buildRelativeLabel(dateString)
    Text(
        text = label,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
        modifier = modifier
    )
}

private fun buildRelativeLabel(dateString: String): String {
    return try {
        val date = try {
            ZonedDateTime.parse(dateString).toLocalDate()
        } catch (_: Exception) {
            LocalDate.parse(dateString.take(10))
        }
        val today = LocalDate.now()
        val days = ChronoUnit.DAYS.between(date, today)
        when {
            days == 0L -> "Today"
            days == 1L -> "Yesterday"
            days < 7L  -> "${days}d ago"
            days < 30L -> "${days / 7}w ago"
            days < 365L -> "${days / 30}mo ago"
            else -> "${days / 365}y ago"
        }
    } catch (_: Exception) {
        dateString.take(10)
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}
