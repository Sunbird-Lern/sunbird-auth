package org.sunbird.keycloak.storage.spi;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jboss.logging.Logger;
import org.sunbird.keycloak.utils.Constants;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class UserSearchService {

  private static Logger logger = Logger.getLogger(UserSearchService.class);

  private UserSearchService() {}

  @SuppressWarnings({"unchecked"})
  public static List<User> getUserByKey(String key, String value) {
    Map<String, Object> userRequest = new HashMap<>();
    Map<String, Object> request = new HashMap<>();
    Map<String, String> filters = new HashMap<>();
    filters.put(key, value);
    request.put("filters", filters);
    request.put("fields", Arrays.asList("email","firstName","lastName","id","phone","userName","countryCode","status"));
    userRequest.put("request", request);
    String searchUrl = System.getenv("sunbird_user_service_base_url")+"/private/user/v1/search";
    Map<String, Object> resMap =
        post(userRequest, searchUrl, System.getenv(Constants.SUNBIRD_LMS_AUTHORIZATION));
    logger.info("UserSearchService:getUserByKey responseMap "+resMap);
    Map<String, Object> result = null;
    Map<String, Object> responseMap = null;
    List<Map<String, Object>> content = null;
    if (null != resMap) {
      result = (Map<String, Object>) resMap.get("result");
    }
    if (null != result) {
      responseMap = (Map<String, Object>) result.get("response");
    }
    if (null != responseMap) {
      content = (List<Map<String, Object>>) (responseMap).get("content");
    }
    if (null != content) {
      List<User> userList = new ArrayList<>();
      if (!content.isEmpty()) {
        logger.info("usermap is not null from ES");
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
    logger.info("UserSearchService: post called");
    try (CloseableHttpClient client = HttpClients.createDefault()) {
      ObjectMapper mapper = new ObjectMapper();
      HttpPost httpPost = new HttpPost(uri);
      logger.info("UserSearchService:post: uri = " + uri+ ", body = "+requestBody);
      String authKey = Constants.BEARER + " " + authorizationKey;
      StringEntity entity = new StringEntity(mapper.writeValueAsString(requestBody));
      httpPost.setEntity(entity);
      httpPost.setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
      httpPost.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
      if (StringUtils.isNotBlank(authKey)) {
        httpPost.setHeader(HttpHeaders.AUTHORIZATION, authKey);
      }
     // httpPost.setHeader("x-authenticated-user-token", getToken());
      CloseableHttpResponse response = client.execute(httpPost);
      logger.info("UserSearchService:post: statusCode = " + response.getStatusLine().getStatusCode());
      return mapper.readValue(response.getEntity().getContent(),
          new TypeReference<Map<String, Object>>() {});
    } catch (Exception e) {
      logger.error("UserSearchService:post: Exception occurred = " + e);
    }
    return null;
  }

  public static String getToken() { 
    String userName = System.getenv("sunbird_sso_username");
    String password = System.getenv("sunbird_sso_password");
    String sunbirdAuthBaseUrl = System.getenv("sunbird_sso_url");
    String urlPath =
        "realms/" + System.getenv("sunbird_sso_realm") + "/protocol/openid-connect/token";
    try (CloseableHttpClient client = HttpClients.createDefault()) {
      HttpPost httpPost = new HttpPost(sunbirdAuthBaseUrl + urlPath);
      StringEntity entity = new StringEntity("client_id=" + System.getenv("sunbird_sso_client_id")
          + "&username=" + userName + "&password=" + password + "&grant_type=password");
      logger.debug("UserSearchService:post: request entity = " + entity);
      httpPost.setEntity(entity);
      httpPost.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
      HttpResponse response = client.execute(httpPost);
      Map<String, Object> map = getResponse(response);
      String token = (String) map.get("access_token");
      if (StringUtils.isNotBlank(token)) {
        return token;
      }
      return "";
    } catch (Exception e) {
      logger.error("UserSearchService: Exception occurred = " + e);
    }
    return "";
  }

  public static Map<String, Object> getResponse(HttpResponse response) {
    InputStream inStream = null;
    BufferedReader reader = null;
    StringBuilder builder = new StringBuilder();
    if (200 == response.getStatusLine().getStatusCode()) {
      try {
        inStream = response.getEntity().getContent();
        reader = new BufferedReader(new InputStreamReader(inStream, StandardCharsets.UTF_8));
        String line = null;
        while ((line = reader.readLine()) != null) {
          builder.append(line);
          String res = builder.toString();
          if (StringUtils.isNotBlank(res)) {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(res, new TypeReference<Map<String, Object>>() {});
          }
        }
      } catch (Exception ex) {
        logger.error("UserSearchService:getResponse: Exception occurred = " + ex);
      }
    }
    return Collections.emptyMap();
  }
}
