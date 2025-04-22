/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package com.example.vocabulary.presentation

import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Check // For Remembered button
import androidx.compose.material.icons.filled.Close // For Forgotten button
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel // Import viewModel
import androidx.wear.compose.material.*
import androidx.wear.tooling.preview.devices.WearDevices
import com.example.vocabulary.R
import com.example.vocabulary.domain.model.Word
import com.example.vocabulary.presentation.theme.VocabularyTheme
import kotlin.math.abs

// Screen sealed class is no longer needed here as ViewModel manages the main state

class MainActivity : ComponentActivity() {
    // Ambient mode handling can be kept or integrated into ViewModel later
    // private var isAmbient by mutableStateOf(false)
    // private val executor = Executors.newSingleThreadExecutor() // Likely not needed directly if Ambient logic moves

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ambient observer initialization - Keep for now, might need adjustments
        // lifecycle.addObserver(AmbientLifecycleObserver(...))

        setContent {
            VocabularyTheme {
                // Get ViewModel instance
                val vocabularyViewModel: VocabularyViewModel = viewModel()
                // Collect UI state
                val uiState by vocabularyViewModel.uiState.collectAsState()

                // Pass state and event handlers to the main App Composable
                VocabularyApp(
                    uiState = uiState,
                    onNextWord = vocabularyViewModel::nextWord,
                    onPreviousWord = vocabularyViewModel::previousWord,
                    onRemembered = vocabularyViewModel::wordRemembered,
                    onForgotten = vocabularyViewModel::wordForgotten,
                    onStartNextBatch = vocabularyViewModel::startNextBatch,
                    onNavigateToScreen = vocabularyViewModel::navigateToScreen,
                    onNextSentence = vocabularyViewModel::nextSentence,
                    onPreviousSentence = vocabularyViewModel::previousSentence
                )
            }
        }
    }

    // onDestroy logic for executor might need adjustment if executor is removed
    // override fun onDestroy() { ... }
}

/**
 * 应用程序的主 Composable，现在从 ViewModel 获取状态并委托事件。
 * 主要负责显示加载状态、错误状态或单词内容。
 *
 * @param uiState 当前的 UI 状态。
 * @param onNextWord 处理下一个单词事件的回调。
 * @param onPreviousWord 处理上一个单词事件的回调。
 * @param onRemembered 处理"记住了"事件的回调。
 * @param onForgotten 处理"忘记了"事件的回调。
 * @param onStartNextBatch 处理"开始下一批"事件的回调。
 * @param onNavigateToScreen 处理切换屏幕的事件回调。
 * @param onNextSentence 处理下一个例句事件的回调。
 * @param onPreviousSentence 处理上一个例句事件的回调。
 */
