/**
 * onPreparePropertys + onAddValidators 示例。
 * <p>
 * 重点不是“写一个巨大的 validate”，而是把规则拆成多个小校验器：
 * 1. preparePropertys 显式准备头/体字段
 * 2. onAddValidators 按规则拆分校验器
 * 3. 错误信息挂到操作结果里，而不是直接抛异常
 */
package kd.cd.common.snippets.operation;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.ExtendedDataEntity;
import kd.bos.entity.plugin.AddValidatorsEventArgs;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.cd.common.plugin.AbstractOperationServicePlugInExt;
import kd.cd.common.plugin.AbstractValidatorExt;
import kd.cd.common.util.DynamicObjectUtils;

import java.math.BigDecimal;
import java.util.List;

public class OpAddValidatorsSample extends AbstractOperationServicePlugInExt {
    private static final String TARGET_BILL_TYPE = "kdcd_measuresettlebill_B_02";
    private static final String FIELD_BILL_NO = "billno";
    private static final String FIELD_CONTRACT = "kdcd_contract";
    private static final String FIELD_BILL_TYPE = "kdcd_billtype";
    private static final String FIELD_INITIALIZATION = "kdcd_initialization";
    private static final String ENTRY_MEASURE_DETAIL = "kdcd_measuredelail";
    private static final String FIELD_TOTAL_TAX = "kdcd_totaltax";
    private static final String ENTRY_INVOICE_DETAIL = "kdcd_invoicedetail";
    private static final String FIELD_PRICE_TAX_TOTAL = "kdcd_pricetaxtotal";

    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        List<String> fieldKeys = e.getFieldKeys();
        fieldKeys.add(FIELD_BILL_NO);
        fieldKeys.add(FIELD_CONTRACT);
        fieldKeys.add(FIELD_CONTRACT + ".number");
        fieldKeys.add(FIELD_BILL_TYPE);
        fieldKeys.add(FIELD_BILL_TYPE + ".number");
        fieldKeys.add(FIELD_INITIALIZATION);
        fieldKeys.addAll(entryFields(ENTRY_MEASURE_DETAIL, ENTRY_INVOICE_DETAIL));
    }

    @Override
    public void onAddValidators(AddValidatorsEventArgs e) {
        e.addValidator(new ContractRequiredValidator());
        e.addValidator(new InvoiceAmountEqualsValidator());
    }

    private static class ContractRequiredValidator extends AbstractValidatorExt {
        @Override
        public void validate() {
            for (ExtendedDataEntity ext : getDataEntities()) {
                DynamicObject bill = ext.getDataEntity();
                if (!isTargetBill(bill) || bill.getBoolean(FIELD_INITIALIZATION)) {
                    continue;
                }
                if (bill.getDynamicObject(FIELD_CONTRACT) == null) {
                    addMessageWithErrCode(ext, "kdcd_contract_required", "合同不能为空");
                }
            }
        }
    }

    private static class InvoiceAmountEqualsValidator extends AbstractValidatorExt {
        @Override
        public void validate() {
            for (ExtendedDataEntity ext : getDataEntities()) {
                DynamicObject bill = ext.getDataEntity();
                if (!isTargetBill(bill) || bill.getBoolean(FIELD_INITIALIZATION)) {
                    continue;
                }

                DynamicObjectCollection measureRows = bill.getDynamicObjectCollection(ENTRY_MEASURE_DETAIL);
                DynamicObjectCollection invoiceRows = bill.getDynamicObjectCollection(ENTRY_INVOICE_DETAIL);
                BigDecimal measureTotal = zeroToDefault(DynamicObjectUtils.sumOf(measureRows, FIELD_TOTAL_TAX));
                BigDecimal invoiceTotal = zeroToDefault(DynamicObjectUtils.sumOf(invoiceRows, FIELD_PRICE_TAX_TOTAL));
                if (measureTotal.compareTo(invoiceTotal) != 0) {
                    addMessageWithErrCode(ext, "kdcd_invoice_amount_not_match", "发票明细价税合计必须等于计量明细本期价税合计");
                }
            }
        }
    }

    private static boolean isTargetBill(DynamicObject bill) {
        DynamicObject billType = bill.getDynamicObject(FIELD_BILL_TYPE);
        return billType != null && TARGET_BILL_TYPE.equals(billType.getString("number"));
    }

    private static BigDecimal zeroToDefault(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }
}
