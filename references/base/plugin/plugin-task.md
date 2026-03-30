# 后台任务插件

## 概述
后台任务插件用于调度作业中的异步任务执行，适合定时批处理、夜间同步、批量修复等场景。

> **适用边界**
> ✅ 本文档直接使用：后台任务插件无封装层，直接参考本文档。

## 核心基类


- 基类：`kd.bos.schedule.executor.AbstractTask`
- 继承关系：`AbstractTask implements Task`

## 核心事件

- `execute(RequestContext ctx, Map<String, Object> params)`：调度中心触发任务执行时进入，承载任务主逻辑。
- `stop()`：调度中心主动停止任务时触发，用于安全退出与资源释放。

## 插件内上下文方法

这些方法更适合作为任务代码里主动调用的上下文能力，不建议继续按“事件”理解：

- `feedbackProgress(...)`：回传进度信息。
- `feedbackCustomdata(...)`：回传自定义运行数据。
- `checkIsStop()`：主动检查是否已收到停止指令。
- `isStop()`：读取当前停止标记。
- `getMessageHandler()`：获取调度消息处理器。
- `setTaskId(String)` / `getTaskId()`：访问任务标识。
- `isSupportReSchedule()`：判断是否支持重新调度。

```java
this.feedbackProgress(20, "开始执行", null);
this.checkIsStop();
this.feedbackCustomdata(java.util.Collections.singletonMap("phase", "load-data"));
```

## 示例代码

示例代码统一维护在模板文件中，直接参考：

- [TaskTemplate.java](../../../assets/TaskTemplate.java)

## 实践建议

1. 长任务应分阶段调用 `feedbackProgress(...)`。
2. 每个大步骤后主动调用 `checkIsStop()`，避免无法及时响应停止。
3. `params` 先做容错校验，避免任务启动即异常。
4. 任务逻辑尽量保证幂等，防止重跑产生重复副作用。

## 常见坑位

- 把 `feedbackProgress`、`feedbackCustomdata` 当成“自动触发事件”，导致代码放错位置。
- 长任务完全不检查 `checkIsStop()`，停止指令无法及时生效。
- 单次处理全量数据，导致内存和事务过大。
- 异常被吞掉，调度侧显示成功但业务未完成。