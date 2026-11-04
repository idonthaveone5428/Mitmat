package dev.rakib.mitmat.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.rakib.mitmat.data.entity.ExpenseEntity
import dev.rakib.mitmat.domain.model.DebtTransaction
import dev.rakib.mitmat.domain.model.ExpenseWithDetails
import dev.rakib.mitmat.ui.screens.ActivityScreen
import dev.rakib.mitmat.ui.screens.AddExpenseScreen
import dev.rakib.mitmat.ui.screens.CreateGroupDialog
import dev.rakib.mitmat.ui.screens.GroupDetailScreen
import dev.rakib.mitmat.ui.screens.GroupsScreen
import dev.rakib.mitmat.ui.screens.HomeScreen
import dev.rakib.mitmat.ui.screens.RecordSettlementDialog
import dev.rakib.mitmat.ui.screens.SettingsScreen
import dev.rakib.mitmat.ui.theme.LavenderAccent
import dev.rakib.mitmat.ui.theme.LocalIsDarkTheme
import dev.rakib.mitmat.ui.theme.MitmatScreenScaffold
import dev.rakib.mitmat.ui.theme.PastelLavender
import dev.rakib.mitmat.ui.viewmodel.MitmatViewModel

enum class NavigationTab(val label: String, val icon: ImageVector, val testTag: String) {
    HOME("Home", Icons.Rounded.Home, "nav_tab_home"),
    GROUPS("Groups", Icons.Rounded.Groups, "nav_tab_groups"),
    ACTIVITY("Activity", Icons.AutoMirrored.Rounded.ReceiptLong, "nav_tab_activity"),
    SETTINGS("Settings", Icons.Rounded.Settings, "nav_tab_settings")
}

sealed interface AppScreen {
    data class MainTabs(val tab: NavigationTab) : AppScreen
    data class GroupDetail(val groupId: Long) : AppScreen
    data class AddExpense(val groupId: Long? = null, val editingExpense: ExpenseWithDetails? = null) : AppScreen
}

