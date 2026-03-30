# 工作流

## 配置自检（首次必做）

1. AI 必须首先确认项目根目录是否存在 `ok-cosmic.json`。
2. 若缺失，先读取 `<SKILL_ROOT>/scripts/ok-cosmic.json` 示例内容，并明确告知用户需要提供或创建该配置文件。
3. 只有在用户明确同意后，才可在项目根目录创建 `ok-cosmic.json` 模板。

## 零幻觉原则（生成代码前必做）

- **严禁直接写代码**：在生成任何涉及 BOS SDK 的代码前，若本地 `references/` 无确切证据，必须执行脚本查询。
- **上下文对齐**：脚本返回的 Markdown 内容应作为"事实来源"直接注入当前会话。

## 项目级配置文件 (`ok-cosmic.json`)

脚本依赖 `ok-cosmic.json` 获取数据库路径以及 API 地址。

- **推荐指定**: 调用时推荐使用 `--config` 参数指定 ok-cosmic.json 文件地址。脚本也支持从当前目录向上自动搜索 `ok-cosmic.json`，但显式指定更可靠。