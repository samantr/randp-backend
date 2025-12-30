package com.app.enums;

public enum PaymentTypes {
    CASH("CSH","نقد"),
    CHECK("CHK","چک"),
    OTHERS("OTH","سایر"),
    ;

    private String code;
    private String title;

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }

    PaymentTypes(String code, String title) {
        this.code = code;
        this.title = title;
    }



}
