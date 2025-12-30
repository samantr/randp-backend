package com.app.enums;

public enum TransactionTypes {

    EXPENSE("EXP","هزینه"),
    TRANSACTION("TRN","تراکنش"),
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

    TransactionTypes(String code, String title) {
        this.code = code;
        this.title = title;
    }

}
