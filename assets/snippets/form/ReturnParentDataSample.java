/**
 * 子页面选中数据后 returnDataToParent(...) 示例。
 * <p>
 * 使用场景：弹窗子页面提供一组候选数据，
 * 用户点确定或双击行后，将选中结果打包回传给父页面。
 * 返回值可按场景选择：
 * 1. 单行 Map；
 * 2. 多行编码 Set；
 * 3. 父页面在 closedCallBack 中把返回结果回填到 MulBasedata 字段。
 */
package kd.cd.common.snippets.form;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.MulBasedataDynamicObjectCollection;
import kd.bos.form.control.Control;
import kd.bos.form.control.EntryGrid;
import kd.bos.form.control.events.RowClickEvent;
import kd.bos.form.control.events.RowClickEventListener;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.cd.common.plugin.AbstractFormPluginExt;
import kd.cd.core.util.CharSequenceUtils;

import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ReturnParentDataSample extends AbstractFormPluginExt implements RowClickEventListener {
    private static final String ENTRY_KEY = "entryentity";
    private static final String BTN_OK = "btnok";
    private static final String FIELD_NUMBER = "number";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_OWNER = "owner";

    // --- 注册按钮和分录双击事件 ---
    @Override
    public void registerListener(EventObject e) {
        addClickListeners(BTN_OK);
        EntryGrid entryGrid = getControl(ENTRY_KEY);
        entryGrid.addRowClickListener(this);
    }

    // --- 点确定按钮回传 ---
    @Override
    public void click(EventObject evt) {
        Control control = (Control) evt.getSource();
        if (BTN_OK.equalsIgnoreCase(control.getKey())) {
            returnSelectedRow();
        }
    }

    // --- 双击行也直接回传 ---
    @Override
    public void entryRowDoubleClick(RowClickEvent evt) {
        returnSelectedRow();
    }

    private void returnSelectedRow() {
        int rowIndex = getModel().getEntryCurrentRowIndex(ENTRY_KEY);
        if (rowIndex < 0) {
            getView().showTipNotification("请先选择一行数据");
            return;
        }

        Object ownerId = getModel().getValue(FIELD_OWNER + "_id", rowIndex);
        if (ownerId == null || "0".equals(String.valueOf(ownerId))) {
            getView().showErrorNotification("所选行未设置负责人，不能回传");
            return;
        }

        String number = (String) getModel().getValue(FIELD_NUMBER, rowIndex);
        if (CharSequenceUtils.isBlank(number)) {
            getView().showErrorNotification("所选行编码为空，不能回传");
            return;
        }

        Map<String, String> result = new HashMap<>();
        result.put("number", number);
        result.put("name", String.valueOf(getModel().getValue(FIELD_NAME, rowIndex)));
        result.put("ownerId", String.valueOf(ownerId));
        getView().returnDataToParent(result);
        getView().close();
    }
}
