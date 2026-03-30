package kd.cd.common;

import kd.bos.workflow.api.AgentExecution;
import kd.bos.workflow.component.approvalrecord.IApprovalRecordItem;
import kd.bos.workflow.engine.extitf.IWorkflowPlugin;
import kd.cd.core.util.CollectionUtils;

import java.util.List;

/**
 * 工作流插件骨架模板（原生 IWorkflowPlugin）。
 * 该类仅用于示例写法，生成后请按实际业务删除无用方法并替换占位常量。
 */
public class IWorkflowPluginTemplate implements IWorkflowPlugin {

    /**
     * 触发时机: 在需要了解当前插件可访问上下文能力时调用。
     * 参数要点: 无入参；仅展示当前插件可通过 this. 访问的方法能力。
     * 典型用途: 作为模板提示，指导在各事件内选择正确的上下文 API。
     */
    private void getContextSample() {
        // execution.getBusinessKey();
        // execution.getEntityNumber();
        // execution.getCurrentFlowElement();
        // execution.getVariable("amount");
        // execution.setVariable("lastNodeName", "财务审核");
        // execution.setAssigneeList(new java.util.ArrayList<>());
    }

    private static final String VAR_AMOUNT = "amount";
    private static final String VAR_START_USER = "startUserId";
    private static final String VAR_LAST_NODE = "lastNodeName";

    /**
     * 触发时机: 工作流节点需要计算参与人时。
     * 参数要点: execution 含流程实例、单据、节点、变量等上下文。
     * 典型用途: 根据单据字段、发起人、组织等信息动态返回审批人。
     */
    @Override
    public List<Long> calcUserIds(AgentExecution execution) {
        super.calcUserIds(execution);
        List<Long> userIds = CollectionUtils.newArrayList();
        Object starter = execution.getVariable(VAR_START_USER);
        if (starter instanceof Number) {
            userIds.add(((Number) starter).longValue());
        }
        return userIds;
    }

    /**
     * 触发时机: 条件分支判断阶段。
     * 参数要点: execution 可读取流程变量、业务主键与节点信息。
     * 典型用途: 根据金额、组织、单据类型等条件决定是否命中当前分支。
     */
    @Override
    public boolean hasTrueCondition(AgentExecution execution) {
        super.hasTrueCondition(execution);
        Object amount = execution.getVariable(VAR_AMOUNT);
        return amount instanceof Number && ((Number) amount).doubleValue() > 100000D;
    }

    /**
     * 触发时机: 流程节点通知阶段。
     * 参数要点: execution 可访问流程变量、当前节点、业务主键。
     * 典型用途: 同步单据状态、写流程变量、发送外部通知。
     */
    @Override
    public void notify(AgentExecution execution) {
        super.notify(execution);
        if (execution.getCurrentFlowElement() != null) {
            execution.setVariable(VAR_LAST_NODE, execution.getCurrentFlowElement().getName());
        }
    }

    /**
     * 触发时机: 流程撤回时。
     * 参数要点: execution 仍可访问当前流程与业务上下文。
     * 典型用途: 撤回时恢复状态、清理流程变量或做补偿处理。
     */
    @Override
    public void notifyByWithdraw(AgentExecution execution) {
        super.notifyByWithdraw(execution);
        execution.setVariable(VAR_LAST_NODE, "withdraw");
    }

    /**
     * 触发时机: 展示审批记录时。
     * 参数要点: item 为当前待格式化的审批记录项。
     * 典型用途: 定制审批记录显示文本、补充节点说明或统一格式。
     */
    @Override
    public IApprovalRecordItem formatFlowRecord(IApprovalRecordItem item) {
        super.formatFlowRecord(item);
        return item;
    }
}
