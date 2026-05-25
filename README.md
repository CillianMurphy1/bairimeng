# 白日夢记账 App

小米 14 / 澎湃系统上的本地私用记账 App。不做云端同步，不上传账单数据。

## 当前版本

- 版本标记：`v0.2.0-v2-bank-autosave`
- 安装包名：`com.personal.xiaomiledger.v2`
- 手机显示名：`白日夢新版`
- 旧版包名：`com.personal.xiaomiledger`，因 debug 签名不一致，当前保留旧版并并存安装新版。

## 已实现

- 手动记录支出、收入、转账。
- 本地账户余额、资产总览、月度收支、搜索账单。
- 左侧栏、预算管理、分类管理、统计分析、自动记账日志。
- 微信/支付宝/淘宝支付成功页的无障碍识别。
- 中国银行、交通银行、招商银行 App 通知识别。
- 银行 App 通知能识别金额时自动入账，并发送“已自动记账”通知。
- 银行通知只有“动账提醒”但没有金额时，弹出确认页补全。

## 权限

新版需要手动开启：

- `白日夢新版支付页面识别`：无障碍权限，用于读取支付成功页文本。
- `白日夢新版支付通知识别`：通知读取权限，用于读取银行/支付通知。
- 通知发送权限：用于提醒已识别、已自动记账。

## 构建

本机工具位于 `D:\xiaomi_app\.tools`，常用命令：

```powershell
$env:JAVA_HOME='D:\xiaomi_app\.tools\jdk17'
$env:ANDROID_HOME='D:\xiaomi_app\.tools\android-sdk'
$env:ANDROID_SDK_ROOT='D:\xiaomi_app\.tools\android-sdk'
$env:GRADLE_USER_HOME='D:\xiaomi_app\.tools\gradle-home'
$env:Path='D:\xiaomi_app\.tools\jdk17\bin;D:\xiaomi_app\.tools\android-sdk\platform-tools;' + $env:Path
.\gradlew.bat assembleDebug --no-daemon
```

安装到手机：

```powershell
.tools\android-sdk\platform-tools\adb.exe install -r app\build\outputs\apk\debug\app-debug.apk
```

只允许覆盖安装，不卸载、不清数据。

## 开发日志

### 2026-05-26

- 建立 Git 版本管理。
- 增加银行 App 通知高置信度自动入账。
- 增加自动保存后的系统通知。
- 优化银行金额提取：优先识别 `消费金额`、`交易金额`、`扣款金额`、`支出金额` 等字段。
- 银行动账无金额时保留确认页兜底。
- 首页重排为净资产、本月收支、快捷操作、资金账户、近期账单。
- 无障碍识别改为底部浮层确认，降低后台拉起 Activity 被系统拦截的概率。
