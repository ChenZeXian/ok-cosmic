# 附件与文件系统工具 (AttachmentUtils)

## 概述
苍穹的附件系统涉及物理文件、URL 签名与业务面板绑定。`AttachmentUtils` (位于 `kd.cd.common.attachment`) 旨在简化跨单据附件同步、文件在线下载以及附件面板的全方位管理。本规范支持所有涉及文件流的操作场景。

> **适用边界**
> ✅ 适用：附件上传/下载/复制/同步/面板绑定。
> ❌ 不适用：纯文件流操作（无业务面板）请直接用 SDK `FileServiceHelper`。

## 核心类
- **`kd.cd.common.attachment.AttachmentUtils`**: **附件处理核心工具类**。
- **`kd.bos.servicehelper.AttachmentServiceHelper`**: 原生附件服务帮助类。
- **`kd.bos.servicehelper.AttDto`**: 附件数据传输对象。
- **`kd.cd.common.attachment.CopyFeature`**: 附件复制选项配置。
- **`kd.cd.common.attachment.BindType`**: 绑定类型枚举。

## 常用 API 方法

### 1. 附件面板复制与绑定
- `copyToPanel(String srcFormId, Object srcPk, String srcPanel, String tarFormId, Object tarPk, String tarPanel)`: **最常用**。实现附件面板的全量文件复制。
- `copyToPanel(String srcFormId, Object srcPk, String srcPanel, String tarFormId, Object tarPk, String tarPanel, CopyFeature feature)`: 带复制选项的附件面板复制。
- `copyToPanel(Collection<Long> attPks, String tarFormId, Object tarPk, String tarPanel)`: 根据附件主键集复制。
- `copyToPanel(Collection<Long> attPks, String tarFormId, Object tarPk, String tarPanel, CopyFeature feature)`: 根据附件主键集复制，带复制选项。
- `bind(String entityId, Object pkValue, List<AttDto> attDtos)`: 将上传的文件正式绑定到具体单据主键（转正）。
- `bindSingle(String entityId, Object pkValue, AttDto attDto)`: 绑定单个文件。

### 2. 文件解析与操作
- `download(String pathOrUrl)`: 将 URL 下载并转换为 `InputStream` 流。**调用方必须负责关闭返回的 InputStream**。
- `physicDelete(String... pathOrUrls)`: 物理删除文件（兼容文件服务器或文件缓存删除）。
- `getFileName(String path)`: 获取文件名。
- `getSuffix(String str)`: 获取后缀。
- `isValidPath(String path)`: 判定是否为有效路径。

### 3. URL 逻辑判定
- `isDownloadUrl(String path)`: 判定是否为下载链接。
- `isTempUrl(String path)`: 判定是否为临时存储区链接。
- `isImageType(String str)`: 判定是否为图片。

### 4. 辅助方法
- `newAttDto(String panelOrField, String path, long size, Object entryRowPk, Consumer<AttDto> consumer)`: 生成附件信息Dto。
- `stripFromDownloadUrl(String url)`: 从下载URL中提取路径。
- `decodeUtf8(String path)`: UTF-8解码。

## 示例代码

### 自动同步订单附件到入库单
```java
package kd.cd.common.demo;

import kd.cd.common.attachment.AttachmentUtils;

public class AttDemo {
    public void sync(String orderPk, String inbillPk) {
        // 1. 同步全量附件面板内容，内部已自动处理物理引用
        AttachmentUtils.copyToPanel(
            "pm_purorderbill", orderPk, "att_panel",
            "stk_purinbill", inbillPk, "att_panel"
        );
    }
}
```

### 下载并处理附件
```java
public void processAttachment(String fileUrl) {
    // 使用 try-with-resources 确保流关闭
    try (InputStream is = AttachmentUtils.download(fileUrl)) {
        // 处理文件流
    } catch (IOException e) {
        // 异常处理
    }
}
```

### 生成并绑定附件Dto
```java
public void bindNewAttachment(String entityId, Object pkValue, String path, long size) {
    AttDto attDto = AttachmentUtils.newAttDto("att_panel", path, size, null, dto -> {
        dto.setName("附件名称");
    });
    State result = AttachmentUtils.bindSingle(entityId, pkValue, attDto);
    if (!result.isSuccess()) {
        throw new RuntimeException(result.getMessage());
    }
}
```

## CopyFeature 配置选项
`CopyFeature` 用于控制附件复制时的行为：
- `setCreatorId(Long)` - 设置创建人ID
- `setUseCurrentUserAsCreator(boolean)` - 是否使用当前用户作为创建人
- `setCopyLastModifyTime(boolean)` - 是否复制最后修改时间
- `setUseCurrentUserAsModifier(boolean)` - 是否使用当前用户作为修改人
- `setFileSource(String)` - 设置文件来源
- `setClearDesc(boolean)` - 是否清除描述

## 实践建议
1. **优先使用面板复制**: 不要手动解析附件子表获取 URL 拼接。使用 `copyToPanel` 最稳健，且性能最优。
2. **区分正式区与临时区**: 前端上传的文件位于临时区，如果不调用 `bind` 绑定到单据，系统会在一定时间后自动清理。
3. **文件名转义**: 处理含中文字符的文件名时，必须确认编码环境，防止出现乱码。
4. **流关闭**: `download` 方法返回的 InputStream 必须由调用方关闭，建议使用 try-with-resources。

## 常见坑位
1. **物理删除误伤**: `physicDelete` 会立刻销毁物理文件，如果该文件被多个单据引用，可能导致其他单据附件失效。
2. **下载 URL 超时**: 直接将 `download` 获取的 InputStream 返回前端时，如果流未及时关闭，可能导致连接池溢出。
3. **附件面板标识错误**: `copyToPanel` 的参数是面板的 **Key**，而非字段 Key，请在设计器属性栏确认。
4. **流未关闭**: 忘记关闭 `download` 返回的 InputStream 会导致文件句柄泄露或底层连接池耗尽。