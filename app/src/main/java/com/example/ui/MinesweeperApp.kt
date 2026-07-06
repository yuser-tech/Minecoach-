package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.game.Cell
import com.example.game.Difficulty
import com.example.game.GameStatus
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinesweeperApp(
    viewModel: MinesweeperViewModel,
    modifier: Modifier = Modifier
) {
    val board by viewModel.boardState.collectAsState()
    val gameStatus by viewModel.gameStatus.collectAsState()
    val minesLeft by viewModel.minesLeft.collectAsState()
    val totalMines by viewModel.totalMines.collectAsState()
    val difficulty by viewModel.difficulty.collectAsState()
    val timerValue by viewModel.timerValue.collectAsState()
    val isFlagMode by viewModel.isFlagMode.collectAsState()
    val winsCount by viewModel.winsCount.collectAsState()
    val lossesCount by viewModel.lossesCount.collectAsState()
    val isCoachLoading by viewModel.isCoachLoading.collectAsState()
    val latestAdvice by viewModel.latestCoachAdvice.collectAsState()
    val chatHistory by viewModel.coachChatHistory.collectAsState()

    var showHistoryDialog by remember { mutableStateOf(false) }
    var showOnboardingDialog by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Minesweeper Coach",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                        Text(
                            "Tactical AI Field Advisor",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showHistoryDialog = true },
                        modifier = Modifier.testTag("tactical_log_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Tactical Logs",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { showOnboardingDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "How to Play"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Stats & Control Board
            StatsPanel(
                wins = winsCount,
                losses = lossesCount,
                onResetStats = { viewModel.resetStats() }
            )

            // Difficulty Chips selector
            DifficultySelector(
                currentDifficulty = difficulty,
                onDifficultySelected = { viewModel.startNewGame(it) }
            )

            // Timer & Mines Remaining HUD
            HudPanel(
                timerValue = timerValue,
                minesLeft = minesLeft,
                totalMines = totalMines,
                gameStatus = gameStatus,
                onRestart = { viewModel.startNewGame(difficulty) }
            )

            // Main Interactive Minefield Grid
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (board.isNotEmpty()) {
                    MinesweeperGrid(
                        board = board,
                        rows = difficulty.rows,
                        cols = difficulty.cols,
                        onCellClick = { r, c -> viewModel.onCellClicked(r, c) },
                        onCellLongClick = { r, c -> viewModel.onCellLongPressed(r, c) }
                    )
                }
            }

            // Coach Sandy Dashboard Panel
            CoachDashboard(
                latestAdvice = latestAdvice,
                isLoading = isCoachLoading,
                gameStatus = gameStatus,
                onAskCoach = { viewModel.askCoachForHelp() },
                onViewLogs = { showHistoryDialog = true },
                isFlagMode = isFlagMode,
                onToggleFlagMode = { viewModel.toggleFlagMode() }
            )
        }
    }

    // Interactive Full-Thread Conversational History Dialog
    if (showHistoryDialog) {
        TacticalLogsDialog(
            chatHistory = chatHistory,
            onDismiss = { showHistoryDialog = false }
        )
    }

    // Onboarding Quick-Help Rules Overlay
    if (showOnboardingDialog) {
        OnboardingDialog(
            onDismiss = { showOnboardingDialog = false }
        )
    }
}

