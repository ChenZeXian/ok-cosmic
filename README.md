# ok-cosmic

**作者：** 陈泽贤、钱芮名

**注意：仅限内部使用**

面向金蝶云苍穹开发的 AI Skill，提供插件模板、离线 API 知识查询、表单元数据查询与文档增强能力。

---

## 📌 概览

`ok-cosmic` 是一个面向金蝶云苍穹开发的 AI Skill，聚焦表单插件、单据插件、列表插件、操作插件、BOTP 转换、报表插件、工作流、后台任务、OpenAPI 等常见二开场景。它结合本地知识库、文档库、查询脚本、模板骨架和规则约束，帮助 AI 更准确地理解苍穹 SDK、ServiceHelper、元数据字段与插件生命周期，降低 API 幻觉和字段误用风险。

## 🧠 核心能力

- `🧩` 插件选型与模板骨架：支持苍穹常见开发场景的插件选型、模板骨架和开发约束
- `🔎` 离线 API 查询：支持查询类、方法、返回值、参数说明、继承链和方法注释
- `🗂️` 表单元数据查询：支持查询字段标识、引用类型和枚举映射
- `📝` 文档增强回填：支持利用文档库补齐参数名、参数说明和父类/接口方法注释
- `⚡` 紧凑事实输出：支持低噪音输出，便于继续交给 AI 生成代码

## 🚀 快速开始与安装

### 最短路径

- 如果您是首次完整安装，请按 **步骤 1 → 8** 顺序执行
- 如果您已经具备 Java / Python 环境，可直接从 **步骤 2 或步骤 4** 开始
- 如果知识库已经构建完成，只需要在项目中使用，可直接从 **步骤 6** 开始
- 如果您只需要把 Skill 安装到本机多个 Agent，可直接看 **步骤 7**

下面是完整安装与使用流程。

## 1. 环境检查

在开始之前，请确保您的系统满足以下要求：

### 必需环境

| 依赖项 | 版本要求   | 检查命令 | 备注 |
|--------|--------|----------|------|
| Java | JDK 8+ | `java -version` | 必须配置 JAVA_HOME |
| Python | 3.8+   | `python3 --version` | 用于 API 知识查询脚本 |

### 检查清单

```bash
# 检查 Java 版本
java -version

# 检查 Python 版本
python3 --version

# 检查 JAVA_HOME 是否配置（Mac/Linux）
echo $JAVA_HOME

# 检查 JAVA_HOME 是否配置（Windows）
echo %JAVA_HOME%
```

---

## 2. 部署自定义封装库

将 `setup/cuslib/` 目录下的两个 JAR 包部署到苍穹环境：

### 2.1 本地环境部署

将以下 JAR 文件复制到苍穹本地开发环境的 `cus` 目录：

```
setup/cuslib/
├── kd-cd-cosmic-commons.jar   # 公共封装库
└── kd-cd-cosmic-features.jar  # 功能封装库
```

**目标路径示例：**
```bash
# Mac/Linux
cp setup/cuslib/*.jar /path/to/cosmic/cus/

# Windows
copy setup\cuslib\*.jar C:\path\to\cosmic\cus\
```

### 2.2 Dev 线上环境部署

将上述 JAR 包同时部署到 Dev 线上环境，确保封装库在开发调试时可用。

---

## 3. 元数据 API 注册

将自定义接口注册到 Dev 环境的 OpenAPI，用于在线查询表单元数据。

### 3.1 注册接口

在苍穹开发平台中，将以下自定义接口注册到 OpenAPI：

```
kd.cd.feature.ai.mcp.metadata.BillMetadataController
```

**注册步骤：**
1. 登录 Dev 环境苍穹开发平台
2. 进入 **OpenAPI 管理** → **自定义接口**
3. 新增接口，填写控制器类名
4. **推荐使用基本认证（Basic Auth）** 方式注册

### 3.2 获取 API URL

注册成功后，获取元数据 API URL，格式如下：

```
https://deverp.xxxx.com/ierp/kapi/v2/xxxx/xxxx_devportal_ext/runtime/getMetaFields?openApiSign=MkVZSnhFbHBiTFQ5dk4txxxxxxxxxxxc3NjM1MDcyMA==
```

### 3.3 配置 API 地址

将获取的 API URL 填入配置文件 `ok-cosmic.json` 的 `meta.apiUrl` 字段：

```json
{
  "meta": {
    "apiUrl": "https://deverp.xxxx.com/ierp/kapi/v2/xxxx/xxxx_devportal_ext/runtime/getMetaFields?openApiSign=YOUR_SIGN_HERE",
    "timeoutSeconds": 30
  }
}
```

---

## 4. 知识库构建