@Composable
fun VocabularyApp(
    uiState: VocabularyUiState,
    onNextWord: () -> Unit,
    onPreviousWord: () -> Unit,
    onRemembered: () -> Unit,
    onForgotten: () -> Unit,
    onStartNextBatch: () -> Unit,
    onNavigateToScreen: (Screen) -> Unit,
    onNextSentence: () -> Unit,
    onPreviousSentence: () -> Unit
) {
    // --- MediaPlayer 相关 (Kept similar for now, can be refactored) ---
    val context = LocalContext.current
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    // Release MediaPlayer when the Composable leaves composition or the audio source changes
    DisposableEffect(uiState.currentWord?.audioResId) { // Keyed to audioResId
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
            Log.i("MediaPlayer", "Released on Composable dispose or audio change")
        }
    }

    val playWordAudio: (Int?) -> Unit = { audioResId ->
        if (audioResId != null) {
            mediaPlayer?.release() // Release previous player first
            mediaPlayer = null
            Log.d("MediaPlayer", "Previous player released before new playback")
            try {
                mediaPlayer = MediaPlayer.create(context, audioResId).apply {
                    setOnPreparedListener { start() }
                    setOnCompletionListener { mp ->
                        mp.release()
                        if (mediaPlayer == mp) { mediaPlayer = null }
                        Log.d("MediaPlayer", "Released on completion")
                    }
                    setOnErrorListener { mp, _, _ ->
                        Log.e("MediaPlayer", "Error during playback")
                        mp.release()
                        if (mediaPlayer == mp) { mediaPlayer = null }
                        true
                    }
                }
            } catch (e: Exception) {
                Log.e("MediaPlayer", "Error creating MediaPlayer", e)
                mediaPlayer?.release()
                mediaPlayer = null
            }
        } else {
            Log.w("MediaPlayer", "Audio resource ID is null, skipping playback")
        }
    }
    // --- End MediaPlayer ---

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
        contentAlignment = Alignment.Center
    ) {
        when {
            // 加载状态
            uiState.isLoading -> {
                CircularProgressIndicator()
            }
            // 错误状态
            uiState.error != null -> {
                Text("Error: ${uiState.error}", color = MaterialTheme.colors.error)
            }
            // 批次完成状态
            uiState.isBatchComplete -> {
                 Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize().padding(16.dp)
                 ) {
                    Text("当前批次已完成！", style = MaterialTheme.typography.title3, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    // 添加按钮以开始下一批次
                    Button(onClick = onStartNextBatch) { // Use the passed callback
                        Text("开始下一批")
                    }
                 }
            }
            // 正常显示内容 (根据当前屏幕决定)
            uiState.currentWord != null -> {
                // --- 手势处理 (恢复垂直滑动) ---
                var dragAmountX by remember { mutableStateOf(0f) }
                var dragAmountY by remember { mutableStateOf(0f) }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        // Key by current word AND screen to reset gestures correctly
                        .pointerInput(uiState.currentWordInBatchIndex, uiState.currentScreen) {
                            detectDragGestures(
                                onDragStart = { dragAmountX = 0f; dragAmountY = 0f },
                                onDragEnd = {
                                    val absX = abs(dragAmountX)
                                    val absY = abs(dragAmountY)
                                    val dragThreshold = 75f

                                    if (absX > absY) { // 水平滑动为主
                                        if (dragAmountX < -dragThreshold) { // 左滑
                                            when (uiState.currentScreen) {
                                                is Screen.WordScreen -> onPreviousWord() // 修改：左滑切换到上一个单词
                                                is Screen.SentencesScreen -> onNextSentence()
                                                is Screen.RootScreen -> onNavigateToScreen(Screen.PhrasesScreen) // 从词根左滑到词组
                                                else -> {} // 词组页面等暂时不支持左滑
                                            }
                                        } else if (dragAmountX > dragThreshold) { // 右滑
                                             when (uiState.currentScreen) {
                                                is Screen.WordScreen -> onNextWord() // 修改：右滑切换到下一个单词
                                                is Screen.SentencesScreen -> onPreviousSentence()
                                                is Screen.PhrasesScreen -> onNavigateToScreen(Screen.RootScreen) // 从词组右滑回词根
                                                else -> {} // 根屏幕等暂时不支持右滑
                                            }
                                        }
                                    } else { // 垂直滑动为主
                                         if (dragAmountY < -dragThreshold) { // 上滑
                                            when (uiState.currentScreen) {
                                                is Screen.WordScreen -> onNavigateToScreen(Screen.RootScreen) // 从单词上滑到词根
                                                else -> onNavigateToScreen(Screen.WordScreen) // 其他屏幕上滑返回单词主页
                                            }
                                        } else if (dragAmountY > dragThreshold) { // 下滑
                                             when (uiState.currentScreen) {
                                                is Screen.WordScreen -> onNavigateToScreen(Screen.SentencesScreen(0)) // 下滑到第一条例句
                                                else -> onNavigateToScreen(Screen.WordScreen) // 其他屏幕下滑返回单词主页
                                            }
                                        }
                                    }
                                    // 重置拖动距离
                                    dragAmountX = 0f
                                    dragAmountY = 0f
                                },
                                onDrag = { change, drag ->
                                    // 累积拖动距离
                                    dragAmountX += drag.x
                                    dragAmountY += drag.y
                                    change.consume()
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // --- 根据当前屏幕显示不同的 Card --- 
                    when (val screen = uiState.currentScreen) {
                        is Screen.WordScreen -> {
                            WordCard(
                                word = uiState.currentWord,
                                isMeaningHidden = uiState.isCurrentWordMeaningHidden,
                                playWordAudio = playWordAudio,
                                onRemembered = onRemembered,
                                onForgotten = onForgotten,
                                memoryLevel = uiState.currentWordMemoryLevel
                            )
                        }
                        is Screen.SentencesScreen -> {
                            SentenceCard(
                                sentences = uiState.currentWord?.sentences ?: emptyList(), // Safe call
                                currentIndex = screen.sentenceIndex
                            )
                        }
                        is Screen.RootScreen -> { // 恢复 RootScreen
                            RootCard(rootInfo = uiState.currentWord?.rootInfo) // 只传递 rootInfo
                        }
                        is Screen.PhrasesScreen -> { // 新增：处理词组屏幕
                            PhrasesCard(phrases = uiState.currentWord?.phrases)
                        }
                    }
                }
            }
            // 无单词可显示（初始状态或异常）
            else -> {
                 Text("没有可显示的单词", style = MaterialTheme.typography.title3)
            }
        }
    }
}

/**
 * 显示单词信息的基础卡片，现在包含隐藏释义的逻辑和占位按钮。
 *
 * @param word 当前要显示的单词，可能为 null。
 * @param isMeaningHidden 指示是否隐藏中文释义。
 * @param playWordAudio 播放音频的回调函数。
 * @param onRemembered "记住了"按钮点击的回调。
 * @param onForgotten "忘记了"按钮点击的回调。
 * @param memoryLevel 当前单词的批次记忆等级。
 */
@Composable
fun WordCard(
    word: Word?, // Word can be null if state is inconsistent briefly
    isMeaningHidden: Boolean,
    playWordAudio: (Int?) -> Unit,
    onRemembered: () -> Unit,
    onForgotten: () -> Unit,
    memoryLevel: Int
) {
    if (word == null) {
        // Handle null case, maybe show a placeholder or nothing
        Text("加载中...")
        return
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp) // Adjust padding
    ) {
        // Word and Phonetic Section
        Text(
            text = word.word,
            style = MaterialTheme.typography.title1,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = word.phonetic,
                style = MaterialTheme.typography.body2,
                textAlign = TextAlign.Center
            )
            word.audioResId?.let { // Show icon only if audio exists
                Spacer(modifier = Modifier.width(8.dp)) // Increased spacing
                Icon(
                    imageVector = Icons.Filled.VolumeUp,
                    contentDescription = "播放发音",
                    modifier = Modifier
                        .size(20.dp) // Slightly larger icon
                        .clickable { playWordAudio(word.audioResId) },
                    tint = MaterialTheme.colors.primary
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp)) // Increased spacing

        // Meaning Section (Conditional Visibility)
        Box(modifier = Modifier.weight(1f).padding(bottom = 8.dp), contentAlignment = Alignment.Center) { // Allow meaning to take up space, add padding bottom
            if (!isMeaningHidden) {
                Text(
                    text = word.meaning,
                    style = MaterialTheme.typography.body1,
                    textAlign = TextAlign.Center,
                    maxLines = 4 // Allow more lines for meaning
                )
            } else {
                 Text(
                    text = "???", // Placeholder when meaning is hidden
                    style = MaterialTheme.typography.title2,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Memory Level Indicator
        MemoryLevelIndicator(currentLevel = memoryLevel, maxLevel = 4) // Use maxLevel = 4 as defined in ViewModel
        Spacer(modifier = Modifier.height(12.dp)) // Spacing before buttons

        // Action Buttons Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly, // Space out buttons
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onForgotten,
                modifier = Modifier.size(ButtonDefaults.SmallButtonSize),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red.copy(alpha = 0.8f))
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "忘记了",
                    modifier = Modifier.size(ButtonDefaults.SmallIconSize)
                )
            }
            Button(
                 onClick = onRemembered,
                 modifier = Modifier.size(ButtonDefaults.SmallButtonSize),
                 colors = ButtonDefaults.buttonColors(backgroundColor = Color.Green.copy(alpha = 0.8f))
            ) {
                 Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "记住了",
                    modifier = Modifier.size(ButtonDefaults.SmallIconSize)
                 )
            }
        }
         Spacer(modifier = Modifier.height(8.dp)) // Padding at the bottom
    }
}

