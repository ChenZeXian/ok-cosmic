# 文件服务 (File Service)

## 概述
文件服务用于在苍穹平台中存储、下载和管理物理文件（如单据附件、单据图片）。它支持高可用部署，并提供统一的 SDK 接口，屏蔽了底层存储介质（文件服务器、S3、OSS 等）的差异。

## 核心类
- **`kd.bos.fileservice.FileServiceFactory`**: 获取文件服务的工厂类。
- **`kd.bos.fileservice.FileService`**: 执行文件操作的核心接口。
- **`kd.bos.fileservice.FileItem`**: 代表一个待上传的文件对象（包含文件名、输入流等）。

## 常用 API 方法
### 获取服务
- `FileServiceFactory.getAttachmentFileService()`: 获取通用的附件存储服务。
- `FileServiceFactory.getImageFileService()`: 获取专用的图片存储服务。

### 文件操作
- `upload(FileItem item)`: 上传文件，返回文件在服务器上的唯一 URL（路径）。
- `download(String url)`: 下载文件，返回文件字节数组。
- `getInputStream(String url)`: 获取文件的输入流（适用于大文件）。
- `delete(String url)`: 物理删除服务器上的文件。
- `preview(String url)`: 获取文件预览地址。

## 示例代码
```java
import kd.bos.fileservice.FileService;
import kd.bos.fileservice.FileServiceFactory;
import kd.bos.fileservice.FileItem;
import java.io.InputStream;

public class FileDemo {
    public String uploadFile(String fileName, InputStream is) {
        // 1. 获取附件服务
        FileService fs = FileServiceFactory.getAttachmentFileService();
        
        // 2. 构造上传对象
        FileItem item = new FileItem(fileName, is);
        
        // 3. 执行上传并返回 URL
        return fs.upload(item);
    }

    public void deleteFile(String fileUrl) {
        FileService fs = FileServiceFactory.getAttachmentFileService();
        fs.delete(fileUrl);
    }
}
```

## 实践建议
1. **区分场景**：普通的业务附件使用 `getAttachmentFileService`；如果是单据上的头像、商品图片，建议使用 `getImageFileService`，以便平台执行特定的缩略图优化。
2. **资源释放**：通过 `getInputStream` 获取流后，务必在 `finally` 块或 `try-with-resources` 中手动关闭。
3. **URL 存储**：上传成功返回的 `url` 是访问该文件的唯一凭证，应将其保存到单据对应的附件表或字段中。

## 常见坑位
1. **文件后缀**：上传时 `FileItem` 的文件名后缀应准确，否则可能导致预览（`preview`）功能失效。
2. **URL 格式**：存储和传递 URL 时，不要随意拼凑，应保持 `fs.upload` 返回的原始字符串。
3. **权限控制**：物理删除（`delete`）操作不可逆，执行前务必在业务层做好权限校验。
