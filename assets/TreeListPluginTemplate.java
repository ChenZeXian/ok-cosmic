package kd.cd.common;

import kd.bos.form.ShowType;
import kd.bos.form.control.Search;
import kd.bos.form.control.events.RefreshNodeEvent;
import kd.bos.form.control.events.SearchEnterEvent;
import kd.bos.form.control.events.TreeNodeEvent;
import kd.bos.list.events.BeforeShowBillFormEvent;
import kd.bos.list.events.BuildTreeListFilterEvent;
import kd.bos.list.plugin.AbstractTreeListPlugin;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;

import java.util.EventObject;

/**
 * 左树右表列表插件骨架模板（原生 AbstractTreeListPlugin）。
 * 该类仅用于示例写法，生成后请按实际业务删除无用事件并替换占位常量。
 */
public class TreeListPluginTemplate extends AbstractTreeListPlugin {

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

    private static final String FIELD_GROUP = "group.id";
    private static final String TREE_NODE_ALL = "0";

    // ===== 核心事件 =====

    /**
     * 触发时机: 视图初始化后、事件注册阶段。
     * 参数要点: e 为通用事件对象，通常用于注册监听。
     * 典型用途: 注册树搜索框、工具栏等监听。
     */
    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
    }

    /**
     * 触发时机: 左树首次构建时，以及树模型重新创建时。
     * 参数要点: 此时已可通过 this.getTreeModel() 访问树模型。
     * 典型用途: 设置根节点、默认展开层级、默认筛选条件。
     */
    @Override
    public void initializeTree(EventObject e) {
        super.initializeTree(e);
        if (this.getTreeModel() != null) {
            this.getTreeModel().setRootVisable(true);
        }
    }

    /**
     * 触发时机: 树节点点击后。
     * 参数要点: e 可读取当前节点信息。
     * 典型用途: 切换右侧列表数据范围、刷新按钮状态或页面提示。
     */
    @Override
    public void treeNodeClick(TreeNodeEvent e) {
        super.treeNodeClick(e);
        this.reload();
    }

    /**
     * 触发时机: 树节点刷新时。
     * 参数要点: e 指向当前待刷新的节点。
     * 典型用途: 懒加载子节点、刷新节点状态或补充树结构。
     */
    @Override
    public void refreshNode(RefreshNodeEvent e) {
        super.refreshNode(e);
        this.getView().showTipNotification("刷新树节点：" + e.getNodeId());
    }

    /**
     * 触发时机: 节点选中后构建右侧列表过滤时。
     * 参数要点: e 可追加 QFilter 到右侧列表。
     * 典型用途: 按当前节点 ID 生成右表过滤条件。
     */
    @Override
    public void buildTreeListFilter(BuildTreeListFilterEvent e) {
        super.buildTreeListFilter(e);
        // 生成过滤条件
        QFilter filter = new QFilter("fieldKey", QCP.equals, "value");
        e.addQFilter(filter);
        e.setCancel(true);   // 略过系统内置的分组过滤条件
    }

    /**
     * 触发时机: 左树工具栏初始化时。
     * 参数要点: 可调整工具栏按钮可见性与可用状态。
     * 典型用途: 初始化新增、删除、刷新等树工具栏按钮。
     */
    @Override
    public void initTreeToolbar(EventObject e) {
        super.initTreeToolbar(e);
    }

    /**
     * 触发时机: 左树工具栏按钮点击时。
     * 参数要点: e 可识别按钮来源。
     * 典型用途: 处理树节点新增、删除、刷新等动作。
     */
    @Override
    public void treeToolbarClick(EventObject e) {
        super.treeToolbarClick(e);
        this.getView().showTipNotification("已触发左树工具栏动作");
    }

    /**
     * 触发时机: 左树搜索框回车时。
     * 参数要点: evt 包含搜索关键字。
     * 典型用途: 按关键字过滤左树节点或触发远程搜索。
     */
    @Override
    public void search(SearchEnterEvent evt) {
        super.search(evt);
        Search search = (Search)evt.getSource();
        //搜索控件标识，可以根据该标识，判断是哪个控件，然后做逻辑处理
        String key = search.getKey();
        //搜索的文本内容
        this.getView().showTipNotification("树搜索关键字：" + evt.getText());
    }

    /**
     * 触发时机: 打开右侧单据详情前。
     * 参数要点: e 可获取单据打开参数并追加自定义参数。
     * 典型用途: 标记当前打开动作来源于左树右表场景。
     */
    @Override
    public void beforeShowBill(BeforeShowBillFormEvent e) {
        super.beforeShowBill(e);
        e.getParameter().getOpenStyle().setShowType(ShowType.Modal);
    }
}
