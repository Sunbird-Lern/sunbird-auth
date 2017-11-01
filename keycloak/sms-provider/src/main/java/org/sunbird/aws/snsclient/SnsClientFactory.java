package org.sunbird.aws.snsclient;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.SetSMSAttributesRequest;

/**
 * Created by nickpack on 09/08/2017.
 */
// TODO(shriharshs): Create an interface for the SMS client factories
public class SnsClientFactory {
    private static AmazonSNSClient snsClient = null;

    public static AmazonSNSClient getSnsClient(String clientToken, String clientSecret) {
        if (null == snsClient) {
            BasicAWSCredentials CREDENTIALS = new BasicAWSCredentials(clientToken, clientSecret);
            snsClient = new AmazonSNSClient(CREDENTIALS).withRegion(Region.getRegion(Regions.AP_SOUTHEAST_1));

            SetSMSAttributesRequest setRequest = new SetSMSAttributesRequest()
                    .addAttributesEntry("DefaultSMSType", "Transactional");

            snsClient.setSMSAttributes(setRequest);
        }
        return snsClient;
    }
}
