# 架构设计与依赖原则

## 1. 模块边界与依赖
- **禁止 Feature 横向依赖**: Feature 之间不得直接依赖，跨模块跳转统一使用 `:core:navigation` 中的路由声明，并在 `:app` 中装配。
- **纯粹性要求**: `:core:model` 保持纯 Kotlin，禁止引入 Android SDK 依赖。

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
