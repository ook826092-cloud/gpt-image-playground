# GPT Image Playground — Android 原生版

基于 Jetpack Compose 的安卓原生客户端，与原有 Next.js / Tauri 网页版同源，但完全使用 Kotlin 重写并面向移动端做原生适配。客户端**直连**各图像生成供应商，不内置任何服务端。

> 仓库根目录仍保留原 Web/Tauri/Rust 代码（`src/`、`src-tauri/`、`pages/`、`api/` 等），它们与本目录的安卓工程互不依赖。安卓工程的全部源码与配置都在 `android/` 目录内。

## 功能范围

本目录实现的是「核心图像工作台」，覆盖：

- **工作台（Workbench）**：文生图、图生图编辑、参考图（Photo Picker 多选）、模型/尺寸/质量/格式/背景/审核/数量等高级参数、结果预览、错误提示。
- **相册（Album）**：图片与视频两个 Tab，`LazyVerticalGrid` 网格预览，长按/点击出 `ModalBottomSheet` 操作面板（分享、删除），通过 `FileProvider` 分享到其他 App。
- **设置（Settings）**：四个供应商凭据卡片（OpenAI 兼容 / Google Gemini / SenseNova / Seedream 豆包），每个含 API Key（可显隐）、Base URL、保存/清空；默认模型选择；主题（浅色/深色/跟随系统）；语言（跟随系统 / 简体中文 / English）；关于卡片。

以下能力在本次迁移中**未实现**（可后续迭代）：视频生成、批量处理、管理后台、云同步、加密短链分享、提示词模板库、蒙版编辑、流式输出、自定义模型录入。相册的「视频」Tab 目前只展示占位空态。

## 技术栈

| 类别 | 选型 |
| --- | --- |
| UI | Jetpack Compose + Material 3（`compose-bom 2024.10.01`） |
| 导航 | Navigation Compose，底部三 Tab |
| 语言 | Kotlin 2.0.21，JVM 17 |
| 构建 | AGP 8.7.2，Gradle 8.10.2 |
| 持久化 | Room 2.6.1（历史记录）+ DataStore Preferences（AppConfig，JSON 单 key） |
| 序列化 | kotlinx.serialization 1.7.3 |
| 网络 | OkHttp 4.12.0（直连供应商，无 Retrofit） |
| 图片加载 | Coil 2.7.0（含磁盘/内存缓存） |
| 异步 | kotlinx-coroutines 1.9.0 |
| 依赖注入 | 手写 `ServiceLocator`（不引入 Hilt/KSP-DI） |
| 启动 | AndroidX Core SplashScreen + Edge-to-Edge |
| 分享 | `androidx.core.content.FileProvider` |

## 支持的供应商与模型

| 供应商 | 端点 | 模型 |
| --- | --- | --- |
| OpenAI 兼容 | `/v1/images/generations`、`/v1/images/edits` | gpt-image-2 / 1.5 / 1 / 1-mini |
| Google Gemini | `/v1beta/models/<id>:generateContent?key=...` | gemini-3.1-flash-image-preview (Nano Banana 2) |
| SenseNova | OpenAI 兼容 | sensenova-u1-fast |
| Seedream 豆包 | OpenAI 兼容 + JSON 编辑模式（`image` 字段为 data URI 数组） | doubao-seedream 5.0 / 5.0-lite / 4.5 / 4.0 / 3.0-t2i |

供应商路由在 [ImageProviderService.kt](app/src/main/java/com/gptimage/playground/data/network/ImageProviderService.kt) 中按 `model.provider` 分发到 `OpenAIImageClient` 或 `GeminiImageClient`。

## 目录结构

```
android/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/gptimage/playground/
│       │   ├── PlaygroundApp.kt              # Application + Coil ImageLoaderFactory
│       │   ├── MainActivity.kt               # Splash + Edge-to-Edge + i18n provider
│       │   ├── ServiceLocator.kt             # 手动 DI 容器
│       │   ├── data/
│       │   │   ├── model/                     # AppConfig / Provider / Generation / HistoryItem
│       │   │   ├── db/                        # Room AppDatabase + HistoryDao
│       │   │   ├── datastore/SettingsStore.kt # AppConfig → DataStore (JSON)
│       │   │   ├── network/                   # OpenAI / Gemini 客户端 + Service + HttpFactory
│       │   │   └── repository/                # Settings / History / ImageGeneration
│       │   └── ui/
│       │       ├── AppRootViewModel.kt
│       │       ├── i18n/Strings.kt           # ChineseStrings / EnglishStrings + LocalStrings
│       │       ├── navigation/                # AppDestination + AppRoot (Scaffold + NavHost)
│       │       ├── theme/                     # Color / Type / Theme (Material 3)
│       │       └── screens/
│       │           ├── workbench/             # 工作台
│       │           ├── album/                 # 相册
│       │           └── settings/              # 设置
│       └── res/
│           ├── values/                        # strings.xml / colors.xml / themes.xml
│           ├── values-en/strings.xml
│           ├── values-night/themes.xml
│           ├── xml/                           # file_paths / backup_rules / data_extraction_rules
│           └── mipmap-*/                      # 应用图标（复用 src-tauri 资源）
├── gradle/
│   ├── libs.versions.toml                     # 版本目录
│   └── wrapper/gradle-wrapper.properties
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## 构建与运行

### 前置条件

- Android Studio Ladybug（或更高）/ 命令行 Gradle 8.10.2+
- Android SDK Platform 35（compileSdk = 35，minSdk = 24，targetSdk = 35）
- JDK 17

### 命令

```bash
# 在 android/ 目录下
./gradlew assembleDebug          # 产出 debug APK
./gradlew installDebug           # 安装到已连接设备
./gradlew assembleRelease        # 产出 release APK（需要签名配置）
```

Debug 包 applicationId 为 `com.gptimage.playground.debug`（带 `.debug` 后缀），release 为 `com.gptimage.playground`。

> 当前 `release` 构建类型未配置签名密钥，需在本机 `~/.gradle/gradle.properties` 中补充 `GPTIMAGE_PLAYGROUND_STORE_FILE` 等签名信息并在 `app/build.gradle.kts` 中接入 `signingConfigs` 后才能产出可安装的 release 包。

## i18n

- 运行时切换语言由 `LocalStrings` CompositionLocal 实现，无需重启 Activity；中文/英文两套 `Strings` 在 [Strings.kt](app/src/main/java/com/gptimage/playground/ui/i18n/Strings.kt) 中维护。
- 「跟随系统」会在 `MainActivity.resolveStrings()` 中读取设备 locale，`zh*` → 简体中文，`en*` → English，其他默认中文。
- `res/values/strings.xml` 与 `res/values-en/strings.xml` 仅保留系统层（app 标签等）所需字符串，与 Compose 层 `Strings` 保持同步。

## 与原 Web/Tauri 版的关系

- 原 `src/lib/model-registry.ts`、`src/lib/provider-config.ts` 的供应商清单与默认 Base URL 已在 `data/model/Provider.kt` 中以 Kotlin 对齐。
- 原 `src/lib/providers/*` 的请求构造逻辑迁移到 `data/network/OpenAIImageClient.kt` 与 `GeminiImageClient.kt`。
- 原「保留不动」：`src/`、`src-tauri/`、`pages/`、`api/` 等不在安卓构建路径内，二者互不影响。
