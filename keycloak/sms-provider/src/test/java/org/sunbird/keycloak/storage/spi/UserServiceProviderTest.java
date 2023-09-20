package org.sunbird.keycloak.storage.spi;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.StorageId;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({StorageId.class,KeycloakSession.class,ComponentModel.class,UserService.class,
  UserAdapter.class,RealmModel.class,UserModel.class,UserSearchService.class,GroupModel.class})
@PowerMockIgnore({"javax.management.*", "javax.net.ssl.*", "javax.security.*"})
public class UserServiceProviderTest {

  private static KeycloakSession session = null;
  private static ComponentModel model = null;
  private static UserService userService = null;
  private static UserAdapter userAdapter = null;
  private static RealmModel realm = null;
  private static UserModel userModel = null;
  private static User user = null;
  private static List<User> userList = new ArrayList<>();
  private static GroupModel groupModel = null;
  
  
  @BeforeClass
  public static void setUp() throws Exception {
    user = new User("12345", "firstName", "lastName");
    user.setUsername("username");
    user.setEmail("amit@gmail.com");
    user.setPhone("9876543210");
    userList.add(user);
    mockStatic(StorageId.class);
    mockStatic(UserSearchService.class);
    groupModel = PowerMockito.mock(GroupModel.class);
    session = PowerMockito.mock(KeycloakSession.class);
    model = PowerMockito.mock(ComponentModel.class);
    userService = PowerMockito.mock(UserService.class);
    realm =  PowerMockito.mock(RealmModel.class);
    userAdapter = PowerMockito.mock(UserAdapter.class);
    PowerMockito.when(StorageId.externalId(Mockito.anyString())).thenReturn("12345");
    PowerMockito.when(userService.getById("12345")).thenReturn(user);
    PowerMockito.whenNew(UserAdapter.class).withArguments(session, realm, model, user).thenReturn(userAdapter);
  }
  
  @Test
  public void getUserByIdTest(){
    UserServiceProvider userServiceProvider = new UserServiceProvider(session, model, userService);
    userModel = userServiceProvider.getUserById(realm,"12345");
    assertEquals("firstName", userModel.getFirstName());
  }
  
  @Test
  public void getUserByUsernameTest(){
    UserServiceProvider userServiceProvider = new UserServiceProvider(session, model, userService);
    PowerMockito.when(userService.getByUsername("username")).thenReturn(userList);
    userModel = userServiceProvider.getUserByUsername(realm,"username");
    assertEquals("firstName", userModel.getFirstName()); 
    assertEquals("username", userModel.getUsername());
  }
  
  @Test
  public void getUserByEmailTest(){
    UserServiceProvider userServiceProvider = new UserServiceProvider(session, model, userService);
    PowerMockito.when(userService.getByUsername("amit@gmail.com")).thenReturn(userList);
    userModel = userServiceProvider.getUserByEmail(realm,"amit@gmail.com");
    assertEquals("amit@gmail.com", userModel.getEmail());
  }
  
  @Test
  public void searchForUserTest(){
    UserServiceProvider userServiceProvider = new UserServiceProvider(session, model, userService);
    PowerMockito.when(userService.getByUsername("amit@gmail.com")).thenReturn(userList);
    List<UserModel> userModelList = userServiceProvider.searchForUserStream(realm, "amit@gmail.com").collect(Collectors.toList());
    assertEquals("amit@gmail.com", userModelList.get(0).getEmail());
  }
  
  
  @Test
  public void searchForUserWithPaginationTest(){
    UserServiceProvider userServiceProvider = new UserServiceProvider(session, model, userService);
    PowerMockito.when(userService.getByUsername("amit@gmail.com")).thenReturn(userList);
    List<UserModel> userModelList = userServiceProvider.searchForUserStream(realm, "amit@gmail.com", 0, 5).collect(Collectors.toList());
    assertEquals("amit@gmail.com", userModelList.get(0).getEmail());
  }
  
  @Test
  public void searchForUserWithParamsWithPaginationTest(){
    UserServiceProvider userServiceProvider = new UserServiceProvider(session, model, userService);
    Map<String,String> params = new HashMap<>();
    List<UserModel> userModelList = userServiceProvider.searchForUserStream(realm, params, 1,5).collect(Collectors.toList());
    assertEquals(0, userModelList.size());
  }
  
  @Test
  public void searchForUserWithParamsTest(){
    UserServiceProvider userServiceProvider = new UserServiceProvider(session, model, userService);
    Map<String,String> params = new HashMap<>();
    List<UserModel> userModelList = userServiceProvider.searchForUserStream(realm, params).collect(Collectors.toList());
    assertEquals(0, userModelList.size());
  }
  
  @Test
  public void getGroupMembersTest(){
    UserServiceProvider userServiceProvider = new UserServiceProvider(session, model, userService);
    List<UserModel> userModelList = userServiceProvider.getGroupMembersStream(realm,groupModel).collect(Collectors.toList());
    assertEquals(0, userModelList.size());
  }
  
  @Test
  public void getGroupMembersWithPaginationTest(){
    UserServiceProvider userServiceProvider = new UserServiceProvider(session, model, userService);
    List<UserModel> userModelList = userServiceProvider.getGroupMembersStream(realm, groupModel,1,5).collect(Collectors.toList());
    assertEquals(0, userModelList.size());
  }
  
  @Test
  public void getUsersCountTest(){
    UserServiceProvider userServiceProvider = new UserServiceProvider(session, model, userService);
    int userCount = userServiceProvider.getUsersCount(realm);
    assertEquals(0, userCount);
  }
  
  /*@Test
  public void getUsersWithPaginationTest(){
    UserServiceProvider userServiceProvider = new UserServiceProvider(session, model, userService);
    List<UserModel> userModelList = userServiceProvider.getUsers(realm,1,5);
    assertEquals(0, userModelList.size());
  }
  
  @Test
  public void getUsersTest(){
    UserServiceProvider userServiceProvider = new UserServiceProvider(session, model, userService);
    List<UserModel> userModelList = userServiceProvider.getUsers(realm);
    assertEquals(0, userModelList.size());
  }*/
  
 
}
