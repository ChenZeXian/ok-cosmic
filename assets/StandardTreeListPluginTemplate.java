package kd.cd.common;

import kd.bos.form.control.Search;
import kd.bos.form.control.events.BeforeItemClickEvent;
import kd.bos.form.control.events.SearchEnterEvent;
import kd.bos.form.events.BeforeDoOperationEventArgs;
import kd.bos.form.events.ClosedCallBackEvent;
import kd.bos.form.events.MessageBoxClosedEvent;
import kd.bos.form.operate.AbstractOperate;
import kd.bos.list.plugin.StandardTreeListPlugin;

import java.util.EventObject;

/**
 * 标准树列表插件骨架模板（原生 StandardTreeListPlugin）。
 * 该类仅用于示例写法，生成后请按实际业务删除无用事件并替换占位常量。
 */
public class StandardTreeListPluginTemplate extends StandardTreeListPlugin {

    /**
     * 触发时机: 在需要了解当前插件可访问上下文能力时调用。
     * 参数要点: 无入参；仅展示当前插件可通过 this. 访问的方法能力。
     * 典型用途: 作为模板提示，指导在各事件内选择正确的上下文 API。
     */
    private void getContextSample() {
        // this.getTreeListView();
        // this.getTreeModel();
        // this.getView();
        // this.getModel();
    }

    private static final String OP_DELETE = "delete";
    private static final String OP_DISABLE = "disable";

    /**
     * 触发时机: 插件注册监听阶段。
     * 参数要点: e 为通用事件对象。
     * 典型用途: 注册树工具栏、搜索框等监听器。
     */
    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        this.addItemClickListeners("tree_toolbar");
    }


    /**
     * 触发时机: 工具栏项点击前。
     * 参数要点: evt.getItemKey() 标识当前按钮，可取消默认处理。
     * 典型用途: 在删除、停用前做前置校验或弹框确认。
     */
    @Override
    public void beforeItemClick(BeforeItemClickEvent evt) {
        super.beforeItemClick(evt);
        if (OP_DELETE.equals(evt.getItemKey()) && this.getSelectedRows().isEmpty()) {
            evt.setCancel(true);
            this.getView().showErrorNotification("请先选择数据");
        }
    }

    /**
     * 触发时机: 标准操作执行前。
     * 参数要点:
     * - (AbstractOperate) args.getSource(): 获取操作对象。
     * - operate.getOperateKey(): 获取操作标识。
     * - args.setCancel(true): 取消操作。
     * 典型用途: 在停用、删除等操作前校验选择数据是否合法。
     */
    @Override
    public void beforeDoOperation(BeforeDoOperationEventArgs args) {
        super.beforeDoOperation(args);
        AbstractOperate operate = (AbstractOperate) args.getSource();
        if (OP_DISABLE.equals(operate.getOperateKey()) && this.getSelectedRows().isEmpty()) {
            args.setCancel(true);
            this.getView().showErrorNotification("请先选择数据");
        }
    }

    /**
     * 触发时机: 左树搜索回车时。
     * 参数要点: evt 携带搜索关键字。
     * 典型用途: 过滤树节点或联动刷新右侧数据。
     */
    @Override
    public void search(SearchEnterEvent evt) {
        super.search(evt);
        Search search = (Search) evt.getSource();
        //搜索控件标识，可以根据该标识，判断是哪个控件，然后做逻辑处理
        String key = search.getKey();
        //搜索的文本内容
        this.getView().showTipNotification("树搜索关键字：" + evt.getText());
    }

    /**
     * 触发时机: 左树工具栏按钮点击时。
     * 参数要点: e 可识别当前点击来源。
     * 典型用途: 处理新增分组、刷新分组、展开节点等动作。
     */
    @Override
    public void treeToolbarClick(EventObject e) {
        super.treeToolbarClick(e);
        this.getView().showTipNotification("已触发标准树列表工具栏动作");
    }

    /**
     * 触发时机: 确认框关闭后。
     * 参数要点: event.getResult() 可判断用户确认结果。
     * 典型用途: 处理删除、停用等二次确认后的后续逻辑。
     */
    @Override
    public void confirmCallBack(MessageBoxClosedEvent event) {
        super.confirmCallBack(event);
    }

    /**
     * 触发时机: 子页面关闭回调时。
     * 参数要点: closedCallBackEvent 可读取返回动作与回参。
     * 典型用途: 子页面关闭后刷新当前树列表。
     */
    @Override
    public void closedCallBack(ClosedCallBackEvent closedCallBackEvent) {
        super.closedCallBack(closedCallBackEvent);
        this.reload();
    }
}