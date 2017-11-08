package org.sunbird.sms.msg91;

import org.sunbird.sms.provider.ISmsProvider;
import org.sunbird.sms.provider.ISmsProviderFactory;

import java.util.Map;

public class Msg91SmsProviderFactory implements ISmsProviderFactory {

    private static Msg91SmsProvider msg91SmsProvider = null;

    @Override
    public ISmsProvider create(Map<String, String> configurations) {
        if (msg91SmsProvider == null){
            msg91SmsProvider = new Msg91SmsProvider();
            msg91SmsProvider.configure(configurations);
        }

        return msg91SmsProvider;
    }
}
