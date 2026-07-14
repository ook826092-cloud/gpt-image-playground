package com.gptimage.playground.ui.screens.workbench

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gptimage.playground.ServiceLocator
import com.gptimage.playground.data.model.PromptTemplate
import com.gptimage.playground.data.model.PromptTemplateCategory
import com.gptimage.playground.data.repository.ImportEntry
import com.gptimage.playground.data.repository.PromptTemplateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * 提示词模板库 ViewModel。
 * 持有当前列表 / 选中分类 / 搜索词 / 编辑表单状态，由 [PromptTemplatesDialog] 观察。
 */
class PromptTemplateViewModel(
    application: Application,
    private val repo: PromptTemplateRepository
) : AndroidViewModel(application) {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow(PromptTemplateRepository.CATEGORY_ALL)
    val selectedCategoryId: StateFlow<String> = _selectedCategoryId.asStateFlow()

    private val _selectedTemplateId = MutableStateFlow<String?>(null)
    val selectedTemplateId: StateFlow<String?> = _selectedTemplateId.asStateFlow()

    /** 浏览（browse） / 编辑（edit） / 管理（manage） */
    private val _panelMode = MutableStateFlow(PanelMode.BROWSE)
    val panelMode: StateFlow<PanelMode> = _panelMode.asStateFlow()

    /** 编辑表单。null 表示当前不在编辑或新建。 */
    private val _editForm = MutableStateFlow<EditForm?>(null)
    val editForm: StateFlow<EditForm?> = _editForm.asStateFlow()

    /** 一次性状态提示，由 UI 消费后清空。 */
    private val _statusMessage = MutableStateFlow<TemplateStatus?>(null)
    val statusMessage: StateFlow<TemplateStatus?> = _statusMessage.asStateFlow()

    val data: StateFlow<TemplateData> = repo.observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = TemplateData()
        )

    fun setQuery(value: String) {
        _query.value = value
    }

    fun selectCategory(categoryId: String) {
        _selectedCategoryId.value = categoryId
    }

    fun selectTemplate(templateId: String) {
        _selectedTemplateId.value = templateId
    }

    // —— 面板切换 ——
    fun showBrowse() {
        _panelMode.value = PanelMode.BROWSE
        _editForm.value = null
    }

    fun showManage() {
        _panelMode.value = PanelMode.MANAGE
        _editForm.value = null
    }

    /** 进入"新建模板"模式，prompt 默认填入当前工作台 prompt。 */
    fun startCreate(currentPrompt: String) {
        _panelMode.value = PanelMode.EDIT
        _editForm.value = EditForm(
            templateId = null,
            name = "",
            categoryId = PromptTemplateRepository.CATEGORY_USER_DEFAULT,
            prompt = currentPrompt,
            description = ""
        )
    }

    /** 进入"编辑现有模板"模式。仅本地模板可编辑。 */
    fun startEdit(template: PromptTemplate) {
        if (template.source != PromptTemplate.SOURCE_USER) return
        _panelMode.value = PanelMode.EDIT
        _editForm.value = EditForm(
            templateId = template.templateId,
            name = template.name,
            categoryId = template.categoryId,
            prompt = template.prompt,
            description = template.description.orEmpty()
        )
    }

    fun updateEditForm(
        name: String? = null,
        categoryId: String? = null,
        prompt: String? = null,
        description: String? = null
    ) {
        _editForm.update { current ->
            current?.copy(
                name = name ?: current.name,
                categoryId = categoryId ?: current.categoryId,
                prompt = prompt ?: current.prompt,
                description = description ?: current.description
            )
        }
    }

    fun resetEditPromptTo(currentPrompt: String) {
        _editForm.update { it?.copy(prompt = currentPrompt) }
    }

    fun saveEdit(onDone: () -> Unit) {
        val form = _editForm.value ?: return
        if (form.name.isBlank() || form.prompt.isBlank()) {
            _statusMessage.value = TemplateStatus.Required
            return
        }
        viewModelScope.launch {
            if (form.templateId == null) {
                repo.createUserTemplate(
                    name = form.name,
                    categoryId = form.categoryId.ifBlank { PromptTemplateRepository.CATEGORY_USER_DEFAULT },
                    prompt = form.prompt,
                    description = form.description
                )
                _statusMessage.value = TemplateStatus.SavedNew
            } else {
                repo.updateUserTemplate(
                    templateId = form.templateId,
                    name = form.name,
                    categoryId = form.categoryId.ifBlank { PromptTemplateRepository.CATEGORY_USER_DEFAULT },
                    prompt = form.prompt,
                    description = form.description
                )
                _statusMessage.value = TemplateStatus.SavedUpdate
            }
            _selectedCategoryId.value = form.categoryId.ifBlank { PromptTemplateRepository.CATEGORY_USER_DEFAULT }
            _panelMode.value = PanelMode.BROWSE
            _editForm.value = null
            onDone()
        }
    }

    fun copyAsUser(template: PromptTemplate) {
        viewModelScope.launch {
            val copy = repo.copyAsUserTemplate(template.templateId) ?: return@launch
            _statusMessage.value = TemplateStatus.CopiedAsLocal
            _selectedCategoryId.value = copy.categoryId
            _selectedTemplateId.value = copy.templateId
            _panelMode.value = PanelMode.BROWSE
        }
    }

    fun deleteUser(template: PromptTemplate) {
        if (template.source != PromptTemplate.SOURCE_USER) return
        viewModelScope.launch {
            repo.deleteUserTemplate(template.templateId)
            _statusMessage.value = TemplateStatus.Deleted
            if (_selectedTemplateId.value == template.templateId) {
                _selectedTemplateId.value = null
            }
        }
    }

    fun consumeStatus() {
        _statusMessage.value = null
    }

    // —— 导入 / 导出 ——
    fun importFromJson(rawJson: String) {
        viewModelScope.launch {
            val result = runCatching { parseImportJson(rawJson) }
            val entries = result.getOrElse {
                _statusMessage.value = TemplateStatus.ImportFailed(it.message ?: "JSON invalid")
                return@launch
            }
            if (entries.isEmpty()) {
                _statusMessage.value = TemplateStatus.ImportFailed("empty")
                return@launch
            }
            val count = repo.importUserTemplates(entries)
            _statusMessage.value = TemplateStatus.ImportedCount(count)
        }
    }

    fun exportToJson(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val list = repo.exportUserTemplates()
            val json = buildExportJson(list)
            _statusMessage.value = TemplateStatus.Exported
            onResult(json)
        }
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

        internal fun parseImportJson(raw: String): List<ImportEntry> {
            val element = json.parseToJsonElement(raw)
            val arr: JsonArray = when (element) {
                is JsonArray -> element
                is JsonObject -> {
                    val inner = element["templates"]
                    if (inner is JsonArray) inner else error("missing 'templates' array")
                }
                else -> error("invalid root")
            }
            return arr.mapNotNull { item ->
                val obj = item as? JsonObject ?: return@mapNotNull null
                val name = obj["name"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
                val prompt = obj["prompt"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
                if (name.isEmpty() || prompt.isEmpty()) return@mapNotNull null
                val categoryId = obj["categoryId"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
                val description = obj["description"]?.jsonPrimitive?.contentOrNull?.trim()?.ifEmpty { null }
                ImportEntry(name, categoryId, prompt, description)
            }
        }

        internal fun buildExportJson(list: List<PromptTemplate>): String {
            val arr = JsonArray(list.map { tpl ->
                JsonObject(mapOf(
                    "id" to JsonPrimitive(tpl.templateId),
                    "name" to JsonPrimitive(tpl.name),
                    "categoryId" to JsonPrimitive(tpl.categoryId),
                    "prompt" to JsonPrimitive(tpl.prompt),
                    "description" to (tpl.description?.let { JsonPrimitive(it) } ?: JsonPrimitive(""))
                ))
            })
            val root = JsonObject(mapOf(
                "version" to JsonPrimitive(1),
                "exportedAt" to JsonPrimitive(java.time.Instant.now().toString()),
                "templates" to arr
            ))
            return json.encodeToString(JsonObject.serializer(), root)
        }
    }
}

enum class PanelMode { BROWSE, EDIT, MANAGE }

/** 状态提示枚举。UI 端在 Composable 里转 i18n 文案。 */
sealed interface TemplateStatus {
    data object Required : TemplateStatus
    data object SavedNew : TemplateStatus
    data object SavedUpdate : TemplateStatus
    data object Deleted : TemplateStatus
    data object CopiedAsLocal : TemplateStatus
    data object Exported : TemplateStatus
    data class ImportedCount(val count: Int) : TemplateStatus
    data class ImportFailed(val reason: String) : TemplateStatus
}

data class EditForm(
    val templateId: String?,
    val name: String,
    val categoryId: String,
    val prompt: String,
    val description: String
)

data class TemplateData(
    val categories: List<PromptTemplateCategory> = emptyList(),
    val templates: List<PromptTemplate> = emptyList()
)

class PromptTemplateViewModelFactory(
    private val locator: ServiceLocator
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PromptTemplateViewModel(
            application = locator.application,
            repo = locator.promptTemplateRepository
        ) as T
    }
}
