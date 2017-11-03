package org.sunbird.sms;

import java.util.Map;

/**
 * Created by joris on 18/11/2016.
 */
public class SMSAuthenticatorUtil {

    public static String getConfigString(Map<String, String> config, String configName) {
        return getConfigString(config, configName, null);
    }

    public static String getConfigString(Map<String, String> config, String configName, String defaultValue) {

        String value = defaultValue;

        if (config.containsKey(configName)) {
            // Get value
            value = config.get(configName);
        }

        return value;
    }

}
