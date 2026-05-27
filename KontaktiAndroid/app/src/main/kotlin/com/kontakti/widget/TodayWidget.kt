package com.kontakti.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.graphics.Color
import com.kontakti.MainActivity

/**
 * Home-screen widget rendering the top 3 Today items.
 *
 * Design summary
 * --------------
 * - Single column. Header with "Today" + total count.
 * - Up to 3 rows: initials avatar (40dp) + name + one-line reason.
 * - Tapping the widget opens MainActivity (which lands on the Today tab).
 * - Glance handles small (2x1), medium (4x1), large (4x2) via SizeMode.Exact —
 *   the column simply renders fewer rows when there's no vertical space.
 * - Data source is [TodayWidgetState] (a DataStore<Preferences>) which the
 *   main app writes to after each Today refresh.
 */
class TodayWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val (items, total) = TodayWidgetState.read(context)
        provideContent {
            GlanceTheme {
                WidgetContent(items = items, total = total)
            }
        }
    }

    @Composable
    private fun WidgetContent(items: List<TodayWidgetState.WidgetItem>, total: Int) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color(0xFFFFFFFF)))
                .cornerRadius(16.dp)
                .padding(12.dp)
                .clickable(actionStartActivity<MainActivity>())
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = GlanceModifier.fillMaxWidth()
            ) {
                Text(
                    "Today",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(Color(0xFF111827))
                    )
                )
                Spacer(GlanceModifier.width(8.dp))
                Text(
                    if (total > 0) "$total to reach out" else "All clear",
                    style = TextStyle(color = ColorProvider(Color(0xFF6B7280)))
                )
            }
            Spacer(GlanceModifier.height(8.dp))

            if (items.isEmpty()) {
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Nothing for today",
                        style = TextStyle(color = ColorProvider(Color(0xFF6B7280)))
                    )
                }
            } else {
                items.take(3).forEach { item ->
                    WidgetRow(item)
                    Spacer(GlanceModifier.height(6.dp))
                }
            }
        }
    }

    @Composable
    private fun WidgetRow(item: TodayWidgetState.WidgetItem) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = GlanceModifier.fillMaxWidth()
        ) {
            // Initials avatar — Glance doesn't easily support remote image loading
            // without extra plumbing, so we render initials in a colored circle.
            InitialsCircle(item.name)
            Spacer(GlanceModifier.width(8.dp))
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    item.name,
                    maxLines = 1,
                    style = TextStyle(
                        fontWeight = FontWeight.Medium,
                        color = ColorProvider(Color(0xFF111827))
                    )
                )
                Text(
                    item.reason,
                    maxLines = 1,
                    style = TextStyle(color = ColorProvider(Color(0xFF6B7280)))
                )
            }
        }
    }

    @Composable
    private fun InitialsCircle(name: String) {
        val initials = buildString {
            val parts = name.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
            if (parts.isNotEmpty()) append(parts[0].first().uppercaseChar())
            if (parts.size > 1) append(parts[1].first().uppercaseChar())
        }.ifEmpty { "?" }
        Box(
            modifier = GlanceModifier
                .size(36.dp)
                .cornerRadius(18.dp)
                .background(ColorProvider(Color(0xFF2563EB))),
            contentAlignment = Alignment.Center
        ) {
            Text(
                initials,
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}
