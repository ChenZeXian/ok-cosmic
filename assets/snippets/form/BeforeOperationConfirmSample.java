/**
 * beforeDoOperation 拦截 + 弹窗确认后继续执行示例。
 * <p>
 * 使用场景：
 * 1. 提交前需要先检查分录或关联业务数据，命中风险则先打开说明页；
 * 2. 某些检查必须先保存拿到单据主键，再在 afterDoOperation 中弹结果页；
 * 3. 用户确认后，带 continue 标记重新调用原提交操作。
 */
package kd.cd.common.snippets.form;

import kd.bos.bill.OperationStatus;
import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.form.CloseCallBack;
import kd.bos.form.FormShowParameter;
import kd.bos.form.ShowType;
import kd.bos.form.events.AfterDoOperationEventArgs;
import kd.bos.form.events.BeforeDoOperationEventArgs;
import kd.bos.form.events.ClosedCallBackEvent;
import kd.bos.form.operate.FormOperate;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.cd.common.form.ShowParameterUtils;
import kd.cd.common.plugin.AbstractFormPluginExt;
import kd.cd.common.util.QueryUtils;
import kd.cd.core.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BeforeOperationConfirmSample extends AbstractFormPluginExt {
    private static final String OP_SAVE = "save";
    private static final String OP_SUBMIT = "submit";
    private static final String CHECK_FORM_ID = "xxxx_billcheckresult";
    private static final String CHECK_CALLBACK_ID = "bill_check_result";
    private static final String CHECK_RESULT_FORM_ID = "xxxx_billcheckdetail";
    private static final String OPTION_CONTINUE = "ContinueToSubmit";
    private static final String PAGE_CACHE_SAVE_THEN_CHECK = "save_then_check";
    private static final String ENTRY_KEY = "billentry";
    private static final String FIELD_SUPPLIER = "supplier";

    // --- 操作前拦截：先检查，再决定是否放行 ---
    @Override
    public void beforeDoOperation(BeforeDoOperationEventArgs args) {
        FormOperate operate = (FormOperate) args.getSource();
        if (!OP_SUBMIT.equals(operate.getOperateKey())) {
            return;
        }

        if ("true".equalsIgnoreCase(operate.getOption().getVariableValue(OPTION_CONTINUE, "false"))) {
            return;
        }

        if (needSaveBeforeCheck()) {
            getView().getPageCache().put(PAGE_CACHE_SAVE_THEN_CHECK, "true");
            getView().invokeOperation(OP_SAVE);
            args.setCancel(true);
            return;
        }

        List<String> warnings = collectSubmitWarnings();
        if (CollectionUtils.isEmpty(warnings)) {
            return;
        }

        FormShowParameter fsp = ShowParameterUtils.getForm(CHECK_FORM_ID, OperationStatus.VIEW, ShowType.Modal, "900px", "600px");
        fsp.setCustomParam("warnings", warnings);
        fsp.setCloseCallBack(new CloseCallBack(this, CHECK_CALLBACK_ID));
        getView().showForm(fsp);
        args.setCancel(true);
    }

    // --- 保存成功后，如需要持久化校验，则查询明细并继续弹结果页 ---
    @Override
    public void afterDoOperation(AfterDoOperationEventArgs args) {
        FormOperate operate = (FormOperate) args.getSource();
        if (!OP_SAVE.equals(operate.getOperateKey())) {
            return;
        }
        if (!"true".equals(getView().getPageCache().get(PAGE_CACHE_SAVE_THEN_CHECK))) {
            return;
        }
        getView().getPageCache().remove(PAGE_CACHE_SAVE_THEN_CHECK);

        Object billPk = getModel().getDataEntity().getPkValue();
        List<Object> resultIds = queryCheckResultIds(billPk);
        if (CollectionUtils.isEmpty(resultIds)) {
            invokeContinueSubmit();
        }
    }

    // --- 说明页关闭后，如用户确认则继续提交 ---
    @Override
    public void closedCallBack(ClosedCallBackEvent e) {
        String actionId = e.getActionId();
        if (!CHECK_CALLBACK_ID.equals(actionId) && !CHECK_RESULT_FORM_ID.equals(actionId)) {
            return;
        }
        if (!shouldContinueSubmit(e.getReturnData())) {
            return;
        }
        invokeContinueSubmit();
    }

    private void invokeContinueSubmit() {
        OperateOption option = OperateOption.create();
        option.setVariableValue(OPTION_CONTINUE, "true");
        getView().invokeOperation(OP_SUBMIT, option);
    }

    private boolean needSaveBeforeCheck() {
        Object pkValue = getModel().getDataEntity().getPkValue();
        return pkValue == null || "0".equals(String.valueOf(pkValue));
    }

    private List<Object> queryCheckResultIds(Object billPk) {
        QFilter filter = new QFilter("sourcebill.id", QCP.equals, billPk);
        return QueryUtils.queryMatchedPkList(
                CHECK_RESULT_FORM_ID,
                filter.toArray()
        );
    }

    private boolean shouldContinueSubmit(Object returnData) {
        if (returnData instanceof Boolean) {
            return (Boolean) returnData;
        }
        if (returnData instanceof Map<?, ?>) {
            Object confirmValue = ((Map<?, ?>) returnData).get("isConfirmSubmit");
            return Boolean.parseBoolean(String.valueOf(confirmValue));
        }
        return Boolean.parseBoolean(String.valueOf(returnData));
    }

    private List<String> collectSubmitWarnings() {
        DynamicObjectCollection entryRows = getModel().getDataEntity(true).getDynamicObjectCollection(ENTRY_KEY);
        List<String> warnings = new ArrayList<>();
        for (DynamicObject row : entryRows) {
            if (row.getDynamicObject(FIELD_SUPPLIER) == null) {
                warnings.add("第" + row.getInt("seq") + "行未选择供应商");
            }
        }
        return warnings;
    }
}
