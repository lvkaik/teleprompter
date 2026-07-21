# realme UI 7.0（Android 16）专项适配指南

> 目标机型：realme GT7 Pro
> 系统：realme UI 7.0（基于 Android 16）
> 配套文档：docs/01-需求文档.md / docs/02-技术方案.md

---

## 1. realme UI 7.0 新变化

realme UI 7.0 基于 **Android 16**，相比 6.0 的关键变化：

| 类别 | realme UI 7.0 / Android 16 新变化 |
|------|-----------------------------------|
| **后台限制** | 引入"应用冻结"（App Freeze）— 长时间无前台运行会被深度冻结，**麦克风等传感器调用可能直接抛 SecurityException** |
| **前台服务** | 前台服务类型必须精确声明，且启动后 5s 内必须 promote；新增 `connectedDevice` / `mediaPlayback` 等新类型 |
| **悬浮窗** | `TYPE_APPLICATION_OVERLAY` 仍可用，但**叠加层的安全策略更严**：被判断为"遮挡关键内容"（如银行 App 输入框）会被强制隐藏 |
| **麦克风指示灯** | Android 12+ 隐私指示灯更严，**Android 16 对长时麦克风使用新增用户可见提示** |
| **通知** | 通知权限运行时申请 + 通知优先级更严格 |
| **权限弹窗** | Android 16 引入**权限分组提示**，用户可一键看到全部敏感权限使用情况 |
| **电源管理** | realme 自家"超级省电"和"夜间深度优化"对 Service 更激进 |

---

## 2. AndroidManifest 关键权限

```xml
<!-- 网络 -->
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>

<!-- 麦克风 -->
<uses-permission android:name="android.permission.RECORD_AUDIO"/>

<!-- 悬浮窗 -->
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW"/>
<uses-permission android:name="android.permission.DETECT_SCREEN_CAPTURE"/>

<!-- 前台服务 -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE"/>

<!-- 通知 -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>

<!-- 电池优化白名单 -->
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS"/>
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM"/>
<uses-permission android:name="android.permission.USE_EXACT_ALARM"/>

<!-- 唤醒 -->
<uses-permission android:name="android.permission.WAKE_LOCK"/>
```

Service 必须显式声明类型：

```xml
<service
    android:name=".service.FloatingWindowService"
    android:exported="false"
    android:foregroundServiceType="specialUse|microphone">
    <property
        android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
        android:value="悬浮提词器：实时显示稿件 + 语音识别驱动自动滚动"/>
</service>
```

---

## 3. 后台保活"三件套"

```
┌─────────────────────────────────────────────────────────┐
│  realme UI 7.0 后台保活                                  │
│                                                          │
│  ① 自启动白名单                                          │
│     设置 → 应用管理 → 提词器 → 自启动 → 允许              │
│     厂商跳转: ComponentName("com.coloros.safecenter",    │
│        "com.coloros.safecenter.permission.startup        │
│         .StartupAppAllListActivity")                     │
│                                                          │
│  ② 电池优化白名单                                        │
│     设置 → 电池 → 关闭电池优化 → 提词器                  │
│     Android 标准: ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS │
│                                                          │
│  ③ 后台高耗电 / 应用冻结                                  │
│     realme UI 7.0 新增"应用冻结"开关                       │
│     设置 → 电池 → 关闭"应用冻结"对提词器                   │
└─────────────────────────────────────────────────────────┘
```

引导跳转代码见 `app/src/main/java/.../ui/keepalive/KeepAliveGuideActivity.kt`。

---

## 4. 关键适配要点

### 4.1 FGS 5 秒规则

```kotlin
override fun onCreate() {
    super.onCreate()
    // 必须第一时间 promote 到前台
    startForeground(NOTI_ID, buildNotification())
}
```

### 4.2 悬浮窗 Flags

```kotlin
flags = FLAG_NOT_FOCUSABLE or
        FLAG_LAYOUT_NO_LIMITS or
        FLAG_NOT_TOUCH_MODAL   // 让下方 App 也能点击
```

### 4.3 120Hz 滚动

使用 `Choreographer` 而非 `delay(16)`，GT7 Pro 120Hz 屏可跑满 120fps。

### 4.4 麦克风权限申请

必须运行时申请 `RECORD_AUDIO` + `POST_NOTIFICATIONS`（Android 13+）。

---

## 5. 风险与对策

| 风险 | 概率 | 对策 |
|------|------|------|
| 应用被冻结后麦克风失效 | 高 | 引导关闭冻结 + 检测后弹窗提示 |
| 前台 Service 被 5s 超时杀掉 | 中 | `onCreate` 立即 `startForeground` |
| 悬浮窗被银行/输入法 App 强制隐藏 | 中 | 提示用户使用普通模式 |
| 通知被智能省电静默 | 中 | 使用 IMPORTANCE_LOW + 用户可关闭 |
| 第三方清洁 App 一键杀进程 | 高 | 引导用户加白名单 |

---

## 6. 测试用例

| # | 用例 | 通过标准 |
|---|------|---------|
| 1 | 关闭电池优化后挂机 1h | 提词器未被杀 |
| 2 | 应用冻结开关切换 | 引导页正确跳转 realme 设置 |
| 3 | 锁屏 + 黑屏 30min 后唤醒 | 通知点击可恢复 |
| 4 | 抖音 + 相机 + 微信 三方切换 | 悬浮窗稳定显示 |
| 5 | 横屏切换 | 悬浮窗位置/大小记忆 |
| 6 | 连续 4h AI 模式 | 无崩溃、ASR 持续工作、电量 < 30% |
| 7 | 关闭所有权限再开启 | 引导路径完整、无 ANR |
| 8 | 与 realme 手机管家并存 | 无冲突弹窗 |