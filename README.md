# Wear OS 词汇学习应用 (Vocabulary App)

一个简单的 Wear OS 应用程序，旨在帮助用户在手表上学习和复习词汇。

## 功能

*   **单词卡片展示**: 以卡片形式清晰地显示单词、音标和中文释义。
*   **手势切换**: 支持通过左右滑动屏幕来切换上一个或下一个单词，提供流畅的浏览体验。
*   **环境模式 (Ambient Mode) 兼容**: 优化了在手表进入低功耗环境模式时的显示，仅保留核心信息（单词本身），节省电量。
*   **基础架构**: 使用现代 Android 开发技术构建，包括 Kotlin 和 Jetpack Compose for Wear OS。

## 截图/演示

*(在此处可以添加应用的截图或 GIF 动图来展示界面和交互)*

## 设置与运行

1.  **克隆仓库**:
    ```bash
    git clone <your-repository-url>
    cd vocabulary
    ```
2.  **打开项目**: 使用 Android Studio (最新稳定版或 Canary 版，确保支持 Compose for Wear OS) 打开项目根目录。
3.  **同步 Gradle**: 等待 Android Studio 自动同步 Gradle 依赖。如果遇到依赖问题（如此前解决的 `wear-ambient` 问题），请确保所有依赖库版本兼容且仓库配置正确。
4.  **运行应用**:
    *   连接一个 Wear OS 设备（物理设备或模拟器）。
    *   在 Android Studio 中选择 `app` 模块和目标设备。
    *   点击运行按钮 (▶️)。

## 技术栈

*   **语言**: [Kotlin](https://kotlinlang.org/)
*   **UI**: [Jetpack Compose for Wear OS](https://developer.android.com/jetpack/compose/wear)
*   **架构**: 基于 MVVM 思想 (虽然简单应用中未完全体现)
*   **构建系统**: [Gradle](https://gradle.org/)
*   **核心库**:
    *   `androidx.wear.compose.material`: Wear OS 的 Material Design 组件。
    *   `androidx.wear.compose.foundation`: Wear OS 的基础布局和手势。
    *   `androidx.activity.compose`: Activity 与 Compose 的集成。
    *   `androidx.wear.ambient`: (旧版，已移除) 处理环境模式。
    *   `androidx.wear`: 提供 Wear OS 特定功能，如 `AmbientModeSupport`。

## 项目结构 (简要)

```
.
├── app/                      # 主应用模块
│   ├── build.gradle.kts      # 应用模块 Gradle 构建脚本
│   ├── src/main/
│   │   ├── java/com/example/vocabulary/
│   │   │   ├── data/         # 数据层 (例如 WordRepository)
│   │   │   ├── domain/       # 领域层 (例如 Word 模型)
│   │   │   └── presentation/ # 表示层 (Activity, Composables, Theme)
│   │   └── res/              # 资源文件 (图标、布局等)
│   └── ...
├── gradle/                   # Gradle Wrapper 和版本目录
│   └── libs.versions.toml    # 依赖库版本管理
├── build.gradle.kts          # 项目级 Gradle 构建脚本
├── settings.gradle.kts       # 项目设置和模块包含
└── README.md                 # 本文件
```

## 贡献

欢迎提出问题 (Issues) 或提交合并请求 (Pull Requests)。

## 许可证

*(可以根据需要添加许可证信息，例如 MIT License)* 