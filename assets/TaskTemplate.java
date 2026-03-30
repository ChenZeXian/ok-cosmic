package kd.cd.common;

import kd.bos.context.RequestContext;
import kd.bos.exception.KDException;
import kd.bos.schedule.executor.AbstractTask;

import java.util.HashMap;
import java.util.Map;

/**
 * 后台任务骨架模板（原生 AbstractTask）。
 * 该类仅用于示例写法，生成后请按实际业务删除无用逻辑并替换占位常量。
 */
public class TaskTemplate extends AbstractTask {

    /**
     * 触发时机: 在需要了解当前插件可访问上下文能力时调用。
     * 参数要点: 无入参；仅展示当前插件可通过 this. 访问的方法能力。
     * 典型用途: 作为模板提示，指导在各事件内选择正确的上下文 API。
     */
    private void getContextSample() {
        // this.getTaskId();
        // this.checkIsStop();
        // this.isStop();
        // this.feedbackProgress(10);
        // this.feedbackProgress(30, "处理中", null);
        // this.feedbackCustomdata(new java.util.HashMap<>());
        // this.getMessageHandler();
        // this.setTaskId("task-id");
        // this.isSupportReSchedule();
    }

    // ===== 核心事件 =====

    /**
     * 触发时机: 调度中心触发任务执行时。
     * 参数要点:
     * - context: 请求上下文，包含用户、组织、租户等信息。
     * - params: 调度参数 map，通常承载任务业务入参。
     * 典型用途: 编排任务主流程、分阶段回传进度、按需检查中止标记。
     */
    @Override
    public void execute(RequestContext context, Map<String, Object> params) throws KDException {
        super.execute(context, Map<String, params);
        this.feedbackProgress(0, "任务开始", null);

        Object taskParam = params == null ? null : params.get("taskParam");
        doStep(taskParam);

        this.checkIsStop();

        Map<String, Object> customData = new HashMap<>();
        customData.put("result", "ok");
        customData.put("taskParam", taskParam);
        this.feedbackProgress(100, "任务完成", customData);
    }

    /**
     * 触发时机: 调度中心主动停止任务时。
     * 参数要点: 可在停止前做资源释放、状态补偿、最后一次进度反馈。
     * 典型用途: 安全退出长任务，避免中途停止导致资源未释放。
     */
    @Override
    public void stop() throws KDException {
        super.stop();
        this.feedbackProgress(95, "收到停止指令，准备安全退出", null);
    }

    private void doStep(Object taskParam) {
        if (taskParam == null) {
            this.feedbackProgress(30, "未传入 taskParam，按默认逻辑执行", null);
            return;
        }
        Map<String, Object> customData = new HashMap<>();
        customData.put("taskParam", taskParam);
        this.feedbackProgress(60, "正在处理 taskParam", customData);
    }
}