@Composable
fun MainAppScaffold(
    viewModel: MitmatViewModel,
    modifier: Modifier = Modifier
) {
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val allGroups by viewModel.allGroups.collectAsStateWithLifecycle()
    val archivedGroups by viewModel.archivedGroups.collectAsStateWithLifecycle()
    val trashedGroups by viewModel.trashedGroups.collectAsStateWithLifecycle()
    val groupSummaries by viewModel.groupSummaries.collectAsStateWithLifecycle()
    val activityFeed by viewModel.activityFeed.collectAsStateWithLifecycle()
    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val groupDetailState by viewModel.selectedGroupDetail.collectAsStateWithLifecycle()

    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.MainTabs(NavigationTab.HOME)) }

    // Dialog states
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var activeSettlementDialogData by remember {
        mutableStateOf<Pair<Long, DebtTransaction?>?>(null) // groupId, debtTransaction
    }

    // Back handling
    BackHandler(enabled = currentScreen !is AppScreen.MainTabs || (currentScreen as AppScreen.MainTabs).tab != NavigationTab.HOME) {
        when (val screen = currentScreen) {
            is AppScreen.GroupDetail -> {
                viewModel.selectGroup(null)
                currentScreen = AppScreen.MainTabs(NavigationTab.GROUPS)
            }
            is AppScreen.AddExpense -> {
                if (screen.groupId != null) {
                    currentScreen = AppScreen.GroupDetail(screen.groupId)
                } else {
                    currentScreen = AppScreen.MainTabs(NavigationTab.HOME)
                }
            }
            is AppScreen.MainTabs -> {
                if (screen.tab != NavigationTab.HOME) {
                    currentScreen = AppScreen.MainTabs(NavigationTab.HOME)
                }
            }
        }
    }

    // Create Group Dialog
    if (showCreateGroupDialog) {
        CreateGroupDialog(
            defaultCurrency = preferences.defaultCurrency,
            onConfirm = { name, currency, colorIndex, participants ->
                viewModel.createGroup(name, currency, colorIndex, participants)
                showCreateGroupDialog = false
            },
            onDismiss = { showCreateGroupDialog = false }
        )
    }

    // Record Settlement Dialog
    activeSettlementDialogData?.let { (groupId, initialDebt) ->
        val group = allGroups.firstOrNull { it.id == groupId }
        val participants = groupDetailState.participants.ifEmpty {
            // fallback if not in detail state
            emptyList()
        }
        val currency = group?.defaultCurrency ?: preferences.defaultCurrency

        RecordSettlementDialog(
            groupId = groupId,
            participants = participants,
            currency = currency,
            initialDebt = initialDebt,
            onConfirm = { payerId, receiverId, amount, note ->
                viewModel.addSettlement(groupId, payerId, receiverId, amount, note)
                activeSettlementDialogData = null
            },
            onDismiss = { activeSettlementDialogData = null }
        )
    }

    MitmatScreenScaffold(modifier = modifier) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (currentScreen is AppScreen.MainTabs) {
                    val activeTab = (currentScreen as AppScreen.MainTabs).tab
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        tonalElevation = 6.dp,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        NavigationTab.entries.forEach { tab ->
                            val isSelected = tab == activeTab
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { currentScreen = AppScreen.MainTabs(tab) },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = LavenderAccent,
                                    selectedTextColor = LavenderAccent,
                                    indicatorColor = PastelLavender
                                ),
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "screen_transition"
                ) { screen ->
                    when (screen) {
                        is AppScreen.MainTabs -> {
                            when (screen.tab) {
                                NavigationTab.HOME -> {
                                    HomeScreen(
                                        dashboardState = dashboardState,
                                        groupSummaries = groupSummaries,
                                        onAddExpenseClick = {
                                            currentScreen = AppScreen.AddExpense()
                                        },
                                        onCreateGroupClick = { showCreateGroupDialog = true },
                                        onGroupClick = { groupId ->
                                            viewModel.selectGroup(groupId)
                                            currentScreen = AppScreen.GroupDetail(groupId)
                                        },
                                        onViewAllGroupsClick = {
                                            currentScreen = AppScreen.MainTabs(NavigationTab.GROUPS)
                                        },
                                        onViewActivityClick = {
                                            currentScreen = AppScreen.MainTabs(NavigationTab.ACTIVITY)
                                        }
                                    )
                                }

                                NavigationTab.GROUPS -> {
                                    GroupsScreen(
                                        groupSummaries = groupSummaries,
                                        onCreateGroupClick = { showCreateGroupDialog = true },
                                        onGroupClick = { groupId ->
                                            viewModel.selectGroup(groupId)
                                            currentScreen = AppScreen.GroupDetail(groupId)
                                        },
                                        onDeleteGroup = { groupId -> viewModel.deleteGroup(groupId) }
                                    )
                                }

                                NavigationTab.ACTIVITY -> {
                                    ActivityScreen(
                                        feedItems = activityFeed,
                                        groups = allGroups,
                                        onGroupClick = { groupId ->
                                            viewModel.selectGroup(groupId)
                                            currentScreen = AppScreen.GroupDetail(groupId)
                                        }
                                    )
                                }

                                NavigationTab.SETTINGS -> {
                                    SettingsScreen(
                                        preferences = preferences,
                                        onSetAppearanceMode = { viewModel.setAppearanceMode(it) },
                                        onSetIsAmoled = { viewModel.setIsAmoled(it) },
                                        allGroups = allGroups,
                                        archivedGroups = archivedGroups,
                                        trashedGroups = trashedGroups,
                                        onViewGroup = { groupId ->
                                            viewModel.selectGroup(groupId)
                                            currentScreen = AppScreen.GroupDetail(groupId)
                                        },
                                        onUnarchiveGroup = { groupId ->
                                            viewModel.archiveGroup(groupId, false)
                                        },
                                        onRestoreGroup = { groupId ->
                                            viewModel.restoreGroup(groupId)
                                        },
                                        onPermanentlyDeleteGroup = { groupId ->
                                            viewModel.permanentlyDeleteGroup(groupId)
                                        },
                                        onEmptyTrash = {
                                            viewModel.emptyTrash()
                                        },
                                        onExportData = { selectedGroupIds, callback ->
                                            viewModel.exportBackupJson(selectedGroupIds, callback)
                                        },
                                        onImportData = { json, callback ->
                                            viewModel.importBackupJson(json, callback)
                                        }
                                    )
                                }
                            }
                        }

                        is AppScreen.GroupDetail -> {
                            GroupDetailScreen(
                                state = groupDetailState,
                                onBackClick = {
                                    viewModel.selectGroup(null)
                                    currentScreen = AppScreen.MainTabs(NavigationTab.GROUPS)
                                },
                                onAddExpenseClick = { groupId ->
                                    currentScreen = AppScreen.AddExpense(groupId = groupId)
                                },
                                onEditExpenseClick = { expenseWithDetails ->
                                    currentScreen = AppScreen.AddExpense(
                                        groupId = expenseWithDetails.expense.groupId,
                                        editingExpense = expenseWithDetails
                                    )
                                },
                                onDeleteExpenseClick = { expenseId ->
                                    viewModel.deleteExpense(expenseId)
                                },
                                onSettleDebtClick = { debt ->
                                    activeSettlementDialogData = Pair(screen.groupId, debt)
                                },
                                onRecordCustomSettlementClick = { groupId ->
                                    activeSettlementDialogData = Pair(groupId, null)
                                },
                                onDeleteSettlementClick = { settlementId ->
                                    viewModel.deleteSettlement(settlementId)
                                },
                                onAddParticipant = { name ->
                                    viewModel.addParticipant(screen.groupId, name)
                                },
                                onEditParticipant = { participant ->
                                    viewModel.updateParticipant(participant)
                                },
                                onDeleteParticipant = { participant ->
                                    viewModel.deleteParticipant(participant)
                                },
                                onDeleteGroup = { groupId ->
                                    viewModel.deleteGroup(groupId)
                                    viewModel.selectGroup(null)
                                    currentScreen = AppScreen.MainTabs(NavigationTab.GROUPS)
                                },
                                onToggleArchiveGroup = { groupId, archive ->
                                    viewModel.archiveGroup(groupId, archive)
                                }
                            )
                        }

                        is AppScreen.AddExpense -> {
                            val activeGroupId = screen.groupId ?: allGroups.firstOrNull()?.id ?: 0L
                            val participants = if (activeGroupId == groupDetailState.group?.id) {
                                groupDetailState.participants
                            } else {
                                remember(activeGroupId, allGroups) {
                                    // Make sure we select this group in viewModel to load its participants
                                    viewModel.selectGroup(activeGroupId)
                                    groupDetailState.participants
                                }
                            }

                            AddExpenseScreen(
                                groups = allGroups,
                                initialGroupId = screen.groupId,
                                participantsForGroup = groupDetailState.participants,
                                onGroupSelected = { groupId ->
                                    viewModel.selectGroup(groupId)
                                },
                                onSaveExpense = { groupId, title, amount, payerId, splitType, category, note, splits, percentages, payers ->
                                    if (screen.editingExpense != null) {
                                        viewModel.updateExpense(
                                            expense = screen.editingExpense.expense.copy(
                                                title = title,
                                                totalAmount = amount,
                                                paidByParticipantId = payerId,
                                                splitType = splitType,
                                                category = category,
                                                note = note
                                            ),
                                            splits = splits,
                                            percentages = percentages,
                                            payers = payers
                                        )
                                    } else {
                                        viewModel.addExpense(
                                            groupId = groupId,
                                            title = title,
                                            amount = amount,
                                            payerId = payerId,
                                            splitType = splitType,
                                            category = category,
                                            note = note,
                                            splits = splits,
                                            percentages = percentages,
                                            payers = payers
                                        )
                                    }
                                },
                                onBackClick = {
                                    if (screen.groupId != null) {
                                        currentScreen = AppScreen.GroupDetail(screen.groupId)
                                    } else {
                                        currentScreen = AppScreen.MainTabs(NavigationTab.HOME)
                                    }
                                },
                                editingExpense = screen.editingExpense
                            )
                        }
                    }
                }
            }
        }
    }
}
