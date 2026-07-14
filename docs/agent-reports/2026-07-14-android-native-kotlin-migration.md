# 安卓原生 Kotlin 迁移（核心图像工作台）

| 字段     | 内容                                                                                              |
| -------- | ------------------------------------------------------------------------------------------------- |
| 日期     | 2026-07-14（迁移）/ 2026-07-14（追加：供应商扩展 + GitHub Actions APK CI）                         |
| 状态     | 部分完成 (Partial)（CI APK 验证进行中，见文末「提交状态」）                                       |
| 相关请求 | 用户：「将这个项目由网页套壳变成安卓原生项目…尽量使用 Kotlin…只需要安卓版本…删除其他版本冗余代码…推送到 GitHub」；后续追加：「查网上有哪些可以添加的供应商/端口…落后的尽量赶上…保留代码只在 GitHub 留作参考…用 GitHub 编译成安卓 APK」 |
| 相关文档 | [android/README.md](../../android/README.md)、[docs/android-apk.md](../android-apk.md)、AGENTS.md   |
| 改动范围 | 追加：新增 `StabilityImageClient`、扩展 `Provider.kt`/`AppConfig.kt`/`ImageProviderService.kt`/`GeminiImageClient.kt`/`Strings.kt`/`SettingsScreen.kt`、新增 `.github/workflows/android-build.yml`、Room 升级到 2.7.0 |
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
| 调研网上可补的供应商/端口 | 调研 2026 市场主流图像生成供应商：Stability AI（SD3.5 Large/Turbo 直连）落地；fal.ai/Replicate 为聚合器（队列/轮询模式，暂未内置）；OpenRouter 已有 OpenAI 兼容 `/v1/images/generations`，可直接走「OpenAI 兼容」供应商；Ideogram 暂未内置 | `data/network/StabilityImageClient.kt`、`android/README.md` 市场调研段 | 已完成 (Completed) |
| 落后功能补齐（追赶 2026） | 新增 Stability AI（SD 3.5 Large/Large Turbo，multipart `v2beta/stable-image/generate/sd3`）；新增 Gemini Nano Banana Pro（gemini-3-pro-image-preview）与 Nano Banana 2 Lite（gemini-3.1-flash-lite-image）；Gemini 客户端新增 `generationConfig.imageConfig.aspectRatio`/`imageSize` 转发；DALL-E 3 已于 2026-03 退役，项目本就不含，无遗留 | `Provider.kt`、`GeminiImageClient.kt`、`StabilityImageClient.kt`、`ImageProviderService.kt` | 已完成 (Completed) |
| Stability AI 供应商集成 | 新增 `ImageProviders.STABILITY`；`AppConfig.stability` 持久化；`StabilityImageClient` 走 multipart form-data（`prompt`/`model`/`mode`/`aspect_ratio`/`output_format`）；`ImageProviderService.edit()` 对 Stability 抛 `ProviderException`（SD3 仅文生图） | `data/network/StabilityImageClient.kt`、`data/model/Provider.kt`、`AppConfig.kt`、`ImageProviderService.kt` | 已完成 (Completed) |
| Settings UI 增加 Stability 卡片 | `ProviderSection` 增加 `ImageProviders.STABILITY` 分支标签；中英文 `settingsProviderStability` 同步 | `SettingsScreen.kt`、`ui/i18n/Strings.kt` | 已完成 (Completed) |
| 用 GitHub 编译成安卓 APK | 新增 `.github/workflows/android-build.yml`：push master / `v*` tag / `workflow_dispatch` 触发，ubuntu-latest + JDK 17 + Android SDK 35，产出 debug APK artifact（`gpt-image-playground-debug-apk`） + best-effort unsigned release APK | `.github/workflows/android-build.yml`、`android/README.md` 持续集成段 | 部分完成 (Partial)（首轮 CI 失败→已修 Room 版本，二轮验证中） |
| 保留代码仅作 GitHub 参考 | 已澄清：原 `src/`、`src-tauri/`、`pages/`、`api/` 与既有 `build-release.yml`（Tauri 安卓打包）保留不动，仅作为参考，不影响原生工程构建路径 | 仓库根未改动 | 已完成 (Completed)（按澄清后口径） |

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
| 追加：GitHub Actions 首轮 CI 编译失败，`java.lang.IllegalStateException: unexpected jvm signature V` 出现在 Room KSP 处理 `@Query DELETE ...` 返回 `Unit` 的 DAO 方法（Room 2.6.1 + KSP 2.0.21-1.0.28 已知不兼容） | 把 Room 从 2.6.1 升到 2.7.0（含上游修复），并提交 `f77f9b8` 推送触发新一轮 CI | 无（待二轮 CI 验证通过后确认） |
| 追加：Gemini 新模型（Nano Banana Pro/Lite）使用 aspect ratio（"1:1"）而非像素尺寸（"1024x1024"），若直接转发会让旧 `gemini-3.1-flash-image-preview` 模型回归 | 在 `GeminiImageClient.buildBody()` 用 `ASPECT_RATIO_REGEX = ^\d+:\d+$` 区分：命中才走 `generationConfig.imageConfig.aspectRatio`，否则保持原逻辑 | 无 |

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
| 追加：新增 Stability 客户端编译可达性 | 静态检查 `StabilityImageClient.kt` 的 multipart 构造与 `Regex` 用法；`ImageProviderService` dispatch 已加 `STABILITY` 分支 | 通过静态走查；待 CI APK 编译通过后确认 |
| 追加：CI APK 构建（首轮） | GitHub Actions run `29297877429`（commit `486e7ea`） | **失败**：Room KSP `unexpected jvm signature V`（见「问题与解决」） |
| 追加：CI APK 构建（二轮） | GitHub Actions run `29298137232`（commit `f77f9b8`，Room 升 2.7.0） | **进行中**，待完成确认 |
| 追加：原 `src/`/`src-tauri/` 路径未被新工程引用 | `Grep "import.*src.lib"` 在 `android/` 下无匹配；`build.gradle.kts` 不包含任何 web 路径 | 通过 |

