package org.sunbird.keycloak.storage.spi;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
@PrepareForTest({StorageId.class,KeycloakSession.class,ComponentModel.class,UserRepository.class,
  UserAdapter.class,RealmModel.class,UserModel.class,EsOperation.class,GroupModel.class})
@PowerMockIgnore({"javax.management.*", "javax.net.ssl.*", "javax.security.*"})
public class CassandraUserStorageProviderTest {

  private static KeycloakSession session = null;
  private static ComponentModel model = null;
  private static UserRepository repository = null;
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
    mockStatic(EsOperation.class);
    groupModel = PowerMockito.mock(GroupModel.class);
    session = PowerMockito.mock(KeycloakSession.class);
    model = PowerMockito.mock(ComponentModel.class);
    repository = PowerMockito.mock(UserRepository.class);
    realm =  PowerMockito.mock(RealmModel.class);
    userAdapter = PowerMockito.mock(UserAdapter.class);
    PowerMockito.when(StorageId.externalId(Mockito.anyString())).thenReturn("12345");
    PowerMockito.when(repository.findUserById("12345")).thenReturn(user);
    PowerMockito.whenNew(UserAdapter.class).withArguments(session, realm, model, user).thenReturn(userAdapter);
  }
  
  @Test
  public void getUserByIdTest(){
    CassandraUserStorageProvider storageProvider = new CassandraUserStorageProvider(session, model, repository);
    userModel = storageProvider.getUserById("12345", realm);
    assertEquals("firstName", userModel.getFirstName());
  }
  
  @Test
  public void getUserByUsernameTest(){
    CassandraUserStorageProvider storageProvider = new CassandraUserStorageProvider(session, model, repository);
    PowerMockito.when(repository.findUserByUsernameOrEmail("username")).thenReturn(userList);
    userModel = storageProvider.getUserByUsername("username", realm);
    assertEquals("firstName", userModel.getFirstName());
    assertEquals("username", userModel.getUsername());
  }
  
  @Test
  public void getUserByEmailTest(){
    CassandraUserStorageProvider storageProvider = new CassandraUserStorageProvider(session, model, repository);
    PowerMockito.when(repository.findUserByUsernameOrEmail("amit@gmail.com")).thenReturn(userList);
    userModel = storageProvider.getUserByEmail("amit@gmail.com", realm);
    assertEquals("amit@gmail.com", userModel.getEmail());
  }
  
  @Test
  public void searchForUserTest(){
    CassandraUserStorageProvider storageProvider = new CassandraUserStorageProvider(session, model, repository);
    PowerMockito.when(repository.findUsers("amit@gmail.com")).thenReturn(userList);
    List<UserModel> userModelList = storageProvider.searchForUser("amit@gmail.com", realm);
    assertEquals("amit@gmail.com", userModelList.get(0).getEmail());
  }
  
  
  @Test
  public void searchForUserTest2(){
    CassandraUserStorageProvider storageProvider = new CassandraUserStorageProvider(session, model, repository);
    PowerMockito.when(repository.findUsers("amit@gmail.com")).thenReturn(userList);
    List<UserModel> userModelList = storageProvider.searchForUser("amit@gmail.com", realm, 0, 5);
    assertEquals("amit@gmail.com", userModelList.get(0).getEmail());
  }
  
  @Test
  public void searchForUserTest3(){
    CassandraUserStorageProvider storageProvider = new CassandraUserStorageProvider(session, model, repository);
    Map<String,String> params = new HashMap<>();
    List<UserModel> userModelList = storageProvider.searchForUser(params,realm,1,5);
    assertEquals(0, userModelList.size());
  }
  
  @Test
  public void searchForUserTest4(){
    CassandraUserStorageProvider storageProvider = new CassandraUserStorageProvider(session, model, repository);
    Map<String,String> params = new HashMap<>();
    List<UserModel> userModelList = storageProvider.searchForUser(params,realm);
    assertEquals(0, userModelList.size());
  }
  
  @Test
  public void getGroupMembersTest(){
    CassandraUserStorageProvider storageProvider = new CassandraUserStorageProvider(session, model, repository);
    List<UserModel> userModelList = storageProvider.getGroupMembers(realm,groupModel);
    assertEquals(0, userModelList.size());
  }
  
  @Test
  public void getGroupMembersTest2(){
    CassandraUserStorageProvider storageProvider = new CassandraUserStorageProvider(session, model, repository);
    List<UserModel> userModelList = storageProvider.getGroupMembers(realm,groupModel,1,5);
    assertEquals(0, userModelList.size());
  }
  
  @Test
  public void getUsersCountTest(){
    CassandraUserStorageProvider storageProvider = new CassandraUserStorageProvider(session, model, repository);
    int userCount = storageProvider.getUsersCount(realm);
    assertEquals(0, userCount);
  }
  
  @Test
  public void getUsersTest(){
    CassandraUserStorageProvider storageProvider = new CassandraUserStorageProvider(session, model, repository);
    List<UserModel> userModelList = storageProvider.getUsers(realm,1,5);
    assertEquals(0, userModelList.size());
  }
  
  @Test
  public void getUsersTest2(){
    CassandraUserStorageProvider storageProvider = new CassandraUserStorageProvider(session, model, repository);
    List<UserModel> userModelList = storageProvider.getUsers(realm);
    assertEquals(0, userModelList.size());
  }
  
  //@Test
  public void searchForUserByUserAttributeTest(){
    CassandraUserStorageProvider storageProvider = new CassandraUserStorageProvider(session, model, repository);
    PowerMockito.when(EsOperation.getUserByKey("phone","9876543210")).thenReturn(userList);
    List<UserModel> userModelList = storageProvider.searchForUserByUserAttribute("phone","9876543210", realm);
    System.out.println(userModelList.size());
    System.out.println(userModelList.get(0).getEmail());
    assertEquals("amit@gmail.com", userModelList.get(0).getEmail());
  }
}
