我已分析现有项目结构，目前项目缺乏标准的自动化测试框架。现有的 `SystemTestRunner` 仅用于生成演示数据，无法进行断言验证。

我将为您构建一套基于 **JUnit 5** 的标准测试体系，包含以下步骤：

### 1. 基础设施搭建
- **添加依赖**: 修改 [pom.xml](file:///d:/work/JavaProject/pom.xml)，引入 `junit-jupiter` (JUnit 5) 和 `mockito` (用于模拟对象)。
- **创建目录**: 建立标准的 Maven 测试目录 `src/test/java/com/bigcomp/accesscontrol`。

### 2. 单元测试 (Unit Testing) implementation
针对核心业务逻辑编写独立的测试用例，不依赖外部数据库：
- **TimeFilterTest**: 深度测试时间规则逻辑（包含时间段跨越、星期排除、特殊日期等边界条件）。
- **AccessRequestProcessorTest**: 使用 Mock 对象模拟数据库和配置数据，专门测试权限判定引擎的核心逻辑（如：Badge过期、无权限、资源紧急状态等场景）。

### 3. 集成测试 (Integration Testing) implementation
测试组件间的真实交互和数据持久化：
- **DatabaseManagerTest**: 使用 SQLite **内存模式** (`jdbc:sqlite::memory:`) 运行测试，验证 CRUD 操作和关联查询，确保测试不污染本地文件数据库。
- **SystemIntegrationTest**: 启动完整的 `AccessControlSystem`，模拟从"刷卡"到"响应"的全流程，验证 Router、ARP 和 DatabaseManager 的协同工作。

### 4. 验证与执行
- 创建测试运行说明，演示如何使用 `mvn test` 执行所有测试并查看结果。

确认后，我将开始搭建测试环境并编写代码。