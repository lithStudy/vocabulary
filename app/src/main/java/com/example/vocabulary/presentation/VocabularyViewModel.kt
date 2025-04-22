package com.example.vocabulary.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vocabulary.data.WordRepository
import com.example.vocabulary.domain.model.Word
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

// 定义 UI 状态的数据类
data class VocabularyUiState(
    val currentBatchWords: List<Word> = emptyList(),       // 当前批次的单词列表
    val currentWordInBatchIndex: Int = -1,                 // 当前单词在批次中的索引
    val batchMemoryState: Map<Int, Int> = emptyMap(),     // <WordId, MemoryLevel> 批次内记忆状态
    val isLoading: Boolean = true,
    val error: String? = null,
    val isCurrentWordMeaningHidden: Boolean = false,       // 当前单词释义是否隐藏
    val isBatchComplete: Boolean = false,                // 当前批次是否完成
    val completedWordsInBatchCount: Int = 0             // 当前批次已完成单词数
) {
    // 获取当前批次中正在显示的单词
    val currentWord: Word? = currentBatchWords.getOrNull(currentWordInBatchIndex)

    // 当前单词的记忆等级
    val currentWordMemoryLevel: Int
        get() = currentWord?.id?.let { batchMemoryState[it] } ?: 0
}

class VocabularyViewModel(private val repository: WordRepository = WordRepository()) : ViewModel() {

    private val _uiState = MutableStateFlow(VocabularyUiState())
    val uiState: StateFlow<VocabularyUiState> = _uiState.asStateFlow()

    private var allWords: List<Word> = emptyList()
    private val batchSize = 20 // 定义批次大小
    private val requiredMemoryLevel = 4 // 定义批次内完成所需的记忆等级

    init {
        loadInitialData()
    }

    // 加载所有单词数据并准备第一个批次
    private fun loadInitialData() {
        viewModelScope.launch {
            // 注意：实际应用中，从数据库加载 allWords 可能更合适
            // 这样 Word 对象本身就能包含持久化的 isLearned, nextReviewDate 等状态
            allWords = repository.getWords() // 目前是从内存 repository 加载
            loadNextBatch() // 加载第一个批次
        }
    }

    // 加载下一个批次（混合复习和新单词）
    private fun loadNextBatch() {
        val currentTime = System.currentTimeMillis()
        val maxReviewWords = 20
        val maxNewWords = 20
        val targetBatchSize = 40 // 目标批次大小

        // 1. 筛选需要复习的单词
        val reviewWords = allWords
            .filter { it.isLearned && it.nextReviewDate <= currentTime }
            .sortedBy { it.nextReviewDate } // 优先复习最久没复习的
            .take(maxReviewWords)

        // 2. 筛选新单词
        val availableNewWords = allWords.filter { !it.isLearned }
        // 计算还需要多少新单词来达到目标批次大小，但不超过 maxNewWords
        val neededNewWordsCount = (targetBatchSize - reviewWords.size).coerceAtMost(maxNewWords)
        val newWords = availableNewWords.take(neededNewWordsCount.coerceAtLeast(0))

        // 3. 合并并打乱批次
        val combinedBatch = (reviewWords + newWords).shuffled()

        if (combinedBatch.isEmpty()) {
            // 如果既没有复习词也没有新词
            _uiState.update {
                it.copy(isLoading = false, currentBatchWords = emptyList(), currentWordInBatchIndex = -1, isBatchComplete = true, completedWordsInBatchCount = 0)
            }
            return
        }

        // 4. 初始化新批次的记忆状态（复习词也重置为0）
        val initialBatchState = combinedBatch.associate { it.id to 0 }

        // 5. 更新 UI 状态
        _uiState.update {
            it.copy(
                isLoading = false,
                currentBatchWords = combinedBatch,
                currentWordInBatchIndex = 0,
                batchMemoryState = initialBatchState,
                isCurrentWordMeaningHidden = false, // 新批次第一个单词默认显示释义
                isBatchComplete = false,
                completedWordsInBatchCount = 0
            )
        }
    }

    /** 供 UI 调用以开始下一个批次 */
    fun startNextBatch() {
        // 重置加载状态或进行其他准备（如果需要）
        // _uiState.update { it.copy(isLoading = true) }
        loadNextBatch()
    }

    /** 处理"记住了"事件 */
    fun wordRemembered() {
        val currentState = _uiState.value
        val currentWord = currentState.currentWord ?: return
        val currentLevel = currentState.currentWordMemoryLevel

        if (currentLevel >= requiredMemoryLevel) return // 如果已经完成，不再处理

        val nextLevel = currentLevel + 1
        val newBatchState = currentState.batchMemoryState.toMutableMap()
        newBatchState[currentWord.id] = nextLevel

        var completedIncrement = 0
        // 更新全局状态（如果达到完成级别）
        if (nextLevel == requiredMemoryLevel) {
            completedIncrement = 1
            val currentTime = System.currentTimeMillis()
            // 简单的艾宾浩斯间隔计算（占位符）
            val nextIntervalDays = calculateNextInterval(currentWord.reviewIntervalDays, currentWord.consecutiveCorrect + 1)
            val nextReviewTime = currentTime + TimeUnit.DAYS.toMillis(nextIntervalDays.toLong())

            repository.updateWordState(
                wordId = currentWord.id,
                isLearned = true,
                lastReviewDate = currentTime,
                nextReviewDate = nextReviewTime,
                reviewIntervalDays = nextIntervalDays,
                consecutiveCorrect = currentWord.consecutiveCorrect + 1
            )
        }

        val batchComplete = checkBatchCompletion(newBatchState, currentState.currentBatchWords)
        val nextCompletedCount = currentState.completedWordsInBatchCount + completedIncrement

        _uiState.update {
            it.copy(
                batchMemoryState = newBatchState,
                isCurrentWordMeaningHidden = nextLevel >= 3, // 等级3开始隐藏释义
                isBatchComplete = batchComplete,
                completedWordsInBatchCount = nextCompletedCount
            )
        }

        // 如果批次未完成，自动导航到下一个未完成的单词
        if (!batchComplete) {
            findNextIncompleteWord(forward = true)
        }
    }

