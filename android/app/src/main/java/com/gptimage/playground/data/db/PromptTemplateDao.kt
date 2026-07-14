package com.gptimage.playground.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gptimage.playground.data.model.PromptTemplate
import com.gptimage.playground.data.model.PromptTemplateCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface PromptTemplateDao {

    // —— 模板 ——
    @Query("SELECT * FROM prompt_templates ORDER BY updatedAt DESC")
    fun observeAllTemplates(): Flow<List<PromptTemplate>>

    @Query("SELECT * FROM prompt_templates WHERE source = :source ORDER BY updatedAt DESC")
    suspend fun getBySource(source: String): List<PromptTemplate>

    @Query("SELECT * FROM prompt_templates ORDER BY updatedAt DESC")
    suspend fun getAllTemplates(): List<PromptTemplate>

    @Query("SELECT * FROM prompt_templates WHERE categoryId = :categoryId ORDER BY updatedAt DESC")
    fun observeTemplatesByCategory(categoryId: String): Flow<List<PromptTemplate>>

    @Query("SELECT * FROM prompt_templates WHERE templateId = :templateId LIMIT 1")
    suspend fun findByTemplateId(templateId: String): PromptTemplate?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: PromptTemplate): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(templates: List<PromptTemplate>)

    @Query("DELETE FROM prompt_templates WHERE templateId = :templateId")
    suspend fun deleteTemplate(templateId: String)

    @Query("DELETE FROM prompt_templates WHERE source = :source")
    suspend fun deleteBySource(source: String): Int

    @Query("SELECT COUNT(*) FROM prompt_templates")
    suspend fun countTemplates(): Int

    // —— 分类 ——
    @Query("SELECT * FROM prompt_template_categories ORDER BY sortOrder ASC, id ASC")
    fun observeCategories(): Flow<List<PromptTemplateCategory>>

    @Query("SELECT * FROM prompt_template_categories WHERE categoryId = :categoryId LIMIT 1")
    suspend fun findCategoryByBusinessId(categoryId: String): PromptTemplateCategory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: PromptTemplateCategory): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<PromptTemplateCategory>)

    @Query("DELETE FROM prompt_template_categories WHERE categoryId = :categoryId")
    suspend fun deleteCategory(categoryId: String)

    @Query("UPDATE prompt_template_categories SET sortOrder = :sortOrder WHERE categoryId = :categoryId")
    suspend fun updateCategorySortOrder(categoryId: String, sortOrder: Long)
}
