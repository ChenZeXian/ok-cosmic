# 生成后自动校验规则 (Post-Lint)

## 触发条件

**每次 AI 生成或修改 `.java` 文件后，自动触发**，无需用户手动请求。

## 执行命令

```bash
python3 <SKILL_ROOT>/scripts/cosmic-post-lint.py <生成的文件或目录> --fix-hint
```

## 校验流程

```
生成代码 ──→ 执行 lint ──→ 有 ERROR? ──→ 是 → 自动修复 → 重新 lint（最多 3 轮）
                              │
                              └→ 否 → 有 WARNING? → 优先修复 / 注释保留理由
                                           │
                                           └→ 否 → ✅ 通过，交付代码
```

## 问题级别处理策略

| 级别 | 处理方式 | 是否阻断 |
|------|----------|----------|
| ❌ ERROR | 必须修复，根据 fix-hint 立即调整代码 | **是** |
| ⚠️ WARNING | 优先修复；若有合理理由可保留，须加注释说明 | 否 |
| 💡 INFO | 建议项，按团队风格决定 | 否 |

## 规则 ID 索引

| ID 前缀 | 类别 | 来源文件 |
|----------|------|----------|
| `HAL-METHOD-*` | 幻觉方法名 | anti-patterns.md |
| `HAL-CLASS-*` | 幻觉类名 | anti-patterns.md |
| `SCENE-*` | 场景错配 | anti-patterns.md |
| `STYLE-*` | 编码偏好 | coding-preferences.md |
| `RESOURCE-*` | 资源管理 | anti-patterns.md |
| `VERIFY-*` | 验证注释 | constraints.md |

## 修复示例

当收到如下 lint 报告时：

```
❌ L 31 [HAL-METHOD-001] 苍穹不存在 setReadOnly() 方法
   > dataEntity.setReadOnly(true);
   💊 修复: 使用 getView().setEnable(false, "key")
```

AI 应：
1. 将 `dataEntity.setReadOnly(true)` 替换为 `getView().setEnable(false, "fieldKey")`
2. 重新执行 lint 确认该 ERROR 消失
3. 检查修复是否引入新问题

## 重试上限

- 单个文件最多执行 **3 轮** "修复→复检" 循环
- 3 轮后仍有 ERROR 未消除，停止自动修复，向用户报告剩余问题清单并请求人工介入
