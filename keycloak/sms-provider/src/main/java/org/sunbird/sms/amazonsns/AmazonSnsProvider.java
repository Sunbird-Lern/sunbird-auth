package org.sunbird.sms.amazonsns;

import com.amazonaws.services.sns.model.MessageAttributeValue;
import com.amazonaws.services.sns.model.PublishRequest;
import org.jboss.logging.Logger;
import org.sunbird.aws.snsclient.SnsClientFactory;
import org.sunbird.sms.SMSConfigurationUtil;
import org.sunbird.sms.SmsConfigurationConstants;
import org.sunbird.sms.provider.ISmsProvider;

import java.util.HashMap;
import java.util.Map;

public class AmazonSnsProvider implements ISmsProvider {

    private static Logger logger = Logger.getLogger(AmazonSnsProvider.class);

    private Map<String, String> configurations;

    @Override
    public boolean send(String phoneNumber, String smsText) {
        logger.debug("AmazonSnsProvider@send : phoneNumber - " + phoneNumber + " & Sms text - " + smsText);

        Map<String, MessageAttributeValue> smsAttributes = new HashMap<String, MessageAttributeValue>();
        smsAttributes.put("AWS.SNS.SMS.SenderID", new MessageAttributeValue()
                .withStringValue("HomeOffice")
                .withDataType("String"));

        String clientToken = SMSConfigurationUtil.getConfigString(configurations, SmsConfigurationConstants.CONF_SMS_TOKEN);
        String clientSecret = SMSConfigurationUtil.getConfigString(configurations, SmsConfigurationConstants.CONF_SMS_SECRET);

        logger.debug("AmazonSnsProvider@send : clientToken - " + clientToken + " & clientSecret - " + clientSecret);


        try {
            SnsClientFactory.getSnsClient(clientToken, clientSecret).publish(new PublishRequest()
                    .withMessage(smsText)
                    .withPhoneNumber(phoneNumber)
                    .withMessageAttributes(smsAttributes));

            return true;
        } catch (Exception e) {
            logger.debug("AmazonSnsProvider@Send : Exception Caught -" + e.getMessage());
            return false;
        }
    }

    @Override
    public void configure(Map<String, String> configurations) {
        this.configurations = configurations;
    }
}
