package kd.cd.common;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.ExtendedDataEntity;
import kd.bos.entity.botp.plugin.AbstractConvertPlugIn;
import kd.bos.entity.botp.plugin.args.AfterBizRuleEventArgs;
import kd.bos.entity.botp.plugin.args.AfterBuildDrawFilterEventArgs;
import kd.bos.entity.botp.plugin.args.AfterBuildQueryParemeterEventArgs;
import kd.bos.entity.botp.plugin.args.AfterBuildRowConditionEventArgs;
import kd.bos.entity.botp.plugin.args.AfterConvertEventArgs;
import kd.bos.entity.botp.plugin.args.AfterCreateLinkEventArgs;
import kd.bos.entity.botp.plugin.args.AfterCreateTargetEventArgs;
import kd.bos.entity.botp.plugin.args.AfterFieldMappingEventArgs;
import kd.bos.entity.botp.plugin.args.AfterGetSourceDataEventArgs;
import kd.bos.entity.botp.plugin.args.BeforeBuildGroupModeEventArgs;
import kd.bos.entity.botp.plugin.args.BeforeBuildRowConditionEventArgs;
import kd.bos.entity.botp.plugin.args.BeforeCreateLinkEventArgs;
import kd.bos.entity.botp.plugin.args.BeforeCreateTargetEventArgs;
import kd.bos.entity.botp.plugin.args.BeforeGetSourceDataEventArgs;
import kd.bos.entity.botp.plugin.args.InitVariableEventArgs;
import kd.bos.entity.botp.runtime.ConvertConst;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;

import java.text.SimpleDateFormat;
import java.util.List;

/**
 * 单据转换插件骨架模板（原生 AbstractConvertPlugIn）。
 * 该类仅用于示例写法，生成后请按实际业务删除无用事件并替换占位常量。
 */
public class ConvertPlugInTemplate extends AbstractConvertPlugIn {

    /**
     * 触发时机: 在需要了解当前插件可访问上下文能力时调用。
     * 参数要点: 无入参；仅展示当前插件可通过 this. 访问的方法能力。
     * 典型用途: 作为模板提示，指导在各事件内选择正确的上下文 API。
     */
    private void getContextSample() {
        // this.getSrcMainType();
        // this.getTgtMainType();
        // this.getRule();
        // this.getOpType();
    }

    // 占位常量：复制模板后请统一替换为业务真实 key。
    private static final String FIELD_SRC_REMARK = "remark";
    private static final String FIELD_TGT_REMARK = "description";
    private static final String FIELD_SRC_BILLNO = "billno";
    private static final String FIELD_SRC_DATE = "date";
    private static final String FIELD_SRC_AMOUNT = "amount";
    private static final String FIELD_DRAW_FILTER_BILLNO = "billno";
    // ===== 初始化与规则编译阶段 =====

    /**
     * 触发时机：转换规则启动后、读取源单前。
     * 参数要点：e 提供源单/目标单/规则上下文，可用于阶段初始化。
     * 典型用途：根据源单/目标单/规则初始化本地变量，决定后续分支处理。
     */
    @Override
    public void initVariable(InitVariableEventArgs e) {
        super.initVariable(e);
        // 最小示例：按上下文分流（选单/下推，源单/目标单类型）。
        this.getSrcMainType();
        this.getTgtMainType();
        this.getRule();
        this.getOpType();
    }

    /**
     * 触发时机：系统编译源单筛选条件前。
     * 参数要点：e 可调整条件编译上下文。
     * 典型用途：忽略规则原生条件或追加插件定制条件。
     */
    @Override
    public void beforeBuildRowCondition(BeforeBuildRowConditionEventArgs e) {
        super.beforeBuildRowCondition(e);
        this.getRule();
    }

    /**
     * 触发时机：系统编译源单筛选条件后。
     * 参数要点：e 可读取已编译条件并做补充。
     * 典型用途：对最终条件做补充说明或记录诊断信息。
     */
    @Override
    public void afterBuildRowCondition(AfterBuildRowConditionEventArgs e) {
        super.afterBuildRowCondition(e);
        this.getSrcMainType();
    }

    /**
     * 触发时机：分单/合并策略构建前。
     * 参数要点：e 包含分组策略上下文，可动态改写。
     * 典型用途：动态调整分组字段、分单策略。
     */
    @Override
    public void beforeBuildGroupMode(BeforeBuildGroupModeEventArgs e) {
        super.beforeBuildGroupMode(e);
        this.getTgtMainType();
    }

    // ===== 源单取数阶段 =====

    /**
     * 触发时机：系统确认字段映射并构建取数参数后。
     * 参数要点：可通过 e.addSrcField(...) 补字段、e.getQFilters() 补过滤。
     * 典型用途：补充插件要用到的源单字段，微调系统生成的 QFilter 条件。
     */
    @Override
    public void afterBuildQueryParemeter(AfterBuildQueryParemeterEventArgs e) {
        super.afterBuildQueryParemeter(e);
        // 最小示例：补充源单字段。
        e.addSrcField(FIELD_SRC_BILLNO);
        e.addSrcField(FIELD_SRC_DATE);
        e.addSrcField(FIELD_SRC_AMOUNT);
        // 若需要基础资料引用属性，可传类似 "currency.name"。
        // e.addSrcField("currency.name");

        // 最小示例：在既有条件基础上追加过滤。
        // e.getQFilters().add(new QFilter("forbidstatus", QCP.not_equals, "B"));
    }

