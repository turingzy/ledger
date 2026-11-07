# Agent 全局核心开发指令

## 核心不变式与硬性红线 (Core Invariants)
- **金额法则**: 所有金额**一律采用 `Long`（单位：分）存储与计算**，严禁使用 `Double` 或 `Float`。
- **空安全**: 严禁使用 `!!` 强转。必须使用 `?.`、`?:`、`checkNotNull()` 或提前返回。
- **异常处理**: 严禁空 `catch (e: Exception) {}` 吞掉异常。必须记录堆栈或向上抛出；严禁吞掉协程 `CancellationException`。
- **极简注释**: 严禁废话注释（不复述代码字面意思）。代码通过自解释命名表达意图，注释仅用于说明“为什么（Why）”或复杂边缘逻辑。
- **代码优雅**: 保持函数单一职责；优先提前返回（Guard Clauses）避免深层嵌套；善用 Kotlin 惯法语义，拒绝过度炫技。
- **状态流安全**: 严禁公开发布 `MutableStateFlow`，必须通过 `.asStateFlow()` 暴露只读 `StateFlow`。
- **协程调度**: 注入 `@Dispatcher(LedgerDispatchers.IO)`，严禁直接写死 `Dispatchers.IO`。
- **架构隔离**: Feature 间严禁直接耦合，页面导航统一走 `cn.turingzy.ledger.navigation`；Room `@Entity` 严禁穿透到 UI/ViewModel。

## 专题规则索引
- [01-架构分层与依赖拓扑](file://.agents/rules/01-architecture-principles.md)
- [02-代码质量、自解释与优雅编码](file://.agents/rules/02-code-quality-and-safety.md)
- [03-Jetpack Compose 规范与性能](file://.agents/rules/03-compose-ui-guidelines.md)
