# 项目约定

- 所有创建或修改的文本文件使用 UTF-8。
- 产品名称为“轻启”，英文及项目名为 `LightStart`，宣传语为“少等一会，轻松打开。”
- 面向各品牌安卓手机，不将通用说明绑定到特定机型或厂商系统。
- 保留 `com.qingping.app` 安装标识和原有数据存储名称，以支持既有用户覆盖升级。
- 每次上传或推送代码前必须构建 APK 并运行 Lint；Windows 可执行 `tools/package.ps1`。
- 每次推送由 GitHub Actions 再构建一次 APK 并保存 Artifact；版本标签推送同时发布 Release APK。
- 不提交 SDK、构建缓存、APK、签名密钥、设备私有数据或凭证；APK 放入 Releases 或 Actions Artifacts。
- 修改后运行与改动相关的测试，准确区分模拟器验证和真机兼容性。
