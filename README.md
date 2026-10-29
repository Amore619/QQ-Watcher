[README.md](https://github.com/user-attachments/files/34139659/README.md)
# QQ关键词监控（QQWatcher）

一个 692 行的安卓小工具：监听 QQ 通知，命中关键词或指定成员发言时，用**横幅 + 声音 + 震动**提醒你，并自动存进记录本。零成本、无需服务器、无需破解 QQ。

> 为"怕错过群里的志愿/二课通知"这个具体痛点而写。

## 效果

QQ 群消息 `志愿活动下午3点集合` → 手机横幅弹出：

```
二班班群  志愿
张三: 今天下午3点图书馆集合
```

点通知直达 QQ；同时自动存入 App 内的记录本。

## 功能

| 功能 | 说明 |
|---|---|
| 关键词监控 | 任何 QQ 消息含指定词（默认"志愿、二课"）即提醒，支持自定义多个 |
| 指定成员监控 | 指定群 + 指定成员（如管理员）发言即提醒 |
| 记录本 | 命中消息自动存档，最多 500 条，可翻看/清空 |
| 开机自启 | 重启手机后自动恢复监听 |
| 前台服务常驻 | 绕过国产 ROM 对通知监听类 App 的通知静默限制 |

## 工作原理

QQ 没有开放消息 API，所以走安卓系统级 **NotificationListenerService**：

```
QQ 收到消息 → 系统通知栏出现"群名 / 发送者: 内容"
            → 我们的服务截获通知的标题与正文
            → 按规则过滤
            → 广播给前台服务 AlertRelayService 弹横幅
            → 同时写入记录本
```

**这不是破解 QQ**，而是读取系统通知栏——和智能手表同步通知是同一个公开接口。

## 构建

无需 Android Studio，命令行即可：

```bash
# 1. 安装 Android SDK 命令行工具后
sdkmanager "platforms;android-34" "build-tools;34.0.0"

# 2. 指定 SDK 路径
echo "sdk.dir=/path/to/android-sdk" > local.properties

# 3. 编译
./gradlew assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

国内网络如果 Gradle 下载失败，把 `gradle/wrapper/gradle-wrapper.properties` 里的地址换成镜像：

```
https://mirrors.cloud.tencent.com/gradle/gradle-8.7-bin.zip
```

## 使用步骤

1. 安装 APK（自编译或 Release 页下载）
2. 打开 App → **授予通知读取权限**（跳转系统设置勾选本应用）
3. 填写规则：关键词、重点群名（须与 QQ 显示一致）、指定成员 → 保存
4. **保持 QQ 群通知开启**（可设免打扰，但不能关闭通知栏显示）
5. 授权后建议重启一次手机

## ROM 适配（重要）

### 小米 / Redmi（MIUI、HyperOS）

系统会**静默通知监听类 App 自己发的通知**（表现为只有图标红点、无横幅）。本项目已通过"前台服务转发"绕过。仍需设置：

- 应用管理 → QQ关键词监控 → **自启动 = 开**
- 省电策略 → **无限制**
- 最近任务 → 长按卡片 → **锁定**
- 通知管理 → 渠道"QQ关键词提醒" → 确认**横幅、声音、震动**均开启

已在 HyperOS 3.0.10.0（Android 15，Xiaomi Civi 3）实测通过。

### 其他

- 华为 EMUI / HarmonyOS 4.x（兼容安卓）：同样可用，但后台管控更严，需手动白名单
- 原生 Android / 类原生：基本无需额外设置

## 已知限制

- 只能监控**出现在通知栏**的消息。QQ 处于前台时新消息不弹通知，自然抓不到；群通知被关闭也抓不到
- 通知文案格式可能随 QQ 版本变化，识别不准时调整 `QQWatcherService.kt` 的 `splitSender()`
- 图片/文件类消息文本为占位符，关键词匹配不到属正常
- 仅支持 Android 8.0+（API 26+）

## 平台支持

| 平台 | 是否可行 | 原因 |
|---|---|---|
| Android | ✅ | 有公开的通知监听接口 |
| iOS | ❌ | 沙箱隔离，无跨 App 读取通知的 API |
| HarmonyOS NEXT | ❌ | 无公开通知监听能力，且不兼容 APK |

## 技术栈

Kotlin · Android SDK 34 · 无第三方依赖 · 692 行代码

## 许可证

MIT
