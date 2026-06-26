# 来电拦截 (Call Blocker)

一款 Android 来电拦截工具，支持按时间段、号码规则智能拦截来电。采用混合拦截策略，优先使用系统级 API，兼容多种设备。

## 功能特性

### 核心能力
- **多种匹配方式**：精确匹配、前缀匹配、拦截海外号码、拦截座机号码、拦截所有来电
- **快捷时间段**：全天、上午 (0-12)、中午 (12-14)、下午 (14-24) 一键选择，也支持自定义时间
- **多时间段**：单条规则可配置多个时间段
- **跨午夜支持**：如 23:00 - 06:00 的时间段也能正确判断
- **号码智能匹配**：自动识别 `+86`、`0086` 等国际区号前缀，手机号和座机号均可识别

### 混合拦截策略
按优先级自动尝试多种拦截方式，确保最大兼容性：
1. **系统级 API** — `TelecomManager.endCall()`（设为默认拨号应用后可用，最稳定）
2. **反射 ITelephony** — 传统方式（Android 8.x 及部分 9+ 设备）
3. **反射 TelephonyManager** — 备用方案（Android 12+）

### 规则管理
- 添加、编辑、删除拦截规则
- 规则唯一性校验，防止重复添加
- 每条规则可单独启用/禁用
- 规则名称备注，方便管理

### 其他特性
- 前台服务保活，通知栏常驻状态提示
- 开机自启（有启用规则时自动恢复服务）
- 暗色/亮色主题跟随系统
- Material Design 界面

## 环境要求

- Android 9.0 (API 28) 及以上
- Android Studio (推荐) 或命令行 Gradle 构建
- JDK 17

## 快速开始

### 1. 用 Android Studio 打开项目

```
File → Open → 选择 call-blocker 目录
```

### 2. 同步 Gradle

Android Studio 会自动提示同步，点击 **Sync Now**。

### 3. 连接设备并运行

- 用 USB 数据线连接安卓手机
- 手机上开启 **开发者选项 → USB 调试**
- 点击绿色三角 **Run ▶** 运行

> 必须使用真机，模拟器无法测试来电拦截功能。

### 4. 授予权限

首次运行时授予所有请求的权限（电话、通话记录、通讯录、通知）。

### 5. 设为默认电话应用（推荐）

点击界面上的 **"设为默认电话应用"** 按钮，按提示操作。设为默认拨号应用后，拦截效果最佳。

## 使用指南

### 添加规则

1. 点击右下角 **+** 按钮
2. 选择匹配方式：
   - **精确匹配**：输入完整号码（如 `165xxxxxxxx`）
   - **前缀匹配**：输入号码前缀（如 `010` 拦截所有北京座机）
   - **拦截海外号码**：自动识别非中国大陆号码
   - **拦截座机号码**：自动识别中国座机号段
   - **拦截所有来电**：不输入号码，拦截一切来电
3. 选择时间段：点击快捷按钮或自定义时间，可添加多个时间段
4. 可选填写规则名称
5. 点击 **保存**

### 编辑规则

点击规则右侧的 **编辑图标（铅笔）** 修改号码、时间等信息。

### 启动拦截

点击界面上方的 **"启动拦截"** 按钮，服务开始运行后会常驻通知栏。

## 项目结构

```
call-blocker/
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/callblocker/app/
│   │   ├── model/
│   │   │   ├── BlockRule.java              # 拦截规则数据模型
│   │   │   └── TimeRange.java              # 时间范围模型
│   │   ├── db/
│   │   │   └── RuleDatabase.java           # SQLite 数据库
│   │   ├── service/
│   │   │   ├── CallBlockService.java       # 前台服务
│   │   │   └── HybridCallBlocker.java      # 混合拦截策略
│   │   ├── receiver/
│   │   │   ├── CallBlockReceiver.java      # 来电广播拦截
│   │   │   └── BootReceiver.java           # 开机自启
│   │   └── ui/
│   │       ├── MainActivity.java           # 主界面
│   │       ├── AddRuleDialogFragment.java  # 添加/编辑规则对话框
│   │       └── RuleAdapter.java            # 规则列表适配器
│   └── res/
│       ├── layout/                         # 界面布局
│       └── values/                         # 字符串、颜色、主题
├── build.gradle
├── settings.gradle
└── README.md
```

## 权限说明

| 权限 | 用途 |
|------|------|
| `READ_PHONE_STATE` | 获取来电状态 |
| `READ_CALL_LOG` | 读取通话记录 |
| `READ_CONTACTS` | 识别来电者姓名 |
| `ANSWER_PHONE_CALLS` | 挂断来电（系统 API） |
| `MANAGE_OWN_CALLS` | 挂断来电（Android 12+） |
| `MODIFY_PHONE_STATE` | 挂断来电（反射方式） |
| `FOREGROUND_SERVICE` | 前台服务保活 |
| `RECEIVE_BOOT_COMPLETED` | 开机自启 |
| `POST_NOTIFICATIONS` | 状态通知 |

## 技术栈

- **语言**：Java
- **最低 SDK**：Android 9.0 (API 28)
- **目标 SDK**：Android 14 (API 34)
- **UI**：Material Components for Android
- **数据库**：SQLite (SQLiteOpenHelper)
- **构建**：Gradle 8.0 + Android Gradle Plugin 8.1.0

## 注意事项

1. **Android 10+ 限制**：第三方应用直接挂断电话的能力受限，建议设为默认拨号应用以获得最佳效果
2. **部分厂商定制系统**（MIUI、ColorOS 等）可能有自己的电池优化策略，建议将本应用加入电池优化白名单
3. 来电拦截需要监听 `PHONE_STATE` 广播，部分系统可能会限制后台广播接收，请确保应用未被冻结

## License

MIT
