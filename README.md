# 白日夢记账 App

小米 14 / 澎湃系统上的本地私用记账 App。数据只保存在手机本地，不做云端同步。

## 当前版本

- 版本标记：`v0.2.1-bank-income-reminder`
- 安装包名：`com.personal.xiaomiledger.v2`
- 手机显示名：`白日夢新版`
- 旧版包名：`com.personal.xiaomiledger`，因 debug 签名不一致，当前保留旧版并并存安装新版。

## 已实现

- 手动记录支出、收入、转账。
- 本地账户余额、资产总览、月度收支、搜索账单。
- 左侧栏、预算管理、分类管理、统计分析、自动记账日志。
- 微信/支付宝/淘宝支付成功页的无障碍识别。
- 中国银行、交通银行、招商银行 App 通知识别。
- 银行 App 支出通知能识别金额时自动入账，并发送提醒。
- 银行 App 收款/入账/到账/转入通知能识别金额时自动入账，并发送提醒。
- 微信零钱、零钱通、支付宝余额、余额宝等支付方式在支付成功页明确出现时，可自动入账。
- 银行通知只有“动账提醒”但没有金额时，弹出确认页补全。
- 点击资产列表里的某个账户进入平账时，会默认选中该账户。

## 权限

新版需要手动开启：

- `白日夢新版支付页面识别`：无障碍权限，用于读取支付成功页文本。
- `白日夢新版支付通知识别`：通知读取权限，用于读取银行/支付通知。
- 通知发送权限：用于提醒已识别、已自动记账。

## 自己编译安装

在 PowerShell 里进入项目目录：

```powershell
cd /d D:\xiaomi_app
```

设置本地 Android 构建环境：

```powershell
$env:JAVA_HOME='D:\xiaomi_app\.tools\jdk17'
$env:ANDROID_HOME='D:\xiaomi_app\.tools\android-sdk'
$env:ANDROID_SDK_ROOT='D:\xiaomi_app\.tools\android-sdk'
$env:GRADLE_USER_HOME='D:\xiaomi_app\.tools\gradle-home'
$env:Path='D:\xiaomi_app\.tools\jdk17\bin;D:\xiaomi_app\.tools\android-sdk\platform-tools;' + $env:Path
```

编译 debug 安装包：

```powershell
.\gradlew.bat assembleDebug --no-daemon
```

确认手机已连接：

```powershell
.tools\android-sdk\platform-tools\adb.exe devices -l
```

覆盖安装到手机：

```powershell
.tools\android-sdk\platform-tools\adb.exe install -r app\build\outputs\apk\debug\app-debug.apk
```

这条安装命令只覆盖当前包名对应的 App，不会卸载，不会清空数据。不要随意修改 `app/build.gradle` 里的 `applicationId`，否则手机会把它当成另一个新 App。

## 关键代码位置

- 自动解析通知：`app/src/main/java/com/personal/xiaomiledger/PaymentParser.java`
- 通知监听入口：`app/src/main/java/com/personal/xiaomiledger/PaymentNotificationListener.java`
- 自动保存逻辑：`app/src/main/java/com/personal/xiaomiledger/AutoSaveManager.java`
- “安乃近提醒”通知文案：`app/src/main/java/com/personal/xiaomiledger/NotificationHelper.java`
- 资产平账页面：`app/src/main/java/com/personal/xiaomiledger/AdjustBalanceActivity.java`

## 开发日志

### 2026-05-26

- 建立 Git 版本管理。
- 增加银行 App 通知高置信度自动入账。
- 增加自动保存后的系统通知。
- 优化银行金额提取：优先识别消费金额、交易金额、扣款金额、支出金额、入账金额、到账金额等字段。
- 银行动账无金额时保留确认页兜底。
- 首页重排为净资产、本月收支、快捷操作、资金账户、近期账单。
- 无障碍识别改为底部浮层确认，降低后台拉起 Activity 被系统拦截的概率。
- 增加银行到账/入账/转入/收款通知的自动收入入账。
- 增加微信零钱/零钱通/支付宝余额/余额宝支付的自动入账兜底；支付方式不明确时仍保留确认，避免误记账户。
- 修复资产列表点击具体账户后，平账页总是默认中国银行的问题。
- 将自动记账通知渠道和标题改为“安乃近提醒”。
