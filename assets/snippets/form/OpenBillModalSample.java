/**
 * 打开单据/动态表单弹窗并处理关闭回调示例。
 * <p>
 * 使用场景：
 * 1. 打开 F7 列表让用户多选基础资料；
 * 2. 打开自定义动态表单并接收回参；
 * 3. 打开特殊成员 F7，并按动态 actionId 前缀回填多选基础资料；
 * 4. 在 closedCallBack 中按 actionId 区分处理分支。
 */
package kd.cd.common.snippets.form;

import kd.bos.bill.OperationStatus;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.entity.MulBasedataDynamicObjectCollection;
import kd.bos.entity.datamodel.ListSelectedRow;
import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.form.CloseCallBack;
import kd.bos.form.FormShowParameter;
import kd.bos.form.ShowType;
import kd.bos.form.events.ClosedCallBackEvent;
import kd.bos.list.ListFilterParameter;
import kd.bos.list.ListShowParameter;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.cd.common.form.ShowParameterUtils;
import kd.cd.common.plugin.AbstractFormPluginExt;
import kd.cd.core.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OpenBillModalSample extends AbstractFormPluginExt {
    private static final String CALLBACK_SELECT_SUPPLIER = "select_supplier";
    private static final String CALLBACK_SELECT_BUDGET = "select_budget";
    private static final String CALLBACK_SELECT_DIM_MEMBER_PREFIX = "select_dim_member:";
    private static final String SUPPLIER_ENTRY = "entrysupplier";
    private static final String SUPPLIER_FIELD = "supplier";
    private static final String BILL_ENTRY = "billentry";
    private static final String SPECIAL_MEMBER_F7_FORM_ID = "bcm_mulmemberf7base_tem";

    // --- 打开 F7 列表弹窗（多选） ---
    public void openSupplierSelector(Object orgId, Object materialId) {
        ListShowParameter showParameter = ShowParameterUtils.getF7List("bd_supplier", true);
        showParameter.setCloseCallBack(new CloseCallBack(this, CALLBACK_SELECT_SUPPLIER));

        ListFilterParameter listFilter = showParameter.getListFilterParameter();
        listFilter.getQFilters().add(new QFilter("enable", QCP.equals, true));
        listFilter.getQFilters().add(new QFilter("createorg.id", QCP.equals, orgId));
        listFilter.getQFilters().add(new QFilter("suppliermaterial.material.masterid.id", QCP.equals, materialId));
        getView().showForm(showParameter);
    }

    // --- 打开自定义动态表单弹窗 ---
    public void openBudgetSelector(String orgNumber, String budgetYear, String costCenter) {
        FormShowParameter fsp = ShowParameterUtils.getForm("xxxx_budgetlist", OperationStatus.ADDNEW, ShowType.Modal, "900px", "600px");
        fsp.setCustomParam("OrgNumber", orgNumber);
        fsp.setCustomParam("BudgetYear", budgetYear);
        fsp.setCustomParam("CostCenter", costCenter);
        fsp.setCloseCallBack(new CloseCallBack(this, CALLBACK_SELECT_BUDGET));
        getView().showForm(fsp);
    }

    // --- 打开特殊成员 F7：通过动态 actionId 回填到目标字段 ---
    public void openMemberSelector(Object modelId, String fieldKey, String memberModel, Object dimensionId) {
        ListShowParameter showParameter = ShowParameterUtils.getF7List(SPECIAL_MEMBER_F7_FORM_ID, true);
        showParameter.setFormId(SPECIAL_MEMBER_F7_FORM_ID);
        showParameter.setCustomParam("KEY_MODEL_ID", String.valueOf(modelId));
        showParameter.setCustomParam("dimensionid", String.valueOf(dimensionId));
        showParameter.setCustomParam("mutilentity", "[]");
        showParameter.setCustomParam("sign", fieldKey);
        showParameter.setCloseCallBack(new CloseCallBack(this, buildMemberCallbackId(fieldKey, memberModel)));
        showParameter.getOpenStyle().setShowType(ShowType.Modal);
        getView().showForm(showParameter);
    }

    // --- 统一处理弹窗关闭回调 ---
    @Override
    public void closedCallBack(ClosedCallBackEvent e) {
        String actionId = e.getActionId();
        if (actionId != null && actionId.startsWith(CALLBACK_SELECT_DIM_MEMBER_PREFIX)) {
            handleDimMemberSelected(actionId, e.getReturnData());
            return;
        }
        if (CALLBACK_SELECT_SUPPLIER.equals(actionId)) {
            handleSupplierSelected(e.getReturnData());
            return;
        }
        if (CALLBACK_SELECT_BUDGET.equals(actionId)) {
            handleBudgetSelected(e.getReturnData());
        }
    }

    private void handleSupplierSelected(Object returnData) {
        if (!(returnData instanceof ListSelectedRowCollection)) {
            return;
        }

        ListSelectedRowCollection selectedRows = (ListSelectedRowCollection) returnData;
        if (selectedRows.isEmpty()) {
            return;
        }

        List<Object> supplierIds = selectedRows.stream()
                .map(ListSelectedRow::getPrimaryKeyValue)
                .collect(Collectors.toList());
        int[] newRows = getModel().batchCreateNewEntryRow(SUPPLIER_ENTRY, supplierIds.size());
        for (int i = 0; i < newRows.length; i++) {
            getModel().setItemValueByID(SUPPLIER_FIELD, supplierIds.get(i), newRows[i]);
        }
        getView().updateView(SUPPLIER_ENTRY);
    }

    private void handleBudgetSelected(Object returnData) {
        if (!(returnData instanceof HashMap<?, ?>)) {
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, String> selectedBudget = (HashMap<String, String>) returnData;
        if (selectedBudget.isEmpty()) {
            return;
        }
        int rowIndex = getModel().getEntryCurrentRowIndex(BILL_ENTRY);
        if (rowIndex < 0) {
            return;
        }
        getModel().setValue("xxxx_worktask", selectedBudget.get("workTask"), rowIndex);
        getModel().setValue("xxxx_worktaskno", selectedBudget.get("workTaskNo"), rowIndex);
        getModel().setValue("xxxx_feeaccount", selectedBudget.get("feeAccount"), rowIndex);
        getModel().setValue("xxxx_feeaccountno", selectedBudget.get("feeAccountNo"), rowIndex);
        getModel().setValue("xxxx_budgetofficer", selectedBudget.get("budgetOfficer"), rowIndex);
    }

    private String buildMemberCallbackId(String fieldKey, String memberModel) {
        return CALLBACK_SELECT_DIM_MEMBER_PREFIX + fieldKey + "|" + memberModel;
    }

    private void handleDimMemberSelected(String actionId, Object returnData) {
        if (!(returnData instanceof DynamicObjectCollection)) {
            return;
        }
        String[] parts = actionId.substring(CALLBACK_SELECT_DIM_MEMBER_PREFIX.length()).split("\\|", 2);
        if (parts.length != 2) {
            return;
        }

        String fieldKey = parts[0];
        String memberModel = parts[1];
        MulBasedataDynamicObjectCollection coll = (MulBasedataDynamicObjectCollection) getModel().getValue(fieldKey);
        if (CollectionUtils.isEmpty(coll)) {
            return;
        }

        for (DynamicObject selectedRow : (DynamicObjectCollection) returnData) {
            DynamicObject row = coll.addNew();
            DynamicObject member = BusinessDataServiceHelper.newDynamicObject(memberModel);
            member.set("id", selectedRow.getLong("mid1"));
            row.set("fbasedataid", member);
        }
        getModel().setValue(fieldKey, coll);
    }
}
