# 零個手电筒 (Zero Torch)

一款功能丰富的 Android 手电筒应用，支持摩斯信号、爆闪模式、定时任务、动态主题和桌面小组件。

![Platform](https://img.shields.io/badge/platform-Android-green.svg)
![API](https://img.shields.io/badge/API-26%2B-brightgreen.svg)
![License](https://img.shields.io/badge/license-MIT-blue.svg)

## ✨ 功能特性

- **🔦 手电筒开关** - 快速开启/关闭手电筒
- **📡 摩斯信号** - 内置多种预设（SOS、HI、HELLO 等），支持自定义
- **⚡ 爆闪模式** - 可调节闪烁频率，支持多种预设档位
- **⏰ 定时任务** - 支持定时开启/关闭，可同时运行两个任务
- **🎨 动态主题** - Android 12+ 支持从壁纸提取主题色
- **🖼️ 桌面小组件** - 1×1 小组件，支持自定义外观
- **🎛️ 自定义设置** - 主题色、圆角、图标、文字等

## 📱 界面预览

| 主界面 | 摩斯信号 | 设置 |
|--------|----------|------|
| 大开关按钮 | 预设列表 | 主题切换 |
| 爆闪滑条 | 自定义输入 | 小组件设置 |
| 定时按钮 | 编码预览 | 爆闪预设 |

## 🚀 快速开始

### 环境要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Android SDK 35
- Gradle 8.11.1

### 构建步骤

1. 克隆仓库
```bash
git clone https://github.com/yourusername/zero-torch.git
cd zero-torch
```

2. 使用 Android Studio 打开项目

3. 构建并运行
```bash
./gradlew assembleDebug
```

### 发布版本构建

1. 在项目根目录创建 `local.properties` 并配置签名信息：
```properties
sdk.dir=/path/to/android/sdk
keystore.path=../keystore/release-signing.p12
keystore.password=your_password
keystore.alias=your_alias
keystore.key.password=your_key_password
```

2. 将证书文件放入 `keystore/` 目录

3. 构建签名版 APK：
```bash
./gradlew assembleRelease
```

## 📁 项目结构

```
zero-torch/
├── app/                    # 应用模块
│   ├── src/main/
│   │   ├── java/          # Kotlin 源码
│   │   │   └── com/flashlight/toolbox/
│   │   │       ├── MainActivity.kt
│   │   │       ├── flashlight/      # 手电筒控制器
│   │   │       ├── ui/screens/      # 界面
│   │   │       ├── ui/theme/        # 主题配置
│   │   │       ├── data/            # 数据存储
│   │   │       └── widget/          # 小组件
│   │   └── res/           # 资源文件
│   └── build.gradle.kts   # 模块构建配置
├── gradle/                # Gradle 包装器
├── build.gradle.kts       # 项目构建配置
└── settings.gradle.kts    # 项目设置
```

## 🛠️ 技术栈

- **Kotlin** - 主要开发语言
- **Jetpack Compose** - 声明式 UI 框架
- **Material 3** - 现代设计系统
- **DataStore** - 偏好设置存储
- **Camera2 API** - 手电筒控制
- **AppWidgetProvider** - 桌面小组件

## 📄 许可证

本项目基于 MIT 许可证开源，详见 [LICENSE](LICENSE) 文件。

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📮 联系方式

- 项目地址：https://github.com/yourusername/zero-torch
- 问题反馈：https://github.com/yourusername/zero-torch/issues

---

⭐ 如果这个项目对你有帮助，请给个 Star！
