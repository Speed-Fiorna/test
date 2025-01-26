package com.example.difytest.constant;

/**
 * 返回状态码
 */
public enum ResultCode {
    SUCCESS(1, ""),
    FAIL(2, "参数异常");
    private int result;
    private String errorMsg;
    public int getResult(){return result;}
    public String getErrorMsg(){return errorMsg;}

    public void setResult(int result) {this.result = result;}

    public void setErrorMsg(String errorMsg) {this.errorMsg = errorMsg;}
    ResultCode(int result, String errorMsg) {
        this.result = result;
        this.errorMsg = errorMsg;
    }
}
