/**
 * QueryUtils.queryDataSet(...) + DataSet 聚合示例。
 * <p>
 * 使用场景：在表单/报表中按条件查询一批单据，
 * 既要拿最新一条记录，也要顺手做金额求和、主键收集。
 * 进阶场景：多 DataSet 按维度 groupBy / leftJoin 后，再做差额统计和动态排序。
 */
package kd.cd.common.snippets.query;

import kd.bos.algo.DataSet;
import kd.bos.algo.Row;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.cd.common.plugin.AbstractFormPluginExt;
import kd.cd.common.util.AlgoUtils;
import kd.cd.common.util.QueryUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class DataSetQueryStatSample extends AbstractFormPluginExt {
    private static final String PUR_ORDER_FORM_ID = "pm_purorderbill";
    private static final String AP_INVOICE_FORM_ID = "ap_invoice";

    // --- 查询已审核采购订单金额汇总，并取最新一条单据 ---
    public BigDecimal queryAuditedOrderTotal(String supplierNumber, Object orgId) {
        QFilter filter = new QFilter("billstatus", QCP.equals, "C")
                .and(new QFilter("supplier.number", QCP.equals, supplierNumber))
                .and(new QFilter("org.id", QCP.equals, orgId));

        try (DataSet ds = QueryUtils.queryDataSet(PUR_ORDER_FORM_ID, "id,billno,amountandtax", "billno desc", 200, filter.toArray())) {
            BigDecimal totalAmount = AlgoUtils.sumOf(ds.copy(), "amountandtax");
            if (ds.hasNext()) {
                Row latestRow = ds.next();
                getView().showTipNotification("最新单据：" + latestRow.getString("billno"));
            }
            return totalAmount == null ? BigDecimal.ZERO : totalAmount;
        }
    }

    // --- 只收集满足条件的主键集合 ---
    public Set<Object> queryAuditedOrderIds(Object orgId) {
        QFilter filter = new QFilter("billstatus", QCP.equals, "C")
                .and(new QFilter("org.id", QCP.equals, orgId));
        try (DataSet ds = QueryUtils.queryDataSet(
                PUR_ORDER_FORM_ID,
                "id",
                filter.toArray())
        ) {
            Set<Object> ids = AlgoUtils.setOf(ds, "id");
            return ids == null ? Collections.emptySet() : ids;
        }
    }

    // --- 按供应商汇总订单金额和发票金额，再计算未开票差额 ---
    public BigDecimal queryUninvoicedAmount(Object orgId) {
        QFilter filter = new QFilter("billstatus", QCP.equals, "C")
                .and(new QFilter("org.id", QCP.equals, orgId));
        try (DataSet orderDs = QueryUtils.queryDataSet(
                PUR_ORDER_FORM_ID,
                "supplier.id supplierId,amountandtax",
                filter.toArray()
        );
             DataSet invoiceDs = QueryUtils.queryDataSet(
                     AP_INVOICE_FORM_ID,
                     "receivablessupp.id supplierId,pricetaxtotal invoicedamount",
                     filter.toArray()
             );
             DataSet invoiceSum = invoiceDs.groupBy(new String[]{"supplierId"}).sum("invoicedamount").finish();
             DataSet joined = orderDs.leftJoin(invoiceSum)
                     .on("supplierId", "supplierId")
                     .select("supplierId", "amountandtax", "invoicedamount")
                     .finish()
        ) {
            BigDecimal gap = BigDecimal.ZERO;
            for (Row row : joined) {
                BigDecimal orderAmount = row.getBigDecimal("amountandtax") == null ? BigDecimal.ZERO : row.getBigDecimal("amountandtax");
                BigDecimal invoicedAmount = row.getBigDecimal("invoicedamount") == null ? BigDecimal.ZERO : row.getBigDecimal("invoicedamount");
                gap = gap.add(orderAmount.subtract(invoicedAmount));
            }
            return gap;
        }
    }

    // --- 动态排序 + 按需移除中间字段：适合报表首条预览或调试 ---
    public void previewOrderSummary(Object orgId, boolean orderByAmount, boolean keepSupplierField) {
        QFilter filter = new QFilter("billstatus", QCP.equals, "C")
                .and(new QFilter("org.id", QCP.equals, orgId));
        try (DataSet ds = QueryUtils.queryDataSet(
                PUR_ORDER_FORM_ID,
                "id,billno,supplier.number supplierNumber,amountandtax",
                filter.toArray())
        ) {
            List<String> orderBys = new ArrayList<>();
            orderBys.add(orderByAmount ? "amountandtax desc" : "billno desc");
            orderBys.add("billno asc");
            DataSet result = ds.orderBy(orderBys.toArray(new String[0]));
            if (!keepSupplierField) {
                result = result.removeFields("supplierNumber");
            }
            if (result.hasNext()) {
                Row firstRow = result.next();
                getView().showTipNotification("首条结果：" + firstRow.getString("billno"));
            }
        }
    }
}
