/**
 * DynamicObjectUtils 常见能力示例。
 * <p>
 * 这个样本只展示“脱离表单模型之后，如何直接操作 DynamicObject”，
 * 不再重复表单层的 getModel().getValue / setValue 写法。
 */
package kd.cd.common.snippets.data;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.cd.common.util.DynamicObjectUtils;
import kd.cd.core.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DynamicObjectOpsSample {

    // ==================== 场景1：原生取值 ====================

    public static Object getValue(DynamicObject bill, String fieldKey) {
        return bill == null ? null : bill.get(fieldKey);
    }

    public static Object nullSafeGet(DynamicObject bill, String fieldPath) {
        return DynamicObjectUtils.nullSafeGet(bill, fieldPath);
    }

    public static String getBaseDataNumber(DynamicObject bill, String fieldKey) {
        Object number = DynamicObjectUtils.nullSafeGet(bill, fieldKey + ".number");
        return number == null ? "" : String.valueOf(number);
    }

    // ==================== 场景2：原生赋值 ====================

    public static void setValue(DynamicObject bill, String fieldKey, Object value) {
        if (bill != null) {
            bill.set(fieldKey, value);
        }
    }

    public static void batchSetValues(DynamicObject bill, Map<String, Object> fieldValues) {
        if (bill == null || CollectionUtils.isEmpty(fieldValues)) {
            return;
        }
        fieldValues.forEach(bill::set);
    }

    // ==================== 场景3：分录集合操作 ====================

    public static DynamicObjectCollection getEntryRows(DynamicObject bill, String entryKey) {
        DynamicObjectCollection rows = bill == null ? null : bill.getDynamicObjectCollection(entryKey);
        return rows == null ? new DynamicObjectCollection() : rows;
    }

    public static BigDecimal sumEntryAmount(DynamicObject bill, String entryKey, String amountField) {
        return DynamicObjectUtils.sumOf(getEntryRows(bill, entryKey), amountField);
    }

    public static Set<Object> collectEntryIds(Collection<DynamicObject> rows) {
        return CollectionUtils.isEmpty(rows) ? Collections.emptySet() : DynamicObjectUtils.setOfIds(rows);
    }

    public static List<String> collectEntryNumbers(Collection<DynamicObject> rows, String numberField) {
        return CollectionUtils.isEmpty(rows) ? Collections.emptyList() : DynamicObjectUtils.listOf(rows, numberField);
    }

    // ==================== 场景4：调试/排查 ====================

    public static boolean containsProperty(DynamicObject bill, String fieldKey) {
        return DynamicObjectUtils.containsKey(bill, fieldKey);
    }

    public static Map<String, Object> dumpBill(DynamicObject bill) {
        return DynamicObjectUtils.dump(bill);
    }
}
