package kd.cd.common;

import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.form.control.events.ItemClickEvent;
import kd.bos.form.events.AfterDoOperationEventArgs;
import kd.bos.form.events.BeforeDoOperationEventArgs;
import kd.cd.common.plugin.AbstractBillPlugInExt;
import kd.cd.core.util.CharSequenceUtils;

import java.math.BigDecimal;
import java.util.EventObject;

/**
 * 单据界面插件骨架模板（原生 AbstractBillPlugIn）。
 * 该类仅用于示例写法，生成后请按实际业务删除无用事件并替换占位常量。
 *
 * @template BillPlugInTemplate
 * @extends AbstractBillPlugInExt (kd.cd.common.plugin)
 * @highFreqEvents afterLoadData, propertyChanged, beforeDoOperation, afterDoOperation, itemClick
 * @medFreqEvents registerListener, beforeF7Select, click
 * @relatedDocs references/adv/plugin-base.md, references/base/plugin/plugin-bill.md
 */
public class BillPlugInTemplate extends AbstractBillPlugInExt {

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

    private static final String TOOLBAR_KEY = "tbmain";
    private static final String BTN_SYNC = "btn_sync";
    private static final String OP_KEY_SAVE = "save";
    private static final String FIELD_QTY = "qty";
    private static final String BASEDATAFIELD = "basedatafield";

    /**
     * 触发时机: 表单初始化完毕，所有事件监听注册前。
     * 参数要点:
     * - `EventObject e`: 通用事件对象。
     * 典型用途: 注册菜单、按钮等交互监听。
     */
    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        this.addItemClickListeners(TOOLBAR_KEY);
        this.addClickListeners(BTN_SYNC);
        this.addBeforeF7SelectListeners(BASEDATAFIELD);
    }

    /**
     * 触发时机: 单据数据从数据库加载完毕后。
     * 参数要点:
     * - `EventObject e`: 通用事件对象。
     * 典型用途: 基于历史数据做界面初始化。
     */
    @Override
    public void afterLoadData(EventObject e) {
        super.afterLoadData(e);
        String billNo = this.getValue("billno");
        if (CharSequenceUtils.isNotEmpty(billNo)) {
            this.getView().showTipNotification("加载单据：" + billNo);
        }
    }

    /**
     * 触发时机: 用户点击工具栏或菜单项时。
     * 参数要点:
     * - `ItemClickEvent evt`: 可通过 `getItemKey()` 获取菜单标识。
     * 典型用途: 处理工具栏动作。
     */
    @Override
    public void itemClick(ItemClickEvent evt) {
        super.itemClick(evt);
        if (BTN_SYNC.equals(evt.getItemKey())) {
            this.getView().showTipNotification("已触发同步动作");
        }
    }

    /**
     * 触发时机: 用户点击表单控件时。
     * 参数要点:
     * - `EventObject evt`: `getSource()` 可获取控件对象。
     * 典型用途: 处理按钮、链接等控件交互。
     */
    @Override
    public void click(EventObject evt) {
        super.click(evt);
        String clickKey = getClickKey(evt);
        if (BTN_SYNC.equals(clickKey)) {
            this.getView().showTipNotification("按钮点击处理完成");
        }
    }

    /**
     * 触发时机: 单据操作执行前。
     * 参数要点:
     * - `BeforeDoOperationEventArgs e`: 通过 `(AbstractOperate) e.getSource()` 获取操作对象。
     * 典型用途: 做轻量前置校验，必要时取消操作。
     */
    @Override
    public void beforeDoOperation(BeforeDoOperationEventArgs e) {
        super.beforeDoOperation(e);
        String opKey = getOpKey(e);
        if (OP_KEY_SAVE.equals(opKey)) {
            BigDecimal qty = this.getValue(BASEDATAFIELD);
            if (qty != null && qty.compareTo(BigDecimal.ZERO) <= 0) {
                e.setCancel(true);
                this.getView().showErrorNotification("数量必须大于 0");
            }
        }
    }

    /**
     * 触发时机: 单据操作执行后。
     * 参数要点:
     * - `AfterDoOperationEventArgs e`: 可读取操作结果与操作标识。
     * 典型用途: 做提示、刷新或后续联动。
     */
    @Override
    public void afterDoOperation(AfterDoOperationEventArgs e) {
        super.afterDoOperation(e);
        this.getView().showTipNotification("操作完成：" + e.getOperateKey());
    }

    /**
     * 触发时机: 字段值写入模型后。
     * 参数要点:
     * - `PropertyChangedArgs e`: 可读取字段标识与变更值。
     * 典型用途: 做字段联动与提示。
     */
    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        super.propertyChanged(e);
        if (FIELD_QTY.equals(e.getProperty().getName())) {
            Integer changedNewValue = getChangedNewValue(e);
            this.getView().showTipNotification("数量已更新为：" + changedNewValue);
        }
    }
}
