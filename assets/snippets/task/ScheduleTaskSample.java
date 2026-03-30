/**
 * 后台定时任务示例。
 * <p>
 * 使用场景：
 * 1. 定时扫描并处理业务数据；
 * 2. 异常监控预警；
 * 3. 数据汇总统计；
 * 4. 接口健康检查。
 */
package kd.cd.common.snippets.task;

import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.exception.KDException;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.schedule.executor.AbstractTask;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.cd.core.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;

public class ScheduleTaskSample extends AbstractTask {
    private static final Log log = LogFactory.getLog(ScheduleTaskSample.class);

    private static final String STAT_FORM_ID = "outapilogdailystats";

    @Override
    public void execute(RequestContext requestContext, Map<String, Object> map) throws KDException {
        // 1. 查询统计数据
        LocalDateTime nextHour = LocalDateTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0);
        DynamicObjectCollection stats = QueryServiceHelper.query(
                STAT_FORM_ID,
                "apinum,apiname,successcount,failcount",
                new QFilter("gentime", QCP.equals, nextHour).toArray()
        );

        if (CollectionUtils.isEmpty(stats)) {
            return;
        }

        // 2. 遍历检查是否需要预警
        for (DynamicObject stat : stats) {
            int failCount = stat.getInt("failcount");
            if (failCount == 0) {
                continue;
            }

            int successCount = stat.getInt("successcount");
            String apiNum = stat.getString("apinum");
            String apiName = stat.getString("apiname");

            // 计算失败率
            BigDecimal failRate = BigDecimal.valueOf((long) failCount * 100)
                    .divide(BigDecimal.valueOf((long) successCount + (long) failCount), 2, RoundingMode.DOWN);

            // 判断是否超过阈值
            if (failRate.compareTo(BigDecimal.valueOf(50)) > 0) {
                sendAlarm(apiNum, apiName, successCount, failCount, failRate);
            }
        }
    }

    // --- 发送预警消息 ---
    private void sendAlarm(String apiNum, String apiName, int successCount, int failCount, BigDecimal failRate) {
        String msg = String.format(
                "API异常预警：\n" +
                        "接口编号：%s\n" +
                        "接口名称：%s\n" +
                        "成功次数：%d\n" +
                        "失败次数：%d\n" +
                        "失败率：%.2f%%",
                apiNum, apiName, successCount, failCount, failRate
        );

        log.warn(msg);
    }
}
