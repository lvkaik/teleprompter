# 悬浮提词器（Floating Teleprompter）

一款 Android 端的悬浮提词器应用，让用户在直播、录视频、演讲、采访等场景下无需手持稿件，支持 AI 语音识别驱动自动滚动。

## 文档目录

| 文档 | 说明 |
|------|------|
| [01-需求文档.md](./01-需求文档.md) | 产品需求文档（PRD） |
| [02-技术方案.md](./02-技术方案.md) | 技术架构、模块设计、依赖 |
| [03-realme-UI7适配指南.md](./03-realme-UI7适配指南.md) | realme GT7 Pro / realme UI 7.0 专项适配 |

## 项目状态

- 文档阶段：✅ 完成
- 工程骨架：🚧 搭建中
- 核心功能：🚧 开发中
- AI 接入：⏳ 待开发

## 快速上手

```bash
# 1. 用 Android Studio 打开项目根目录
# 2. 等待 Gradle 同步完成
# 3. 连接 realme GT7 Pro 真机
# 4. Run 'app'
```

## 核心特性

- 悬浮窗可拖动、可缩放、可调透明度
- 匀速 + AI 智能滚动双模式
- 基于 MiniMax API 的实时 ASR 提词
- 针对 realme UI 7.0 后台管理深度适配

## 目标设备

- 主力机型：realme GT7 Pro（realme UI 7.0 / Android 16）
- 兼容范围：Android 9.0 – 16