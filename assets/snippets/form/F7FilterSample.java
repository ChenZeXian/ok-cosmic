/**
 * F7（基础资料选择控件）过滤条件示例。
 * <p>
 * 使用场景：
 * 1. 打开 F7 前，按当前组织、行物料、启用/审核状态动态过滤可选数据；
 * 2. 按当前体系切换特殊 F7 页面，并注入模型/维度 customParams；
 * 3. 个别字段需要关闭系统默认 QFilter。
 */
package kd.cd.common.snippets.form;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.form.FormShowParameter;
import kd.bos.form.ShowType;
import kd.bos.form.field.events.BeforeF7SelectEvent;
import kd.bos.list.ListShowParameter;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.cd.common.entity.EntityUtils;
import kd.cd.common.plugin.AbstractFormPluginExt;

import java.util.EventObject;
import java.util.List;

public class F7FilterSample extends AbstractFormPluginExt {
    private static final String FIELD_ORG = "org";
    private static final String FIELD_MATERIAL = "material";
    private static final String FIELD_SUPPLIER = "supplier";
    private static final String FIELD_MODEL = "kdcd_model";
    private static final String FIELD_YEAR = "kdcd_year";
    private static final String FIELD_PERIOD = "kdcd_period";
    private static final String SPECIAL_MEMBER_F7_FORM_ID = "bcm_mulmemberf7base_tem";

    // --- 在 registerListener 中注册 F7 监听 ---
    @Override
    public void registerListener(EventObject e) {
        addBeforeF7SelectListeners(FIELD_MATERIAL, FIELD_SUPPLIER, FIELD_MODEL, FIELD_YEAR, FIELD_PERIOD);
    }

    // --- 打开 F7 前按业务条件加过滤 ---
    @Override
    public void beforeF7Select(BeforeF7SelectEvent e) {
        String fieldKey = e.getProperty().getName();
        if (FIELD_MATERIAL.equals(fieldKey)) {
            DynamicObject org = getValue(FIELD_ORG);
            Object orgId = org == null ? null : org.getPkValue();
            if (EntityUtils.isEmptyPk(orgId)) {
                e.setCancel(true);
                getView().showTipNotification("请先选择组织");
                return;
            }

            List<QFilter> filters = e.getCustomQFilters();
            filters.add(new QFilter("enable", QCP.equals, true));
            filters.add(new QFilter("status", QCP.equals, "C"));
            filters.add(new QFilter("createorg.id", QCP.equals, orgId));
            return;
        }

        if (FIELD_SUPPLIER.equals(fieldKey)) {
            int rowIndex = e.getRow();
            DynamicObject material = (DynamicObject) getModel().getValue(FIELD_MATERIAL, rowIndex);
            Object materialMasterId = DynamicObjectUtils.safeGetvalue(material, "masterid.id");
            if (EntityUtils.isEmptyPk(materialMasterId)) {
                e.setCancel(true);
                getView().showTipNotification("请先选择当前行物料");
                return;
            }

            ListShowParameter showParameter = (ListShowParameter) e.getFormShowParameter().getShowParameter();
            showParameter.getListFilterParameter().getQFilters().add(new QFilter("enable", QCP.equals, true));
            return;
        }

        if (FIELD_MODEL.equals(fieldKey)) {
            FormShowParameter formShowParameter = e.getFormShowParameter();
            if (formShowParameter instanceof ListShowParameter) {
                (formShowParameter).setCustomParam("noNeedDefaultQFilter", true);
            }
            return;
        }
    }
}
