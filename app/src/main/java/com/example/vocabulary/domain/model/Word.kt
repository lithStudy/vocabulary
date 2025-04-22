package com.example.vocabulary.domain.model

import androidx.annotation.RawRes

/**
 * 表示一个单词及其相关信息的数据类。
 *
 * @param word 单词本身。
 * @param phonetic 音标。
 * @param meaning 中文释义。
 * @param sentences 例句列表。
 * @param rootInfo 词根词缀信息。
 * @param synonyms 近义词列表。
 * @param antonyms 反义词列表。
 * @param audioResId 关联的音频资源 ID (如果存在)。
 * @param isLearned 标记该单词是否至少完成过一次记忆流程。
 * @param lastReviewDate 上次复习的时间戳。
 * @param nextReviewDate 下次应复习的时间戳。
 * @param reviewIntervalDays 当前复习间隔（天）。
 * @param consecutiveCorrect 连续答对次数（可用于动态调整间隔）。
 */
data class Word(
    val id: Int, // 添加一个唯一ID，方便管理
    val word: String,
    val phonetic: String,
    val meaning: String,
    val sentences: List<String> = emptyList(),
    val rootInfo: String? = null,
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
    @RawRes val audioResId: Int? = null,
    var isLearned: Boolean = false,
    var lastReviewDate: Long = 0L,
    var nextReviewDate: Long = 0L,
    var reviewIntervalDays: Int = 1, // 初始间隔设为1天
    var consecutiveCorrect: Int = 0
) 