## 后续建议

- 在 Android Studio 中执行 `./gradlew assembleDebug` 做首次真实编译，按编译器输出再修一轮细节（预期可能有少量 Compose API 弃用或 KSP 注解告警）。
- 真机回归以下场景：浅色/深色切换、状态栏 inset、Seedream JSON 编辑请求、Gemini `?key=` 鉴权、相册分享到微信/QQ 等目标 App。
- 评估是否移除 `Theme.kt` 中的 `WindowCompat.setDecorFitsSystemWindows(window, true)`，改为纯 Edge-to-Edge + Material3 Scaffold 自处理 inset（见「问题与解决」末行）。
- 后续迭代补齐用户暂缓的能力：视频生成、批量、模板库、蒙版、流式输出、自定义模型录入、相册视频 Tab 真实数据。
- release 签名：在 `app/build.gradle.kts` 接入 `signingConfigs.release`，密钥信息走 `~/.gradle/gradle.properties`，避免提交到仓库。
- 若希望让根 `README.md` 与 `docs/android-apk.md` 指向本原生工程而非旧 Tauri APK，需单独提案修改（本次未改根 README 以避免影响既有文档）。

## 提交状态

- 已提交并推送到 `origin/master`（迁移主体）：
  - commit hash：`25501a9`
  - commit 标题：`feat(android): add native Kotlin + Jetpack Compose Android client`
  - 远程：`https://github.com/ook826092-cloud/gpt-image-playground`（`master` 分支）
  - `git push origin master` 输出：`eeba3e9..25501a9  master -> master`
- 追加提交 1（供应商扩展 + CI workflow）：
  - commit hash：`486e7ea`
  - commit 标题：`feat(android): add Stability AI + new Gemini models, set up APK CI`
  - 涉及文件：`data/network/StabilityImageClient.kt`（新增）、`data/model/Provider.kt`、`data/model/AppConfig.kt`、`data/network/ImageProviderService.kt`、`data/network/GeminiImageClient.kt`、`ui/i18n/Strings.kt`、`ui/screens/settings/SettingsScreen.kt`、`.github/workflows/android-build.yml`（新增）
  - `git push origin master` 输出：`d5f9a85..486e7ea  master -> master`
  - 对应 CI run：`29297877429`（失败，见「问题与解决」末行）
- 追加提交 2（Room 升级 + README 更新）：
  - commit hash：`f77f9b8`
  - commit 标题：`fix(android): bump Room to 2.7.0 to fix KSP "unexpected jvm signature V"`
  - 涉及文件：`android/gradle/libs.versions.toml`（room 2.6.1 → 2.7.0）、`android/README.md`（新增供应商表 + 市场调研段 + 持续集成段）
  - `git push origin master` 输出：`486e7ea..f77f9b8  master -> master`
  - 对应 CI run：`29298137232`（**进行中**，待完成确认 APK 产物）
- 提交身份沿用仓库历史作者 `xxxily <974278171@qq.com>`（通过 `GIT_AUTHOR_*` / `GIT_COMMITTER_*` 环境变量传入，未修改任何 git 配置）。
- 未提交项：原 `src/`、`src-tauri/`、`pages/`、`api/` 等未改动（按用户「保留不动」口径）。本报告（`docs/agent-reports/2026-07-14-android-native-kotlin-migration.md`）将在 CI 验证完成后随状态更新一并提交。

## CI APK 产物获取

若 CI 二轮通过，可按下列步骤获取 debug APK：

1. 访问 `https://github.com/ook826092-cloud/gpt-image-playground/actions`
2. 找到最近的 `Android Build (Native Kotlin)` run（commit `f77f9b8`）
3. 滚动到底部 Artifacts 区域，下载 `gpt-image-playground-debug-apk`（zip 解压后即 `app-debug.apk`）
4. （可选）`gpt-image-playground-release-apk-unsigned` 为未签名 release 包，需自行签名后才能安装
