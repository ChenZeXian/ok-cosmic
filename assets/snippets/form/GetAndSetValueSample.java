/**
 * 表单模型取值/赋值示例。
 * <p>
 * 这个样本只关注 {@code getModel()} 相关 API：
 * 1. 表头 / 分录按行读取
 * 2. 基础资料按对象、按 ID、按编码回写
 * 3. 批量造行与静默改数据包
 */
package kd.cd.common.snippets.form;

import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.cd.common.plugin.AbstractFormPluginExt;
import kd.cd.common.util.DynamicObjectUtils;
import kd.cd.core.util.CollectionUtils;

import java.math.BigDecimal;

public class GetAndSetValueSample extends AbstractFormPluginExt {
    private static final String FIELD_REMARK = "kdtest_remark";
    private static final String FIELD_REGISTRANT = "kdtest_registrant";
    private static final String ENTRY_KEY = "kdtest_reqentryentity";
    private static final String FIELD_QTY = "kdtest_qtyfield";
    private static final String FIELD_MATERIAL = "kdtest_materielfield";

    // ==================== 场景1：表单读值 ====================

    public void readHeaderAndEntryValues() {
        String remark = (String) getModel().getValue(FIELD_REMARK);

        DynamicObject registrant = (DynamicObject) getModel().getValue(FIELD_REGISTRANT);
        Object registrantId = DynamicObjectUtils.nullSafeGet(getModel().getDataEntity(), FIELD_REGISTRANT + ".id");
        String registrantName = registrant == null ? null : registrant.getString("name");

        int rowCount = getModel().getEntryRowCount(ENTRY_KEY);
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            BigDecimal qty = (BigDecimal) getModel().getValue(FIELD_QTY, rowIndex);
            DynamicObject material = (DynamicObject) getModel().getValue(FIELD_MATERIAL, rowIndex);
        }

        DynamicObjectCollection entryRows = getModel().getEntryEntity(ENTRY_KEY);
        if (CollectionUtils.isNotEmpty(entryRows)) {
            for (DynamicObject row : entryRows) {
                BigDecimal qty = row.getBigDecimal(FIELD_QTY);
            }
        }
    }

    // ==================== 场景2：表头 / 基础资料赋值 ====================

    public void fillDefaultHeaderValues() {
        getModel().setValue(FIELD_REMARK, "备注字段默认值");

        DynamicObject currentUser = BusinessDataServiceHelper.loadSingleFromCache(
                "bos_user",
                new QFilter("id", QCP.equals, RequestContext.get().getCurrUserId()).toArray()
        );
        if (currentUser != null) {
            getModel().setValue(FIELD_REGISTRANT, currentUser);
        }

        getModel().setItemValueByID(FIELD_REGISTRANT, RequestContext.get().getCurrUserId());
        getModel().setItemValueByNumber(FIELD_REGISTRANT, "ID-000002");
    }

    // ==================== 场景3：批量造行 ====================

    public void rebuildEntryRows() {
        getModel().deleteEntryData(ENTRY_KEY);
        int[] rowIndexes = getModel().batchCreateNewEntryRow(ENTRY_KEY, 2);
        for (int i = 0; i < rowIndexes.length; i++) {
            int rowIndex = rowIndexes[i];
            getModel().setValue(FIELD_QTY, BigDecimal.TEN.multiply(BigDecimal.valueOf(i + 1L)), rowIndex);
            getModel().setItemValueByNumber(FIELD_MATERIAL, "M000" + (i + 1), rowIndex);
        }
    }

    /**
     * 直接改数据包不会触发 propertyChanged，适合导入回填、批量矫正、静默初始化。
     */
    public void appendEntryRowSilently() {
        DynamicObjectCollection entryRows = getModel().getDataEntity(true).getDynamicObjectCollection(ENTRY_KEY);
        DynamicObject newRow = entryRows.addNew();
        newRow.set(FIELD_QTY, BigDecimal.valueOf(30));
        getView().updateView(ENTRY_KEY);
    }
}
