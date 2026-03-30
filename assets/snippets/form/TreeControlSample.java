/**
 * 树形控件（TreeView）交互示例。
 * <p>
 * 使用场景：
 * 1. 动态构建实体树形结构；
 * 2. 树节点点击/双击事件处理；
 * 3. 树节点搜索定位；
 * 4. 多选/单选节点回传。
 */
package kd.cd.common.snippets.form;

import kd.bos.dataentity.serialization.SerializationUtils;
import kd.bos.entity.tree.TreeNode;
import kd.bos.form.control.Search;
import kd.bos.form.control.TreeView;
import kd.bos.form.control.events.ItemClickEvent;
import kd.bos.form.control.events.SearchEnterEvent;
import kd.bos.form.control.events.SearchEnterListener;
import kd.bos.form.control.events.TreeNodeClickListener;
import kd.bos.form.control.events.TreeNodeEvent;
import kd.cd.common.plugin.AbstractFormPluginExt;
import kd.cd.core.util.CharSequenceUtils;
import kd.cd.core.util.CollectionUtils;

import java.util.EventObject;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class TreeControlSample extends AbstractFormPluginExt implements TreeNodeClickListener, SearchEnterListener {
    private static final String TREE_CONTROL_KEY = "kdcd_treeviewap";
    private static final String ROOT_NODE_CACHE = "rootjson";
    private static final String BTN_OK = "kdcd_ok";
    private static final String MUTI_SELECT_PARAM = "mutiSelect";

    // --- 注册树控件和搜索框监听 ---
    @Override
    public void registerListener(EventObject e) {
        TreeView tree = getTreeView();
        tree.addTreeNodeClickListener(this);
        Search search = getControl("kdcd_searchap");
        search.addEnterListener(this);
        addItemClickListeners("kdcd_toolbarap");
    }

    // --- 初始化树结构 ---
    private void buildTree(String entityId, String entryKey, boolean mutiSelect) {
        TreeView tree = getTreeView();
        String rootId = CharSequenceUtils.isBlank(entryKey) ? entityId : entryKey;
        TreeNode root = recoverRootNode();
        tree.addNode(root);
        tree.setRootVisible(true);
        tree.expand(rootId);
        tree.setMulti(mutiSelect);
    }

    // --- 构建根节点（从自定义参数或缓存恢复） ---
    private TreeNode recoverRootNode() {
        String rootJsonString = getPageCache().get(ROOT_NODE_CACHE);
        return SerializationUtils.fromJsonString(rootJsonString, TreeNode.class);
    }

    // --- 树节点双击事件：选中并返回 ---
    @Override
    public void treeNodeDoubleClick(TreeNodeEvent evt) {
        closeAndReturnData();
    }

    // --- 搜索框回车事件：定位匹配节点 ---
    @Override
    public void search(SearchEnterEvent evt) {
        String searchText = evt.getText();
        if (CharSequenceUtils.isBlank(searchText)) {
            return;
        }

        TreeNode root = recoverRootNode();
        String lastSearchIds = getPageCache().get(searchText);
        List<String> ids;

        if (lastSearchIds == null) {
            // 首次搜索：遍历树找匹配节点
            List<TreeNode> matchedNodes = root.getTreeNodeListByText(CollectionUtils.newArrayList(), searchText, 10000);
            ids = matchedNodes.stream().map(TreeNode::getId).collect(Collectors.toList());
            getPageCache().put(searchText, SerializationUtils.toJsonString(ids));
        } else {
            // 复用缓存
            ids = SerializationUtils.fromJsonString(lastSearchIds, List.class);
        }

        if (ids.isEmpty()) {
            getPageCache().put(searchText, null);
            getView().showSuccessNotification("已是最后一个");
            return;
        }

        // 定位到第一个匹配节点
        String currId = ids.get(0);
        ids.remove(0);
        getPageCache().put(searchText, SerializationUtils.toJsonString(ids));

        TreeNode currNode = root.getTreeNode(currId);
        getTreeView().focusNode(currNode);
    }

    // --- 确定按钮点击：返回选中数据 ---
    @Override
    public void itemClick(ItemClickEvent evt) {
        if (BTN_OK.equals(evt.getItemKey())) {
            closeAndReturnData();
        }
    }

    // --- 关闭页面并返回选中数据 ---
    private void closeAndReturnData() {
        TreeView tree = getTreeView();
        Object returnData;

        // 根据多选/单选模式返回不同数据
        if (Objects.equals(getCustomParam(MUTI_SELECT_PARAM), true)) {
            returnData = tree.getTreeState().getSelectedNodes();
        } else {
            returnData = tree.getTreeState().getFocusNode();
        }

        getView().returnDataToParent(returnData);
        getView().close();
    }

    // --- 获取树控件实例 ---
    private TreeView getTreeView() {
        return getControl(TREE_CONTROL_KEY);
    }
}
