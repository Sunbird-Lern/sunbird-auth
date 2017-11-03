package org.sunbird.keycloak;

/**
 * Created by joris on 18/11/2016.
 */
// TODO(shriharshs): Move strings for mobile, smsCode, countryCode, defaultCountry code to configuration
public class KeycloakSmsAuthenticatorConstants {
    public static final String ATTR_MOBILE = "mobile";
    public static final String ANSW_SMS_CODE = "smsCode";
    public static final String COUNTRY_CODE = "+91";
    public static final String DEFAULT_COUNTRY_CODE = "0";

    public static final String CONF_PRP_SMS_CODE_TTL = "sms-auth.code.ttl";
    public static final String CONF_PRP_SMS_CODE_LENGTH = "sms-auth.code.length";
    public static final String CONF_PRP_SMS_TEXT = "sms-auth.msg.text";

    // AWS
    public static final String CONF_PRP_SMS_CLIENTTOKEN = "sms-auth.sms.clienttoken";
    public static final String CONF_PRP_SMS_CLIENTSECRET = "sms-auth.sms.clientsecret";

    // User credentials (used to persist the sent sms code + expiration time cluster wide)
    public static final String USR_CRED_MDL_SMS_CODE = "sms-auth.code";
    public static final String USR_CRED_MDL_SMS_EXP_TIME = "sms-auth.exp-time";

    public static final String SMS_PROVIDER_CONFIGURATIONS_PATH = "sms-provider/awsSnsCreds.json";


}
