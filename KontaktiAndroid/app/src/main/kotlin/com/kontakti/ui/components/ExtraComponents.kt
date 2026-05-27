package com.kontakti.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kontakti.data.model.Address
import com.kontakti.data.model.PersonEmail
import com.kontakti.data.model.PersonPhone
import com.kontakti.data.model.PersonUrl
import com.kontakti.data.model.RelationshipStrength

@Composable
fun RelationshipStrengthBadge(strength: RelationshipStrength) = StrengthBadge(strength)

@Composable
fun EmailRow(email: PersonEmail, onClick: (() -> Unit)? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(email.value, fontWeight = FontWeight.Medium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!email.label.isNullOrBlank()) {
                    LabeledChip(email.label.uppercase())
                }
                if (email.isPrimary) {
                    if (!email.label.isNullOrBlank()) Spacer(Modifier.width(6.dp))
                    LabeledChip("Primary")
                }
            }
        }
    }
}

@Composable
fun PhoneRow(phone: PersonPhone, onClick: (() -> Unit)? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(phone.value, fontWeight = FontWeight.Medium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!phone.label.isNullOrBlank()) {
                    LabeledChip(phone.label.uppercase())
                }
                if (phone.isPrimary) {
                    if (!phone.label.isNullOrBlank()) Spacer(Modifier.width(6.dp))
                    LabeledChip("Primary")
                }
            }
        }
    }
}

@Composable
fun AddressCard(address: Address) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(Modifier.padding(12.dp)) {
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(address.label.orEmpty().ifBlank { "Address" }, fontWeight = FontWeight.Medium)
                address.street?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                val cityLine = listOfNotNull(address.city, address.region, address.postalCode).joinToString(", ")
                if (cityLine.isNotBlank()) Text(cityLine, style = MaterialTheme.typography.bodySmall)
                address.country?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

@Composable
fun UrlChip(url: PersonUrl) {
    AssistChip(
        onClick = {},
        label = { Text(url.label?.ifBlank { url.value } ?: url.value) },
        leadingIcon = { Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp)) }
    )
}

@Composable
fun SocialHandleChip(label: String, value: String) {
    AssistChip(
        onClick = {},
        label = { Text("$label: $value") }
    )
}

@Composable
fun DoNotContactBadge(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(50))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Icon(
            Icons.Default.Block,
            contentDescription = "Do not contact",
            tint = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.size(12.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            "DNC",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun LabeledChip(text: String, color: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)) {
    Box(
        modifier = Modifier
            .background(color, RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
    }
}
