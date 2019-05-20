package org.sunbird.sms.provider;

import java.util.Map;

public interface ISmsProvider {

    void configure(Map<String, String> configurations);

    boolean send(String phoneNumber, String smsText);
}