/**
 * 显示记忆等级的指示器。
 * @param currentLevel 当前达到的等级。
 * @param maxLevel 总共的等级数。
 * @param modifier Modifier。
 * @param achievedColor 达到等级的颜色。
 * @param unachievedColor 未达到等级的颜色。
 */
@Composable
fun MemoryLevelIndicator(
    currentLevel: Int,
    maxLevel: Int,
    modifier: Modifier = Modifier,
    achievedColor: Color = MaterialTheme.colors.primary,
    unachievedColor: Color = MaterialTheme.colors.onSurface.copy(alpha = 0.2f)
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 从 1 到 maxLevel 创建圆点
        for (level in 1..maxLevel) {
            Box(
                modifier = Modifier
                    .size(8.dp) // 圆点大小
                    .background(
                        color = if (level <= currentLevel) achievedColor else unachievedColor,
                        shape = CircleShape
                    )
            )
            // 在圆点之间添加间距，除了最后一个
            if (level < maxLevel) {
                Spacer(modifier = Modifier.width(4.dp))
            }
        }
    }
}

/** 显示例句，并允许左右滑动切换的卡片 */
@Composable
fun SentenceCard(sentences: List<String>, currentIndex: Int) {
     Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        // 显示标题，如果有多条例句则显示索引
        val title = if (sentences.size > 1) {
            "例句 ${currentIndex + 1}/${sentences.size}"
        } else {
            "例句"
        }
        Text(title, style = MaterialTheme.typography.caption1)
        Spacer(modifier = Modifier.height(8.dp))

        // 显示当前例句或提示
        if (sentences.isNotEmpty() && currentIndex >= 0 && currentIndex < sentences.size) {
             Text(
                text = sentences[currentIndex],
                style = MaterialTheme.typography.body1,
                textAlign = TextAlign.Center
            )
        } else {
            Text("暂无例句", style = MaterialTheme.typography.body1)
        }
        // 可以在这里添加视觉提示，表明可以左右滑动切换例句，上下滑动返回
    }
}

