package bit.iot.common.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller 基类
 * 提供统一的响应方法和异常处理
 * 
 * @author chenhao
 * @date 2026/3/9
 */
public abstract class BaseController {

    /**
     * 成功响应（无数据）
     */
    protected <T> Result<T> success() {
        return Result.success();
    }

    /**
     * 成功响应（带数据）
     */
    protected <T> Result<T> success(T data) {
        return Result.success(data);
    }

    /**
     * 成功响应（带数据和总数，用于分页）
     */
    protected <T> Result<List<T>> success(Page<T> page) {
        return Result.success(page.getRecords(), page.getTotal());
    }

    /**
     * 成功响应（自定义消息）
     */
    protected <T> Result<T> success(String message) {
        return new Result<>(200, message);
    }

    /**
     * 成功响应（带数据和自定义消息）
     */
    protected <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }

    /**
     * 失败响应
     */
    protected <T> Result<T> error(String message) {
        return Result.error(message);
    }

    /**
     * 失败响应（带错误码）
     */
    protected <T> Result<T> error(Integer code, String message) {
        return Result.error(code, message);
    }

    /**
     * 抛出业务异常
     */
    protected void throwBusinessException(String message) {
        throw new BusinessException(message);
    }

    /**
     * 抛出业务异常（带错误码）
     */
    protected void throwBusinessException(Integer code, String message) {
        throw new BusinessException(code, message);
    }

    /**
     * 构建分页响应数据（兼容旧格式）
     */
    protected <T> Map<String, Object> buildPageResult(Page<T> page) {
        Map<String, Object> response = new HashMap<>();
        response.put("records", page.getRecords());
        response.put("total", page.getTotal());
        response.put("size", page.getSize());
        response.put("current", page.getCurrent());
        response.put("pages", (page.getTotal() + page.getSize() - 1) / page.getSize());
        return response;
    }
}
