# SimpleSqliteBrowser — 模块索引（供开发与 AI 辅助）

本文描述仓库内主要包职责、数据流与扩展点，便于快速定位代码。

## 顶层结构

| 路径 | 说明 |
|------|------|
| `src/main/kotlin/.../provider/` | 文件编辑器接入：何时打开自定义编辑器、编辑器生命周期 |
| `src/main/kotlin/.../sqlite/` | 语言与文件类型、`SqliteFileDetector`（扩展名 + 文件头识别） |
| `src/main/kotlin/.../model/` | JDBC 连接与读库（`SqliteModel`、`ConnectionManager`） |
| `src/main/kotlin/.../data/` | 表格/元数据等纯数据结构 |
| `src/main/kotlin/.../mvvm/` | 极简 `LiveData`、`ViewModel`（含 `dispose()` 约定） |
| `src/main/kotlin/.../ui/viewmodel/` | 各 Tab 的异步加载与状态 |
| `src/main/kotlin/.../ui/window/` | 主窗口、表格 Tab、元数据 Tab |
| `src/main/kotlin/.../ui/view/` | 小型 UI 组件 |
| `src/main/kotlin/.../tools/` | 表格模型/渲染器等 Swing 辅助 |
| `tablefilter/` | 内嵌的列过滤 UI 库（coderazzi） |
| `src/main/resources/META-INF/plugin.xml` | 插件 ID、依赖、`fileType`、`fileEditorProvider` |

## 核心数据流（打开 SQLite 库）

1. `SqliteEditorProvider.accept`：扩展名为 `db` / `sqlite` / `sqlite3` 时直接接受；否则在文件有效、非目录、长度在探测上限内时读取文件头 16 字节，匹配 `SQLite format 3\0` 则接受（见 `sqlite/SqliteFileDetector`）→ `SqliteEditor` → `SqliteBrowserMainWindow`
2. `SqliteBrowserMainWindow` 持有两个 `TabbedChildView`：`SqliteTablesWindow`、`SqliteMetadataWindow`
3. 表格：`TableViewModel` 通过 `SqliteModel` + `ConnectionManager` 异步读表名与分页数据；错误经 `loadError` → `Messages.showErrorDialog`
4. 元数据：`MetadataViewModel` 异步读 `SqliteMetadata`；错误同样经 `loadError`
5. 连接失败：`ConnectionManager` 返回 `null` 时，`SqliteModel.openConnectionOrThrow` 抛出 `IllegalStateException`，由 Rx `onError` 转为 `loadError`，避免静默空列表。
6. 关闭编辑器：`SqliteEditor.dispose()` → `SqliteBrowserMainWindow.dispose()` → 各子 Tab `dispose()` → 各 `ViewModel.dispose()`（取消 Rx 订阅）

## 生命周期约定

- 实现 `ViewModel` 的类必须在 `dispose()` 中释放异步资源（当前为 RxJava `CompositeDisposable`）。
- 新增绑定到文件编辑器的后台任务时：在对应 `ViewModel` 中登记并在 `dispose()` 中取消；UI 更新前检查 `disposed`（或通过已 dispose 的 disposable 确保不再投递）。

## 常量

- `Constants.kt`：`EXTENSION` / `EXTENSION_SQLITE` / `EXTENSION_SQLITE3` / `PLUGIN_DISPLAY_NAME` 等与插件全局相关的字符串。

## 构建注意

- `build.gradle.kts` 中可能关闭 `buildSearchableOptions`（Android Studio 无头构建与 Android 插件初始化顺序问题）。详见构建注释。
- 根目录 `.editorconfig`：Kotlin / XML 等的基础缩进与换行约定。

## 测试

- `src/test/kotlin/.../MyPluginTest.kt`：平台轻量测试，修改 `ViewModel` 签名时需同步检查。
