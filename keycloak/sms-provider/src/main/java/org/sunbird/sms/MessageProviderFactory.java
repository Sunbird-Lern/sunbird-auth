package org.sunbird.sms;

import org.sunbird.sms.amazonsns.AmazonSnsFactory;
import org.sunbird.sms.msg91.Msg91SmsProviderFactory;
import org.sunbird.sms.provider.ISmsProvider;

import java.util.Map;

public class MessageProviderFactory {

    private static Msg91SmsProviderFactory msg91SmsProviderFactory;
    private static AmazonSnsFactory amazonSnsFactory = null;


    public static ISmsProvider getMsg91SmsProvider(Map<String, String> configurations) {
        if (msg91SmsProviderFactory == null) {
            msg91SmsProviderFactory = new Msg91SmsProviderFactory();
        }

        return msg91SmsProviderFactory.create(configurations);
    }


    public static ISmsProvider getSnsClient(Map<String, String> configurations) {
        if (amazonSnsFactory == null) {
            amazonSnsFactory = new AmazonSnsFactory();
        }
        return amazonSnsFactory.create(configurations);
    }
}
