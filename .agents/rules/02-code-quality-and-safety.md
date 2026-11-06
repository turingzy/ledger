# 代码质量与健壮性规范

## 1. 注释治理：拒绝废话
- **禁止复述代码字面意思**: 严禁在属性、简单 getter、常规函数上添加显而易见的废话注释（如在 `fun getAccounts()` 上加 `// 获取账户`）。
- **仅解释“Why”而非“What”**: 注释只用于说明非常规设计、规避平台 Bug 或关键业务约束。

## 2. 代码优雅与防御性编程
- **扁平化结构**: 优先使用卫语句（Guard Clauses）提前返回，避免深层 `if-else` 嵌套。
  ```kotlin
  val validTx = transaction ?: return
  ```
- **空安全**: 严禁使用 `!!` 强转。使用 `?.`、`?:` 或提前退出。
- **禁止空 catch**: 严禁 `catch (e: Exception) {}` 吞掉异常。必须记录日志或向上抛出；严禁吞掉协程 `CancellationException`。
- **不可变优先**: 变量优先 `val`；对外只暴露只读 `List`/`Set`，禁止外露 `MutableList`。

## 3. 记账核心：金额处理
- **统一以分存储计算**: 货币金额一律采用 **`Long`（单位：分）**，彻底杜绝 `Double`/`Float` 精度丢失。
- **汇率与除法**: 涉及乘除运算使用 `BigDecimal` 并指定舍入模式，计算完毕转回 `Long`（分）。
- **展示格式化**: 仅在 UI 渲染层调用 `:core:common` 格式化为元。

## 4. 并发与数据完整性
- **协程调度**: 耗时 IO 注入 `@Dispatcher(LedgerDispatchers.IO)`，严禁直接硬编码 `Dispatchers.IO`。
- **软删除感知**: 常规业务查询与统计必须显式带上 `WHERE is_deleted = 0`。
