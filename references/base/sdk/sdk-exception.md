# 异常处理 (Exception Handling)

## 概述
苍穹平台提供了统一的异常处理规范，要求所有的业务和系统异常均通过 `KDException` 或其子类进行表达。核心原则是：统一异常类型、错误码全局唯一、多语言支持以及精准的异常捕获。

## 核心类
- **`kd.bos.exception.KDException`**: 平台所有异常的基类。
- **`kd.bos.exception.KDBizException`**: 业务逻辑异常（通常直接继承自 `KDException`，用于在前端显示友好提示）。
- **`kd.bos.exception.ErrorCode`**: 错误码定义类，包含唯一的错误代码和多语言消息模板。

## 常用 API 方法
- `new ErrorCode(String code, String message)`: 构造错误码。格式建议：`云.应用.变量名`。
- `new KDException(ErrorCode code, Object... args)`: 抛出带错误码和格式化参数的异常。
- `new KDException(Throwable cause, ErrorCode code, Object... args)`: 封装原始异常并抛出。
- `exception.getErrorCode()`: 获取异常中的错误码对象。

## 示例代码
### 1. 定义错误码类
```java
public class MyModuleErrorCode {
    private static ErrorCode create(String code, String message) {
        return new ErrorCode("my.module." + code, message);
    }
    // 使用 %s 作为动态参数占位符
    public final static ErrorCode orderNotFound = create("orderNotFound", "订单【%s】不存在或已删除。");
    public final static ErrorCode dataProcessError = create("dataProcessError", "数据处理失败：%s");
}
```

### 2. 抛出与捕获异常
```java
public void processOrder(String billNo) {
    if (StringUtils.isBlank(billNo)) {
        // 直接抛出业务异常
        throw new KDBizException(MyModuleErrorCode.orderNotFound, billNo);
    }

    try {
        doComplexTask();
    } catch (KDException e) {
        // 关注的业务异常，再次往上抛（无需记录日志，平台会自动处理显示）
        throw e;
    } catch (Exception e) {
        // 系统级异常（如网络、IO），封装为业务异常再抛出，并可在此记录日志
        throw new KDException(e, MyModuleErrorCode.dataProcessError, e.getMessage());
    }
}
```

## 实践建议
1. **业务异常优先**：业务逻辑校验失败时，应优先抛出 `KDBizException`，这样前端会自动拦截并以友好弹窗形式展示 `message`。
2. **错误码规范**：错误码应保持产品全局唯一，且对应的消息应在多语言资源文件中配置。
3. **按需捕获**：只 catch 那些你真正需要处理（如回滚、补偿、转换）的异常，其他异常任其向上抛出到框架层统一处理。

## 常见坑位
1. **吞掉异常不记录日志**：如果 catch 了异常且没有再次抛出，必须使用 `LogFactory` 记录详细堆栈，否则问题将极难定位。
2. **UI 显示非业务语言**：严禁直接将 `SQLException` 等底层堆栈信息抛给前端展示，必须封装为用户可理解的业务语义。
3. **乱用 Exception 基类**：尽量避免直接 `throw new Exception()`，这会导致框架无法精准区分系统错误和业务校验。
