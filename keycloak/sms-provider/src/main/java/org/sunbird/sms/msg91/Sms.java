package org.sunbird.sms.msg91;

import java.io.Serializable;
import java.util.List;

public class Sms implements Serializable{

    private String message;
    private List<String> to;

    public Sms(String message, List<String> to) {
        this.message = message;
        this.to = to;
    }
}
