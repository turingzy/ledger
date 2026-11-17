# UI 一致性重构

状态：本轮清单内的 UI 迁移效果已获用户确认；结构精简已完成，保留已确认的外观、交互与业务行为。

## 完成范围

- 主页面：消费日历、分析、账本、钱包、设置。
- 设置子页：常规、解析规则及编辑、卡面管理及编辑、账单导入导出、备份恢复、关于、开源许可。
- 业务抽屉：记一笔、交易详情、账户选择、新增/编辑账户、周期选择、分析交易明细。
- 弹窗：背景设置、卡面选择、颜色选择、日期/时间、删除账户、恢复/导入/更新及消息确认。
- 通用控件：下拉菜单与箭头、表单边框、按钮、列表行、卡片、分隔线和按压反馈。

## 统一入口

| 内容 | 实际公共实现 |
| --- | --- |
| 页面、安全边距与顶栏 | BasePageFrame / BasePageScaffold；设置子页共用 SettingsSubpageScaffold |
| 卡片与表面 | BaseCard / BaseSurface |
| 弹窗、确认框与抽屉 | BaseDialogWindow / BaseAlertDialog / BaseModalBottomSheet |
| 下拉与表单 | BaseDropdownMenu / BaseDropdownIndicator / BaseFormFieldFrame |
| 按钮、列表与反馈 | BaseActionButton / BaseIconButtonContent / BaseListRow / UiPressFeedback |
| 金额、交易行及账户图标 | BusinessAmountText / BusinessTransactionRow / BusinessAccountIcon |

旧 SettingsPage、LedgerIconButton、LedgerOutlinedButton、LedgerFormFieldFrame、TransactionRow 和 AccountSheet 转发层已删除，调用处直接使用公共组件。原生窗口及表面仅允许在基础组件目录创建，单元测试持续检查此边界。

## 保留的规则

- 下拉展开时箭头朝上，收起时朝右；分段选择框未选中文字浅色、选中使用正文色。
- 通用图标 24dp、紧凑圆形按钮区域 36dp；具体控件保留已确认的尺寸配置。
- 外币交易单位位于金额后方，浅色 11sp；消费日历空日与未来日期复用同一组件，“记一笔”不显示按压反馈。
- 按压背景由卡片裁切圆角；保留毛玻璃顶栏、卡片布局、滑动行为、字段位置和业务回调。
- 系统边距统一经公共容器处理。固定高度录入及账户抽屉由内容负责边距，保留原有键盘策略，避免重复添加或改变展开高度。
- 操作开始与取消使用独立的防抖身份，允许立即取消。

## 结构精简结果

- 基础组件由 16 个文件合并为 5 个：BaseLayouts、BaseForms、BaseButtons、BasePageScaffold、BaseOverlays。
- 业务组件由 6 个文件缩为 3 个：BusinessAmountText、BusinessIcons、BusinessTransactionItem；交互代码由 3 个文件合为 1 个。
- 删除无生产引用的弹窗/抽屉内容壳、账户余额/分类标识/类型徽章封装及交易详情适配重载；预览改为验证真实使用的组件。
- 相对精简前，Kotlin 文件从 324 减至 301，净删 793 行，其中生产代码净删 701 行；仅删除已移除封装对应的无效测试，保留金额、交互、页面及架构边界回归检查。

## 验证与核对

258 个单元测试通过；Debug 构建、设备用例编译和 lint 通过（0 错误）。与精简前快照对照，74 个 ViewModel、数据、模型和导航文件未变；金额输入/表达式/汇总及钱包卡片滑动算法未变，22 个核心渲染与交互函数体保持一致。

集中核对入口仍为 `app/src/main/kotlin/cn/turingzy/ledger/ui/preview/RemainingUiPreviews.kt`；已有主页面和实际使用的业务组件 Preview 保留。

Preview 和设备交互用例仅通过编译，未实际渲染或执行；未安装应用、连接调试或操作设备。后续直接更新本文，不新增逐任务文档。
