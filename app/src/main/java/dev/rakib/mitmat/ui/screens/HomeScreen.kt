package dev.rakib.mitmat.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.PostAdd
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.rakib.mitmat.domain.model.GroupSummary
import dev.rakib.mitmat.ui.components.CategoryBadge
import dev.rakib.mitmat.ui.components.FormatHelper
import dev.rakib.mitmat.ui.components.OverallBalanceBanner
import dev.rakib.mitmat.ui.components.ParticipantAvatar
import dev.rakib.mitmat.ui.components.SummaryStatCard
import dev.rakib.mitmat.ui.theme.DarkMintCard
import dev.rakib.mitmat.ui.theme.DarkPeachCard
import dev.rakib.mitmat.ui.theme.FinancialOwePeach
import dev.rakib.mitmat.ui.theme.FinancialReceiveGreen
import dev.rakib.mitmat.ui.theme.LavenderAccent
import dev.rakib.mitmat.ui.theme.LocalIsDarkTheme
import dev.rakib.mitmat.ui.theme.MintAccent
import dev.rakib.mitmat.ui.theme.MitmatCard
import dev.rakib.mitmat.ui.theme.PastelLavender
import dev.rakib.mitmat.ui.theme.PastelMint
import dev.rakib.mitmat.ui.theme.PastelPeach
import dev.rakib.mitmat.ui.theme.PastelPowderBlue
import dev.rakib.mitmat.ui.theme.PeachAccent
import dev.rakib.mitmat.ui.viewmodel.DashboardUiState

@Composable
fun HomeScreen(
    dashboardState: DashboardUiState,
    groupSummaries: List<GroupSummary>,
    onAddExpenseClick: () -> Unit,
    onCreateGroupClick: () -> Unit,
    onGroupClick: (Long) -> Unit,
    onViewAllGroupsClick: () -> Unit,
    onViewActivityClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen_content"),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // App Header with Bengali Name & Tagline
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Mitmat",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = LavenderAccent
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(মিটমাট)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "Split bills. Stay even.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Financial Summary Cards
        item {
            OverallBalanceBanner(
                netBalance = dashboardState.netBalance,
                currency = dashboardState.defaultCurrency
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryStatCard(
                    title = "You are owed",
                    amount = dashboardState.totalYouAreOwed,
                    currency = dashboardState.defaultCurrency,
                    icon = Icons.Rounded.ArrowDownward,
                    iconColor = FinancialReceiveGreen,
                    lightBg = PastelMint,
                    darkBg = DarkMintCard,
                    modifier = Modifier.weight(1f)
                )

                SummaryStatCard(
                    title = "You owe",
                    amount = dashboardState.totalYouOwe,
                    currency = dashboardState.defaultCurrency,
                    icon = Icons.Rounded.ArrowUpward,
                    iconColor = FinancialOwePeach,
                    lightBg = PastelPeach,
                    darkBg = DarkPeachCard,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Quick Actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onAddExpenseClick,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LavenderAccent,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("add_expense_quick_action")
                ) {
                    Icon(imageVector = Icons.Rounded.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Add Expense", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onCreateGroupClick,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else PastelLavender,
                        contentColor = if (isDark) MaterialTheme.colorScheme.onSurface else LavenderAccent
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("create_group_quick_action")
                ) {
                    Icon(imageVector = Icons.Rounded.GroupAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Create Group", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Active Groups Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Groups",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (groupSummaries.isNotEmpty()) {
                    TextButton(onClick = onViewAllGroupsClick) {
                        Text("See All (${groupSummaries.size})", color = LavenderAccent)
                    }
                }
            }
        }

        if (groupSummaries.isEmpty()) {
            item {
                MitmatCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(PastelLavender),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Groups,
                                contentDescription = null,
                                tint = LavenderAccent,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No groups yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Create a group for your trip, flatmates, or friends to start splitting expenses.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onCreateGroupClick,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LavenderAccent),
                            modifier = Modifier.testTag("home_create_first_group_button")
                        ) {
                            Text("Create First Group")
                        }
                    }
                }
            }
        } else {
            items(groupSummaries.take(3), key = { it.group.id }) { summary ->
                HomeGroupCard(
                    summary = summary,
                    onClick = { onGroupClick(summary.group.id) }
                )
            }
        }

        // Activity Prompt
        item {
            MitmatCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = onViewActivityClick,
                pastelColorLight = PastelPowderBlue.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(PastelPowderBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                                contentDescription = null,
                                tint = LavenderAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "View Recent Activity",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Track expenses and settlements history",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = LavenderAccent
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun HomeGroupCard(
    summary: GroupSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    MitmatCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(PastelLavender),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Groups,
                    contentDescription = null,
                    tint = LavenderAccent,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = summary.group.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${summary.participants.size} participants • Total: ${FormatHelper.formatMoney(summary.totalExpenses, summary.group.defaultCurrency)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                val net = summary.userNetBalance
                val (color, label) = when {
                    net > 0.01 -> Pair(FinancialReceiveGreen, "+${FormatHelper.formatMoney(net, summary.group.defaultCurrency)}")
                    net < -0.01 -> Pair(FinancialOwePeach, "-${FormatHelper.formatMoney(kotlin.math.abs(net), summary.group.defaultCurrency)}")
                    else -> Pair(MaterialTheme.colorScheme.onSurfaceVariant, "Settled")
                }

                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                if (summary.totalSimplifiedDebtsCount > 0) {
                    Text(
                        text = "${summary.totalSimplifiedDebtsCount} debts pending",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