运行初始化工具构建知识库。setup.jar 已自带，无需额外构建。

**Mac/Linux:**
```bash
cd skills/ok-cosmic/setup
chmod +x setup-mac.sh
./setup-mac.sh -libpath=<项目苍穹JAR库目录>
```

**Windows:**
```cmd
cd skills\ok-cosmic\setup
setup-windows.bat -libpath=<项目苍穹JAR库目录>
```

### 4.1 参数说明

| 参数 | 说明 | 默认值 | 必填 |
|------|------|--------|------|
| `-libpath` | 苍穹 SDK JAR 库目录 | 无 | 推荐 |
| `-docsdbpath` | 文档数据库路径 | `ok-cosmic-docs.db` | 否 |
| `-dbpath` | 输出知识库文件路径 | `ok-cosmic-knowledge.db` | 否 |
| `-jarthreads` | JAR 解析并发线程数 | CPU 核心数 | 否 |
| `-verifyjar` | 校验用 JAR 文件路径 | - | 否 |
| `-verifyclass` | 校验用完整类名 | - | 否 |

### 4.2 典型使用示例

```bash
# 使用默认参数
./setup-mac.sh

# 指定自定义路径
./setup-mac.sh -libpath=/path/to/cosmic/libs

# 使用多线程加速构建
./setup-mac.sh -libpath=./libs -jarthreads=8
```

---

## 5. 知识验证

### 5.1 检查初始构建产物

```bash
# 检查 setup 目录中的初始知识库是否存在
ls -la skills/ok-cosmic/setup/ok-cosmic-knowledge.db

# 检查数据库大小（应大于 0）
du -h skills/ok-cosmic/setup/ok-cosmic-knowledge.db
```

### 5.2 检查项目最终知识库（推荐做法）

如果您在后续步骤中采用“项目根目录维护知识库”的方式，请改为检查项目根目录中的 `ok-cosmic-knowledge.db`：

```bash
ls -la ./ok-cosmic-knowledge.db
du -h ./ok-cosmic-knowledge.db
```

### 5.3 验证知识库

构建完成后，在项目根目录验证知识库是否正常（假设已按步骤 6 将知识库和配置文件放到项目根目录）：

```bash
# 测试 API 知识搜索
python3 skills/ok-cosmic/scripts/cosmic-api-knowledge.py --config ok-cosmic.json search QueryServiceHelper

# 测试元数据查询（需要配置 meta.apiUrl）
python3 skills/ok-cosmic/scripts/cosmic-form-metadata.py --config ok-cosmic.json get --bill-name "物料"
```

---

## 6. 项目配置

在您的苍穹开发项目根目录创建 `ok-cosmic.json` 配置文件：

> 建议（适配本地多项目开发）：
> 将 `ok-cosmic-knowledge.db` 复制或移动到**当前项目根目录**，再让 `ok-cosmic.json` 的 `graph.dbPath` 直接指向项目根目录。
> 这样每个项目都可以维护自己的知识库副本，避免多个本地项目共享同一份数据库时相互干扰。

```json
{
  "graph": {
    "dbPath": "/path/to/your-current-project",
    "dbName": "ok-cosmic-knowledge.db",
    "logFile": "/tmp/cosmic_scripts_graph.log"
  },
  "meta": {
    "apiUrl": "http://your-cosmic-server/api",
    "timeoutSeconds": 30
  }
}
```

例如：

1. 将知识库文件放到项目根目录

```bash
cp /Users/yourname/kingdee/code/cosmic-skill/ok-cosmic-knowledge/ok-cosmic-knowledge.db /path/to/your-current-project/
```

2. 在项目根目录创建 `ok-cosmic.json`

```json
{
  "graph": {
    "dbPath": "/path/to/your-current-project",
    "dbName": "ok-cosmic-knowledge.db"
  },
  "meta": {
    "apiUrl": "http://your-cosmic-server/api",
    "timeoutSeconds": 30
  }
}
```

### 配置说明

| 配置项 | 说明 | 默认值 | 是否必填 |
|--------|------|--------|----------|
| `graph.dbPath` | 知识库数据库存储路径 | 当前项目根目录 | 否 |
| `graph.dbName` | 数据库文件名 | `ok-cosmic-knowledge.db` | 否 |
| `graph.logFile` | 日志文件路径 | `/tmp/cosmic_scripts_graph.log` | 否 |
| `meta.apiUrl` | 元数据 API 服务地址 | 空（离线模式） | 否 |
| `meta.timeoutSeconds` | API 超时时间 | 10 | 否 |

## 7. 安装 Skill

如果您当前的目标只是让多个 Agent 共用同一份本地 Skill，可以直接从本步骤开始。

