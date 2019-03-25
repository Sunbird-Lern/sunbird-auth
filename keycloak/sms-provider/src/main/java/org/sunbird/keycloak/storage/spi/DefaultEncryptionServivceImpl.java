/** */
package org.sunbird.keycloak.storage.spi;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

/**
 * Default data encryption service
 *
 * @author Manzarul
 */
public class DefaultEncryptionServivceImpl implements EncryptionService {
  private static Logger logger = Logger.getLogger(DefaultEncryptionServivceImpl.class);
  private static String encryption_key = "";
  private String sunbirdEncryption = "";
  private static final String ON = "ON";
  private static Cipher c;

  static {
    try {
      encryption_key = getSalt();
      Key key = generateKey();
      c = Cipher.getInstance(ALGORITHM);
      c.init(Cipher.ENCRYPT_MODE, key);
    } catch (Exception e) {
      logger.error(e);
    }
  }

  public DefaultEncryptionServivceImpl() {
    sunbirdEncryption = System.getenv("sunbird_encryption");
    if(StringUtils.isBlank(sunbirdEncryption)){
      sunbirdEncryption = ON;
    }
  }

  @Override
  public Map<String, Object> encryptData(Map<String, Object> data) throws Exception {
    if (ON.equalsIgnoreCase(sunbirdEncryption)) {
      if (data == null) {
        return data;
      }
      Iterator<Entry<String, Object>> itr = data.entrySet().iterator();
      while (itr.hasNext()) {
        Entry<String, Object> entry = itr.next();
        if (!(entry.getValue() instanceof Map || entry.getValue() instanceof List)
            && null != entry.getValue()) {
          data.put(entry.getKey(), encrypt(entry.getValue() + ""));
        }
      }
    }
    return data;
  }

  @Override
  public List<Map<String, Object>> encryptData(List<Map<String, Object>> data) throws Exception {
    if (ON.equalsIgnoreCase(sunbirdEncryption)) {
      if (data == null || data.isEmpty()) {
        return data;
      }
      for (Map<String, Object> map : data) {
        encryptData(map);
      }
    }
    return data;
  }

  @Override
  public String encryptData(String data) throws Exception {
    if (ON.equalsIgnoreCase(sunbirdEncryption)) {
      if (StringUtils.isBlank(data)) {
        return data;
      }
      if (null != data) {
        return encrypt(data);
      } else {
        return data;
      }
    } else {
      return data;
    }
  }

  /**
   * this method is used to encrypt the password.
   *
   * @param value String password
   * @param encryption_key
   * @return encrypted password.
   * @throws BadPaddingException
   * @throws IllegalBlockSizeException
   */
  @SuppressWarnings("restriction")
  public static String encrypt(String value)
      throws  IllegalBlockSizeException, BadPaddingException {
    String valueToEnc = null;
    String eValue = value;
    for (int i = 0; i < ITERATIONS; i++) {
      valueToEnc = encryption_key + eValue;
      byte[] encValue = c.doFinal(valueToEnc.getBytes(StandardCharsets.UTF_8));
      eValue = new sun.misc.BASE64Encoder().encode(encValue);
    }
    return eValue;
  }

  private static Key generateKey() {
    return new SecretKeySpec(keyValue, ALGORITHM);
  }

  /** @return */
  public static String getSalt() {
    if (StringUtils.isNotBlank(encryption_key)) {
      return encryption_key;
    } else {
      encryption_key = System.getenv("sunbird_encryption_key");
    }
    if (StringUtils.isBlank(encryption_key)) {
      throw new RuntimeException("Invalid encryption key");
    }
    return encryption_key;
  }
}
