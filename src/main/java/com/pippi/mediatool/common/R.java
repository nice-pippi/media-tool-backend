package com.pippi.mediatool.common;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author: hong
 * @CreateTime: 2026-02-10
 * @Description: 统一返回结果实体
 * @Version: 1.0
 */
@Data
public class R<T> implements Serializable {

    private Integer code;

    private String message; //错误信息

    private T data; //数据

    public static <T> R<T> success(T object) {
        R<T> r = new R<T>();
        r.code = 200;
        r.data = object;
        r.message = "操作成功";
        return r;
    }

    public static <T> R<T> success() {
        R<T> r = new R<>();
        r.code = 200;
        r.data = null;
        r.message = "操作成功";
        return r;
    }

    public static <T> R<T> error(String msg) {
        R<T> r = new R<>();
        r.code = 500;
        r.message = msg;
        return r;
    }
}