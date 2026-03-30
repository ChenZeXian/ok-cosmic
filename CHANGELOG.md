# Changelog

## [1.1.1] - 2026-03-30

### Added
- `manifest.json`: 注册 `cosmic_search_methods` 工具，支持全局方法检索
- `README.md`: 增加更清晰的发布页结构，包括概览、核心能力、最短路径与安装说明

### Changed
- `README.md`: 将安装向导内容重组为正式 README，并补充多 Agent 安装方式
- `README.md`: 修正文档结构、步骤编号与知识库验证说明，提升可读性
- `skills/ok-cosmic/setup/ok-cosmic-docs.db`: 更新为最新构建产物
- `build-doc-db.sh`: 构建完成后切换为 `journal_mode=DELETE` 并执行 `VACUUM`

### Fixed
- Windows 兼容性：`setup-windows.bat` 使用 UTF-8 BOM，降低中文乱码概率
- 包体积：docs 库重新压实并同步到发布包，发布包重新打包为当前版本
- 目录一致性：发布文档、模板命名与安装说明进一步对齐

## [1.1.0] - 2026-03-28

### Added
- `scripts/config_loader.py`: 统一配置加载模块，两个脚本共用
- SKILL.md: 新增场景化 snippets 映射表（18 个 snippet 全覆盖）
- SKILL.md: 决策矩阵拆分左树右表为单据/基础资料两种模板
- manifest.json: 补充 `assets/snippets/` 和 MCP Server 声明
- CHANGELOG.md: 新增版本变更记录
- references/adv/*.md: 每篇文档新增"适用边界"段落
- `README.md`: 新增概览、核心能力、快速开始与安装、安装 Skill 章节
- `README.md`: 新增多 Agent 软链接/目录联接安装示例（macOS/Linux/Windows）
- `scripts/cosmic-api-knowledge.py`: 新增 `detail --compact` 模式
- `scripts/cosmic-api-knowledge.py`: 增强详情输出，支持参数说明、返回说明结构化展示
- `scripts/cosmic-form-metadata.py` / `scripts/cosmic-api-knowledge.py`: 新增脚本头部用途/边界说明
- `ok-cosmic-knowledge`: 增强参数名回填、`@param` 保留、父类/接口方法注释下沉
- `skills/ok-cosmic/setup/ok-cosmic-docs.db`: 更新为最新文档库构建产物
- `scripts/cosmic-post-lint.py`: 代码生成后自动校验入口（17→32 规则）
- `scripts/lint/hal_check.py`: 幻觉方法名（10 条 ERROR）+ 幻觉类名（5 条 ERROR）检查
- `scripts/lint/scene_check.py`: 场景错配检查（5 条 WARNING）
- `scripts/lint/style_check.py`: 编码偏好检查（7 条 WARNING/INFO）
- `scripts/lint/resource_check.py`: 资源管理检查（2 条 WARNING）
- `scripts/lint/verify_check.py`: 验证注释检查（1 条 INFO，仅 --strict）
- `rules/post-lint.md`: 校验流程、级别策略、修复示例、重试上限
- `manifest.json`: 注册 `cosmic_post_lint` 工具
- `.gitignore`: 排除 `.DS_Store` 和 `__pycache__`
- `mcp/requirements.txt`: 声明 MCP Server 依赖

### Changed
- `cosmic-api-knowledge.py`: 改用 `config_loader.load_project_config`，移除内联实现
- `cosmic-form-metadata.py`: 改用 `config_loader.load_project_config`，移除内联实现
- `cosmic-form-metadata.py`: `FormMetadata.__init__` 使用 `__config_dir__` 解析相对路径
- `cosmic-form-metadata.py`: 裸 `except` 修复为 `except Exception`，SSL 可通过配置关闭
- `cosmic-api-knowledge.py`: `ApiGraph._get_conn()` 新增连接缓存，避免重复创建
- `README.md`: 调整章节结构、修复知识库构建代码块、统一项目配置与安装路径说明
- `SKILL.md`: 快速决策矩阵中的模板文件改为直链，并同步新增 `--compact` 用法说明
- `assets/`: 模板文件名移除 `Abstract` 前缀，模板类名与文件名统一
- `setup-windows.bat`: 转为 UTF-8 BOM，降低 Windows `cmd` 中文乱码概率
- `build-doc-db.sh`: 构建完成后切换 `journal_mode=DELETE` 并执行 `VACUUM`
- `ok-cosmic-knowledge/pom.xml`: shade 打包时仅保留 mac Intel/mac Apple Silicon/Windows x64/Windows Arm64 的 sqlite-jdbc native 库
- `setup-mac.sh`: 新增 Java 命令前置检查
- `cheat-sheet.md`: 各 section 添加 `[表单]` `[操作]` 等场景标签
- `workflow.md`: `--config` 从"强制"改为"推荐"，匹配自动搜索能力
- `coding-preferences.md`: 修正元数据截断行数 50→120
- `plugin-dev-flow.md`: 移除不存在的外部路径引用
- `agents/openai.yaml`: 丰富 default_prompt 为完整决策流程
- `references/base/plugin/*.md`: 14 篇文档全部新增"适用边界"块
- `SKILL.md`: 最短决策路径新增第 6 步（post-lint 校验）
- `SKILL.md`: 底部新增「代码生成后自动校验」完整说明
- `rules/plugin-dev-flow.md`: 新增第 8 步（自动校验）
- `agents/openai.yaml`: prompt 补充 post-lint 第 6 步

### Fixed
- 两个脚本的 `load_project_config` 搜索策略不一致（已统一）
- `FormMetadata` 相对路径解析与 `ApiGraph` 行为不一致（已对齐）
- `cosmic-api-knowledge.py`: `search-method` 排序和输出降噪，优先展示强相关方法
- `cosmic-api-knowledge.py`: `detail` 输出去除重复字段，降低 token 噪音
- `ok-cosmic-docs.db`: 保留 `@param/@return` 说明，修复部分方法注释截断问题
- `ok-cosmic-knowledge.db`: 修复部分 `arg0/arg1` 参数名未从 docs 回填的问题
- `setup-mac.sh`: 修正不存在的 `build.sh` 引用
- `README.md`: 移除已废弃的 `-docthreads` 参数
- `README.md`: 第 5 步验证命令改用项目根目录 `ok-cosmic.json`
- `Main.java`: 移除未使用的 `docThreads` 变量及 CLI 解析
- `cosmic-form-metadata.py`: 修复最后一处裸 `except`
- 清理 4 个 `.DS_Store` 文件

## [1.0.0] - 初始版本

- 完整的插件模板体系（15 个模板）
- 离线 API 知识图谱查询
- 表单元数据在线查询与本地缓存
- 规则约束与反模式清单
- 场景化代码片段
