# JCode CLI 跨平台发布方案

## 目标体验

JCode CLI 面向 Windows 和 macOS 提供原生安装包。用户完成安装并重新打开终端后，可以直接运行：

```shell
jcode --version
jcode chat
```

安装包必须同时提供 JCode、Java 21 运行时和平台启动器。用户不需要自行安装 JDK，也不需要手动配置 `JAVA_HOME` 或 `PATH`。

首次运行缺少模型配置时，CLI 应引导用户输入 API 地址、API Key 和模型 ID；用户也可以主动运行：

```shell
jcode config init
```

## 发布产物

每个版本至少发布以下安装包：

```text
jcode-<version>-windows-x64.msi
jcode-<version>-macos-arm64.pkg
jcode-<version>-macos-x64.pkg
SHA256SUMS
```

安装包使用 `jlink` 生成精简 Java 21 Runtime，并使用 `jpackage` 生成平台安装包。由于 `jpackage` 不支持跨平台构建，每种产物必须在对应操作系统和架构上构建。

## Windows 安装方案

推荐采用每用户安装，避免要求管理员权限。默认目录：

```text
%LOCALAPPDATA%\Programs\JCode\
├── jcode.exe
├── app\
└── runtime\
```

`jpackage` 使用以下关键参数：

```text
--type msi
--win-console
--win-per-user-install
--win-dir-chooser
--win-upgrade-uuid <固定 UUID>
```

- `--win-console` 用于保证 stdin、stdout 和交互式终端正常工作。
- `--win-upgrade-uuid` 必须在所有版本中保持不变，确保新版本可以升级旧版本。
- 使用 `--resource-dir` 覆盖 WiX `main.wxs`，通过 MSI Environment Table 将安装目录追加到当前用户的 `PATH`。
- PATH 项必须由 MSI 组件管理，卸载时仅删除 JCode 自己添加的路径，禁止覆盖用户现有 PATH。
- 安装完成后提示用户重新打开 PowerShell 或 Windows Terminal。

最终用户不需要设置 Java、`JAVA_HOME` 或 PATH。

## macOS 安装方案

分别为 Apple Silicon 和 Intel Mac 构建 PKG。推荐安装结构：

```text
/Applications/JCode.app/
├── Contents/MacOS/JCode
├── Contents/app/
└── Contents/runtime/
```

通过 PKG 的 `postinstall` 脚本创建命令入口：

```text
/usr/local/bin/jcode
    -> /Applications/JCode.app/Contents/MacOS/JCode
```

卸载或升级逻辑必须正确维护该链接，且不能覆盖指向其他程序的同名文件。

公开发布的 PKG 必须完成：

1. 使用 Developer ID 对应用和安装包签名。
2. 启用 Hardened Runtime。
3. 使用 `notarytool` 提交 Apple 公证。
4. 使用 `stapler` 将公证票据附加到发布产物。

用户不需要修改 `.zshrc`、安装 JDK 或配置 PATH。

## 首次配置与凭据

运行环境和模型配置是两件不同的事情：安装器负责 Java 和 PATH；用户仍需提供自己的模型凭据。

新增 `jcode config init`，并在 `ask` 或 `chat` 的交互式首次运行中自动触发。建议流程：

```text
欢迎使用 JCode

API 服务地址 [默认地址]:
API Key: ********
模型 ID [默认模型]:

配置已保存。
```

默认配置文件位置：

```text
Windows: %USERPROFILE%\.jcode\config.properties
macOS:   ~/.jcode/config.properties
```

安全要求：

- API Key 输入时不回显。
- `config show` 不得输出 API Key 明文。
- macOS 配置文件权限设置为仅当前用户可读写，例如 `0600`。
- Windows 配置文件限制为当前用户访问；后续可以迁移到 Windows Credential Manager 或系统钥匙串。
- 非交互环境缺少配置时直接返回明确错误，并提示可用的环境变量或配置文件位置，不启动向导。
- 环境变量仍作为 CI 和自动化场景的配置方式保留，并维持现有优先级。

## CI/CD 发布流程

Git tag 使用语义化版本，例如 `v0.1.0`。GitHub Actions 发布矩阵：

| Runner | 验证内容 | 产物 |
| --- | --- | --- |
| Windows runner | PowerShell 执行、权限和 CLI 冒烟测试 | Windows x64 MSI |
| macOS ARM64 runner | zsh 执行、权限和 CLI 冒烟测试 | macOS ARM64 PKG |
| macOS Intel runner | zsh 和启动器冒烟测试 | macOS x64 PKG |

发布流水线：

```text
创建 v* tag
  -> 全平台测试
  -> Maven 构建 shaded JAR
  -> jlink 生成平台 Runtime
  -> jpackage 生成 MSI/PKG
  -> Windows 代码签名 / macOS 签名与公证
  -> 安装后冒烟测试
  -> 生成 SHA256SUMS
  -> 上传 GitHub Release
```

发布前必须验证：

- 在干净系统上安装，无预装 Java 也能运行。
- 新终端中可以直接执行 `jcode --version`、`jcode config doctor` 和 `jcode chat`。
- 安装路径包含空格时仍能正常启动。
- 覆盖安装和版本升级不会丢失用户配置。
- 卸载会清除启动器和 PATH 项，但保留用户配置，除非用户明确选择删除。
- Windows PowerShell 与 macOS zsh 的命令工具均通过平台测试。

## 实施顺序

1. 实现 `jcode config init` 和交互式首次运行向导。
2. 为配置文件补充权限保护和非交互模式判断。
3. 增加 `jlink` Runtime 构建配置。
4. 实现 Windows MSI、WiX PATH 配置和升级规则。
5. 实现 macOS PKG、命令链接、签名及公证流程。
6. 建立跨平台 GitHub Actions 和 GitHub Release 自动发布。
7. 后续可增加 Homebrew Tap，作为 macOS 用户的第二种安装入口。
