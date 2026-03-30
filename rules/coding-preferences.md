# 编码偏好与实现细则

## 代码偏好

- **插件基类（封装层）**：
  优先选择 `kd.cd.common.plugin` 包下的扩展基类：
  `AbstractBillPlugInExt`、`AbstractFormPluginExt`、`AbstractListPluginExt`、`AbstractOperationServicePlugInExt`、`AbstractValidatorExt`
- **原生插件基类**：
  BOTP 转换（`AbstractConvertPlugIn`）和反写（`AbstractWriteBackPlugIn`）**没有** `*Ext` 封装版本，直接用 `kd.bos.entity.botp.plugin` 包下的原生基类；遇到此场景时无需寻找封装类，直接走原生兜底路由。
- 需要错误汇总时：
  用 `OpUtils.addErrorMessage(...)`、`OpUtils.getCompleteFailMsg(...)`、`PushResult.failThenThrow()`
- 字符串判空优先使用 `CharSequenceUtils`；不要在示例代码里混用 `StringUtils`、`ObjectUtils` 做字符串空白判断。
- 集合判空优先使用 `CollectionUtils`；不要手写 `!= null && !isEmpty()` 或混用不统一的集合判空方式。
- 对异常优先使用日志框架记录；`*Ext` 基类里直接使用内置的 `public final Log log`，非插件类可直接使用
  `kd.bos.logging.LogFactory`。
- 如果必须给出原生写法：
  先说明为什么仓库封装不适用，再给最小可行实现，不要把原生样板扩散成默认风格。
- `IWorkflowPlugin` 这类接口型插件没有可调用的 `super.xxx()`；"先调 super"规则仅适用于继承型插件基类。

## 实现细则

- **元数据查询约束**：调用 `cosmic-form-metadata.py` 时，脚本会**自动隐藏**常见的审计字段（如创建人、创建组织、单据状态等）。如果任务确实需要这些字段，请在 `--fuzzy` 中显式指定。同时概览模式默认截断前 120 条，未找到时请增加模糊搜索词。
- 如果脚本未查到字段或表单元数据，必须提醒用户确认其提供的表单名称/标识是否正确，再继续处理。
- 未经用户明确要求，不要偏离 `assets/FormPluginTemplate.java` 的代码风格；确需偏离时，在答案里说明原因。
- 若必须新增模板之外的 `import`，需仅新增最小集合，并在答案里说明新增原因。
- 对单据状态流转，优先使用 `OpUtils`；不要散落调用多个 `OperationServiceHelper`。
- 只有在需要连续调用多个操作时，才优先使用 `OperateChain`；单次 `save`、`submit`、`audit` 优先使用 `OpUtils`。
- 对单据转换，优先使用 `BotpUtils`；不要手拼 `PushArgs`/`DrawArgs` 以外的重复样板。
- 你在处理基础资料（BaseData）相关的业务动作时，请务必先查阅 `BaseDataServiceHelper` 是否已有现成的方法。
- 在操作插件里，除非需要准备的字段非常多，否则不要使用 allFields()；优先按实际场景显式准备字段。
- 对查询，优先使用 `QueryUtils` + `AlgoUtils`；不要先写裸 SQL 或循环查库，除非实体查询表达不了。
- 查询基础资料时，优先使用 `BusinessDataServiceHelper.loadFromCache(...)`；不要对基础资料反复用普通查询接口查库。
- 对动态对象取值，优先使用 `DynamicObjectUtils`；不要直接深链式 `get("a.b.c")`。
- 对附件处理，优先使用 `AttachmentUtils` 和 uploader；不要直接散落调用 `AttachmentServiceHelper`。
- `references/base/*` 只用于补齐原生知识、事件签名和缺失能力；不要因为能写原生 API 就绕开现有封装。