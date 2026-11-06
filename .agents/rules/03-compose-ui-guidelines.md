# Jetpack Compose UI 规范

## 1. 状态提升 (State Hoisting)
- **容器与纯视图拆分**:
  - `*Screen` (有状态容器): 负责注入 ViewModel 并收集 `uiState`。
  - `*Content` (无状态视图): 仅接收数据模型和事件回调 lambda，**严禁直接传入 ViewModel**，确保可直接在 IDE 中 Preview 和独立测试。

## 2. 性能与主题规范
- **列表 Key**: `LazyColumn` / `LazyRow` 的 `items` **必须指定唯一 `key`**，避免数据变更时引发整表不必要的重绘。
  ```kotlin
  items(items = transactions, key = { it.id }) { item -> ... }
  ```
- **禁止硬编码颜色**: 严格引用 `MaterialTheme.colorScheme.*`（如 `primary`, `surfaceVariant`），自动适配深色模式，严禁硬编码十六进制颜色。
- **Modifier 规范**: 公共 Composable 将 `modifier: Modifier = Modifier` 作为第一个可选参数，并应用到根组件。