    /**
     * 触发时机：执行源单查询前。
     * 参数要点：e 提供最终查询上下文，可做最后一次拦截/补充。
     * 典型用途：最后一次调整取数语句与条件。
     */
    @Override
    public void beforeGetSourceData(BeforeGetSourceDataEventArgs e) {
        super.beforeGetSourceData(e);
        this.getOpType();
    }

    /**
     * 触发时机：系统拿到源单数据后。
     * 参数要点：e 可读取源数据并按需替换或补充。
     * 典型用途：读取源数据并补充额外引用数据，必要时替换系统数据集。
     */
    @Override
    public void afterGetSourceData(AfterGetSourceDataEventArgs e) {
        super.afterGetSourceData(e);
        this.getSrcMainType();
    }

    // ===== 目标单构建阶段 =====

    /**
     * 触发时机：创建目标单数据包前（常见于选单追加场景）。
     * 参数要点：e 提供目标构建上下文，可设置默认参数。
     * 典型用途：对现有目标单做预处理。
     */
    @Override
    public void beforeCreateTarget(BeforeCreateTargetEventArgs e) {
        super.beforeCreateTarget(e);
        this.getTgtMainType();
    }

    /**
     * 触发时机：创建目标单数据包后（常见于下推场景）。
     * 参数要点：e 可访问目标单数据包并填充默认值。
     * 典型用途：为目标单设置默认值。
     */
    @Override
    public void afterCreateTarget(AfterCreateTargetEventArgs e) {
        super.afterCreateTarget(e);
        this.getRule();
    }

    /**
     * 触发时机：系统字段映射完成后。
     * 参数要点：e.getSourceDataEntity()/e.getTargetDataEntity() 分别为源/目标对象。
     * 典型用途：在系统映射结果上补值或覆盖字段。
     */
    @Override
    public void afterFieldMapping(AfterFieldMappingEventArgs e) {
        super.afterFieldMapping(e);
        // 取目标单，单据头数据包 （可能会生成多张单，是个数组）
        String targetEntityNumber = this.getTgtMainType().getName();
        ExtendedDataEntity[] billDataEntitys = e.getTargetExtDataEntitySet().FindByEntityKey(targetEntityNumber);
        SimpleDateFormat timesdf = new SimpleDateFormat("yyyy-MM-dd");
        // 逐单处理
        for(ExtendedDataEntity billDataEntity : billDataEntitys){
            // 取当前目标单，对应的源单行
            List<DynamicObject> srcRows = (List<DynamicObject>)billDataEntity.getValue(ConvertConst.ConvExtDataKey_SourceRows);
            // 取源单第一行上的字段值，忽略其他行
            DynamicObject srcRow = srcRows.get(0);
            String billno = (String)e.getFldProperties().get("billno").getValue(srcRow);
            // 给目标单字段赋值
            billDataEntity.setValue("fieldKey", billno);
        }
    }

    /**
     * 触发时机：业务规则执行后。
     * 参数要点：e 提供规则执行结果上下文。
     * 典型用途：做规则后字段修正。
     */
    @Override
    public void afterBizRule(AfterBizRuleEventArgs e) {
        super.afterBizRule(e);
        this.getOpType();
    }

    // ===== 关联关系与收尾阶段 =====

    /**
     * 触发时机：记录来源/去向关联关系前。
     * 参数要点：可通过 e.setCancel(true) 取消关系写入。
     * 典型用途：按条件取消关联关系写入。
     */
    @Override
    public void beforeCreateLink(BeforeCreateLinkEventArgs e) {
        super.beforeCreateLink(e);
        // 最小示例：按业务需要取消关联关系。
        // e.setCancel(true);
    }

    /**
     * 触发时机：记录关联关系后。
     * 参数要点：e 提供关联链路上下文，可追加同步逻辑。
     * 典型用途：基于已生成关联关系做数据同步携带。
     */
    @Override
    public void afterCreateLink(AfterCreateLinkEventArgs e) {
        super.afterCreateLink(e);
        this.getRule();
    }

    /**
     * 触发时机：整个转换流程末尾。
     * 参数要点：e 汇总了本次转换结果，可做统一收尾。
     * 典型用途：对生成目标单做最终收尾处理。
     */
    @Override
    public void afterConvert(AfterConvertEventArgs e) {
        super.afterConvert(e);
        this.getOpType();
    }

    // ===== 选单场景补充事件 =====

    /**
     * 触发时机：系统生成选单条件后。
     * 参数要点：可通过 e.setPlugFilter(...) 注入插件过滤条件。
     * 典型用途：叠加插件自定义选单过滤。
     */
    @Override
    public void afterBuildDrawFilter(AfterBuildDrawFilterEventArgs e) {
        super.afterBuildDrawFilter(e);
        // 最小示例：限制选单只显示单据编号包含 1 的数据。
        QFilter filter = new QFilter(FIELD_DRAW_FILTER_BILLNO, QCP.like, "1");
        e.setPlugFilter(filter);
    }
}
