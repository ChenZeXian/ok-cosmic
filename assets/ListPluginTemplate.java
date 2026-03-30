package kd.cd.common;

import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.form.control.events.ItemClickEvent;
import kd.bos.form.events.AfterDoOperationEventArgs;
import kd.bos.form.events.BeforeDoOperationEventArgs;
import kd.bos.form.operate.AbstractOperate;
import kd.bos.list.BillList;
import kd.cd.common.operate.OpUtils;
import kd.cd.common.plugin.AbstractListPluginExt;
import kd.cd.core.util.CollectionUtils;

import java.util.EventObject;
import java.util.Map;
import java.util.Set;

/**
 * 列表插件骨架模板。
 * 该类仅用于示例写法，生成后请按实际业务删除无用事件并替换占位常量。
 *
 * @template ListPluginTemplate
 * @extends AbstractListPluginExt (kd.cd.common.plugin)
 * @highFreqEvents itemClick, beforeDoOperation, afterDoOperation
 * @medFreqEvents registerListener, propertyChanged, setFilter
 * @relatedDocs references/adv/plugin-base.md, references/base/plugin/plugin-list.md
 */
public class ListPluginTemplate extends AbstractListPluginExt {

    /**
     * 触发时机: 在需要了解当前插件可访问上下文能力时调用。
     * 参数要点: 无入参；仅展示当前插件可通过 this. 访问的方法能力。
     * 典型用途: 作为模板提示，指导在各事件内选择正确的上下文 API。
     */
    private void getContextSample() {
        // this.getView();
        // this.getModel();
        // this.getPageCache();
    }


    // 占位常量：复制模板后请统一替换为业务真实 key。
    private static final String TOOLBAR_KEY = "toolbar";
    private static final String BTN_BATCH_AUDIT = "btn_batchaudit";
    private static final String BTN_CUSTOM_OP = "btn_customop";
    private static final String ENTITY_ID = "entityid";
    private static final String OP_KEY_AUDIT = "audit";
    private static final String OP_KEY_DELETE = "delete";
    private static final String FIELD_STATUS = "billstatus";
    private static final String FIELD_BILL_NO = "billno";
    private static final String COLOR_WARNING = "#FFF1B8";

    // ===== 生命周期事件 =====

    /**
     * 触发时机: 列表初始化完毕，所有事件监听注册前。
     * 参数要点:
     * - EventObject e: 通用事件参数。
     * 典型用途: 注册列表工具栏、菜单、行按钮等的事件监听。
     *
     */

    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        // 注册工具栏点击事件。
        this.addItemClickListeners(TOOLBAR_KEY);
    }

    // ===== 工具栏按钮点击事件 =====

    /**
     * 触发时机: 用户点击列表工具栏按钮或菜单项时。
     * 参数要点:
     * - ItemClickEvent evt: 包含点击的菜单项信息。
     * - evt.getItemKey(): 获取被点击的菜单项标识。
     * 典型用途: 处理工具栏按钮的点击事件，如批量审核、导出等。
     *
     */

    @Override
    public void itemClick(ItemClickEvent evt) {
        super.itemClick(evt);
        String key = evt.getItemKey();
        if (BTN_BATCH_AUDIT.equals(key)) {
            doBatchAudit();
        } else if (BTN_CUSTOM_OP.equals(key)) {
            doCustomOp();
            //刷新列表
            BillList billList = getBillList();
            billList.refresh();
        }
    }

    // ===== 操作前后事件 =====

    /**
     * 触发时机: 用户在列表执行操作（如删除行）前。
     * 参数要点:
     * - BeforeDoOperationEventArgs args: 操作前置事件参数。
     * - getOperate(args): 获取当前操作对象。
     * - operate.getOperateKey(): 获取操作类型。
     * - args.setCancel(true): 取消操作。
     * 典型用途: 列表操作前校验。
     *
     */

    @Override
    public void beforeDoOperation(BeforeDoOperationEventArgs args) {
        super.beforeDoOperation(args);
        AbstractOperate operate = getOperate(args);
        String opKey = operate.getOperateKey();
        if (OP_KEY_AUDIT.equals(opKey)) {
            Set<Object> pks = getSelectedRowPkValues();
            Map<String, String> variables = operate.getOption().getVariables();
            if (CollectionUtils.isEmpty(pks) && variables.containsKey(FIELD_STATUS)) {
                args.setCancel(true);
                this.getView().showErrorNotification("请先选择需要审核的单据");
            }
        }
        if (OP_KEY_DELETE.equals(opKey) && CollectionUtils.isEmpty(getSelectedRowPkValues())) {
            args.setCancel(true);
            this.getView().showErrorNotification("未选中任何数据，不能执行删除");
        }
    }

    /**
     * 触发时机: 列表操作（删除、审核等）完成后。
     * 参数要点:
     * - AfterDoOperationEventArgs e: 操作后置事件参数。
     * 典型用途: 操作完成后的处理，如刷新列表、显示提示等。
     *
     */

    @Override
    public void afterDoOperation(AfterDoOperationEventArgs e) {
        super.afterDoOperation(e);
        this.getView().showTipNotification("列表操作完成：" + e.getOperateKey());
        this.getView().invokeOperation("refresh");
    }

    // ===== 字段值变更事件 =====

    /**
     * 触发时机: 用户在列表中修改单元格值后。
     * 参数要点:
     * - PropertyChangedArgs e: 属性变更事件参数。
     * - e.getProperty().getName(): 获取变更字段标识。
     * - getChangedRowIndex(e): 获取变更行号。
     * - getChangedNewValue(e): 获取新值。
     * - getChangedOldValue(e): 获取旧值。
     * 典型用途: 根据单元格变化进行行高亮、联动等操作。
     *
     */

    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        super.propertyChanged(e);
        String fieldKey = e.getProperty().getName();
        if (!FIELD_STATUS.equals(fieldKey)) {
            return;
        }
        int rowIndex = getChangedRowIndex(e);
        setBackColor4ListRows(COLOR_WARNING, rowIndex);
    }

    // ---- 业务方法 ----

    private void doBatchAudit() {
        Set<Object> pks = getSelectedRowPkValues();
        if (CollectionUtils.isEmpty(pks)) {
            this.getView().showTipNotification("请先选择需要审核的单据");
            return;
        }
        OpUtils.executeOperateOrThrow(OP_KEY_AUDIT, ENTITY_ID, pks.toArray());
        this.getView().invokeOperation("refresh");
    }

    private void doCustomOp() {
        Set<Object> pks = getSelectedRowPkValues();
        if (CollectionUtils.isEmpty(pks)) {
            this.getView().showTipNotification("请先选择需要处理的数据");
            return;
        }
        this.getView().showSuccessNotification("已触发自定义列表动作，选中条数：" + pks.size());
    }
}
