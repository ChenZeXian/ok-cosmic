/**
 * 分录行遍历与表头汇总示例。
 * <p>
 * 使用场景：分录数量/金额变化后，实时刷新表头汇总字段。
 * 参考写法：优先复用 getModel().getEntryEntity(...) + DynamicObjectUtils.sumOf(...)。
 */
package kd.cd.common.snippets.form;

import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.datamodel.events.AfterAddRowEventArgs;
import kd.bos.entity.datamodel.events.AfterDeleteRowEventArgs;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.cd.common.plugin.AbstractFormPluginExt;
import kd.cd.common.util.DynamicObjectUtils;

import java.math.BigDecimal;

public class EntryRowCalculateSample extends AbstractFormPluginExt {
    private static final String ENTRY_KEY = "billentry";
    private static final String FIELD_QTY = "qty";
    private static final String FIELD_AMOUNT = "amount";
    private static final String FIELD_TOTAL_QTY = "totalqty";
    private static final String FIELD_TOTAL_AMOUNT = "totalamount";

    // --- 分录字段变化后触发重算 ---
    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        String fieldKey = e.getProperty().getName();
        if (!FIELD_QTY.equals(fieldKey) && !FIELD_AMOUNT.equals(fieldKey)) {
            return;
        }
        recalcEntrySummary();
    }

    // --- 新增分录后触发重算 ---
    @Override
    public void afterAddRow(AfterAddRowEventArgs e) {
        if (ENTRY_KEY.equals(e.getEntryProp().getName())) {
            recalcEntrySummary();
        }
    }

    // --- 删除分录后触发重算 ---
    @Override
    public void afterDeleteRow(AfterDeleteRowEventArgs e) {
        if (ENTRY_KEY.equals(e.getEntryProp().getName())) {
            recalcEntrySummary();
        }
    }

    // --- 汇总计算 ---
    private void recalcEntrySummary() {
        DynamicObjectCollection entryRows = getModel().getEntryEntity(ENTRY_KEY);

        BigDecimal totalQty = DynamicObjectUtils.sumOf(entryRows, FIELD_QTY);
        BigDecimal totalAmount = DynamicObjectUtils.sumOf(entryRows, FIELD_AMOUNT);
        if (totalQty == null) {
            totalQty = BigDecimal.ZERO;
        }
        if (totalAmount == null) {
            totalAmount = BigDecimal.ZERO;
        }

        getModel().setValue(FIELD_TOTAL_QTY, totalQty);
        getModel().setValue(FIELD_TOTAL_AMOUNT, totalAmount);
    }
}
