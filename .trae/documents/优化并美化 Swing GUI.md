## 目标
- 让界面观感更现代一致：统一字体/间距/配色/控件风格，减少“默认 Swing”质感。
- 提升可用性：表格更易读、表单更清晰、日志更好用、状态反馈更明确。
- 不引入新第三方依赖（当前 [pom.xml](file:///d:/work/JavaProject/pom.xml) 未包含 UI 主题库），仅用标准 Swing 完成。

## 现状快速诊断（基于代码浏览）
- 未在入口处设置 Look & Feel（见 [Main.java](file:///d:/work/JavaProject/src/main/java/com/bigcomp/accesscontrol/Main.java)），整体控件外观依赖系统默认，跨机器不一致。
- 面板内部存在零散字体设置（例如 [RealTimeMonitorPanel.java](file:///d:/work/JavaProject/src/main/java/com/bigcomp/accesscontrol/gui/RealTimeMonitorPanel.java) 中对 JTextArea 设置字体），但缺少全局一致性。
- 多处使用匿名按钮创建（例如 [UserManagementPanel.java](file:///d:/work/JavaProject/src/main/java/com/bigcomp/accesscontrol/gui/UserManagementPanel.java)），按钮尺寸/间距不统一。
- 主要布局以 BorderLayout + GridBagLayout/FlowLayout 混用为主，缺少统一的边距（EmptyBorder）与分区标题（TitledBorder），视觉层级偏弱。

## 改动范围（文件）
- 入口与窗口：
  - [Main.java](file:///d:/work/JavaProject/src/main/java/com/bigcomp/accesscontrol/Main.java)
  - [MainWindow.java](file:///d:/work/JavaProject/src/main/java/com/bigcomp/accesscontrol/gui/MainWindow.java)
- 各功能面板：
  - [UserManagementPanel.java](file:///d:/work/JavaProject/src/main/java/com/bigcomp/accesscontrol/gui/UserManagementPanel.java)
  - [ResourceManagementPanel.java](file:///d:/work/JavaProject/src/main/java/com/bigcomp/accesscontrol/gui/ResourceManagementPanel.java)
  - [ResourceGroupManagementPanel.java](file:///d:/work/JavaProject/src/main/java/com/bigcomp/accesscontrol/gui/ResourceGroupManagementPanel.java)
  - [ProfileManagementPanel.java](file:///d:/work/JavaProject/src/main/java/com/bigcomp/accesscontrol/gui/ProfileManagementPanel.java)
  - [RealTimeMonitorPanel.java](file:///d:/work/JavaProject/src/main/java/com/bigcomp/accesscontrol/gui/RealTimeMonitorPanel.java)
  - [LogViewerPanel.java](file:///d:/work/JavaProject/src/main/java/com/bigcomp/accesscontrol/gui/LogViewerPanel.java)
  - [EventSimulationPanel.java](file:///d:/work/JavaProject/src/main/java/com/bigcomp/accesscontrol/gui/EventSimulationPanel.java)

## 具体优化项
### 1) 全局主题与一致性（最关键）
- 在 [Main.java](file:///d:/work/JavaProject/src/main/java/com/bigcomp/accesscontrol/Main.java) 启动时：
  - 优先设置系统 Look & Feel（Windows 下更贴近原生），失败则 fallback 到 Nimbus。
  - 统一全局字体：为 UIManager 的常见 key（Label/Button/TabbedPane/Table 等）设置同一套字体与字号。
  - 统一全局间距：为 TabbedPane、OptionPane、Table header 等设置更舒适的 padding。

### 2) 主窗口结构更“产品化”
- 改造 [MainWindow.java](file:///d:/work/JavaProject/src/main/java/com/bigcomp/accesscontrol/gui/MainWindow.java)：
  - Tab 标题更短、层级更清晰（可选：中英文一致化）。
  - 增加底部状态栏（JPanel + JLabel）：展示“数据库连接/最后一次刷新/最近一次访问结果”等。
  - 菜单栏补全常用项：刷新数据、导出日志、打开数据目录等（若已有对应能力则接入）。

### 3) 表格与表单观感提升（管理类面板）
- 对所有 JTable：
  - 设置 rowHeight、header 字体、自动排序（RowSorter）、列宽策略。
  - 增加“斑马纹”渲染（自定义 DefaultTableCellRenderer）让行更易读。
  - 对关键列做对齐（ID 居左、枚举居中等）。
- 对所有表单区：
  - 顶部表单增加外边距（EmptyBorder）和分区标题（TitledBorder）。
  - 统一 GridBagConstraints（insets、fill、weightx），避免控件挤压。
  - 统一按钮宽度/高度与间距；危险操作（删除）用更明显的样式（例如前景色/确认对话框更明确）。

### 4) 日志与模拟面板更易用
- [LogViewerPanel.java](file:///d:/work/JavaProject/src/main/java/com/bigcomp/accesscontrol/gui/LogViewerPanel.java)：
  - 日志区增加行号/筛选区更明确的布局。
  - 增加“复制/清空/导出”按钮区，并统一按钮样式。
  - 对不同级别/结果（granted/denied）使用颜色高亮（仍保持可读）。
- [EventSimulationPanel.java](file:///d:/work/JavaProject/src/main/java/com/bigcomp/accesscontrol/gui/EventSimulationPanel.java)：
  - 将统计区、控制区、日志区做清晰分栏（SplitPane 或 BoxLayout）。
  - 统一字体与间距，减少信息拥挤。

### 5) 实时监控更直观
- [RealTimeMonitorPanel.java](file:///d:/work/JavaProject/src/main/java/com/bigcomp/accesscontrol/gui/RealTimeMonitorPanel.java)：
  - 使用 JSplitPane：左侧地图/右侧事件与读卡器详情，提升信息层次。
  - 增加 legend（图例）与更清晰的状态颜色（通过 MapViewPanel 绘制逻辑实现）。
  - 事件日志中对“通过/拒绝”增加颜色或前缀标签，提高扫读效率。

## 验证方式（完成后我会执行）
- 运行 `mvn test` 确保改动不影响测试。
- 启动 GUI（例如通过现有脚本或 `mvn exec:java`）人工检查：
  - Tab、表格、表单、日志、监控页面排版是否正常；
  - Windows 下 DPI 缩放（125%/150%）是否仍可用。

## 交付结果
- 视觉：统一字体/间距/表格样式，整体更现代。
- 交互：状态栏反馈、日志可读性增强、危险操作更安全。
- 代码：不引入新依赖，主要是对现有 GUI 类做结构与样式整理。

如果确认，我将按以上顺序开始改造：先全局主题与 MainWindow，再逐个面板统一风格，最后跑测试并启动 GUI 进行验收。