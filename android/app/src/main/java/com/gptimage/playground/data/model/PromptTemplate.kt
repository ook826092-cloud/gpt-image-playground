package com.gptimage.playground.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 提示词模板分类。预设分类 [source] = [SOURCE_DEFAULT]，用户自建分类 [source] = [SOURCE_USER]。
 * 用户自建分类的 [id] 用作 [PromptTemplate] 的 [PromptTemplate.categoryId]。
 */
@Entity(
    tableName = "prompt_template_categories",
    indices = [Index(value = ["categoryId"], unique = true)]
)
data class PromptTemplateCategory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    /** 业务 ID：预设分类为 "style-transfer" 等；用户分类用 UUID。 */
    val categoryId: String,
    val name: String,
    val description: String? = null,
    val source: String = SOURCE_DEFAULT,
    /** 排序值，越小越靠前；预设分类按内置顺序，置顶时改为负数。 */
    val sortOrder: Long = 0L
) {
    companion object {
        const val SOURCE_DEFAULT = "default"
        const val SOURCE_USER = "user"
    }
}

/**
 * 提示词模板。预设模板 [source] = [SOURCE_DEFAULT]，用户模板 [source] = [SOURCE_USER]。
 */
@Entity(
    tableName = "prompt_templates",
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["templateId"], unique = true)
    ]
)
data class PromptTemplate(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    /** 业务 ID：预设模板为 "style-transfer-watercolor" 等；用户模板用 UUID。 */
    val templateId: String,
    val name: String,
    val categoryId: String,
    val prompt: String,
    val description: String? = null,
    val source: String = SOURCE_DEFAULT,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val SOURCE_DEFAULT = "default"
        const val SOURCE_USER = "user"
    }
}
