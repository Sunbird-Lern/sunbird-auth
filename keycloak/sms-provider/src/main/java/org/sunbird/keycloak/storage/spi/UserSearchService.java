package org.sunbird.keycloak.storage.spi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;
import org.sunbird.keycloak.utils.Constants;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.sunbird.keycloak.utils.HttpClientUtil;

public class UserSearchService {

  private static Logger logger = Logger.getLogger(UserSearchService.class);

  private UserSearchService() {}

  @SuppressWarnings({"unchecked"})
  public static List<User> getUserByKey(String key, String value) {
    Map<String, Object> userRequest = new HashMap<>();
    Map<String, Object> request = new HashMap<>();
    request.put("key",key.toLowerCase());
    request.put("value", value);
    request.put("fields", Arrays.asList("email","firstName","lastName","id","phone","userName","status"));
    userRequest.put("request", request);
    String userLookupUrl = System.getenv("sunbird_user_service_base_url")+"/private/user/v1/lookup";
    Map<String, Object> resMap =
      post(userRequest, userLookupUrl, System.getenv(Constants.SUNBIRD_LMS_AUTHORIZATION));
    logger.info("UserSearchService:getUserByKey responseMap "+resMap);
    Map<String, Object> result = null;
    List<Map<String, Object>> content = null;
    if (null != resMap) {
      result = (Map<String, Object>) resMap.get("result");
    }
    if (null != result) {
      content = (List<Map<String, Object>>) result.get("response");
    }
    if (null != content) {
      List<User> userList = new ArrayList<>();
      if (!content.isEmpty()) {
        content.forEach(userMap -> {
          if (null != userMap) {
            userList.add(createUser(userMap));
          }
        });
      }
      return userList;
    }
    return Collections.emptyList();
  }

  private static User createUser(Map<String, Object> userMap) {
    User user = new User();
    user.setEmail((String) userMap.get(Constants.EMAIL));
    user.setFirstName((String) userMap.get("firstName"));
    user.setId((String) userMap.get(Constants.ID));
    user.setLastName((String) userMap.get("lastName"));
    user.setPhone((String) userMap.get(Constants.PHONE));
    user.setUsername((String) userMap.get("userName"));
    user.setCountryCode((String) userMap.get("countryCode"));
    if ( null != userMap.get("status") && ((Integer)userMap.get("status")) == 0) {
      user.setEnabled(false);
    } else {
      user.setEnabled(true);
    }
    return user;
  }

  public static Map<String, Object> post(Map<String, Object> requestBody, String uri,
                                         String authorizationKey) {
    try {
      logger.info("UserSearchService:post: uri = " + uri+ ", body = "+requestBody);
      ObjectMapper mapper = new ObjectMapper();
      HttpClientUtil.getInstance();
      String authKey = Constants.BEARER + " " + authorizationKey;
      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
      headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
      if (StringUtils.isNotBlank(authKey)) {
        headers.put(HttpHeaders.AUTHORIZATION, authKey);
      }
      String response = HttpClientUtil.post(uri, mapper.writeValueAsString(requestBody), headers);
      return mapper.readValue(response,
        new TypeReference<Map<String, Object>>() {});
    }catch (Exception ex) {
      logger.error("UserSearchService:post: Exception occurred = " + ex);
    }
    return null;
  }
}