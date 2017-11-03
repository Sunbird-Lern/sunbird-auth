package org.sunbird.sms.provider;

public interface ISmsProvider extends ISmsProviderConfigurations {

    boolean send(String phoneNumber, String smsText);
}
