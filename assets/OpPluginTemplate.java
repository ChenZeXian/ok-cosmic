package kd.cd.common;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.ExtendedDataEntity;
import kd.bos.entity.plugin.AddValidatorsEventArgs;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.AfterOperationArgs;
import kd.bos.entity.plugin.args.BeforeOperationArgs;
import kd.bos.entity.plugin.args.BeginOperationTransactionArgs;
import kd.bos.entity.plugin.args.EndOperationTransactionArgs;
import kd.bos.entity.validate.AbstractValidator;
import kd.cd.common.plugin.AbstractOperationServicePlugInExt;
import kd.cd.common.plugin.AbstractValidatorExt;
import kd.cd.core.util.CharSequenceUtils;

import java.util.List;

/**
 * 操作插件骨架模板。
 * 该类仅用于示例写法，生成后请按实际业务删除无用事件并替换占位常量。
 *
 * @template OpPluginTemplate
 * @extends AbstractOperationServicePlugInExt (kd.cd.common.plugin)
 * @highFreqEvents onPreparePropertys, endOperationTransaction, onAddValidators
 * @medFreqEvents beforeExecuteOperationTransaction, beginOperationTransaction, afterExecuteOperationTransaction
 * @lowFreqEvents onReturnOperation
 * @relatedDocs references/adv/operate-chain.md, references/base/plugin/plugin-operation.md
 * @snippets assets/snippets/silent-audit.java
 */
public class OpPluginTemplate extends AbstractOperationServicePlugInExt {

    /**
     * 触发时机: 在需要了解当前插件可访问上下文能力时调用。
     * 参数要点: 无入参；仅展示当前插件可通过 this. 访问的方法能力。
     * 典型用途: 作为模板提示，指导在各事件内选择正确的上下文 API。
     */
    private void getContextSample() {
        // this.billEntityType;
        // this.operateMeta;
        // this.operationResult;
        // this.getOption();
        // this.entryFields(ENTRY_KEY_MAIN);
        // this.addErrorMessage(null, ERROR_MSG_DEMO);
    }

    // 占位常量：复制模板后请统一替换为业务真实 key。
    private static final String OP_KEY_SAVE = "save";
    private static final String OP_KEY_SUBMIT = "submit";
    private static final String OP_KEY_AUDIT = "audit";
    private static final String OP_KEY_UNAUDIT = "unaudit";
    private static final String FIELD_BILL_NO = "billno";
    private static final String FIELD_BILL_STATUS = "billstatus";
    private static final String ENTRY_KEY_MAIN = "entryentity";
    private static final String OPTION_KEY_DEMO = "key3";
    private static final String OPTION_VALUE_DEMO = "value1";
    private static final String ERROR_CODE_DEMO = "OperateError_001";
    private static final String ERROR_MSG_REQUIRED_BILL_NO = "单据编号不能为空";
    private static final String ERROR_MSG_DEMO = "xxxxxx";

    // ===== 字段准备事件 =====