    /** 处理"忘记了"事件 */
    fun wordForgotten() {
        val currentState = _uiState.value
        val currentWord = currentState.currentWord ?: return

        val newBatchState = currentState.batchMemoryState.toMutableMap()
        newBatchState[currentWord.id] = 0 // 重置批次记忆度

        // 重置全局连续正确次数
        repository.updateWordState(
            wordId = currentWord.id,
            isLearned = currentWord.isLearned, // isLearned 状态不变
            lastReviewDate = currentWord.lastReviewDate, // 复习日期不变
            nextReviewDate = currentWord.nextReviewDate,
            reviewIntervalDays = currentWord.reviewIntervalDays, // 间隔可能需要调整，暂不处理
            consecutiveCorrect = 0 // 重置连续正确次数
        )

         _uiState.update {
            it.copy(
                batchMemoryState = newBatchState,
                isCurrentWordMeaningHidden = false // 忘记了就显示释义
            )
        }

        // 自动导航到下一个未完成的单词
        findNextIncompleteWord(forward = true)
    }

    /** 导航到下一个单词（跳过已完成的） */
    fun nextWord() {
        findNextIncompleteWord(forward = true)
    }

    /** 导航到上一个单词（跳过已完成的） */
    fun previousWord() {
        findNextIncompleteWord(forward = false)
    }

    /**
     * 查找并导航到下一个或上一个未完成的单词。
     * @param forward true 表示向前查找，false 表示向后查找。
     */
    private fun findNextIncompleteWord(forward: Boolean) {
        val currentState = _uiState.value
        if (currentState.currentBatchWords.isEmpty() || currentState.isBatchComplete) return

        val batchWords = currentState.currentBatchWords
        val currentIdx = currentState.currentWordInBatchIndex
        val size = batchWords.size
        var attempts = 0 // 防止无限循环

        var nextIndex = currentIdx
        while (attempts < size) {
            if (forward) {
                nextIndex = (nextIndex + 1) % size
            } else {
                nextIndex = (nextIndex - 1 + size) % size
            }

            val nextWord = batchWords[nextIndex]
            val nextWordLevel = currentState.batchMemoryState[nextWord.id] ?: 0

            // 如果找到未完成的单词
            if (nextWordLevel < requiredMemoryLevel) {
                 _uiState.update {
                    it.copy(
                        currentWordInBatchIndex = nextIndex,
                        isCurrentWordMeaningHidden = nextWordLevel >= 3
                    )
                }
                return // 找到并更新，退出循环
            }
            attempts++
        }

        // 如果循环一圈都没找到未完成的单词，说明批次已完成（理论上应该在 wordRemembered 中捕获）
        if (!currentState.isBatchComplete) {
             _uiState.update { it.copy(isBatchComplete = true) }
        }
    }

    /** 检查当前批次是否所有单词都已达到记忆要求 */
    private fun checkBatchCompletion(batchState: Map<Int, Int>, batchWords: List<Word>): Boolean {
        if (batchWords.isEmpty()) return false
        return batchWords.all { word ->
            (batchState[word.id] ?: 0) >= requiredMemoryLevel
        }
    }

     /**
     * 计算下一个复习间隔（基于艾宾浩斯曲线的简化模型）。
     * 实际应用可能需要根据用户表现进行更动态的调整。
     */
    private fun calculateNextInterval(currentInterval: Int, consecutiveCorrect: Int): Int {
        // 常见的艾宾浩斯间隔序列 (天): 1, 2, 4, 7, 15, 30, 60, ...
        // 这里我们直接使用连续答对次数作为索引来查找间隔
        return when (consecutiveCorrect) {
            1 -> 1    // 第一次完成 -> 1天后
            2 -> 2    // 第二次完成 -> 2天后
            3 -> 4    // 第三次完成 -> 4天后
            4 -> 7    // 第四次完成 -> 7天后
            5 -> 15   // 第五次完成 -> 15天后
            6 -> 30   // 第六次完成 -> 30天后
            7 -> 60   // 第七次完成 -> 60天后
            // 可以根据需要继续添加更多间隔
            else -> (currentInterval * 2).coerceAtLeast(90) // 超过预设序列后，大致翻倍，至少90天
        }
    }

    // --- 后续需要添加处理复习逻辑的方法，如 loadReviewBatch ---

} 