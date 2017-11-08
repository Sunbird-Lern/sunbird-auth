package org.sunbird.sms.provider;

import java.util.Map;

public interface ISmsProviderFactory  {

    ISmsProvider create(Map<String, String> configurations);
}
