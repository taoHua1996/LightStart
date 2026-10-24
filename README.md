# 轻启 · LightStart

**少等一会，轻松打开。**

开屏广告跳过助手，面向各品牌安卓手机，通过无障碍服务辅助跳过开屏广告，并按应用记录跳过操作。最低支持 Android 8（API 26），不同设备的授权和后台设置入口可能不同。没有网络权限、Root、VPN 或悬浮窗依赖。

这是一个独立项目：源码、Gradle Wrapper、文档、测试脚本都在本目录，构建不引用任何相邻项目，可整目录移动。请由 Android Studio 配置 SDK，或使用下方 bootstrap 脚本准备环境。

- [下载 Release APK](https://github.com/taoHua1996/LightStart/releases)
- [每次推送的 APK 构建](https://github.com/taoHua1996/LightStart/actions/workflows/android.yml)

**边界：不能全面拦截广告或摇一摇跳转；不能保证强停、撤权或厂商后台清理后仍然运行。**

- [完整方案与权限限制](docs/方案与限制.md)
- [设备验收清单](docs/验收清单.md)
- [安卓手机使用与验收](docs/安卓手机使用与验收.md)
- [构建与发布](docs/构建与发布.md)
- [0.3.0 修改与验证](docs/0.3.0修改与验证.md)

## 使用

1. 安装调试 APK 并打开轻启。
2. 在概览点击“开启无障碍服务”，阅读并同意披露，然后在系统设置启用“轻启 · 开屏跳过”。部分侧载设备需要先允许“受限设置”。
3. 返回概览，按提示完成设置：无障碍已连接、自动跳过总开关已开启、至少启用一个应用。服务连接状态会自动刷新；“已连接”本身不代表已启用应用。
4. 在应用页点“一键启用”，或单独开启需要处理的应用；默认规则自动生效，不需要逐个填写。可用“全部关闭”关闭所有可配置应用。回到概览点击“测试跳过功能”，再到记录页查看演示记录。
5. 只有默认规则无效时，才需要打开“高级规则（可选）”设置精确文字或控件资源 ID；不建议配置“确定”等通用文字。误触时先关闭该应用。一键操作覆盖当前载入的全部可配置应用，不受搜索影响，不覆盖已有自定义规则；新安装应用需再次启用。
6. 在手机设置中按需允许自启动、调整电池策略；这些只是降低被清理概率，无法保证永久运行。

“次数”统计系统接受的点击，不代表已确认广告消失。演示与被拒绝的点击不会增加第三方累计。

## 构建

Android Studio 打开本目录，使用 JDK 17 或更高版本（本机使用 JDK 21）、Android SDK Platform 36 / Build Tools 36.0.0。最低 Android 8（API 26），目标 Android 16（API 36）。所有文本文件 UTF-8。

```powershell
# 构建、Lint、签名校验并复制 APK：配置 local.properties 或 ANDROID_HOME
powershell -ExecutionPolicy Bypass -File tools/package.ps1

# 独立核心规则回归（需要 java / javac）
powershell -ExecutionPolicy Bypass -File tools/test-core.ps1
```

本地最新版 APK 保存在 `releases/LightStart-0.3.1-debug.apk`。使用与旧版相同密钥构建时，可覆盖安装并保留统计和规则。安卓安装标识沿用 `com.qingping.app`，以保留升级兼容性；用户可见名称为“轻启”。Gradle 原始输出为 `app/build/outputs/apk/debug/app-debug.apk`。

当前为调试签名版本。不同开发环境、不同 CI 运行的签名可能不同，不能保证相互覆盖安装。正式发布须配置稳定的私有签名密钥，详见[构建与发布](docs/构建与发布.md)。

## 每次上传都打包 APK

推送前运行 `tools/package.ps1`。GitHub Actions 对每次 push、Pull Request 和手动触发执行核心规则测试、APK 构建及 Lint；成功运行提供带版本和提交标识的 APK Artifact，保留 90 天。推送与应用版本一致的 `v*` 标签时，自动创建 Release 并附加 APK 与 SHA-256 校验文件。构建失败不发布安装包。

APK、SDK、构建缓存、签名密钥和本地设备历史截图不进入源码 Git 历史。

无本地构建环境时，`python tools/bootstrap.py` 会从官方来源将 Gradle 8.13 与 Android 命令行工具下载到忽略目录 `.tools/`，然后可运行 SDK Manager 安装所需平台与构建工具。`local.properties` 是本机路径，不应提交。

## 结构

- `SkipPolicy.java`：纯 Java 精确匹配与时间窗，一次会话最多一次动作。
- `SkipAccessibilityService.java`：活动窗口校验、受控遍历、控件点击与记录。
- `Store.java`：偏好设置、SQLite 日累计/应用累计/滚动明细。
- `MainActivity.java`：权限、应用配置和统计。
- `DemoActivity.java`：无广告、无网络的模拟开屏。
- `BootReceiver.java`：开机时间记录，不强行启动服务。
- `tools/PolicyChecks.java`：规则回归断言。

首次版本没有收集任何第三方应用的真实广告规则；特定应用是否可跳过必须用目标手机测试。

## MuMu 验证

已在 MuMu Android 12（API 32）验证授权、演示实际自动点击、暂停、独立包名测试样例的应用开关与统计，以及 11 项 SQLite 设备断言。修复了页面切换时活动窗口返回旧页/空值导致漏跳的问题；本地规则回归共 39 项通过。模拟器通过不代表所有机型或真实广告兼容。

设备工具：`python tools/device-ui.py <ADB设备地址> dump|click|screenshot [文字或文件路径]`。无障碍效果验证时应避免持续运行 `uiautomator dump`，它的 UI Automation 会暂时影响服务连接；点击后用截图和本地统计核对。

## 开源许可

使用 [MIT License](LICENSE)。欢迎提交 Issue 和 Pull Request，贡献前请阅读 [CONTRIBUTING.md](CONTRIBUTING.md)。
