package org.sunbird.keycloak.storage.spi;

import java.util.List;
import java.util.Map;

/**
 * DecryptionService provides methods for decryption of data.
 */
public interface DecryptionService {

  String ALGORITHM = "AES";
  int ITERATIONS = 3;
  byte[] keyValue =
      new byte[] {'T', 'h', 'i', 's', 'A', 's', 'I', 'S', 'e', 'r', 'c', 'e', 'K', 't', 'e', 'y'};

  /**
   * Decrypt given data (map). Decryption of a key value that is a map or list is skipped.
   *
   * @param data Input data
   * @return Decrypted data
   */
  Map<String, Object> decryptData(Map<String, Object> data);

  /**
   * Decrypt given data (list of map).
   *
   * @param data Input data
   * @return Decrypted data
   */
  List<Map<String, Object>> decryptData(List<Map<String, Object>> data);

  /**
   * Decrypt given data (string).
   *
   * @param data Input data
   * @return Decrypted data
   */
  String decryptData(String data);

  /**
   * Decrypt given data (string) or throw exception in case of any error.
   *
   * @param data Input data
   * @return Decrypted data
   * @throws ProjectCommonException in case of an error during decryption
   */
  String decryptData(String data, boolean throwExceptionOnFailure);

}
