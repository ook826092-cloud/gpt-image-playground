package com.gptimage.playground.data.repository

import com.gptimage.playground.data.db.PromptTemplateDao
import com.gptimage.playground.data.model.PromptTemplate
import com.gptimage.playground.data.model.PromptTemplateCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.UUID

/**
 * 提示词模板仓库。负责：
 * - 首次启动时把内置预设（[DefaultPromptTemplates]）写入 Room（用 source 区分）
 * - 提供分类 + 模板的 Flow 观察接口
 * - 用户模板的 CRUD + 导入导出
 *
 * 设计与 Web 版 `prompt-template-storage.ts` 对齐：预设不可原地修改，
 * 用户模板通过 [createUserTemplate] / [updateUserTemplate] / [deleteUserTemplate] 管理。
 */
class PromptTemplateRepository(
    private val dao: PromptTemplateDao
) {

    /** 联合观察分类 + 模板，按 sortOrder 排序。 */
    fun observeAll(): Flow<Pair<List<PromptTemplateCategory>, List<PromptTemplate>>> =
        combine(dao.observeCategories(), dao.observeAllTemplates()) { categories, templates ->
            val sortedCats = categories.sortedWith(
                compareBy<PromptTemplateCategory>({ it.sortOrder }, { it.id })
            )
            val sortedTpls = templates.sortedWith(
                compareBy<PromptTemplate>({ it.categoryId }, { it.name })
            )
            sortedCats to sortedTpls
        }

    suspend fun ensureSeeded() {
        if (dao.countTemplates() > 0) return
        dao.insertCategories(DefaultPromptTemplates.categories)
        dao.insertTemplates(DefaultPromptTemplates.templates)
    }

    // —— 用户模板 CRUD ——
    suspend fun createUserTemplate(
        name: String,
        categoryId: String,
        prompt: String,
        description: String? = null
    ): PromptTemplate {
        val now = System.currentTimeMillis()
        val template = PromptTemplate(
            templateId = "user-${UUID.randomUUID()}",
            name = name.trim(),
            categoryId = categoryId,
            prompt = prompt,
            description = description?.trim()?.ifEmpty { null },
            source = PromptTemplate.SOURCE_USER,
            createdAt = now,
            updatedAt = now
        )
        dao.insertTemplate(template)
        return template
    }

    suspend fun updateUserTemplate(
        templateId: String,
        name: String,
        categoryId: String,
        prompt: String,
        description: String?
    ): Boolean {
        val existing = dao.findByTemplateId(templateId) ?: return false
        if (existing.source != PromptTemplate.SOURCE_USER) return false
        val updated = existing.copy(
            name = name.trim(),
            categoryId = categoryId,
            prompt = prompt,
            description = description?.trim()?.ifEmpty { null },
            updatedAt = System.currentTimeMillis()
        )
        dao.insertTemplate(updated)
        return true
    }

    suspend fun deleteUserTemplate(templateId: String): Boolean {
        val existing = dao.findByTemplateId(templateId) ?: return false
        if (existing.source != PromptTemplate.SOURCE_USER) return false
        dao.deleteTemplate(templateId)
        return true
    }

    /** 把预设模板复制为用户副本（用于"复制为本地模板再编辑"流程）。 */
    suspend fun copyAsUserTemplate(templateId: String): PromptTemplate? {
        val source = dao.findByTemplateId(templateId) ?: return null
        val now = System.currentTimeMillis()
        val copy = source.copy(
            templateId = "user-${UUID.randomUUID()}",
            name = "「${source.name}」的副本",
            source = PromptTemplate.SOURCE_USER,
            createdAt = now,
            updatedAt = now
        )
        dao.insertTemplate(copy)
        return copy
    }

    // —— 导入 / 导出 ——
    suspend fun importUserTemplates(templates: List<ImportEntry>): Int {
        var imported = 0
        val now = System.currentTimeMillis()
        for ((index, entry) in templates.withIndex()) {
            val name = entry.name.trim()
            val prompt = entry.prompt.trim()
            val categoryId = entry.categoryId.trim().ifEmpty { CATEGORY_USER_DEFAULT }
            if (name.isEmpty() || prompt.isEmpty()) continue

            // 确保分类存在
            if (dao.findCategoryByBusinessId(categoryId) == null) {
                dao.insertCategory(
                    PromptTemplateCategory(
                        categoryId = categoryId,
                        name = categoryId,
                        description = "导入的用户自定义分类",
                        source = PromptTemplateCategory.SOURCE_USER,
                        sortOrder = 1000L + index
                    )
                )
            }
            dao.insertTemplate(
                PromptTemplate(
                    templateId = "user-imported-${now}-${index}",
                    name = name,
                    categoryId = categoryId,
                    prompt = prompt,
                    description = entry.description?.trim()?.ifEmpty { null },
                    source = PromptTemplate.SOURCE_USER,
                    createdAt = now,
                    updatedAt = now
                )
            )
            imported++
        }
        return imported
    }

    /** 导出所有用户模板（不含预设）。 */
    suspend fun exportUserTemplates(): List<PromptTemplate> =
        dao.getBySource(PromptTemplate.SOURCE_USER)

    companion object {
        /** 默认用户分类 ID，对应 Web 版的 "我的模板"。 */
        const val CATEGORY_USER_DEFAULT = "custom"
        const val CATEGORY_ALL = "all"
    }
}

/** 导入条目（用于 [PromptTemplateRepository.importUserTemplates]）。 */
data class ImportEntry(
    val name: String,
    val categoryId: String,
    val prompt: String,
    val description: String? = null
)
