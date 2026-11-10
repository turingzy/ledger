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

## 图标与视觉一致性
- **统一图标来源**: 所有界面图标必须使用 Google **Material Symbols (new)**，统一采用 **Rounded、Weight 400、Grade 0、Optical Size 24**；默认使用未填充版本，选中态按需使用对应填充版本。
- **禁止混用**: 不得使用旧版 Material Icons（`Icons.Default`、`Icons.Outlined`、`Icons.AutoMirrored` 等）、重新引入 `material-icons-extended`，或用自绘路径、字符、Emoji 冒充界面图标。
- **统一维护入口**: 优先复用 `app/src/main/kotlin/cn/turingzy/ledger/ui/component/MaterialSymbols.kt`。缺少图标时，在 `scripts/sync_material_symbols.py` 中添加官方符号映射并运行同步，保留来源及许可证信息；不得手工修改生成的矢量路径。
- **统一视觉语言**: 同类控件保持一致的图标尺寸、线条粗细、颜色语义、圆角、间距和点击反馈，优先复用现有共享组件。颜色遵循主题，兼容深浅色模式。
- **尊重已确认设计**: 修改图标或局部样式时，保持无关布局、字体、尺寸及交互不变；图标本身颜色、背景颜色和点击反馈分别处理，不得自行联动修改。卡片上的圆形图标按钮优先复用 `SummaryCardIconButton`，具体颜色与反馈遵循当前页面要求。

## 专题规则索引
- [01-架构分层与依赖拓扑](file://.agents/rules/01-architecture-principles.md)
- [02-代码质量、自解释与优雅编码](file://.agents/rules/02-code-quality-and-safety.md)
- [03-Jetpack Compose 规范与性能](file://.agents/rules/03-compose-ui-guidelines.md)