建议在本机只维护一份 `ok-cosmic` skill 源目录，然后让不同 Agent 的 skill 安装目录通过软链接（或目录联接）指向它。这样后续你只需要更新一处，多个 Agent 都能同步生效。

### 7.1 推荐目录结构

```text
/Users/yourname/skills/ok-cosmic            # 唯一维护的 skill 源目录
~/.claude/skills/ok-cosmic                 # 指向上面的软链接
~/.opencode/skills/ok-cosmic               # 指向上面的软链接
~/.codex/skills/ok-cosmic                  # 指向上面的软链接
~/.gemini/antigravity/skills/ok-cosmic     # 指向上面的软链接
```

### 7.2 macOS / Linux

1. 准备统一维护目录

```bash
mkdir -p ~/skills
cp -R /path/to/ok-cosmic ~/skills/ok-cosmic
```

2. 为不同 Agent 创建软链接

```bash
mkdir -p ~/.claude/skills ~/.opencode/skills ~/.codex/skills ~/.gemini/antigravity/skills
ln -sfn ~/skills/ok-cosmic ~/.claude/skills/ok-cosmic
ln -sfn ~/skills/ok-cosmic ~/.opencode/skills/ok-cosmic
ln -sfn ~/skills/ok-cosmic ~/.codex/skills/ok-cosmic
ln -sfn ~/skills/ok-cosmic ~/.gemini/antigravity/skills/ok-cosmic
```

### 7.3 Windows

1. 准备统一维护目录

```powershell
mkdir $env:USERPROFILE\skills -ErrorAction SilentlyContinue
Copy-Item -Path D:\path\to\ok-cosmic -Destination $env:USERPROFILE\skills\ok-cosmic -Recurse
```

2. 为不同 Agent 创建目录联接（推荐管理员 PowerShell）

```powershell
mkdir $env:USERPROFILE\.claude\skills -ErrorAction SilentlyContinue
mkdir $env:USERPROFILE\.opencode\skills -ErrorAction SilentlyContinue
mkdir $env:USERPROFILE\.codex\skills -ErrorAction SilentlyContinue

cmd /c mklink /J "$env:USERPROFILE\.claude\skills\ok-cosmic" "$env:USERPROFILE\skills\ok-cosmic"
cmd /c mklink /J "$env:USERPROFILE\.opencode\skills\ok-cosmic" "$env:USERPROFILE\skills\ok-cosmic"
cmd /c mklink /J "$env:USERPROFILE\.codex\skills\ok-cosmic" "$env:USERPROFILE\skills\ok-cosmic"
```

### 7.4 维护建议

- 平时只修改统一维护目录中的 skill 源文件
- 如果某个 Agent 已经存在旧目录，先删除旧目录，再创建软链接
- 更新后若 Agent 有缓存，重启对应 Agent 进程或刷新技能列表

---

## 8. 加载 Skill 开始编程

在您的苍穹开发项目中，通过 Agent 加载 ok-cosmic Skill，即可开始 AI 辅助编程。

### 8.1 加载 Skill

在项目根目录下，告诉 Agent 加载技能：

```
加载 ok-cosmic 技能
```

或者直接提出开发需求，Agent 会自动识别并加载相关技能：

```
帮我开发一个销售订单的表单插件，实现字段联动校验
```

### 8.2 开始编程之旅

Skill 加载后，您可以：

| 场景 | 示例指令                        |
|------|-----------------------------|
| 表单插件开发 | "帮我写一个表单插件，实现字段联动"          |
| 操作插件开发 | "开发一个审核操作插件，保存前校验数据"        |
| BOTP 转换开发 | "实现销售订单到出库单的下推转换"           |
| API 查询 | "查询 QueryServiceHelper 的用法" |
| 元数据查询 | "获取物料表单的字段列表（ai生成时会一般自动触发）" |

---

## 🛠️ 常见问题排查

### Q1: Java 版本不兼容

**错误信息:** `Unsupported major.minor version`

**解决方案:** 升级 Java 到 JDK 8 或更高版本

### Q2: 知识库搜索无结果

**可能原因:**
1. 数据库路径配置错误
2. 知识库构建失败或数据为空
3. 搜索关键词过于精确

**排查步骤:**
```bash
# 检查配置文件路径
cat ok-cosmic.json

# 检查数据库文件大小
ls -la skills/ok-cosmic/setup/ok-cosmic-knowledge.db

# 尝试更宽泛的搜索
python3 cosmic-api-knowledge.py --config ok-cosmic.json search Helper --kind helper
```

### Q3: 元数据查询超时

**解决方案:**
1. 增加 `meta.timeoutSeconds` 配置值
2. 检查网络连接是否正常
3. 确认 `meta.apiUrl` 配置正确