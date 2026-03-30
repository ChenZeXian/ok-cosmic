package kd.cd.common;


import kd.bos.print.core.plugin.AbstractPrintPlugin;
import kd.bos.print.core.plugin.event.AfterOutputWidgetEvent;
import kd.bos.print.core.plugin.event.BeforeLoadDataEvent;
import kd.bos.print.core.plugin.event.BeforeOutputWidgetEvent;
import kd.bos.print.core.plugin.event.CustomDataLoadEvent;

import java.util.List;
import java.util.Map;

/**
 * 打印插件骨架模板（原生 AbstractPrintPlugin）。
 * 该类仅用于示例写法，生成后请按实际业务删除无用事件并替换占位常量。
 */
public class PrintPluginTemplate extends AbstractPrintPlugin {

    /**
     * 触发时机: 在需要了解当前插件可访问上下文能力时调用。
     * 参数要点: 无入参；仅展示当前插件可通过 this. 访问的方法能力。
     * 典型用途: 作为模板提示，指导在各事件内选择正确的上下文 API。
     */
    private void getContextSample() {
        // this.getMainDataVisitor();
        // this.getDataVisitor("ds_main");
        // this.getPrintSetting();
        // this.getExtParam();
        // this.getTplInfo();
        // this.isPreview();
    }

    /**
     * 触发时机: 打印引擎读取数据前。
     * 参数要点: evt 可取消默认数据加载；取消后通常需在自定义数据事件中重新提供数据。
     * 典型用途: 拦截默认打印取数，切换成自定义数据包来源。
     */
    @Override
    public void beforeLoadData(BeforeLoadDataEvent evt) {
        super.beforeLoadData(evt);
        if ("ds_custom".equals(evt.getDataSource().getDsName())) {
            evt.setCancleLoadData(true);
        }
    }

    /**
     * 触发时机: 自定义数据源加载数据时。
     * 参数要点: evt 含数据源标识、过滤条件与当前数据包。
     * 典型用途: 构造自定义打印数据、加工默认读取的数据集合。
     */
    @Override
    public void loadCustomData(CustomDataLoadEvent evt) {
        super.loadCustomData(evt);
        if (!"ds_custom".equals(evt.getDataSource().getDsName())) {
            return;
        }
        List rows = evt.getCustomDataRows();
        if (rows != null) {
            rows.clear();
        }
        Map extParam = this.getExtParam();
        if (extParam != null) {
            extParam.put("customDataLoaded", Boolean.TRUE);
            extParam.put("customDataSource", evt.getDataSource().getDsName());
        }
    }

    /**
     * 触发时机: 打印控件输出前。
     * 参数要点: evt 可读取控件标识、当前输出值并在输出前改写。
     * 典型用途: 格式化文本、替换图片地址、控制单元格输出内容。
     */
    @Override
    public void beforeOutputWidget(BeforeOutputWidgetEvent evt) {
        super.beforeOutputWidget(evt);
        Map extParam = this.getExtParam();
        if (extParam != null) {
            extParam.put("beforeOutputWidget", evt.getWidgetKey());
        }
    }

    /**
     * 触发时机: 打印控件输出后。
     * 参数要点: evt 可读取输出结果并做后置调整。
     * 典型用途: 记录输出过程、对合并行列或后置格式做补充处理。
     */
    @Override
    public void afterOutputWidget(AfterOutputWidgetEvent evt) {
        super.afterOutputWidget(evt);
        Map extParam = this.getExtParam();
        if (extParam != null) {
            extParam.put("afterOutputWidget", evt.getWidgetKey());
        }
    }
}
