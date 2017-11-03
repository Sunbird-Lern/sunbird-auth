package org.sunbird.keycloak;

import com.amazonaws.util.StringUtils;
import org.jboss.logging.Logger;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.UserModel;
import org.sunbird.sms.MessageProviderFactory;
import org.sunbird.sms.provider.ISmsProvider;
import org.sunbird.utils.JsonUtil;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Created by joris on 18/11/2016.
 */
public class KeycloakSmsAuthenticatorUtil {

    private static Logger logger = Logger.getLogger(KeycloakSmsAuthenticatorUtil.class);

    public static String getAttributeValue(UserModel user, String attributeName) {
        String result = null;
        List<String> values = user.getAttribute(attributeName);
        if (values != null && values.size() > 0) {
            result = values.get(0);
        }

        return result;
    }

    public static String getConfigString(AuthenticatorConfigModel config, String configName) {
        return getConfigString(config, configName, null);
    }

    public static String getConfigString(AuthenticatorConfigModel config, String configName, String defaultValue) {

        String value = defaultValue;

        if (config.getConfig() != null) {
            // Get value
            value = config.getConfig().get(configName);
        }

        return value;
    }

    public static Long getConfigLong(AuthenticatorConfigModel config, String configName) {
        return getConfigLong(config, configName, null);
    }

    public static Long getConfigLong(AuthenticatorConfigModel config, String configName, Long defaultValue) {

        Long value = defaultValue;

        if (config.getConfig() != null) {
            // Get value
            Object obj = config.getConfig().get(configName);
            try {
                value = Long.valueOf((String) obj); // s --> ms
            } catch (NumberFormatException nfe) {
                logger.error("Can not convert " + obj + " to a number.");
            }
        }

        return value;
    }

    public static String createMessage(String code, String mobileNumber, AuthenticatorConfigModel config) {
        String text = KeycloakSmsAuthenticatorUtil.getConfigString(config, KeycloakSmsAuthenticatorConstants.CONF_PRP_SMS_TEXT);
        text = text.replaceAll("%sms-code%", code);
        text = text.replaceAll("%phonenumber%", mobileNumber);

        return text;
    }

    public static String setDefaultCountryCodeIfZero(String mobileNumber) {
        if (mobileNumber.startsWith(KeycloakSmsAuthenticatorConstants.DEFAULT_COUNTRY_CODE)) {
            mobileNumber = KeycloakSmsAuthenticatorConstants.COUNTRY_CODE + mobileNumber.substring(1);
        } else if (mobileNumber.startsWith(KeycloakSmsAuthenticatorConstants.COUNTRY_CODE)) {
            mobileNumber = mobileNumber;
        } else {
            mobileNumber = KeycloakSmsAuthenticatorConstants.COUNTRY_CODE + mobileNumber;
        }

        return mobileNumber;
    }

    static boolean sendSmsCode(String mobileNumber, String code, AuthenticatorConfigModel config) {
        // Send an SMS

        String smsText = createMessage(code, mobileNumber, config);
        logger.debug("KeycloakSmsAuthenticatorUtil@sendSmsCode : smsText - " + smsText);

        String filePath = new File(KeycloakSmsAuthenticatorConstants.SMS_PROVIDER_CONFIGURATIONS_PATH).getAbsolutePath();
        logger.debug("KeycloakSmsAuthenticatorUtil@sendSmsCode : filePath - " + filePath);

        if (!StringUtils.isNullOrEmpty(filePath)) {
            Map<String, String> configurations = JsonUtil.readFromJson(filePath);
            logger.debug("KeycloakSmsAuthenticatorUtil@sendSmsCode : configurations - " + configurations);

            ISmsProvider amazonSmsProvider = MessageProviderFactory.getSnsClient(configurations);

            if (amazonSmsProvider != null) {
                return amazonSmsProvider.send(setDefaultCountryCodeIfZero(mobileNumber), smsText);
            }
        }

        return false;
    }

    static String getSmsCode(long nrOfDigits) {
        if (nrOfDigits < 1) {
            throw new RuntimeException("Number of digits must be bigger than 0");
        }

        double maxValue = Math.pow(10.0, nrOfDigits); // 10 ^ nrOfDigits;
        Random r = new Random();
        long code = (long) (r.nextFloat() * maxValue);
        return Long.toString(code);
    }

    public static boolean validateTelephoneNumber(String telephoneNumber) {
        String pattern = "\\d{10}|(?:\\d{3}-){2}\\d{4}|\\(\\d{3}\\)\\d{3}-?\\d{4}";
        return telephoneNumber.matches(pattern);
    }
}
