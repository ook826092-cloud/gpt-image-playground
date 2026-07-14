# 安卓原生 Kotlin 迁移（核心图像工作台）

| 字段     | 内容                                                                                              |
| -------- | ------------------------------------------------------------------------------------------------- |
| 日期     | 2026-07-14                                                                                        |
| 状态     | 部分完成 (Partial)                                                                                |
| 相关请求 | 用户：「将这个项目由网页套壳变成安卓原生项目…尽量使用 Kotlin…只需要安卓版本…删除其他版本冗余代码…推送到 GitHub」 |
| 相关文档 | [android/README.md](../../android/README.md)、[docs/android-apk.md](../android-apk.md)、AGENTS.md   |
| 改动范围 | 新增 `android/` 原生工程（Gradle + Kotlin + Compose）；原 `src/`、`src-tauri/` 等按用户要求保留不动 |
| 提交状态 | 见文末「提交状态」                                                                                |

## 范围核对

| 请求目标 | 实际结果 | 证据 | 状态 |
| -------- | -------- | ---- | ---- |
| 由网页套壳改为安卓原生 Kotlin 项目 | 新建 `android/` 模块，纯 Kotlin + Jetpack Compose，不依赖 Next.js / Tauri / Rust | `android/app/src/main/java/com/gptimage/playground/**` | 已完成 (Completed) |
| 尽量为安卓做原生适配（移动端 UI） | Material 3 + 底部导航 + Edge-to-Edge + Splash + Photo Picker + FileProvider 分享 + Coil 缓存 | `MainActivity.kt`、`ui/navigation/AppRoot.kt`、`ui/screens/**` | 已完成 (Completed) |
| 现代化 UI 框架（Jetpack Compose） | 全部 UI 用 Compose，无 XML 布局（仅主题/字符串资源） | `ui/theme/**`、`ui/screens/**` | 已完成 (Completed) |
| 主要功能切换在底部导航栏 | `AppDestination` 三 Tab：工作台 / 相册 / 设置 | `ui/navigation/AppDestination.kt`、`AppRoot.kt` | 已完成 (Completed) |
| 专门的相册（图片 + 视频相册） | 相册页含「图片」「视频」双 Tab；图片 Tab 走 Room 历史库，视频 Tab 暂为占位空态 | `ui/screens/album/AlbumScreen.kt` | 部分完成 (Partial) |
| 多供应商配置 | 四个供应商凭据卡片（OpenAI / Google / SenseNova / Seedream），每个含 API Key（显隐）、Base URL、保存/清空 | `ui/screens/settings/SettingsScreen.kt`、`SettingsViewModel.kt` | 已完成 (Completed) |
| 客户端直连供应商 | OkHttp 直连，无内置服务端；Seedream JSON 编辑模式、Gemini `?key=` 查询参数均已实现 | `data/network/OpenAIImageClient.kt`、`GeminiImageClient.kt`、`ImageProviderService.kt` | 已完成 (Completed) |
| 文生图 + 图生图编辑 | 工作台支持文生图与基于参考图的编辑；参考图通过 Photo Picker 多选（最多 6 张） | `ui/screens/workbench/WorkbenchScreen.kt`、`WorkbenchViewModel.kt` | 已完成 (Completed) |
| 历史记录 | Room 持久化 `history_items`，图片存 `filesDir/generated/`，相册实时观察 | `data/db/**`、`data/repository/HistoryRepository.kt` | 已完成 (Completed) |
| 删除其他版本冗余代码 | 按用户后续澄清「保留不动」处理：原 `src/`、`src-tauri/`、`pages/`、`api/` 未删除，避免破坏既有可发布资产 | 仓库根未改动 | 不适用 (N/A)（按澄清后口径） |
| 推送到 GitHub | 见文末「提交状态」 | git log | 见文末 |
| 国际化（中英文） | 运行时 `LocalStrings` 切换；中英两套 `Strings`；SYSTEM 跟随设备 locale | `ui/i18n/Strings.kt`、`MainActivity.resolveStrings()` | 已完成 (Completed) |
| 浅色/深色主题 | Material 3 双色板 + `values-night/themes.xml`；设置可选 Light/Dark/System | `ui/theme/**`、`res/values-night/themes.xml` | 已完成 (Completed) |

## 问题与解决