/** 显示词根词缀信息的卡片 */
@Composable
fun RootCard(rootInfo: String?) { // 恢复为只接收 rootInfo
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
         verticalArrangement = Arrangement.Center // 居中显示
    ) {
        Text("词根词缀", style = MaterialTheme.typography.caption1)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = rootInfo ?: "暂无词根信息",
            style = MaterialTheme.typography.body1,
            textAlign = TextAlign.Center
        )
         // 可以在这里添加视觉提示，表明可以左滑查看词组，上下滑动返回
    }
}

/** 显示词组列表的卡片 */
@Composable
fun PhrasesCard(phrases: List<Pair<String, String>>?) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("常见词组", style = MaterialTheme.typography.caption1)
        Spacer(modifier = Modifier.height(8.dp))

        if (phrases?.isNotEmpty() == true) {
            phrases.forEach { (phrase, meaning) ->
                 Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = phrase,
                        style = MaterialTheme.typography.body2,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = meaning,
                        style = MaterialTheme.typography.caption1.copy(color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        } else {
            Text("暂无常见词组", style = MaterialTheme.typography.body1)
        }
         // 可以在这里添加视觉提示，表明可以右滑返回词根，上下滑动返回
    }
}

// --- Preview (Updated for new WordCard signature) ---
@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    VocabularyTheme {
        // Create a dummy Word object for preview
        val previewWord = Word(
            id = 1,
            word = "Preview",
            phonetic = "/ˈpriːvjuː/",
            meaning = "n. 预览；试映",
            audioResId = R.raw.word_absence // Use an existing raw resource for preview if needed
        )
        WordCard(
            word = previewWord,
            isMeaningHidden = false, // Preview with meaning shown
            playWordAudio = {}, // No-op for preview
            onRemembered = {}, // Provide empty lambdas for preview
            onForgotten = {},
            memoryLevel = 2 // Preview with level 2
        )
    }
}

// SentenceCard, RootCard, SynonymsCard, AntonymsCard are removed for now.

