## 目标
- 将当前 Swing UI 从“系统/传统风格”升级为更现代的视觉（圆角、间距、统一字体/配色、滚动条/表格更精致）。
- 尽量集中在全局样式入口改造，避免在每个面板重复写 UI 代码。

## 现状调研结论
- 入口已做了全局样式：在 [Main.java](file:///d:/work/JavaProject/src/main/java/com/bigcomp/accesscontrol/Main.java#L29-L60) 里设置 Look & Feel，并通过 UIManager 全量替换字体、调整 Tab/表格/对话框字体与部分 Insets。
- 依赖目前没有现代 Look & Feel 库： [pom.xml](file:///d:/work/JavaProject/pom.xml#L22-L58) 仅包含 SQLite/Jackson/JUnit/Mockito。

## 方案（推荐）
### 1) 引入现代 Look & Feel：FlatLaf（Light 默认）
- 在 [pom.xml](file:///d:/work/JavaProject/pom.xml) 增加依赖：`com.formdev:flatlaf`（可选再加 `flatlaf-intellij-themes` 以便后续切主题）。
- 在 [Main.java](file:///d:/work/JavaProject/src/main/java/com/bigcomp/accesscontrol/Main.java) 的 `applyGlobalUiStyle()` 中优先启用 FlatLaf（默认 FlatLightLaf），失败再回退系统 LAF/Nimbus。

### 2) 统一“现代化 UI 参数”（全部走 UIManager）
在 `applyGlobalUiStyle()` 里增加/调整 UI 默认值（仅集中一处）：
- 圆角：Button/TextComponent/ScrollBar/ProgressBar 等 arc 参数
- 间距：按钮内边距、Tab 内边距、表格单元格 padding
- 颜色：选中背景、焦点颜色、分隔线更柔和
- 表格：更清晰的 selection、grid/striped 行与 header 字体更协调
- 字体：保留当前 pickBestUiFont 逻辑（中文显示友好），但配合 FlatLaf 的默认字号与渲染设置

### 3) 小范围面板级细化（仅必要处）
- 针对少数“按钮密集”的面板（如事件模拟、资源组等），统一按钮高度、间距、对齐方式，让整体更像现代管理后台。
- 这些调整尽量复用现有布局，不大改结构。

## 预计改动文件
- [pom.xml](file:///d:/work/JavaProject/pom.xml)
- [Main.java](file:///d:/work/JavaProject/src/main/java/com/bigcomp/accesscontrol/Main.java)
- （可选，少量微调）若干 GUI 面板文件：`EventSimulationPanel.java`、`ResourceGroupManagementPanel.java`、`MainWindow.java` 等

## 验证方式
- 本地运行主程序入口 [Main.java](file:///d:/work/JavaProject/src/main/java/com/bigcomp/accesscontrol/Main.java)，检查：
  - 启动后整体风格是否现代化（按钮圆角、输入框、滚动条、Tab、表格）
  - 中英文切换是否正常（不影响 I18n）
  - 常见对话框、表格列拖拽、分栏布局是否正常

## 可选增强（后续）
- 增加“浅色/深色主题切换”菜单项（FlatLaf 支持很好）。
- 引入少量现代图标（避免空白按钮/提升辨识度），但会涉及资源文件管理。

如果你确认这个方案，我将按上述步骤开始修改依赖与全局样式代码，并跑起来验证整体效果。