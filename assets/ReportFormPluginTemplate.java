package kd.cd.common;

import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.NumberFormatProvider;
import kd.bos.entity.datamodel.events.PackageDataEvent;
import kd.bos.entity.report.FilterInfo;
import kd.bos.entity.report.ReportColumn;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.entity.report.queryds.ReportFilterFieldConfig;
import kd.bos.form.control.events.FilterContainerInitEvent;
import kd.bos.form.events.FilterContainerSearchClickArgs;
import kd.bos.form.field.events.BeforeFilterF7SelectEvent;
import kd.bos.lang.Lang;
import kd.bos.list.column.ListOperationColumnDesc;
import kd.bos.report.ReportList;
import kd.bos.report.events.CreateColumnEvent;
import kd.bos.report.plugin.AbstractReportFormPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 报表界面插件骨架模板（原生 AbstractReportFormPlugin）。
 * 该类仅用于示例写法，生成后请按实际业务删除无用事件并替换占位常量。
 */
public class ReportFormPluginTemplate extends AbstractReportFormPlugin {

    /**
     * 触发时机: 在需要了解当前插件可访问上下文能力时调用。
     * 参数要点: 无入参；仅展示当前插件可通过 this. 访问的方法能力。
     * 典型用途: 作为模板提示，指导在各事件内选择正确的上下文 API。
     */
    private void getContextSample() {
        // this.getView();
        // this.getModel();
        // this.getPageCache();
        // this.getControl("reportlistap");
        // this.getQueryParam();
    }

    private static final String GRID_KEY = "reportlistap";
    private static final String FILTER_ORG = "org";
    private static final String FILTER_BILL_STATUS = "billstatus";

    // ===== 核心事件 =====

    /**
     * 触发时机: 默认查询参数初始化时。
     * 参数要点: queryParam 可设置默认过滤、排序、方案值。
     * 典型用途: 为报表设置默认组织、默认状态等初始查询条件。
     */
    @Override
    public void initDefaultQueryParam(ReportQueryParam queryParam) {
        super.initDefaultQueryParam(queryParam);
        FilterInfo filter = queryParam.getFilter();
        if (filter == null) {
            filter = new FilterInfo();
            queryParam.setFilter(filter);
        }
        filter.addFilterItem("fieldKey", RequestContext.get().getOrgId());
    }

    /**
     * 触发时机: 过滤容器初始化时。
     * 参数要点: contInitEvent 可访问过滤容器定义，queryParam 为当前查询参数。
     * 典型用途: 初始化过滤项默认值、隐藏字段或补充联动字段。
     */
    @Override
    protected void filterContainerInit(FilterContainerInitEvent contInitEvent, ReportQueryParam queryParam) {
        super.filterContainerInit(contInitEvent, queryParam);
        initDefaultQueryParam(queryParam);
    }

    /**
     * 触发时机: 过滤项 F7（基础资料/引用数据选择控件）打开前。
     * 参数要点: args 可追加基础资料过滤条件。
     * 典型用途: 限定 F7（基础资料/引用数据选择控件）可选范围，如只显示启用组织或启用基础资料。
     */
    @Override
    public void filterContainerBeforeF7Select(BeforeFilterF7SelectEvent args) {
        super.filterContainerBeforeF7Select(args);
        args.addCustomQFilter(new kd.bos.orm.query.QFilter("enable", kd.bos.orm.query.QCP.equals, true));
    }

    /**
     * 触发时机: 点击过滤容器“查询”按钮时。
     * 参数要点: args 含当前过滤值。
     * 典型用途: 在查询按钮点击时读取过滤值、补充联动参数或提示信息。
     */
    @Override
    public void filterContainerSearchClick(FilterContainerSearchClickArgs args) {
        super.filterContainerSearchClick(args);
        Object orgValue = args.getFilterValue(FILTER_ORG);
        if (orgValue != null) {
            this.getView().showTipNotification("按组织过滤：" + orgValue);
        }
    }

    /**
     * 触发时机: 查询前校验时。
     * 参数要点: 返回 false 会阻断查询。
     * 典型用途: 避免空条件、超大范围或非法查询进入数据库。
     */
    @Override
    public boolean verifyQuery(ReportQueryParam queryParam) {
        super.verifyQuery(queryParam);
        return queryParam.getFilter() != null;
    }

