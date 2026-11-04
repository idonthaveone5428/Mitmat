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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import dev.rakib.mitmat.ui.components.ConfirmDeleteDialog
import dev.rakib.mitmat.ui.components.FormatHelper
import dev.rakib.mitmat.ui.components.ParticipantAvatar
import dev.rakib.mitmat.ui.theme.FinancialOwePeach
import dev.rakib.mitmat.ui.theme.FinancialReceiveGreen
import dev.rakib.mitmat.ui.theme.LavenderAccent
import dev.rakib.mitmat.ui.theme.LocalIsDarkTheme
import dev.rakib.mitmat.ui.theme.MintAccent
import dev.rakib.mitmat.ui.theme.MitmatCard
import dev.rakib.mitmat.ui.theme.PastelLavender
import dev.rakib.mitmat.ui.theme.PastelMint
import dev.rakib.mitmat.ui.theme.PastelPeach
import dev.rakib.mitmat.ui.theme.PastelPink
import dev.rakib.mitmat.ui.theme.PeachAccent

@Composable
fun GroupsScreen(
    groupSummaries: List<GroupSummary>,
    onCreateGroupClick: () -> Unit,
    onGroupClick: (Long) -> Unit,
    onDeleteGroup: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    var groupToDelete by remember { mutableStateOf<GroupSummary?>(null) }

    if (groupToDelete != null) {
        ConfirmDeleteDialog(
            title = "Move to Trash Bin",
            message = "Move '${groupToDelete?.group?.name}' to the Trash Bin? Groups in the trash bin can be restored or deleted immediately from Settings, and are automatically deleted after 7 days.",
            onConfirm = {
                groupToDelete?.let { onDeleteGroup(it.group.id) }
                groupToDelete = null
            },
            onDismiss = { groupToDelete = null }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("groups_list"),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Your Groups",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Manage shared bills with friends & groups",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(PastelLavender),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Groups,
                                    contentDescription = null,
                                    tint = LavenderAccent,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No Groups Created",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Create a group to start tracking expenses for trips, flatmates, or events.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = onCreateGroupClick,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = LavenderAccent),
                                modifier = Modifier.testTag("create_first_group_button")
                            ) {
                                Icon(imageVector = Icons.Rounded.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Create Group")
                            }
                        }
                    }
                }
            } else {
                items(groupSummaries, key = { it.group.id }) { summary ->
                    GroupListItem(
                        summary = summary,
                        onClick = { onGroupClick(summary.group.id) },
                        onDeleteClick = { groupToDelete = summary }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }

        // Floating Action Button to create a group
        FloatingActionButton(
            onClick = onCreateGroupClick,
            shape = RoundedCornerShape(18.dp),
            containerColor = LavenderAccent,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("fab_create_group")
        ) {
            Icon(imageVector = Icons.Rounded.Add, contentDescription = "Create Group")
        }
    }
}

@Composable
fun GroupListItem(
    summary: GroupSummary,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pastelAccent = when (summary.group.colorIndex % 4) {
        0 -> PastelLavender
        1 -> PastelMint
        2 -> PastelPeach
        else -> PastelPink
    }

    MitmatCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        pastelColorLight = pastelAccent.copy(alpha = 0.4f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(pastelAccent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Groups,
                            contentDescription = null,
                            tint = LavenderAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = summary.group.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Currency: ${summary.group.defaultCurrency}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.testTag("delete_group_${summary.group.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Delete Group",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Participants Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                    summary.participants.take(5).forEachIndexed { index, p ->
                        ParticipantAvatar(
                            name = p.name,
                            colorIndex = p.avatarColorIndex,
                            size = 30.dp
                        )
                    }
                    if (summary.participants.size > 5) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+${summary.participants.size - 5}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    val net = summary.userNetBalance
                    val (color, status) = when {
                        net > 0.01 -> Pair(FinancialReceiveGreen, "You are owed ${FormatHelper.formatMoney(net, summary.group.defaultCurrency)}")
                        net < -0.01 -> Pair(FinancialOwePeach, "You owe ${FormatHelper.formatMoney(kotlin.math.abs(net), summary.group.defaultCurrency)}")
                        else -> Pair(MaterialTheme.colorScheme.onSurfaceVariant, "You are even")
                    }
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = color
                    )
                    Text(
                        text = "Total spent: ${FormatHelper.formatMoney(summary.totalExpenses, summary.group.defaultCurrency)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
