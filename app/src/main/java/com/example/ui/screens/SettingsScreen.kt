package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SettingsBrightness
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.preferences.AppearanceMode
import com.example.data.preferences.AppPreferences
import com.example.data.preferences.DesignTheme
import com.example.ui.theme.LavenderAccent
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.MitmatCard
import com.example.ui.theme.PastelLavender
import com.example.ui.theme.PastelPink
import com.example.ui.theme.PastelPowderBlue

@Composable
fun SettingsScreen(
    preferences: AppPreferences,
    onSetDesignTheme: (DesignTheme) -> Unit,
    onSetAppearanceMode: (AppearanceMode) -> Unit,
    onSeedDemoData: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Personalize your theme and appearance",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 1. Appearance Section
        item {
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = LavenderAccent
            )
        }

        item {
            MitmatCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Design Theme Selector (Material 3 vs Frosted Glass)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Design Theme",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ThemeChoiceCard(
                                title = "Pastel M3",
                                description = "Clean Material 3 surfaces & tonal elevation",
                                isSelected = preferences.designTheme == DesignTheme.MATERIAL_3,
                                onClick = { onSetDesignTheme(DesignTheme.MATERIAL_3) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("theme_material3_button")
                            )

                            ThemeChoiceCard(
                                title = "Frosted Glass",
                                description = "Translucent glass surfaces & soft glow",
                                isSelected = preferences.designTheme == DesignTheme.FROSTED_GLASS,
                                onClick = { onSetDesignTheme(DesignTheme.FROSTED_GLASS) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("theme_frosted_glass_button")
                            )
                        }
                    }

                    // Appearance Mode (System, Light, Dark)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Color Appearance",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AppearanceChip(
                                label = "System",
                                icon = Icons.Rounded.SettingsBrightness,
                                isSelected = preferences.appearanceMode == AppearanceMode.SYSTEM,
                                onClick = { onSetAppearanceMode(AppearanceMode.SYSTEM) },
                                modifier = Modifier.weight(1f)
                            )
                            AppearanceChip(
                                label = "Light",
                                icon = Icons.Rounded.LightMode,
                                isSelected = preferences.appearanceMode == AppearanceMode.LIGHT,
                                onClick = { onSetAppearanceMode(AppearanceMode.LIGHT) },
                                modifier = Modifier.weight(1f)
                            )
                            AppearanceChip(
                                label = "Dark",
                                icon = Icons.Rounded.DarkMode,
                                isSelected = preferences.appearanceMode == AppearanceMode.DARK,
                                onClick = { onSetAppearanceMode(AppearanceMode.DARK) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // 2. Contact Developer Section (Rakib)
        item {
            Text(
                text = "Contact Developer",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = LavenderAccent
            )
        }

        item {
            MitmatCard(
                modifier = Modifier.fillMaxWidth(),
                pastelColorLight = PastelLavender.copy(alpha = 0.45f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profile picture with Rakib's actual provided photo
                    Image(
                        painter = painterResource(id = R.drawable.rakib_profile),
                        contentDescription = "Rakib - Developer",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .border(2.5.dp, LavenderAccent, CircleShape)
                            .testTag("developer_profile_image")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Rakib",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Developer of Mitmat",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Social Media Buttons (Instagram and Facebook)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Instagram Button
                        Button(
                            onClick = { openSocialProfile(context, "instagram", "iii.rakib", "https://www.instagram.com/iii.rakib/") },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PastelPink,
                                contentColor = Color(0xFFC13584)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("contact_instagram_button")
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_instagram),
                                contentDescription = "Instagram",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Instagram",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF833AB4)
                            )
                        }

                        // Facebook Button
                        Button(
                            onClick = { openSocialProfile(context, "facebook", "iii.rakib", "https://www.facebook.com/iii.rakib/") },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PastelPowderBlue,
                                contentColor = Color(0xFF1877F2)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("contact_facebook_button")
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_facebook),
                                contentDescription = "Facebook",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Facebook",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1877F2)
                            )
                        }
                    }
                }
            }
        }

        // 3. About Mitmat Section
        item {
            Text(
                text = "About Mitmat",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = LavenderAccent
            )
        }

        item {
            MitmatCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Mitmat (মিটমাট)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = LavenderAccent
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "v1.0.0",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Mitmat is a simple expense-splitting app that helps you share bills, track expenses, and settle balances without the awkward calculations.\n\nIt does what it says.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Split bills. Stay even.",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = LavenderAccent
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = onSeedDemoData,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Rounded.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reload Demo Groups & Sample Data")
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
fun ThemeChoiceCard(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) LavenderAccent else Color.Transparent

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) PastelLavender.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .border(2.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) LavenderAccent else MaterialTheme.colorScheme.onSurface
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = LavenderAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun AppearanceChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(label, fontSize = 12.sp) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
        shape = RoundedCornerShape(12.dp),
        modifier = modifier,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = PastelLavender,
            selectedLabelColor = LavenderAccent
        )
    )
}

fun openSocialProfile(context: Context, platform: String, username: String, webUrl: String) {
    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl))
    try {
        val appIntent = when (platform) {
            "instagram" -> {
                Intent(Intent.ACTION_VIEW, Uri.parse("http://instagram.com/_u/$username")).apply {
                    setPackage("com.instagram.android")
                }
            }
            "facebook" -> {
                Intent(Intent.ACTION_VIEW, Uri.parse("fb://facewebmodal/f?href=$webUrl")).apply {
                    setPackage("com.facebook.katana")
                }
            }
            else -> webIntent
        }
        context.startActivity(appIntent)
    } catch (_: Exception) {
        try {
            context.startActivity(webIntent)
        } catch (_: Exception) {
            // Safe fallback
        }
    }
}