    /**
     * 触发时机: 报表查询执行前。
     * 参数要点: queryParam 含过滤、分页、排序等上下文。
     * 典型用途: 追加查询过滤条件、设置报表控件状态或修正参数。
     */
    @Override
    public void beforeQuery(ReportQueryParam queryParam) {
        super.beforeQuery(queryParam);
        // 设置取数排序规则
        queryParam.setSortInfo("xxx");
        // 设置自定义参数到取数插件
        queryParam.setCustomParam(new HashMap<>());
        // 设置是否树形报表
        queryParam.setTreeReportList(false);
        // 设置报表过滤条件配置
        queryParam.setReportFilterFieldConfig(new ReportFilterFieldConfig());
        // 设置通用过滤条件
        queryParam.setFilter(new FilterInfo());
        // 设置多语言
        queryParam.setMulLang(Lang.zh_CN);
        // 设置报表自定义过滤条件
        queryParam.setCustomFilter(new ArrayList<>());
        // 设置提示信息
        queryParam.setMessage("查询完成");
    }

    /**
     * 触发时机: 报表查询执行后。
     * 参数要点: queryParam 保留本次查询上下文。
     * 典型用途: 查询完成后输出提示、记录日志或刷新附属区域。
     */
    @Override
    public void afterQuery(ReportQueryParam queryParam) {
        super.afterQuery(queryParam);
        this.getView().showTipNotification("报表查询完成");
    }

    /**
     * 触发时机: 报表列创建后。
     * 参数要点: event 可调整列标题、宽度、隐藏状态等。
     * 典型用途: 动态调整列显示方式或列元数据。
     */
    @Override
    public void afterCreateColumn(CreateColumnEvent event) {
        super.afterCreateColumn(event);
        this.getView().showTipNotification("报表列初始化完成");
    }

    /**
     * 触发时机: 打包单元格数据时。
     * 参数要点: packageDataEvent 含当前行列数据上下文。
     * 典型用途: 对界面展示值做格式化；该事件通常不用于导出逻辑。
     */
    @Override
    public void packageData(PackageDataEvent packageDataEvent) {
        super.packageData(packageDataEvent);
        if (packageDataEvent.getSource() instanceof ReportColumn) {
            DynamicObject dObject = packageDataEvent.getRowData();
            String applyType = dObject.getString("kdec_applytype");
            if(applyType.equals("A")) {
                ReportColumn column = (ReportColumn) packageDataEvent.getSource();
                System.out.println(column.getFieldKey());
                if ("kdec_totalday".equals(column.getFieldKey())) {
                    packageDataEvent.setFormatValue(100);
                }
            }
        }
    }

    /**
     * 触发时机: 行数据返回后、渲染前。
     * 参数要点:
     * - gridPK: 当前报表 grid 标识。
     * - rowData: 查询结果行集合。
     * - queryParam: 查询参数。
     * 典型用途: 对查询结果行做补值、格式修正和显示增强。
     */
    @Override
    public void processRowData(String gridPK, DynamicObjectCollection rowData, ReportQueryParam queryParam) {
        super.processRowData(gridPK, rowData, queryParam);
        if (!GRID_KEY.equals(gridPK)) {
            return;
        }
        for (DynamicObject row : rowData) {
            if (row.get("name") == null) {
                row.set("name", "未命名");
            }
        }
    }

    /**
     * 触发时机: 导出分批数据预处理时。
     * 参数要点: data 为当前待导出的数据集合。
     * 典型用途: 导出前补值、替换导出文本或标准化数值格式。
     */
    @Override
    public void preProcessExportData(List exportColumns,
                                     DynamicObjectCollection data,
                                     NumberFormatProvider numberFormatProvider) {
        super.preProcessExportData(exportColumns, data, numberFormatProvider);
        for (DynamicObject row : data) {
            if (row.get("name") == null) {
                row.set("name", "导出默认名称");
            }
        }
    }

    /**
     * 触发时机: 设置导出 Excel 文件名时。
     * 参数要点: list 为导出文件名片段集合。
     * 典型用途: 自定义导出文件名，便于区分组织、期间或报表类型。
     */
    @Override
    public void setExcelName(List list) {
        super.setExcelName(list);
        list.add("report_export");
    }

    /**
     * 触发时机: 初始化列排序和过滤能力时。
     * 参数要点: allColumns 为全部列定义。
     * 典型用途: 控制列是否支持排序、列头过滤等交互行为。
     */
    @Override
    public void setSortAndFilter(List allColumns) {
        super.setSortAndFilter(allColumns);
        this.getView().showTipNotification("初始化排序与过滤能力");
    }
}
