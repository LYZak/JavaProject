## 目标
- 在“事件模拟/模拟工作台”中，用户点击“用户列表/读卡器列表”的任一行时，立即在界面中展示对应的详细信息（无需弹窗也可选支持双击弹窗）。

## 界面改造
- 在右侧区域（控制区下方、Feed 上方）新增一个“详情”面板：
  - 采用 `JSplitPane` 垂直分割：上方详情、下方 Feed，保证日志仍可见。
  - 详情面板内部用 `CardLayout`：`未选择` / `用户详情` / `读卡器详情`。
  - 详情内容用不可编辑 `JTextArea` 或 HTML `JEditorPane`（便于对齐字段）。

## 交互与事件
- 为 `userTable` 与 `readerTable` 添加 `ListSelectionListener`（以及可选 `MouseListener` 处理双击）。
- 处理排序场景：使用 `convertRowIndexToModel` 从视图行号换算到模型行号，确保取到正确的 userId/readerId。
- 当用户在 Users/Readers Tab 切换时保留当前详情，或切换到对应类型卡片。

## 详情内容（字段来源）
- **用户详情**：
  - 基本信息：userId、姓名、性别、类型、badgeId。
  - 徽章信息：badgeCode、创建/过期/最后更新、valid、是否需要更新。
  - 权限信息：该用户绑定的 profiles（来自 `badge_profiles` 关联），并展示每个 profile 的可访问组摘要（可选）。
- **读卡器详情**：
  - readerId、绑定 resourceId。
  - 资源信息：resource 名称/类型/位置/楼栋/楼层/状态、badgeReaderId。
  - 资源所属组：resourceId → groupName（来自 `resource_group_members`）。
- （可选增强）展示最近 N 条相关日志（按 resourceId 或 badgeCode 过滤 `data/logs/...`），用于“模拟更直观”。

## 国际化
- 为新增 UI 文案补齐 `I18n` key（如：`simw.details.title`、`simw.details.noneSelected`、`simw.details.user`、`simw.details.reader`、字段名等）。
- 保持 `applyLanguage()` 语言切换后详情面板也能即时刷新。

## 验证
- 新增小型单元测试：
  - 详情渲染/格式化函数在缺字段（无 badge、无 group）时的输出稳定性。
  - `convertRowIndexToModel` 的使用不会因排序导致取错行（可通过构造 `TableRowSorter` 的单测）。
- 手工验证：生成 300 用户/400 读卡器后，点击多行快速切换，详情刷新不卡顿。

## 交付文件
- 主要改动：`SimulationWorkbenchPanel.java`（新增详情面板与监听、填充详情逻辑）。
- 辅助改动：`MainWindow.java`（新增 I18n 字典项）。
- 可选新增：`SimulationDetailsFormatter.java`（将“取数据/拼文本”从 UI 分离以便测试）。