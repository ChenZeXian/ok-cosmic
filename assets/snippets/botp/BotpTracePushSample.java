/**
 * BOTP 链路查询与下推示例。
 * <p>
 * 适合对照记忆这几组 API：
 * 1. 查全部上下游：{@code findAllSourceBills / findAllTargetBills}
 * 2. 只查某一种上下游单据：传 {@code 目标单类型 + 当前单类型 + 主键集}
 * 3. 只看直接关系：{@code findDirectSourceBills / findDirectTargetBills}
 * 4. 下推但不保存：{@code pushNoSave}
 * 5. 下推后补字段再保存：{@code OpUtils.executeOperateOrThrow("save", ...)}
 */
package kd.cd.common.snippets.botp;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.cd.common.operate.OpUtils;
import kd.cd.common.util.BotpUtils;
import kd.cd.common.util.PushResult;
import kd.cd.common.util.QueryUtils;
import kd.cd.core.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class BotpTracePushSample {
    private static final String MEASURE_SETTLE = "kdcd_measuresettlebill";
    private static final String AP_PAYAPPLY = "ap_payapply";
    private static final String CAS_PAYBILL = "cas_paybill";
    private static final String CEXP_DEDUCTION = "kdcd_cexp_deduction";

    // ==================== 场景1：全链路下查 ====================

    /**
     * 以“源单主键 -> 目标单类型 -> 目标单主键集”结构返回全部下游。
     * 适合做整条链路预览或按单据类型分组校验。
     */
    public static Map<Long, Map<String, Set<Long>>> findAllTargetBills(Collection<Long> measureIds) {
        if (CollectionUtils.isEmpty(measureIds)) {
            return Collections.emptyMap();
        }
        return BotpUtils.findAllTargetBills(MEASURE_SETTLE, measureIds);
    }

    /**
     * 只关心一种目标单据类型时，直接使用扁平化 API。
     */
    public static Set<Long> findAllPayApplyIds(Collection<Long> measureIds) {
        if (CollectionUtils.isEmpty(measureIds)) {
            return Collections.emptySet();
        }
        return BotpUtils.findAllTargetBillsFlat(AP_PAYAPPLY, MEASURE_SETTLE, measureIds);
    }

    // ==================== 场景2：直接下查 ====================

    /**
     * 付款申请单只往下看一层付款处理单，不追完整链路。
     */
    public static Set<Long> findDirectPayBillIds(Collection<Long> payApplyIds) {
        if (CollectionUtils.isEmpty(payApplyIds)) {
            return Collections.emptySet();
        }
        return BotpUtils.findDirectTargetBills(CAS_PAYBILL, AP_PAYAPPLY, payApplyIds).values().stream()
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
    }

    // ==================== 场景3：反查上游 ====================

    /**
     * 从扣款清单反查全部来源计量结算单。
     */
    public static Set<Long> findAllMeasureIdsByDeduction(Collection<Long> deductionIds) {
        if (CollectionUtils.isEmpty(deductionIds)) {
            return Collections.emptySet();
        }
        return BotpUtils.findAllSourceBillsFlat(MEASURE_SETTLE, CEXP_DEDUCTION, deductionIds);
    }

    /**
     * 只看直接来源时，仍然建议保留“目标单主键 -> 源单主键集”的映射，后续更好回填。
     */
    public static Map<Long, Set<Long>> findDirectMeasureIdsByDeduction(Collection<Long> deductionIds) {
        if (CollectionUtils.isEmpty(deductionIds)) {
            return Collections.emptyMap();
        }
        return BotpUtils.findDirectSourceBills(MEASURE_SETTLE, CEXP_DEDUCTION, deductionIds);
    }

    // ==================== 场景4：链路结果回填/提示 ====================

    /**
     * 构建“源单 -> 指定目标单”的映射，适合附件复制、状态回写、批量关联处理。
     */
    public static Map<Long, Set<Long>> findMeasureToDeductionMap(QFilter filter) {
        Set<Long> measureIds = QueryUtils.queryMatchedPkSet(MEASURE_SETTLE, filter.toArray());
        if (CollectionUtils.isEmpty(measureIds)) {
            return Collections.emptyMap();
        }
        return BotpUtils.findAllTargetBills(CEXP_DEDUCTION, MEASURE_SETTLE, measureIds);
    }

    /**
     * 只提示某一类下游单据时，先查目标主键，再回查单号，避免把整条链路拼到消息里。
     */
    public static String buildPayApplyExistsMessage(Long measureId, String measureBillNo) {
        Set<Long> payApplyIds = BotpUtils.findAllTargetBillsFlat(
                AP_PAYAPPLY,
                MEASURE_SETTLE,
                Collections.singleton(measureId)
        );
        if (CollectionUtils.isEmpty(payApplyIds)) {
            return null;
        }
        QFilter filter = new QFilter("id", QCP.in, payApplyIds);
        Map<Long, String> billNoMap = QueryUtils.queryAsMap(
                AP_PAYAPPLY,
                "id",
                "billno",
                filter.toArray()
        );
        String payApplyNos = billNoMap.values().stream().sorted().collect(Collectors.joining("、"));
        return String.format("计量结算单[%s]已生成付款申请单：%s", measureBillNo, payApplyNos);
    }

    // ==================== 场景5：下推生成 ====================

    /**
     * 下推但先不保存，适合需要补字段、补分录、做二次校验的场景。
     */
    public static DynamicObject pushNoSave(Long measureId, String ruleId) {
        PushResult pushResult = BotpUtils.pushNoSave(MEASURE_SETTLE, AP_PAYAPPLY, measureId, ruleId)
                .failThenThrow();
        return pushResult.getSingleDataPack();
    }

    /**
     * 下推后补充头字段，再调用统一保存操作。
     */
    public static DynamicObject pushAndSave(Long measureId, String sourceBillNo, String ruleId) {
        DynamicObject targetBill = pushNoSave(measureId, ruleId);
        if (targetBill == null) {
            return null;
        }
        targetBill.set("kdcd_sourcebillno", sourceBillNo);
        OpUtils.executeOperateOrThrow("save", AP_PAYAPPLY, new DynamicObject[]{targetBill});
        return targetBill;
    }
}
