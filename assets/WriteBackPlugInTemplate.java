package kd.cd.common;

import kd.bos.entity.BillEntityType;
import kd.bos.entity.LinkSetItemElement;
import kd.bos.entity.botp.plugin.AbstractWriteBackPlugIn;
import kd.bos.entity.botp.plugin.args.AfterBuildSourceBillIdsEventArgs;
import kd.bos.entity.botp.plugin.args.AfterCalcWriteValueEventArgs;
import kd.bos.entity.botp.plugin.args.AfterCloseRowEventArgs;
import kd.bos.entity.botp.plugin.args.AfterCommitAmountEventArgs;
import kd.bos.entity.botp.plugin.args.AfterExcessCheckEventArgs;
import kd.bos.entity.botp.plugin.args.AfterReadSourceBillEventArgs;
import kd.bos.entity.botp.plugin.args.AfterSaveSourceBillEventArgs;
import kd.bos.entity.botp.plugin.args.BeforeCloseRowEventArgs;
import kd.bos.entity.botp.plugin.args.BeforeCreateArticulationRowEventArgs;
import kd.bos.entity.botp.plugin.args.BeforeExcessCheckEventArgs;
import kd.bos.entity.botp.plugin.args.BeforeExecWriteBackRuleEventArgs;
import kd.bos.entity.botp.plugin.args.BeforeReadSourceBillEventArgs;
import kd.bos.entity.botp.plugin.args.BeforeSaveSourceBillEventArgs;
import kd.bos.entity.botp.plugin.args.BeforeSaveTransEventArgs;
import kd.bos.entity.botp.plugin.args.BeforeTrackEventArgs;
import kd.bos.entity.botp.plugin.args.FinishWriteBackEventArgs;
import kd.bos.entity.botp.plugin.args.PreparePropertysEventArgs;
import kd.bos.entity.botp.plugin.args.RollbackSaveEventArgs;

/**
 * 单据反写插件骨架模板（原生 AbstractWriteBackPlugIn）。
 * 该类仅用于示例写法，生成后请按实际业务删除无用事件并替换占位常量。
 */
public class WriteBackPlugInTemplate extends AbstractWriteBackPlugIn {

    /**
     * 触发时机: 在需要了解当前插件可访问上下文能力时调用。
     * 参数要点: 无入参；仅展示当前插件可通过 this. 访问的方法能力。
     * 典型用途: 作为模板提示，指导在各事件内选择正确的上下文 API。
     */
    private void getContextSample() {
        // this.getCurrLinkSetItem();
        // this.getTargetSubMainType();
        // this.getOpType();
        // this.setContext(null, null, null);
    }

    // 占位常量：复制模板后请统一替换为业务真实 key。
    private static final String FIELD_TGT_AMOUNT = "writeback_amount";
    private static final String FIELD_SRC_TOTAL = "totalamount";

    // ===== 反写预处理阶段 =====

    /**
     * 触发时机: 反写执行前，框架准备目标单字段阶段。
     * 参数要点: e.getFieldKeys() 为后续反写读取字段集合。
     *
     */

    @Override
    public void preparePropertys(PreparePropertysEventArgs e) {
        super.preparePropertys(e);
        e.getFieldKeys().add(FIELD_TGT_AMOUNT);
    }

    /**
     * 触发时机: 系统构建来源单据 ID 集合后。
     * 参数要点: e 可读取来源单范围，用于诊断或二次过滤。
     */
    @Override
    public void afterBuildSourceBillIds(AfterBuildSourceBillIdsEventArgs e) {
        super.afterBuildSourceBillIds(e);
        this.getCurrLinkSetItem();
    }

    /**
     * 触发时机: 读取来源单前。
     * 参数要点: e.getFieldKeys() 控制来源单需加载字段。
     *
     */

    @Override
    public void beforeReadSourceBill(BeforeReadSourceBillEventArgs e) {
        super.beforeReadSourceBill(e);
        e.getFieldKeys().add(FIELD_SRC_TOTAL);
    }

    /**
     * 触发时机: 来源单读取完成后。
     * 参数要点: 可访问来源单数据并做缓存，供后续规则使用。
     */
    @Override
    public void afterReadSourceBill(AfterReadSourceBillEventArgs e) {
        super.afterReadSourceBill(e);
        this.getTargetSubMainType();
    }

    // ===== 超额与规则执行阶段 =====

    /**
     * 触发时机: 超额校验前。
     * 参数要点: 可按业务改写超额校验上下文。
     */

    @Override
    public void beforeExcessCheck(BeforeExcessCheckEventArgs e) {
        super.beforeExcessCheck(e);
        this.getCurrLinkSetItem();
    }

    /**
     * 触发时机: 超额校验后。
     * 参数要点: 可读取超额结果并决定后续行为。
     */
    @Override
    public void afterExcessCheck(AfterExcessCheckEventArgs e) {
        super.afterExcessCheck(e);
        this.getTargetSubMainType();
    }

