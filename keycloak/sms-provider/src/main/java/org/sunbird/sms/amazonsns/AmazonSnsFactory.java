package org.sunbird.sms.amazonsns;


import org.sunbird.sms.provider.ISmsProvider;
import org.sunbird.sms.provider.ISmsProviderFactory;

import java.util.Map;

public class AmazonSnsFactory implements ISmsProviderFactory {
    private static AmazonSnsProvider amazonSnsProvider = null;

    @Override
    public ISmsProvider create(Map<String, String> configurations) {
        if (amazonSnsProvider == null) {
            amazonSnsProvider = new AmazonSnsProvider();
            amazonSnsProvider.configure(configurations);
        }

        return amazonSnsProvider;
    }
}
