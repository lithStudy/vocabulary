package com.example.vocabulary.data

import com.example.vocabulary.R
import com.example.vocabulary.domain.model.Word

class WordRepository {
    // 模拟的单词数据源。在实际应用中，这应该来自数据库或网络。
    // 添加了 id 字段，并确保其他新字段使用默认值。
    private val words = mutableListOf(
        Word(
            id = 1,
            word = "absence",
            phonetic = "/ˈæbsəns/",
            meaning = "n. 缺席；缺乏",
            sentences = listOf("His absence was noted by the teacher.", "There is a complete absence of evidence."),
            rootInfo = "abs- (away) + esse (be)",
            synonyms = listOf("lack", "nonattendance"),
            antonyms = listOf("presence", "attendance"),
            audioResId = R.raw.word_absence,
            phrases = listOf(
                "in the absence of" to "在缺乏...的情况下",
                "leave of absence" to "准假；休假"
            )
        ),
        Word(
            id = 2,
            word = "compose",
            phonetic = "/kəmˈpoʊz/",
            meaning = "v. 组成；创作；使平静",
            sentences = listOf("Water is composed of hydrogen and oxygen.", "She composed a symphony.", "Compose yourself before the meeting."),
            rootInfo = "com- (together) + ponere (put)",
            synonyms = listOf("create", "constitute", "calm"),
            antonyms = listOf("decompose", "agitate"),
            audioResId = R.raw.word_vocabulary,
            phrases = listOf(
                "be composed of" to "由...组成",
                "compose music/poetry" to "作曲/作诗"
            )
        ),
        Word(
            id = 3,
            word = "vocabulary",
            phonetic = "/voʊˈkæbjəleri/",
            meaning = "n. 词汇；词汇量",
            sentences = listOf("Reading helps to expand your vocabulary.", "This technical vocabulary is hard to master."),
            rootInfo = "vox (voice) -> vocare (call) -> vocabulum (word)",
            synonyms = listOf("lexicon", "word list"),
            audioResId = R.raw.word_vocabulary,
            phrases = listOf(
                "expand one's vocabulary" to "扩大词汇量",
                "limited vocabulary" to "有限的词汇量"
            )
        ),
        // --- 添加更多示例单词以满足批次需求 ---
        Word(id = 4, word = "ubiquitous", phonetic = "/juːˈbɪkwɪtəs/", meaning = "adj. 无所不在的", audioResId = R.raw.word_absence), // 假设有对应音频
        Word(id = 5, word = "ephemeral", phonetic = "/ɪˈfemərəl/", meaning = "adj. 短暂的", audioResId = R.raw.word_absence),
        Word(id = 6, word = "mellifluous", phonetic = "/məˈlɪfluəs/", meaning = "adj. 声音甜美的"), // 无音频
        Word(id = 7, word = "serendipity", phonetic = "/ˌserənˈdɪpəti/", meaning = "n. 意外发现珍宝的运气"),
        Word(id = 8, word = "perspicacious", phonetic = "/ˌpɜːrspɪˈkeɪʃəs/", meaning = "adj. 有洞察力的"),
        Word(id = 9, word = "gregarious", phonetic = "/ɡrɪˈɡeəriəs/", meaning = "adj. 合群的；爱社交的"),
        Word(id = 10, word = "magnanimous", phonetic = "/mæɡˈnænɪməs/", meaning = "adj. 宽宏大量的"),
        Word(id = 11, word = "obfuscate", phonetic = "/ˈɒbfəskeɪt/", meaning = "v. 使模糊；使困惑"),
        Word(id = 12, word = "plethora", phonetic = "/ˈpleθərə/", meaning = "n. 过多；过剩"),
        Word(id = 13, word = "quixotic", phonetic = "/kwɪkˈsɒtɪk/", meaning = "adj. 不切实际的；唐吉诃德式的"),
        Word(id = 14, word = "recalcitrant", phonetic = "/rɪˈkælsɪtrənt/", meaning = "adj. 顽抗的；不服从的"),
        Word(id = 15, word = "taciturn", phonetic = "/ˈtæsɪtɜːn/", meaning = "adj. 沉默寡言的"),
        Word(id = 16, word = "vicissitude", phonetic = "/vɪˈsɪsɪtjuːd/", meaning = "n. 变迁；兴衰"),
        Word(id = 17, word = "zeitgeist", phonetic = "/ˈzaɪtɡaɪst/", meaning = "n. 时代精神"),
        Word(id = 18, word = "acumen", phonetic = "/əˈkjuːmən/", meaning = "n. 敏锐；聪明"),
        Word(id = 19, word = "belligerent", phonetic = "/bəˈlɪdʒərənt/", meaning = "adj. 好战的"),
        Word(id = 20, word = "capitulate", phonetic = "/kəˈpɪtʃuleɪt/", meaning = "v. (有条件地)投降"),
        Word(id = 21, word = "deleterious", phonetic = "/ˌdeləˈtɪəriəs/", meaning = "adj. 有害的"),
        Word(id = 22, word = "enervate", phonetic = "/ˈenərveɪt/", meaning = "v. 使衰弱；使失去活力"),
        Word(id = 23, word = "facetious", phonetic = "/fəˈsiːʃəs/", meaning = "adj. 滑稽的；爱开玩笑的"),
        Word(id = 24, word = "homogeneous", phonetic = "/ˌhoʊməˈdʒiːniəs/", meaning = "adj. 同种类的；同性质的")
        // 至少添加了24个单词，以便测试批次功能
    )

    /** 返回所有单词的列表 */
    fun getWords(): List<Word> {
        return words.toList() // 返回副本以防止外部修改
    }

    /**
     * 更新指定单词的记忆状态（占位符实现）。
     * 在实际应用中，这里应该更新数据库中的记录。
     *
     * @param wordId 要更新的单词 ID。
     * @param isLearned 新的 isLearned 状态。
     * @param lastReviewDate 新的上次复习日期。
     * @param nextReviewDate 新的下次复习日期。
     * @param reviewIntervalDays 新的复习间隔。
     * @param consecutiveCorrect 新的连续答对次数。
     */
    fun updateWordState(
        wordId: Int,
        isLearned: Boolean,
        lastReviewDate: Long,
        nextReviewDate: Long,
        reviewIntervalDays: Int,
        consecutiveCorrect: Int
    ) {
        val wordIndex = words.indexOfFirst { it.id == wordId }
        if (wordIndex != -1) {
            val currentWord = words[wordIndex]
            words[wordIndex] = currentWord.copy(
                isLearned = isLearned,
                lastReviewDate = lastReviewDate,
                nextReviewDate = nextReviewDate,
                reviewIntervalDays = reviewIntervalDays,
                consecutiveCorrect = consecutiveCorrect
            )
            // 仅用于调试，实际应用中不需要或使用更完善的日志
            // println("Updated word $wordId: isLearned=$isLearned, nextReview=$nextReviewDate")
        } else {
            // 处理未找到单词的情况，例如记录日志
            // println("Word with id $wordId not found for update.")
        }
    }

     /**
     * 根据 ID 获取单个单词（如果需要）
     */
    fun getWordById(id: Int): Word? {
        return words.find { it.id == id }
    }
} 