    /**
     * 触发时机: 执行反写规则前。
     * 参数要点: 可做最终拦截和环境准备。
     */

    @Override
    public void beforeExecWriteBackRule(BeforeExecWriteBackRuleEventArgs e) {
        super.beforeExecWriteBackRule(e);
        this.getOpType();
    }

    /**
     * 触发时机: 反写值计算完成后。
     * 参数要点: 可读取计算结果并做二次修正。
     */
    @Override
    public void afterCalcWriteValue(AfterCalcWriteValueEventArgs e) {
        super.afterCalcWriteValue(e);
        this.getOpType();
    }

    // ===== 关联行与关行处理 =====

    /**
     * 触发时机: 创建关联行前。
     * 参数要点: 可按条件跳过关联行生成。
     */

    @Override
    public void beforeCreateArticulationRow(BeforeCreateArticulationRowEventArgs e) {
        super.beforeCreateArticulationRow(e);
        this.getCurrLinkSetItem();
    }

    /**
     * 触发时机: 关行前。
     * 参数要点: 可校验关行条件并拦截。
     */
    @Override
    public void beforeCloseRow(BeforeCloseRowEventArgs e) {
        super.beforeCloseRow(e);
        this.getCurrLinkSetItem();
    }

    /**
     * 触发时机: 关行后。
     * 参数要点: 可记录关行结果并同步状态。
     */

    @Override
    public void afterCloseRow(AfterCloseRowEventArgs e) {
        super.afterCloseRow(e);
        this.getTargetSubMainType();
    }

    // ===== 提交与保存阶段 =====

    /**
     * 触发时机: 反查来源/去向链路前。
     * 参数要点: 可覆盖链路追踪策略。
     */
    @Override
    public void beforeTrack(BeforeTrackEventArgs e) {
        super.beforeTrack(e);
        this.getCurrLinkSetItem();
    }

    /**
     * 触发时机: 反写数量/金额提交后。
     * 参数要点: 可读取提交汇总结果。
     */

    @Override
    public void afterCommitAmount(AfterCommitAmountEventArgs e) {
        super.afterCommitAmount(e);
        this.getOpType();
    }

    /**
     * 触发时机: 保存事务提交前。
     * 参数要点: 适合同事务内最后修正。
     */
    @Override
    public void beforeSaveTrans(BeforeSaveTransEventArgs e) {
        super.beforeSaveTrans(e);
        this.getTargetSubMainType();
    }

    /**
     * 触发时机: 保存来源单前。
     * 参数要点: 可校验来源单最终写入数据。
     */

    @Override
    public void beforeSaveSourceBill(BeforeSaveSourceBillEventArgs e) {
        super.beforeSaveSourceBill(e);
        this.getTargetSubMainType();
    }

    /**
     * 触发时机: 保存来源单后。
     * 参数要点: 可进行后置日志、状态同步。
     */
    @Override
    public void afterSaveSourceBill(AfterSaveSourceBillEventArgs e) {
        super.afterSaveSourceBill(e);
        this.getTargetSubMainType();
    }

    /**
     * 触发时机: 保存失败并回滚时。
     * 参数要点: e 包含回滚上下文，可做补偿记录。
     */

    @Override
    public void rollbackSave(RollbackSaveEventArgs e) {
        super.rollbackSave(e);
        this.getOpType();
    }

    /**
     * 触发时机: 反写流程结束（成功或失败后收尾阶段）。
     * 参数要点: 可统一释放资源、输出日志。
     */
    @Override
    public void finishWriteBack(FinishWriteBackEventArgs e) {
        super.finishWriteBack(e);
        this.getCurrLinkSetItem();
    }

    // ===== 上下文与元信息 =====

    /**
     * 获取当前关联关系项。
     * 注意: 模板示例返回 null，实际使用时需根据业务逻辑实现。
     */
    @Override
    public LinkSetItemElement getCurrLinkSetItem() {
        super.getCurrLinkSetItem();
        return null;
    }

    /**
     * 获取目标单据类型。
     * 注意: 模板示例返回 null，实际使用时需根据业务逻辑实现。
     */
    @Override
    public BillEntityType getTargetSubMainType() {
        super.getTargetSubMainType();
        return null;
    }

    /**
     * 获取操作类型。
     * 注意: 模板示例返回 null，实际使用时需根据业务逻辑实现。
     */
    @Override
    public String getOpType() {
        super.getOpType();
        return null;
    }

    /**
     * 设置上下文信息。
     * 注意: 模板示例为空实现，实际使用时需根据业务逻辑实现。
     */
    @Override
    public void setContext(BillEntityType targetSubMainType, String opType, LinkSetItemElement linkSetItem) {
        super.setContext(targetSubMainType, opType, linkSetItem);
    }
}
