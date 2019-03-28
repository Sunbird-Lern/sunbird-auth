package org.sunbird.keycloak.utils;

public class Constants {
  
  private Constants(){}
  
  public static final String MULTIPLE_USER_ASSOCIATED_WITH_PHONE = "Multiple users are associated with this phone.";
  public static final String MULTIPLE_USER_ASSOCIATED_WITH_EMAIL = "Multiple users are associated with this email.";
  public static final String MULTIPLE_USER_ASSOCIATED_WITH_USERNAME = "Multiple users are associated with this username.";
  public static final String REDIRECT_URI = "redirectUri";
  public static final String CLIENT_ID = "clientId";
  public static final String REQUIRED_ACTION = "requiredAction";
  public static final String USERNAME = "userName";
  public static final String EXPIRATION_IN_SECS = "expirationInSecs";
  public static final String IS_AUTH_REQUIRED = "isAuthRequired";
  public static final String KEY = "key";
  public static final String LINK = "link";
  public static final String BEARER = "Bearer";
  public static final String ADMIN = "admin";
  public static final int DEFAULT_LINK_EXPIRATION_IN_SECS = 7200;
  public static final String UPDATE_PASSWORD = "UPDATE_PASSWORD";
  public static final String VERIFY_EMAIL = "VERIFY_EMAIL";
  
  public static final String ERROR_NOT_ENABLED = " not enabled";
  public static final String ERROR_NOT_AUTHORIZED = "Not Authorized.";
  public static final String ERROR_USER_IS_DISABLED = "User is disabled.";
  public static final String ERROR_CREATE_LINK = "Failed to create link";
  public static final String ERROR_REALM_ADMIN_ROLE_ACCESS = "Does not have realm admin role.";
  public static final String ERROR_INVALID_PARAMETER_VALUE = "Invalid value {0} for parameter {1}.";
  public static final String ERROR_MANDATORY_PARAM_MISSING = "Mandatory parameter {0} is missing.";
  public static final String OTP = "otp";
  public static final String EMAIL = "email";
  public static final String TTL = "ttl";
  public static final String SUNBIRD_LMS_AUTHORIZATION = "sunbird_authorization";
  
  public static final String MAIL_SUBJECT = "Reset password";
  public static final String SUBJECT = "subject";
  public static final String EMAIL_TEMPLATE_TYPE = "emailTemplateType";
  public static final String REALM_NAME = "realmName";
  public static final String SEND_NOTIFICATION_URI = "/user/v1/notification/email";
  public static final String SUNBIRD_LMS_BASE_URL = "sunbird_lms_base_url";
  public static final String BODY = "body";
  public static final String RECIPIENT_EMAILS = "recipientEmails";
  public static final String FORGOT_PASSWORD_EMAIL_TEMPLATE = "forgotPasswordWithOTP";
  public static final String REQUEST = "request";
  public static final String FIRST_NAME = "firstname";
  public static final String ID = "id";
  public static final String PHONE = "phone";
  public static final String SUNBIRD_CASSANDRA_IP = "sunbird_cassandra_host";
  public static final String SUNBIRD_CASSANDRA_PORT = "sunbird_cassandra_port";
  public static final String LAST_NAME = "lastname";
}
