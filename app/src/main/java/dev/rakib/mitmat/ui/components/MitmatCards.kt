package dev.rakib.mitmat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.rakib.mitmat.domain.model.DebtTransaction
import dev.rakib.mitmat.ui.theme.AvatarColors
import dev.rakib.mitmat.ui.theme.DarkMintCard
import dev.rakib.mitmat.ui.theme.DarkPeachCard
import dev.rakib.mitmat.ui.theme.DarkPinkCard
import dev.rakib.mitmat.ui.theme.FinancialOwePeach
import dev.rakib.mitmat.ui.theme.FinancialReceiveGreen
import dev.rakib.mitmat.ui.theme.FinancialSettledSlate
import dev.rakib.mitmat.ui.theme.LavenderAccent
import dev.rakib.mitmat.ui.theme.LocalIsDarkTheme
import dev.rakib.mitmat.ui.theme.MintAccent
import dev.rakib.mitmat.ui.theme.MitmatCard
import dev.rakib.mitmat.ui.theme.PastelMint
import dev.rakib.mitmat.ui.theme.PastelPeach
import dev.rakib.mitmat.ui.theme.PeachAccent
import java.text.DecimalFormat

object FormatHelper {
    private val decimalFormat = DecimalFormat("#,##0.00")

    fun formatMoney(amount: Double, currency: String = "৳"): String {
        return "$currency${decimalFormat.format(amount)}"
    }
}

@Composable
fun ParticipantAvatar(
    name: String,
    colorIndex: Int = 0,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp
) {
    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val color = AvatarColors[colorIndex.coerceIn(0, AvatarColors.lastIndex) % AvatarColors.size]

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.42f).sp
        )
    }
}

@Composable
fun SummaryStatCard(
    title: String,
    amount: Double,
    currency: String,
    icon: ImageVector,
    iconColor: Color,
    lightBg: Color,
    darkBg: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    MitmatCard(
        modifier = modifier,
        pastelColorLight = lightBg,
        pastelColorDark = darkBg,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = FormatHelper.formatMoney(amount, currency),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun OverallBalanceBanner(
    netBalance: Double,
    currency: String,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val (statusText, statusColor, lightBg, darkBg, icon) = when {
        netBalance > 0.01 -> Quintuple(
            "You are owed in total",
            FinancialReceiveGreen,
            PastelMint.copy(alpha = 0.6f),
            DarkMintCard,
            Icons.Rounded.ArrowDownward
        )
        netBalance < -0.01 -> Quintuple(
            "You owe in total",
            FinancialOwePeach,
            PastelPeach.copy(alpha = 0.6f),
            DarkPeachCard,
            Icons.Rounded.ArrowUpward
        )
        else -> Quintuple(
            "You are all settled up!",
            FinancialSettledSlate,
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.surfaceVariant,
            Icons.Rounded.CheckCircle
        )
    }

    MitmatCard(
        modifier = modifier.fillMaxWidth(),
        pastelColorLight = lightBg,
        pastelColorDark = darkBg
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Overall Balance",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (kotlin.math.abs(netBalance) < 0.01) "৳0.00" else FormatHelper.formatMoney(kotlin.math.abs(netBalance), currency),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = statusColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

private data class Quintuple<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)

@Composable
fun DebtItemCard(
    debt: DebtTransaction,
    currency: String,
    onSettleClick: () -> Unit,
    showSettleButton: Boolean = true,
    modifier: Modifier = Modifier
) {
    MitmatCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ParticipantAvatar(name = debt.fromName, size = 36.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            ParticipantAvatar(name = debt.toName, size = 36.dp)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${debt.fromName} owes ${debt.toName}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = FormatHelper.formatMoney(debt.amount, currency),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = LavenderAccent
                )
            }

            if (showSettleButton) {
                Button(
                    onClick = onSettleClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PastelMint,
                        contentColor = MintAccent
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("settle_button_${debt.fromParticipantId}_${debt.toParticipantId}")
                ) {
                    Text("Settle", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
