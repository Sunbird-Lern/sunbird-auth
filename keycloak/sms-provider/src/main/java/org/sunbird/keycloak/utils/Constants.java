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

}
