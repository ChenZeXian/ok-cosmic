/**
 * 基础资料查询示例。
 * <p>
 * 使用场景：根据编码、ID 或批量条件加载基础资料并回写界面。
 * 关键点：
 * 1. 基础资料优先走 BusinessDataServiceHelper.loadFromCache(...)。
 * 2. 需要先按编码查主键时，优先用 QueryUtils.queryPkByNumber(...)。
 * 3. 批量场景先查主键集合，再一次性 loadFromCache，避免循环查库。
 * 4. 带组织权限过滤时使用 BaseDataServiceHelper.queryBaseData(...)。
 */
package kd.cd.common.snippets.query;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.basedata.BaseDataServiceHelper;
import kd.cd.common.plugin.AbstractFormPluginExt;
import kd.cd.common.util.QueryUtils;
import kd.cd.core.util.CollectionUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class BaseDataQuerySample extends AbstractFormPluginExt {
    private static final String BD_SUPPLIER = "bd_supplier";
    private static final String BD_CUSTOMER = "bd_customer";
    private static final String BOS_USER = "bos_user";
    private static final String BD_TAXRATE = "bd_taxrate";
    private static final String FIELD_ID = "id";
    private static final String FIELD_NUMBER = "number";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_ENABLE = "enable";
    private static final String FIELD_STATUS = "status";

    // ==================== 场景1：按主键批量走缓存查询 ====================

    /**
     * 根据主键集合批量走缓存，返回 id -> DynamicObject 映射
     * 适用场景：已知主键集合，需要批量获取完整对象
     */
    public Map<Object, DynamicObject> loadEnabledAuditedSuppliers(Collection<?> supplierIds) {
        if (CollectionUtils.isEmpty(supplierIds)) {
            return Collections.emptyMap();
        }

        QFilter filter = new QFilter(FIELD_ID, QCP.in, supplierIds)
                .and(new QFilter(FIELD_ENABLE, QCP.equals, true))
                .and(new QFilter(FIELD_STATUS, QCP.equals, "C"));
        return BusinessDataServiceHelper.loadFromCache(BD_SUPPLIER,
                String.join(",", FIELD_ID, FIELD_NUMBER, FIELD_NAME), filter.toArray());
    }

    /**
     * 批量加载用户信息（项目中常见场景：获取审批人姓名等）
     *
     * @param userIds 用户主键集合
     * @return 用户名称拼接字符串
     */
    public String batchLoadUserNames(Collection<Object> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return "";
        }
        Map<Object, DynamicObject> map = BusinessDataServiceHelper.loadFromCache(BOS_USER,
                new QFilter[]{new QFilter(FIELD_ID, QCP.in, userIds)});
        return Arrays.toString(map.values().stream().map(o -> o.get(FIELD_NAME)).toArray());
    }

    /**
     * 批量加载并构建 Map（按编码或名称分组）
     *
     * @param supplierIds 供应商主键集合
     * @return 编码 -> DynamicObject 映射
     */
    public Map<String, DynamicObject> loadSuppliersMapByNumber(Collection<?> supplierIds) {
        Map<Object, DynamicObject> idMap = loadEnabledAuditedSuppliers(supplierIds);
        return idMap.values().stream()
                .collect(Collectors.toMap(o -> o.getString(FIELD_NUMBER), o -> o, (a, b) -> a));
    }

    // ==================== 场景2：按编码批量查询 ====================

    /**
     * 先按业务条件查主键集合，再批量加载缓存对象
     */
    public Map<Object, DynamicObject> queryAndLoadSuppliersByNumbers(Collection<String> supplierNumbers) {
        if (CollectionUtils.isEmpty(supplierNumbers)) {
            return Collections.emptyMap();
        }
        QFilter filter = new QFilter(FIELD_NUMBER, QCP.in, supplierNumbers);
        Set<Object> supplierIds = QueryUtils.queryMatchedPkSet(
                BD_SUPPLIER,
                filter.toArray()
        );
        return loadEnabledAuditedSuppliers(supplierIds);
    }

    /**
     * 根据税率值批量查询税率档案
     *
     * @param taxRates 税率值集合（如 6, 9, 13）
     * @return 税率值 -> 税率档案对象
     */
    public Map<java.math.BigDecimal, DynamicObject> loadTaxRateByValues(Collection<java.math.BigDecimal> taxRates) {
        if (CollectionUtils.isEmpty(taxRates)) {
            return Collections.emptyMap();
        }
        QFilter filter = new QFilter("taxrate", QCP.in, taxRates)
                .and(FIELD_ENABLE, QCP.equals, true)
                .and(FIELD_STATUS, QCP.equals, "C");
        Map<Object, DynamicObject> map = BusinessDataServiceHelper.loadFromCache(BD_TAXRATE,
                String.join(",", FIELD_ID, FIELD_NUMBER, FIELD_NAME, "taxrate"), filter.toArray());
        return map.values().stream()
                .collect(Collectors.toMap(o -> o.getBigDecimal("taxrate"), o -> o, (a, b) -> a));
    }

    // ==================== 场景3：带组织权限过滤的批量查询 ====================

    /**
     * 根据税务登记号查询供应商/客户（带组织权限过滤）
     * 适用场景：发票导入时根据税号匹配往来户
     *
     * @param orgId    组织ID
     * @param taxNo    税务登记号
     * @param isPurBiz 是否采购业务（true:供应商, false:客户）
     * @return 供应商/客户对象
     */
    public DynamicObject querySupplierOrCustomerByTaxNo(long orgId, String taxNo, boolean isPurBiz) {
        QFilter qfilter = new QFilter(FIELD_ENABLE, QCP.equals, Boolean.TRUE)
                .and(FIELD_STATUS, QCP.equals, "C")
                .and(new QFilter("tx_register_no", QCP.equals, taxNo));

        String entity = isPurBiz ? BD_SUPPLIER : BD_CUSTOMER;
        DynamicObjectCollection results = BaseDataServiceHelper.queryBaseData(
                entity, orgId, qfilter, String.join(",", FIELD_ID, FIELD_NUMBER, FIELD_NAME, "entry_bank.bankaccount"));

        if (!CollectionUtils.isEmpty(results)) {
            return results.get(0);
        }
        return null;
    }

    // ==================== 场景5：批量查询用于数据转换/回填 ====================

    /**
     * 批量查询基础资料名称并做展示转换。
     * <p>
     * 为了避免“不同基础资料的名称字段不一致”这一类误导，
     * 示例里把 nameField 显式作为参数传入。
     *
     * @param entityId  基础资料标识
     * @param nameField 名称字段，如 name / fullname / shortname
     * @param ids       主键集合
     * @return 名称拼接字符串
     */
    public String batchLoadDimensionNames(String entityId, String nameField, Set<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return "";
        }
        Map<Object, DynamicObject> valsFromDb = BusinessDataServiceHelper.loadFromCache(
                entityId, nameField, new QFilter(FIELD_ID, QCP.in, ids).toArray());
        List<String> names = valsFromDb.values().stream()
                .map(x -> x.getString(nameField))
                .collect(Collectors.toList());
        return String.join("，", names);
    }

    // ==================== 场景6：查询主键集合用于后续处理 ====================

    /**
     * 查询满足条件的主键集合（用于后续批量操作）
     *
     * @param formId  表单标识
     * @param filter  过滤条件
     * @return 主键集合
     */
    public Set<Object> queryPkSetForBatchProcess(String formId, QFilter filter) {
        return QueryUtils.queryMatchedPkSet(formId, filter.toArray());
    }
}
