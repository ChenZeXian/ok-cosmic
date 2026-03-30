# 日志框架 (Logging Framework)

## 概述
苍穹平台提供了一套统一的日志处理接口，底层默认基于 Logback 实现。它支持标准的日志级别（DEBUG, INFO, WARN, ERROR），并具备异步写入、链路追踪及集群环境下的统一日志收集能力。

## 核心类
- **`kd.bos.logging.LogFactory`**: 获取日志对象的工厂类。
- **`kd.bos.logging.Log`**: 日志执行接口。

## 常用 API 方法
### 获取日志实例
- `LogFactory.getLog(Class<?> clazz)`: 推荐方式，根据类名获取日志对象。
- `LogFactory.getLog(String name)`: 根据标识名获取。

### 级别判断
- `isDebugEnabled()` / `isInfoEnabled()` / `isWarnEnabled()` / `isErrorEnabled()`

### 记录日志
- `debug(String message, Object... args)`
- `info(String message, Object... args)`
- `warn(String message, Object... args)`
- `error(String message, Throwable t)`: 记录错误及异常堆栈。

## 示例代码
```java
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;

public class LogDemo {
    // 1. 定义静态常量日志对象
    private static final Log logger = LogFactory.getLog(LogDemo.class);

    public void doWork(String billNo) {
        // 2. 先判断级别，再输出（减少字符串拼接开销）
        if (logger.isInfoEnabled()) {
            logger.info("开始处理单据: {}", billNo);
        }

        try {
            process();
        } catch (Exception e) {
            // 3. 记录异常堆栈
            logger.error("单据处理失败: " + billNo, e);
        }
    }
}
```

## 实践建议
1. **先判断后输出**：输出 DEBUG 或较长的 INFO 日志时，务必包裹在 `isXXXEnabled()` 判断中，以避免在高并发下产生大量的字符串计算开销。
2. **占位符写法**：推荐使用 `{}` 占位符，而不是手写字符串拼接。
3. **异常完整记录**：在 `error` 级别中，务必将异常对象 `e` 作为最后一个参数传入，以便记录完整的堆栈信息。

## 常见坑位
1. **`System.out` 滥用**：严禁在生产代码中使用 `System.out.println`，这些输出无法被集中收集和管理。
2. **循环内输出**：避免在处理几万行的循环内输出大量的日志，这会导致严重的 IO 瓶颈。
3. **敏感信息泄露**：记录日志时注意脱敏，避免将用户的密码、个人手机号等敏感信息直接打入日志。
