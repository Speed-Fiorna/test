package com.example.difytest.util;

import com.example.difytest.constant.ResultCode;

import java.util.HashMap;
import java.util.Map;

public class ResponseUtil {
    public static Map<String, Object> success(Object data) {
        Map<String, Object> object = new HashMap();
        object.put("result", ResultCode.SUCCESS);
        object.put("data", data);
        return object;
    }

    public static Map<String, Object> fail(String msg) {
        Map<String, Object> object = new HashMap();
        object.put("result", ResultCode.FAIL);
        object.put("error",msg);
        return object;
    }
    public static Map<String, Object> success() {
        Map<String, Object> object = new HashMap();
        object.put("result", ResultCode.SUCCESS);
        return object;
    }

    public static Map<String, Object> fail() {
        Map<String, Object> object = new HashMap();
        object.put("result", ResultCode.FAIL);
        return object;
    }
}
