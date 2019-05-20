package org.sunbird.keycloak.storage.spi;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({System.class,CloseableHttpClient.class,HttpClients.class,HttpResponse.class,
  InputStream.class,BufferedReader.class,InputStreamReader.class,StatusLine.class,
  HttpEntity.class})
@PowerMockIgnore({"javax.management.*", "javax.net.ssl.*", "javax.security.*"})
@Ignore
public class UserSearchServiceTest {
  
  private static CloseableHttpClient client = null;
  //private static HttpResponse httpResponse = null;
  private static InputStream inputStream = null;
  private static InputStreamReader inputStreamReader = null;
  private static BufferedReader bufferedReader = null;
  private static HttpResponse httpResponse = null;
  private static StatusLine statusLine = null;
  private static HttpEntity httpEntity = null;

  @BeforeClass
  public static void setUp() throws Exception {
    client = PowerMockito.mock(CloseableHttpClient.class);
    inputStream = PowerMockito.mock(InputStream.class);
    inputStreamReader = PowerMockito.mock(InputStreamReader.class);
    bufferedReader = PowerMockito.mock(BufferedReader.class);
    httpResponse = PowerMockito.mock(CloseableHttpResponse.class);
    //httpResponse = PowerMockito.mock(HttpResponse.class);
    statusLine = PowerMockito.mock(StatusLine.class);
    httpEntity = PowerMockito.mock(HttpEntity.class);
    //HttpResponse res = PowerMockito.spy(new HttpResponse(request, httpRequest));
    mockStatic(System.class);
    mockStatic(HttpClients.class);
    PowerMockito.when(System.getenv("sunbird_sso_username")).thenReturn("sunbird_sso_username");
    PowerMockito.when(System.getenv("sunbird_sso_password")).thenReturn("sunbird_sso_password");
    PowerMockito.when(System.getenv("sunbird_sso_url")).thenReturn("sunbird_sso_url");
    PowerMockito.when(System.getenv("sunbird_sso_realm")).thenReturn("sunbird_sso_realm");
    PowerMockito.when(System.getenv("sunbird_sso_client_id")).thenReturn("sunbird_sso_client_id");
    PowerMockito.when(HttpClients.createDefault()).thenReturn(client);
    //PowerMockito.when(client.execute(Mockito.anyObject())).thenReturn(httpResponse);
    PowerMockito.when(httpResponse.getStatusLine()).thenReturn(statusLine);
    PowerMockito.when(statusLine.getStatusCode()).thenReturn(200);
    PowerMockito.when(httpResponse.getEntity()).thenReturn(httpEntity);
    PowerMockito.when(httpEntity.getContent()).thenReturn(inputStream);
    PowerMockito.whenNew(InputStreamReader.class).withArguments(inputStream, StandardCharsets.UTF_8).thenReturn(inputStreamReader);
    PowerMockito.whenNew(BufferedReader.class).withArguments(inputStreamReader).thenReturn(bufferedReader);
    PowerMockito.when(bufferedReader.readLine()).thenReturn("{\"access_token\":\"token1\"}");
    
  }

  @Test
  public void getUserByKeyTest(){
    String token = UserSearchService.getToken(); 
    assertEquals("token1",token);
  }
}
