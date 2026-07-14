package com.gptimage.playground.ui.screens.workbench

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gptimage.playground.PlaygroundApp
import com.gptimage.playground.data.model.PromptTemplate
import com.gptimage.playground.data.model.PromptTemplateCategory
import com.gptimage.playground.data.model.PromptTemplate as TemplateModel
import com.gptimage.playground.data.repository.PromptTemplateRepository
import com.gptimage.playground.ui.i18n.LocalStrings
import com.gptimage.playground.ui.i18n.Strings
import kotlinx.coroutines.launch

/**
 * 提示词模板库弹窗。移动端全屏，桌面端居中较大尺寸。
 *
 * @param currentPrompt 当前工作台的提示词，用于「新建模板」时预填
 * @param onApplyTemplate 用户点「使用模板」时回调，把模板 prompt 应用回工作台
 * @param onDismiss 关闭弹窗
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromptTemplatesDialog(
    currentPrompt: String,
    onApplyTemplate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val app = LocalContext.current.applicationContext as PlaygroundApp
    val viewModel: PromptTemplateViewModel = viewModel(
        factory = PromptTemplateViewModelFactory(app.locator)
    )
    val strings = LocalStrings.current
    val data by viewModel.data.collectAsState()
    val query by viewModel.query.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
    val selectedTemplateId by viewModel.selectedTemplateId.collectAsState()
    val panelMode by viewModel.panelMode.collectAsState()
    val editForm by viewModel.editForm.collectAsState()
    val status by viewModel.statusMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // status -> snackbar
    LaunchedEffect(status) {
        val msg = status?.let { it.toText(strings) }
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeStatus()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Bookmarks,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.size(8.dp))
                                Text(strings.templatesTitle)
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Filled.ArrowBack, contentDescription = strings.commonClose)
                            }
                        }
                    )
                },
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { padding ->
                when (panelMode) {
                    PanelMode.BROWSE -> BrowsePanel(
                        viewModel = viewModel,
                        data = data,
                        query = query,
                        selectedCategoryId = selectedCategoryId,
                        selectedTemplateId = selectedTemplateId,
                        strings = strings,
                        currentPrompt = currentPrompt,
                        onApplyTemplate = { prompt ->
                            onApplyTemplate(prompt)
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    )
                    PanelMode.EDIT -> editForm?.let { form ->
                        EditPanel(
                            form = form,
                            isEditing = form.templateId != null,
                            strings = strings,
                            onNameChange = { viewModel.updateEditForm(name = it) },
                            onCategoryChange = { viewModel.updateEditForm(categoryId = it) },
                            onPromptChange = { viewModel.updateEditForm(prompt = it) },
                            onDescriptionChange = { viewModel.updateEditForm(description = it) },
                            onClearRefill = { viewModel.resetEditPromptTo(currentPrompt) },
                            onBack = { viewModel.showBrowse() },
                            onSave = { viewModel.saveEdit(onDone = {}) },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding)
                        )
                    }
                    PanelMode.MANAGE -> ManagePanel(
                        viewModel = viewModel,
                        data = data,
                        strings = strings,
                        onBack = { viewModel.showBrowse() },
                        onEditTemplate = { viewModel.startEdit(it) },
                        onDeleteTemplate = { viewModel.deleteUser(it) },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    )
                }
            }
        }
    }
}

// ============ 浏览面板 ============
@Composable
private fun BrowsePanel(
    viewModel: PromptTemplateViewModel,
    data: TemplateData,
    query: String,
    selectedCategoryId: String,
    selectedTemplateId: String?,
    strings: Strings,
    currentPrompt: String,
    onApplyTemplate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // 搜索框 + 添加按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.setQuery(it) },
                placeholder = { Text(strings.templatesSearchPlaceholder) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.size(8.dp))
            FilledTonalButton(onClick = { viewModel.startCreate(currentPrompt) }) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(4.dp))
                Text(strings.templatesAdd)
            }
        }

        // 分类 chips（横向滚动）
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 全部模板 chip
            item {
                FilterChip(
                    selected = selectedCategoryId == PromptTemplateRepository.CATEGORY_ALL,
                    onClick = { viewModel.selectCategory(PromptTemplateRepository.CATEGORY_ALL) },
                    label = { Text(strings.templatesAll) }
                )
            }
            // 我的模板 chip
            item {
                FilterChip(
                    selected = selectedCategoryId == PromptTemplateRepository.CATEGORY_USER_DEFAULT,
                    onClick = { viewModel.selectCategory(PromptTemplateRepository.CATEGORY_USER_DEFAULT) },
                    label = { Text(strings.templatesMyTemplates) }
                )
            }
            // 各分类
            items(data.categories, key = { it.categoryId }) { category ->
                FilterChip(
                    selected = selectedCategoryId == category.categoryId,
                    onClick = { viewModel.selectCategory(category.categoryId) },
                    label = { Text(category.name) }
                )
            }
        }

        Spacer(Modifier.size(8.dp))

        // 过滤后的模板列表
        val filtered = rememberFilteredTemplates(
            templates = data.templates,
            query = query,
            selectedCategoryId = selectedCategoryId
        )

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (query.isNotBlank() || selectedCategoryId != PromptTemplateRepository.CATEGORY_ALL) {
                        strings.templatesEmptyFiltered
                    } else {
                        strings.templatesEmpty
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.templateId }) { template ->
                    TemplateCard(
                        template = template,
                        isSelected = selectedTemplateId == template.templateId,
                        strings = strings,
                        onClick = { viewModel.selectTemplate(template.templateId) },
                        onUse = { onApplyTemplate(template.prompt) },
                        onEdit = {
                            if (template.source == PromptTemplate.SOURCE_USER) {
                                viewModel.startEdit(template)
                            } else {
                                viewModel.copyAsUser(template)
                            }
                        },
                        onCopyAsLocal = { viewModel.copyAsUser(template) },
                        onDelete = { viewModel.deleteUser(template) }
                    )
                }
            }
        }

        // 底部"管理"按钮
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            OutlinedButton(onClick = { viewModel.showManage() }) {
                Icon(Icons.Filled.FolderCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(4.dp))
                Text(strings.templatesManage)
            }
        }
    }
}

@Composable
private fun TemplateCard(
    template: PromptTemplate,
    isSelected: Boolean,
    strings: Strings,
    onClick: () -> Unit,
    onUse: () -> Unit,
    onEdit: () -> Unit,
    onCopyAsLocal: () -> Unit,
    onDelete: () -> Unit
) {
    val isUser = template.source == PromptTemplate.SOURCE_USER
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.size(8.dp))
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            text = if (isUser) strings.templatesLocal else strings.templatesPreset,
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (isUser) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.tertiaryContainer
                        }
                    )
                )
            }
            template.description?.takeIf { it.isNotBlank() }?.let { desc ->
                Spacer(Modifier.size(4.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.size(8.dp))
            Text(
                text = template.prompt,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.size(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = onUse) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text(strings.templatesUse)
                }
                Spacer(Modifier.size(8.dp))
                if (isUser) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = strings.templatesEdit)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = strings.templatesDelete,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    IconButton(onClick = onCopyAsLocal) {
                        Icon(Icons.Filled.FolderCopy, contentDescription = strings.templatesCopyAsLocal)
                    }
                }
            }
        }
    }
}

// ============ 编辑/新建面板 ============
@Composable
private fun EditPanel(
    form: EditForm,
    isEditing: Boolean,
    strings: Strings,
    onNameChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onPromptChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onClearRefill: () -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(4.dp))
                Text(strings.templatesBackToBrowse)
            }
            Text(
                text = if (isEditing) strings.templatesEdit else strings.templatesAdd,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.size(48.dp))
        }

        OutlinedTextField(
            value = form.name,
            onValueChange = onNameChange,
            label = { Text(strings.templatesNameLabel) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = form.categoryId,
            onValueChange = onCategoryChange,
            label = { Text(strings.templatesCategoryLabel) },
            singleLine = true,
            supportingText = {
                Text(strings.templatesMyTemplates)
            },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = form.description,
            onValueChange = onDescriptionChange,
            label = { Text(strings.templatesDescriptionLabel) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = form.prompt,
            onValueChange = onPromptChange,
            label = { Text(strings.templatesPromptLabel) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 160.dp, max = 320.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(onClick = onClearRefill) {
                Text(strings.templatesClearRefill)
            }
            Button(onClick = onSave) {
                Text(strings.commonSave)
            }
        }
    }
}

// ============ 管理面板 ============
@Composable
private fun ManagePanel(
    viewModel: PromptTemplateViewModel,
    data: TemplateData,
    strings: Strings,
    onBack: () -> Unit,
    onEditTemplate: (PromptTemplate) -> Unit,
    onDeleteTemplate: (PromptTemplate) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userTemplates = remember(data.templates) {
        data.templates.filter { it.source == PromptTemplate.SOURCE_USER }
    }

    // 导出 launcher：先点导出按钮 -> 选文件 -> ViewModel 准备 JSON -> 写入
    // 由于 ActivityResultContracts.CreateDocument 先要弹文件选择，但我们需要先有 JSON 才能写入
    // 简化方案：在点击导出按钮时直接触发 ViewModel.exportToJson，把 JSON 拿到后再开 launcher 写入
    // 但 launcher 是异步的，无法"先准备数据再开 launcher"。这里换思路：用 ActivityResultContracts.CreateDocument
    // 让 launcher 接收一个事先准备的 input（即 JSON 字符串），然后在回调里写入
    val exportInput = remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val json = exportInput.value
        if (uri != null && json != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                }
            }
        }
        exportInput.value = null
    }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val raw = input.readBytes().toString(Charsets.UTF_8)
                    viewModel.importFromJson(raw)
                }
            }
        }
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(4.dp))
                Text(strings.templatesBackToBrowse)
            }
            Text(strings.templatesManage, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.size(48.dp))
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(strings.templatesImport, style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) }) {
                        Icon(Icons.Filled.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(4.dp))
                        Text(strings.templatesImport)
                    }
                    Button(
                        onClick = {
                            viewModel.exportToJson { json ->
                                exportInput.value = json
                                val filename = "gpt-image-prompt-templates-${java.time.LocalDate.now()}.json"
                                exportLauncher.launch(filename)
                            }
                        },
                        enabled = userTemplates.isNotEmpty()
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(4.dp))
                        Text(strings.templatesExport)
                    }
                }
                Text(
                    text = strings.templatesUserOnly,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            text = "${strings.templatesMyTemplates} (${userTemplates.size})",
            style = MaterialTheme.typography.titleSmall
        )

        if (userTemplates.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = strings.templatesEmpty,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            userTemplates.forEach { template ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = template.name,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            template.description?.takeIf { it.isNotBlank() }?.let { desc ->
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        IconButton(onClick = { onEditTemplate(template) }) {
                            Icon(Icons.Filled.Edit, contentDescription = strings.templatesEdit)
                        }
                        IconButton(onClick = { onDeleteTemplate(template) }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = strings.templatesDelete,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============ 工具 ============

@Composable
private fun rememberFilteredTemplates(
    templates: List<PromptTemplate>,
    query: String,
    selectedCategoryId: String
): List<PromptTemplate> {
    return androidx.compose.runtime.remember(templates, query, selectedCategoryId) {
        val q = query.trim().lowercase()
        templates.filter { t ->
            val categoryMatch = selectedCategoryId == PromptTemplateRepository.CATEGORY_ALL ||
                t.categoryId == selectedCategoryId
            if (!categoryMatch) return@filter false
            if (q.isBlank()) return@filter true
            val haystack = listOf(t.name, t.description.orEmpty(), t.prompt, t.categoryId)
                .joinToString(" ")
                .lowercase()
            haystack.contains(q)
        }
    }
}

/** 把 [TemplateStatus] 转 i18n 文案。 */
private fun TemplateStatus.toText(strings: Strings): String = when (this) {
    TemplateStatus.Required -> strings.templatesNamePromptRequired
    TemplateStatus.SavedNew -> strings.templatesSavedNew
    TemplateStatus.SavedUpdate -> strings.templatesSavedUpdate
    TemplateStatus.Deleted -> strings.templatesDeleted
    TemplateStatus.CopiedAsLocal -> strings.templatesCopiedAsLocal
    TemplateStatus.Exported -> strings.templatesExported
    is TemplateStatus.ImportedCount -> strings.templatesImportedCount(count)
    is TemplateStatus.ImportFailed -> strings.templatesImportFailed(reason)
}
