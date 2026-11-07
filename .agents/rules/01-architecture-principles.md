## 1. 架构分层与包结构隔离 (Clean Architecture & Package-by-Feature)
当前项目处于单模块架构（随时可通过已有的 Convention Plugins 平滑演进为多模块），严格遵循高内聚、低耦合的包层级设计：
- **领域纯粹性**: `cn.turingzy.ledger.model` 保持纯 Kotlin 数据结构与业务枚举，严禁引入 Android 视图/Context 依赖。
- **页面解耦与路由中心**: Feature 间不得直接强引用内部私有组件，跨页面跳转统一通过 `cn.turingzy.ledger.navigation` 声明的导航路由与参数进行解耦分发。
- **UI 与数据层解耦**: UI 只与 ViewModel 和 Repository 接口通信，严禁直接依赖数据库 DAO 与实体。

## 2. 领域模型隔离 (SSOT)
- UI 与 ViewModel 严禁直接使用 Room `@Entity`。
- Repository 负责将数据库实体映射为不可变 Domain Model 对外暴露：
  ```kotlin
  // 正例: 映射为领域模型
  fun TransactionEntity.asExternalModel() = Transaction(...)
  ```

## 3. 状态管理
- 页面暴露单一不可变的 `UiState`。
- 严禁公开暴露 `MutableStateFlow`，必须通过 `.asStateFlow()` 暴露只读状态：
  ```kotlin
  private val _uiState = MutableStateFlow(TransactionUiState())
  val uiState: StateFlow<TransactionUiState> = _uiState.asStateFlow()
  ```
