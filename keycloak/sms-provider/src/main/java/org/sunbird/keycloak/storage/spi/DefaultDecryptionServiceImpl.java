package org.sunbird.keycloak.storage.spi;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

public class DefaultDecryptionServiceImpl implements DecryptionService {
  private static Logger logger = Logger.getLogger(DefaultDecryptionServiceImpl.class);
  private static String sunbird_encryption = "";

  private String sunbirdEncryption = "ON";
  private static final String ON = "ON";
  private static Cipher c;

  static {
    try {
      sunbird_encryption = DefaultEncryptionServivceImpl.getSalt();
      Key key = generateKey();
      c = Cipher.getInstance(ALGORITHM);
      c.init(Cipher.DECRYPT_MODE, key);
    } catch (Exception e) {
      logger.error(e);
    }
  }

  @Override
  public Map<String, Object> decryptData(Map<String, Object> data) {
    if (ON.equalsIgnoreCase(sunbirdEncryption)) {
      if (data == null) {
        return data;
      }
      Iterator<Entry<String, Object>> itr = data.entrySet().iterator();
      while (itr.hasNext()) {
        Entry<String, Object> entry = itr.next();
        if (!(entry.getValue() instanceof Map || entry.getValue() instanceof List)
            && null != entry.getValue()) {
          data.put(entry.getKey(), decrypt(entry.getValue() + "", false));
        }
      }
    }
    return data;
  }

  @Override
  public List<Map<String, Object>> decryptData(List<Map<String, Object>> data) {
    if (ON.equalsIgnoreCase(sunbirdEncryption)) {
      if (data == null || data.isEmpty()) {
        return data;
      }

      for (Map<String, Object> map : data) {
        decryptData(map);
      }
    }
    return data;
  }

  @Override
  public String decryptData(String data) {
    return decryptData(data, false);
  }

  @Override
  public String decryptData(String data, boolean throwExceptionOnFailure) {
    if (ON.equalsIgnoreCase(sunbirdEncryption)) {
      if (StringUtils.isBlank(data)) {
        return data;
      } else {
        return decrypt(data, throwExceptionOnFailure);
      }
    } else {
      return data;
    }
  }

  public static String decrypt(String value, boolean throwExceptionOnFailure) {
    try {
      String dValue = null;
      String valueToDecrypt = value.trim();
      for (int i = 0; i < ITERATIONS; i++) {
        byte[] decordedValue = new sun.misc.BASE64Decoder().decodeBuffer(valueToDecrypt);
        byte[] decValue = c.doFinal(decordedValue);
        dValue =
            new String(decValue, StandardCharsets.UTF_8).substring(sunbird_encryption.length());
        valueToDecrypt = dValue;
      }
      return dValue;
    } catch (Exception ex) {
      if (throwExceptionOnFailure) {
        logger.error(ex);
        throw new RuntimeException(ex);
      }
    }
    return value;
  }

  private static Key generateKey() {
    return new SecretKeySpec(keyValue, ALGORITHM);
  }
}