@Composable
fun StatsPanel(
    wins: Int,
    losses: Int,
    onResetStats: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Wins",
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Secured: $wins",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Dangerous,
                        contentDescription = "Losses",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Defeated: $losses",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (wins > 0 || losses > 0) {
                TextButton(
                    onClick = onResetStats,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier
                        .height(30.dp)
                        .testTag("reset_stats_button")
                ) {
                    Text("Clear Records", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun DifficultySelector(
    currentDifficulty: Difficulty,
    onDifficultySelected: (Difficulty) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        Difficulty.values().forEach { diff ->
            val isSelected = currentDifficulty == diff
            InputChip(
                selected = isSelected,
                onClick = { onDifficultySelected(diff) },
                label = { Text(diff.label, fontSize = 12.sp) },
                modifier = Modifier.testTag("difficulty_${diff.name.lowercase()}"),
                colors = InputChipDefaults.inputChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

@Composable
fun HudPanel(
    timerValue: Int,
    minesLeft: Int,
    totalMines: Int,
    gameStatus: GameStatus,
    onRestart: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Mines left HUD card
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
            modifier = Modifier.width(100.dp)
        ) {
            Row(
                modifier = Modifier.padding(vertical = 6.dp, horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = "Mines Left",
                    tint = Color(0xFFFF3D00),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = String.format("%02d", minesLeft),
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Restart status face button
        IconButton(
            onClick = onRestart,
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
                .testTag("reset_game_button")
        ) {
            val iconVec = when (gameStatus) {
                GameStatus.WON -> Icons.Default.SentimentVerySatisfied
                GameStatus.LOST -> Icons.Default.SentimentVeryDissatisfied
                else -> Icons.Default.Refresh
            }
            Icon(
                imageVector = iconVec,
                contentDescription = "Restart Game",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(22.dp)
            )
        }

        // Timer HUD card
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
            modifier = Modifier.width(100.dp)
        ) {
            Row(
                modifier = Modifier.padding(vertical = 6.dp, horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = "Timer",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = String.format("%03d", timerValue),
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun MinesweeperGrid(
    board: List<List<Cell>>,
    rows: Int,
    cols: Int,
    onCellClick: (Int, Int) -> Unit,
    onCellLongClick: (Int, Int) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        val totalSpace = maxWidth
        // Leave tiny paddings for cell gaps
        val cellSize = (totalSpace - 16.dp) / cols

        Column(
            verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            for (r in 0 until rows) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (c in 0 until cols) {
                        val cell = board[r][c]
                        MinesweeperCell(
                            cell = cell,
                            size = cellSize,
                            onClick = { onCellClick(r, c) },
                            onLongClick = { onCellLongClick(r, c) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MinesweeperCell(
    cell: Cell,
    size: Dp,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val cellColor = when {
        cell.isRevealed -> {
            if (cell.isMine) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp)
            }
        }
        cell.isFlagged -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
        else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    }

    val cellBorder = if (cell.isRevealed) {
        BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    } else {
        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    }

    Box(
        modifier = Modifier
            .size(size)
            .padding(1.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(cellColor)
            .border(cellBorder, RoundedCornerShape(4.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("cell_${cell.row}_${cell.col}"),
        contentAlignment = Alignment.Center
    ) {
        when {
            cell.isRevealed && cell.isMine -> {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Exploded Mine",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(size * 0.6f)
                )
            }
            cell.isFlagged -> {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = "Flagged Mine",
                    tint = Color(0xFFFF3D00),
                    modifier = Modifier.size(size * 0.65f)
                )
            }
            cell.isRevealed && cell.adjacentMines > 0 -> {
                Text(
                    text = cell.adjacentMines.toString(),
                    color = getNumberColor(cell.adjacentMines),
                    fontSize = (size.value * 0.5f).sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun CoachDashboard(
    latestAdvice: String,
    isLoading: Boolean,
    gameStatus: GameStatus,
    onAskCoach: () -> Unit,
    onViewLogs: () -> Unit,
    isFlagMode: Boolean,
    onToggleFlagMode: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Coach Identity Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(42.dp)) {
                        Image(
                            painter = painterResource(id = R.drawable.coach_avatar_1783258291516),
                            contentDescription = "Captain Sandy Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        )
                        // Online green dot
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(Color(0xFF4CAF50), CircleShape)
                                .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                .align(Alignment.BottomEnd)
                        )
                    }

                    Column {
                        Text(
                            text = "Captain Sandy",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (isLoading) "Scanning terrain..." else "Tactical Advisor Active",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isLoading) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Dig / Flag Mode Switcher (Active Tool Selector)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = { if (isFlagMode) onToggleFlagMode() },
                        modifier = Modifier
                            .size(34.dp)
                            .background(
                                if (!isFlagMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                                RoundedCornerShape(6.dp)
                            )
                            .testTag("tool_dig")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Dig Mode",
                            tint = if (!isFlagMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = { if (!isFlagMode) onToggleFlagMode() },
                        modifier = Modifier
                            .size(34.dp)
                            .background(
                                if (isFlagMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                                RoundedCornerShape(6.dp)
                            )
                            .testTag("tool_flag")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = "Flag Mode",
                            tint = if (isFlagMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Coach Advice Speech Bubble
            Surface(
                shape = RoundedCornerShape(topStart = 0.dp, topEnd = 14.dp, bottomStart = 14.dp, bottomEnd = 14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 6.dp, top = 4.dp)
            ) {
                Box(
                    modifier = Modifier.padding(10.dp)
                ) {
                    if (isLoading) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Stand by recruit, transmitting radar report...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        Text(
                            text = latestAdvice,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 19.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAskCoach,
                    enabled = !isLoading && gameStatus != GameStatus.WON && gameStatus != GameStatus.LOST,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("coach_scan_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "Coach Help",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Coach Scan", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = onViewLogs,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = "Tactical Logs",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tactical Logs", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun TacticalLogsDialog(
    chatHistory: List<CoachMessage>,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ListAlt,
                            contentDescription = "Logs",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Tactical Comms Log",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Conversations List
                val listState = rememberLazyListState()
                val coroutineScope = rememberCoroutineScope()

                // Auto Scroll to bottom when list changes
                LaunchedEffect(chatHistory.size) {
                    if (chatHistory.isNotEmpty()) {
                        coroutineScope.launch {
                            listState.animateScrollToItem(chatHistory.size - 1)
                        }
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(chatHistory) { chat ->
                        val isCoach = chat.sender == "Captain Sandy"
                        val bubbleColor = if (isCoach) {
                            MaterialTheme.colorScheme.surfaceVariant
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        }

                        val textColor = if (isCoach) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        }

                        val alignment = if (isCoach) Alignment.Start else Alignment.End
                        val bubbleShape = if (isCoach) {
                            RoundedCornerShape(topStart = 0.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 12.dp)
                        } else {
                            RoundedCornerShape(topStart = 12.dp, topEnd = 0.dp, bottomStart = 12.dp, bottomEnd = 12.dp)
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = alignment
                        ) {
                            // Sender badge
                            Text(
                                text = chat.sender,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isCoach) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )

                            // Dialogue box
                            Surface(
                                shape = bubbleShape,
                                color = bubbleColor,
                                modifier = Modifier.widthIn(max = 260.dp)
                            ) {
                                Text(
                                    text = chat.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textColor,
                                    modifier = Modifier.padding(10.dp),
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Return to Battle Field")
                }
            }
        }
    }
}

@Composable
fun OnboardingDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Banner image
                Image(
                    painter = painterResource(id = R.drawable.minesweeper_hero_1783258309327),
                    contentDescription = "Minesweeper Tactics Board",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(10.dp))
                )

                Text(
                    text = "Tactical Briefing, Recruit!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OnboardingRuleRow(
                        number = "1",
                        title = "Deploy & Clear",
                        description = "Your first press is 100% safe! Click any block on the grid to create a landing zone."
                    )
                    OnboardingRuleRow(
                        number = "2",
                        title = "Read Adjacency Numbers",
                        description = "Numbers show how many mines exist in the 8 surrounding fields. Clear safe blocks, flag mines!"
                    )
                    OnboardingRuleRow(
                        number = "3",
                        title = "Active Tools Switcher",
                        description = "Use the Search (Dig) and Flag tools to toggle click behavior, or long-press cells to flag them directly."
                    )
                    OnboardingRuleRow(
                        number = "4",
                        title = "Enlist Captain Sandy",
                        description = "Get stuck? Press 'Coach Scan'! Sandy uses AI to read the field and logically explain safe operations!"
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("I'm Ready, Captain!")
                }
            }
        }
    }
}

@Composable
fun OnboardingRuleRow(
    number: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(description, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary, lineHeight = 14.sp)
        }
    }
}

fun getNumberColor(count: Int): Color {
    return when (count) {
        1 -> Color(0xFF1976D2) // Neon-adjacent Blue
        2 -> Color(0xFF2E7D32) // Neon-adjacent Green
        3 -> Color(0xFFC62828) // Neon-adjacent Red
        4 -> Color(0xFF1565C0) // Royal Blue
        5 -> Color(0xFF6A1B9A) // Deep Purple
        6 -> Color(0xFF00838F) // Cyan/Teal
        7 -> Color(0xFF212121) // Charcoal Black
        8 -> Color(0xFF555555) // Steel Gray
        else -> Color.Transparent
    }
}
