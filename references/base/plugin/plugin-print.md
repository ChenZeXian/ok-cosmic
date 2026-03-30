# 打印插件

## 概述
打印插件用于控制打印数据加载、控件输出前后加工以及自定义数据源取数。

> **适用边界**
> ✅ 本文档直接使用：打印插件无封装层，直接参考本文档。

## 核心基类
- 基类：`kd.bos.print.core.plugin.AbstractPrintPlugin`
- 继承关系：`AbstractPrintPlugin implements IPrintPlugin`

## 核心事件

- `beforeLoadData(BeforeLoadDataEvent evt)`：打印数据加载前触发，可取消默认取数。
- `loadCustomData(CustomDataLoadEvent evt)`：自定义数据源加载数据时触发。
- `beforeOutputWidget(BeforeOutputWidgetEvent evt)`：控件输出前触发。
- `afterOutputWidget(AfterOutputWidgetEvent evt)`：控件输出后触发。

## 插件内上下文方法

以下更适合作为打印插件内主动访问的上下文能力：

- `getMainDataVisitor()`
- `getDataVisitor(String dataSource)`
- `getPrintSetting()`
- `getExtParam()`
- `getTplInfo()`
- `isPreview()`

```java
Map extParam = this.getExtParam();
Object tplInfo = this.getTplInfo();
boolean preview = this.isPreview();
```

## 其他扩展点

- `parseRichImg(...)`：富文本图片解析扩展。
- `setExtParam(...)` 等 getter/setter：上下文读写接口，不建议写成事件说明。

## 示例代码

示例代码统一维护在模板文件中，直接参考：

- [PrintPluginTemplate.java](../../../assets/PrintPluginTemplate.java)

## 实践建议

1. 取消默认取数后，要在 `loadCustomData(...)` 中补回数据。
2. 输出前后事件更适合做格式化和控件值调整，不要承载重型查询。
3. 上下文 getter/setter 保持在模板提示里即可，不必按事件去展开。
4. 自定义数据源场景优先让模板和数据源标识一一对应。

## 常见坑位

- 把 `getExtParam()`、`getPrintSetting()` 这类上下文访问方法写成事件。
- 取消了默认取数却没有补自定义数据，导致打印空白。
- 在输出前后事件里执行过重逻辑，拖慢打印性能。