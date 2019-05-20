package org.sunbird.sms.msg91;

import java.io.Serializable;
import java.util.List;

public class ProviderDetails implements Serializable {
    private String sender;
    private String route;
    private String country;
    private List<Sms> sms;

    public ProviderDetails(String sender, String route, String country, List<Sms> sms) {
        this.sender = sender;
        this.route = route;
        this.country = country;
        this.sms = sms;
    }
}
