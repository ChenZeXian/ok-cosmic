package kd.cd.common;

import kd.bos.algo.DataSet;
import kd.bos.entity.report.*;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;

import java.util.List;

/**
 * 报表取数插件骨架模板（原生 AbstractReportListDataPlugin）。
 * 该类仅用于示例写法，生成后请按实际业务删除无用事件并替换占位常量。
 */
public class ReportListDataPluginTemplate extends AbstractReportListDataPlugin {

    /**
     * 触发时机: 在需要了解当前插件可访问上下文能力时调用。
     * 参数要点: 无入参；仅展示当前插件可通过 this. 访问的方法能力。
     * 典型用途: 作为模板提示，指导在各事件内选择正确的上下文 API。
     */
    private void getContextSample() {
        // this.getQueryParam();
        // this.getSelectedObj();
        // this.setProgress(30);
    }

    private static final String QUERY_KEY = "report-query";
    private static final String ENTITY_NAME = "entity_name";
    private static final String SELECT_FIELDS = "id,billno,name";
    private static final String ORDER_BY = "id desc";
    private static final String FIELD_BILL_NO = "billno";

    // ===== 核心事件 =====

    /**
     * 触发时机: 报表发起查询时。
     * 参数要点:
     * - queryParam.getFilter() 为过滤面板条件。
     * - selectedObj 为左侧树或左表选中对象。
     * 典型用途: 按查询条件与左侧选中对象组装 DataSet。
     */
    @Override
    public DataSet query(ReportQueryParam queryParam, Object selectedObj) {
        super.query(queryParam, selectedObj);
        FilterInfo filter = queryParam.getFilter();
        int top = 1000;
        QFilter[] filters = filter == null ? null : filter.getQFilters().toArray(new QFilter[0]);
        this.setProgress(20);
        return QueryServiceHelper.queryDataSet(QUERY_KEY, ENTITY_NAME, SELECT_FIELDS, filters, ORDER_BY, top);
    }

    /**
     * 触发时机: 报表初始化列定义后。
     * 参数要点: columns 为系统生成的列集合，可调整显示列属性。
     * 典型用途: 动态隐藏列、修改列宽、冻结列或调整显示顺序。
     */
    @Override
    public List<AbstractReportColumn> getColumns(List<AbstractReportColumn> columns) {
        super.getColumns(columns);
        for (int i = 0; i < columns.size(); i++) {
            ReportColumn rColumn = (ReportColumn) columns.get(i);
            String key = rColumn.getFieldKey();
            if (key.equals("textfield")) {
                rColumn.setFreeze(true);
                columns.set(i, rColumn);
            }
        }
        return columns;
    }
}