| 问题 | 解决办法 | 剩余风险 |
| ---- | -------- | -------- |
| `OpenAIImageClient` 自定义 `JsonPrimitive.contentOrNull()` 是冗余实现（两分支都返回 `content`），且遮蔽标准库 | 改用标准库 `?.jsonPrimitive?.content`，删除自定义扩展 | 无 |
| `WorkbenchViewModel` 误 `import com.gptimage.playground.data.model.GenerationOutcome`，该类不存在（只在 `data.repository` 包里有同名 sealed interface） | 删除该 import，仅保留 `import ...repository.GenerationOutcome as RepoGenerationOutcome` | 无 |
| `WorkbenchScreen` 在 `rememberLauncherForActivityResult` 回调里调用 `LocalContext.current`（非 Composable 作用域，会编译失败） | 在回调外先 `val context = LocalContext.current`，回调内用 `context.contentResolver` | 无 |
| `WorkbenchScreen` 尺寸/背景/审核等标签硬编码英文（"Background"/"Moderation"/"Square"/"Landscape"/"Portrait"/"Default"），违反 AGENTS.md 第 2/8 条 | 在 `Strings` 增补 6 个字段并中英文同步；`SizeSection` 去掉 `remember(model)` 缓存以便语言切换即时刷新 | 无 |
| `MainActivity` 中 `AppLanguage.SYSTEM` 直接返回 `ChineseStrings`，英文设备选「跟随系统」会得到中文 | 新增 `resolveStrings()` 读取 `resources.configuration.locales`，按 `zh*`/`en*` 解析，其他默认中文 | 仅识别 zh/en，其他 locale 默认中文（可后续扩展） |
| `AppDatabase` 用 `fallbackToDestructiveMigration(dropAllTables = true)`，该布尔重载在 Room 2.6.1 不存在（2.7+ 才有） | 改回 `fallbackToDestructiveMigration()` 无参版本 | 无 |
| `libs.versions.toml` / `build.gradle.kts` / `proguard-rules.pro` 声明了 Retrofit 依赖与 keep 规则，但网络层实际只用 OkHttp | 删除全部 Retrofit 相关依赖与 proguard 规则，保留 OkHttp/Room/kotlinx.serialization keep 规则 | 无 |
| `setDecorFitsSystemWindows(window, true)` 与 `enableEdgeToEdge()` 语义冲突 | 维持现状未改（未在真机验证前不贸然切换），见「验证」 | 可能在某些设备出现状态栏区域双倍 padding，需真机回归 |
| 无 Android SDK 环境，无法执行 `./gradlew assembleDebug` | 做静态走查（导入、API 签名、作用域、proguard） | 无法保证零编译错误，需用户在 Android Studio 中首次构建 |

## 验证

| 检查项 | 命令或场景 | 结果 |
| ------ | ---------- | ---- |
| 项目结构与文件清单 | `LS /workspace/android` | 全部模块文件就位 |
| Retrofit 残留引用 | `Grep -i retrofit /workspace/android` | 无匹配（已彻底移除） |
| Gradle / AGP / Kotlin 版本兼容 | 静态核对 `libs.versions.toml` + `gradle-wrapper.properties` | AGP 8.7.2 + Gradle 8.10.2 + Kotlin 2.0.21 + KSP 2.0.21-1.0.28，相互兼容 |
| Room API 兼容 | 核对 `fallbackToDestructiveMigration` 签名 | 已切到 2.6.1 可用的无参版本 |
| Compose 作用域合规 | 检查 `LocalContext.current` 不出现在非 Composable 回调 | 已修复 WorkbenchScreen |
| i18n 全覆盖 | 比对 `Strings` 字段与 Workbench/Album/Settings 引用 | 新增 6 字段已在中英文同步，无硬编码可见字符串残留（语言自名「简体中文」「English」按惯例不翻译） |
| 主题双主题 | `values/themes.xml` + `values-night/themes.xml` | 浅/深色都定义 `Theme.GptImagePlayground` + PostSplash |
| FileProvider authority | Manifest `${applicationId}.fileprovider` 与 AlbumScreen `${context.packageName}.fileprovider` 一致；`file_paths.xml` 覆盖 `generated/` | 一致 |
| 编译验证 | `./gradlew assembleDebug` | **未执行**（沙箱无 Android SDK，JDK 25 + Gradle 8.14.4 在但不具备 Android 编译能力） |
| 真机/模拟器运行 | — | **未执行**（无设备与 SDK） |
| 浅色/深色 / 移动端布局 | — | **未执行**（同上） |

## 后续建议

- 在 Android Studio 中执行 `./gradlew assembleDebug` 做首次真实编译，按编译器输出再修一轮细节（预期可能有少量 Compose API 弃用或 KSP 注解告警）。
- 真机回归以下场景：浅色/深色切换、状态栏 inset、Seedream JSON 编辑请求、Gemini `?key=` 鉴权、相册分享到微信/QQ 等目标 App。
- 评估是否移除 `Theme.kt` 中的 `WindowCompat.setDecorFitsSystemWindows(window, true)`，改为纯 Edge-to-Edge + Material3 Scaffold 自处理 inset（见「问题与解决」末行）。
- 后续迭代补齐用户暂缓的能力：视频生成、批量、模板库、蒙版、流式输出、自定义模型录入、相册视频 Tab 真实数据。
- release 签名：在 `app/build.gradle.kts` 接入 `signingConfigs.release`，密钥信息走 `~/.gradle/gradle.properties`，避免提交到仓库。
- 若希望让根 `README.md` 与 `docs/android-apk.md` 指向本原生工程而非旧 Tauri APK，需单独提案修改（本次未改根 README 以避免影响既有文档）。

## 提交状态

- 待执行 `git add android/ docs/agent-reports/2026-07-14-android-native-kotlin-migration.md` → `git commit` → `git push origin master`。
- 具体 commit hash 见 `git log` 输出；本报告将在推送后补录 hash（如未补录，以 `git log -- android/` 最新一条为准）。