    /**
     * 触发时机: 操作执行前，框架装载数据字段阶段。
     * 参数要点:
     * - {@link PreparePropertysEventArgs#getFieldKeys()} 为本次操作需预加载的字段集合。
     * - 未加入的字段在后续事件里可能取值为空。
     *
     */

    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        // 按实际场景显式准备字段，避免运行期缺字段。
        List<String> fieldKeys = e.getFieldKeys();
        fieldKeys.add(FIELD_BILL_NO);
        fieldKeys.add(FIELD_BILL_STATUS);
        // 需要整张分录的场景可直接准备分录字段。
        fieldKeys.addAll(entryFields(ENTRY_KEY_MAIN));
    }

    // ===== 校验器注册事件 =====

    /**
     * 触发时机: 进入操作执行链前，注册业务校验器阶段。
     * 参数要点:
     * - {@link AddValidatorsEventArgs#addValidator(AbstractValidator)} 可追加多个校验器。
     * - 校验器在事务开启前执行，失败会阻断操作。
     *
     */

    @Override
    public void onAddValidators(AddValidatorsEventArgs e) {
        super.onAddValidators(e);
        // 注册自定义校验器（示例）。
        e.addValidator(new BillNoRequiredValidator());
    }

    // ===== 事务前事件 =====

    /**
     * 触发时机: 校验通过后、数据库事务开启前。
     * 参数要点:
     * - {@link BeforeOperationArgs#getOperationKey()} 获取当前操作 key（save/submit/audit...）。
     * - {@link BeforeOperationArgs#getDataEntities()} 获取待处理单据数组。
     * - 可通过 e.setCancel(true) 直接取消操作。
     *
     */

    @Override
    public void beforeExecuteOperationTransaction(BeforeOperationArgs e) {
        super.beforeExecuteOperationTransaction(e);
        // 校验通过后、开启事务前的最后拦截点。
        String opKey = e.getOperationKey();
        DynamicObject[] dataEntities = e.getDataEntities();
        if (OP_KEY_SAVE.equals(opKey) || OP_KEY_SUBMIT.equals(opKey)) {
            for (DynamicObject bill : dataEntities) {
                bill.set(OPTION_KEY_DEMO, "A");
            }
        }
        // 若需直接取消整个操作，可按需启用：
        // e.setCancel(true);
        // e.setCancelMessage("xxxxxx");
    }

    // ===== 事务中事件 =====

    /**
     * 触发时机: 事务已开启，主操作执行中。
     * 参数要点:
     * - {@link BeginOperationTransactionArgs#getDataEntities()} 可安全参与同事务处理。
     * - 适合同步写入关联表、BOTP 关系追踪等“必须同事务成功/失败”的逻辑。
     *
     */

    @Override
    public void beginOperationTransaction(BeginOperationTransactionArgs e) {
        super.beginOperationTransaction(e);
        // 事务已开启，适合处理需要与主操作同事务的数据同步。
        String opKey = e.getOperationKey();
        DynamicObject[] bills = e.getDataEntities();
    }

    // ===== 事务结束（未提交）事件 =====

    /**
     * 触发时机: SQL 已执行完成，但事务尚未提交。
     * 参数要点:
     * - {@link EndOperationTransactionArgs#getOperationKey()} 可区分操作类型。
     * - 适合做同事务内收尾，不建议调用外部系统。
     *
     */

    @Override
    public void endOperationTransaction(EndOperationTransactionArgs e) {
        super.endOperationTransaction(e);
        String opKey = e.getOperationKey();
        if (OP_KEY_AUDIT.equals(opKey)) {
            log.info("endOperationTransaction, opKey={}", opKey);
        }
    }

    // ===== 事务提交后事件 =====

    /**
     * 触发时机: 事务提交成功后。
     * 参数要点:
     * - {@link AfterOperationArgs} 可读取操作结果。
     * - 适合通知、日志、异步任务；失败不影响主事务。
     *
     */

    @Override
    public void afterExecuteOperationTransaction(AfterOperationArgs e) {
        super.afterExecuteOperationTransaction(e);
        String opKey = e.getOperationKey();
        if (OP_KEY_AUDIT.equals(opKey)) {
            DynamicObject[] entities = e.getDataEntities();
        }
    }

    /**
     * 示例校验器：校验表头编号必填。
     */
    private static class BillNoRequiredValidator extends AbstractValidatorExt {
        @Override
        public void validate() {
        super.validate();
            for (ExtendedDataEntity ext : getDataEntities()) {
                DynamicObject bill = ext.getDataEntity();
                String billNo = bill.getString(FIELD_BILL_NO);
                if (CharSequenceUtils.isBlank(billNo)) {
                    // 强制校验拦截
                    this.addErrorMessage(ext, ERROR_MSG_REQUIRED_BILL_NO);
                    // 弱提示拦截
                    this.addWarningMessage(ext, ERROR_MSG_DEMO);
                    // this.addWarningMessage(billDataEntity,
                }
            }
        }
    }
}
