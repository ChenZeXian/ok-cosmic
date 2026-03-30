package kd.cd.common;

import java.io.Serializable;
import java.util.Map;

import kd.bos.exception.ErrorCode;
import kd.bos.exception.KDBizException;
import kd.bos.openapi.common.custom.annotation.ApiController;
import kd.bos.openapi.common.custom.annotation.ApiGetMapping;
import kd.bos.openapi.common.custom.annotation.ApiMapping;
import kd.bos.openapi.common.custom.annotation.ApiModel;
import kd.bos.openapi.common.custom.annotation.ApiParam;
import kd.bos.openapi.common.custom.annotation.ApiPostMapping;
import kd.bos.openapi.common.custom.annotation.ApiRequestBody;
import kd.bos.openapi.common.custom.annotation.ApiResponseBody;
import kd.bos.openapi.common.result.CustomApiResult;

/**
 * 开放平台自定义 API 骨架模板（注解模式）。
 * 该类仅用于示例写法，生成后请按实际业务删除无用接口并替换占位常量。
 */
@ApiController(value = "open", desc = "示例开放 API")
@ApiMapping("/template")
public class OpenApiControllerTemplate implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 触发时机: 在需要了解当前控制器可通过 this. 访问哪些能力时调用。
     * 参数要点: 无入参；OpenAPI 控制器不具备表单插件的 view/model/pageCache 上下文。
     * 典型用途: 告诉 AI 当前类只能通过 this. 访问本类方法与普通 Java 成员，业务上下文需从入参获取。
     */
    private void getContextSample() {
        // this.getClass();
        // this.getById(1001L);
        // this.saveMap(java.util.Collections.emptyMap());
        // this.saveBody(new UserModel());
    }

    private static final String ERR_CODE_PARAM = "OpenApi_001";

    @ApiGetMapping(value = "/getById", desc = "按 id 查询示例")
    public CustomApiResult<String> getById(
            @ApiParam(value = "主键ID", required = true, example = "1001") Long id) {
        if (id == null || id <= 0L) {
            return CustomApiResult.fail(ERR_CODE_PARAM, "id 必须大于 0");
        }
        return CustomApiResult.success("U-" + id);
    }

    @ApiPostMapping(value = "/saveMap", desc = "Map 入参保存示例")
    public CustomApiResult<@ApiResponseBody("true-成功，false-失败") Boolean> saveMap(
            @ApiParam(value = "业务数据", required = true) Map<String, Object> data) {
        if (data == null || data.get("id") == null) {
            throw new KDBizException(new ErrorCode(ERR_CODE_PARAM, "id 不能为空"));
        }
        return CustomApiResult.success(Boolean.TRUE);
    }

    @ApiPostMapping(value = "/saveBody", desc = "@ApiRequestBody 入参示例")
    public CustomApiResult<@ApiResponseBody("保存后的模型") UserModel> saveBody(
            @ApiRequestBody(value = "用户模型", required = true) UserModel model) {
        if (model == null || model.getUserName() == null) {
            return CustomApiResult.fail(ERR_CODE_PARAM, "userName 不能为空");
        }
        return CustomApiResult.success(model);
    }

    /**
     * 示例请求体模型。
     */
    @ApiModel
    public static class UserModel implements Serializable {
        private static final long serialVersionUID = 1L;

        @ApiParam(value = "用户ID", example = "1001")
        private Long id;

        @ApiParam(value = "用户名", required = true, example = "Tom")
        private String userName;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }
    }
}
