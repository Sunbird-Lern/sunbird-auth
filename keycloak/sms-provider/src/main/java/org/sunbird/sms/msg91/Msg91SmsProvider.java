package org.sunbird.sms.msg91;

import com.amazonaws.util.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jboss.logging.Logger;
import org.sunbird.sms.SMSConfigurationUtil;
import org.sunbird.sms.SmsConfigurationConstants;
import org.sunbird.sms.provider.ISmsProvider;
import org.sunbird.utils.JsonUtil;

import javax.ws.rs.HttpMethod;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Msg91SmsProvider implements ISmsProvider {

    private static Logger logger = Logger.getLogger(Msg91SmsProvider.class);
    private static String BASE_URL = "http://api.msg91.com/";
    private static String GET_URL = "api/sendhttp.php?";
    private static String POST_URL = "api/v2/sendsms";

    private Map<String, String> configurations;

    @Override
    public void configure(Map<String, String> configurations) {
        this.configurations = configurations;
    }

    @Override
    public boolean send(String phoneNumber, String smsText) {
        return sendSms(phoneNumber, smsText);
    }

    private boolean sendSms(String mobileNumber, String smsText) {
        // Send an SMS
        logger.debug("Msg91SmsProvider@Sending " + smsText + "  to mobileNumber " + mobileNumber);

        String authKey = SMSConfigurationUtil.getConfigString(configurations, SmsConfigurationConstants.CONF_AUTH_KEY);
        String sender = SMSConfigurationUtil.getConfigString(configurations, SmsConfigurationConstants.CONF_SMS_SENDER);
        String country = SMSConfigurationUtil.getConfigString(configurations, SmsConfigurationConstants.CONF_SMS_COUNTRY);
        String smsMethodType = SMSConfigurationUtil.getConfigString(configurations, SmsConfigurationConstants.CONF_SMS_METHOD_TYPE);
        String smsRoute = SMSConfigurationUtil.getConfigString(configurations, SmsConfigurationConstants.CONF_SMS_ROUTE);
        String httpMethod = SMSConfigurationUtil.getConfigString(configurations, SmsConfigurationConstants.CONF_SMS_METHOD_TYPE);

        logger.debug("Msg91SmsProvider@SMS Provider parameters \n" +
                "Gateway - " + BASE_URL + "\n" +
                "authKey - " + authKey + "\n" +
                "sender - " + sender + "\n" +
                "country - " + country + "\n" +
                "smsMethodType - " + smsMethodType + "\n" +
                "smsRoute - " + smsRoute + "\n"
        );


        CloseableHttpClient httpClient = null;
        try {

            httpClient = HttpClients.createDefault();

            String path = null;

            if (!StringUtils.isNullOrEmpty(sender) && !StringUtils.isNullOrEmpty(smsRoute)
                    && !StringUtils.isNullOrEmpty(mobileNumber) && !StringUtils.isNullOrEmpty(authKey) && !StringUtils.isNullOrEmpty(country)
                    && !StringUtils.isNullOrEmpty(smsText)) {

                mobileNumber = removePlusFromMobileNumber(mobileNumber);

                logger.debug("Msg91SmsProvider - after removePlusFromMobileNumber " + mobileNumber);

                if (httpMethod.equals(HttpMethod.GET)) {
                    logger.debug("Inside GET");
                    path = getCompletePath(BASE_URL + GET_URL, sender, smsRoute, mobileNumber, authKey, country, URLEncoder.encode(smsText, "UTF-8"));

                    logger.debug("Msg91SmsProvider -Executing request - " + path);

                    HttpGet httpGet = new HttpGet(path);

                    CloseableHttpResponse response = httpClient.execute(httpGet);
                    StatusLine sl = response.getStatusLine();
                    response.close();
                    if (sl.getStatusCode() != 200) {
                        logger.error("SMS code for " + mobileNumber + " could not be sent: " + sl.getStatusCode() + " - " + sl.getReasonPhrase());
                    }
                    return sl.getStatusCode() == 200;
                } else if (httpMethod.equals(HttpMethod.POST)) {
                    logger.debug("Inside POST");

                    path = BASE_URL + POST_URL;
                    logger.debug("Msg91SmsProvider -Executing request - " + path);

                    HttpPost httpPost = new HttpPost(path);

                    //add content-type headers
                    httpPost.setHeader("content-type", "application/json");

                    //add authkey header
                    httpPost.setHeader("authkey", authKey);

                    List<String> mobileNumbers = new ArrayList<>();
                    mobileNumbers.add(mobileNumber);

                    //create sms
                    Sms sms = new Sms(URLEncoder.encode(smsText, "UTF-8"), mobileNumbers);

                    List<Sms> smsList = new ArrayList<>();
                    smsList.add(sms);

                    //create body
                    ProviderDetails providerDetails = new ProviderDetails(sender, smsRoute, country, smsList);

                    String providerDetailsString = JsonUtil.toJson(providerDetails);

                    if (!StringUtils.isNullOrEmpty(providerDetailsString)) {
                        logger.debug("Msg91SmsProvider - Body - " + providerDetailsString);

                        HttpEntity entity = new ByteArrayEntity(providerDetailsString.getBytes("UTF-8"));
                        httpPost.setEntity(entity);

                        CloseableHttpResponse response = httpClient.execute(httpPost);
                        StatusLine sl = response.getStatusLine();
                        response.close();
                        if (sl.getStatusCode() != 200) {
                            logger.error("SMS code for " + mobileNumber + " could not be sent: " + sl.getStatusCode() + " - " + sl.getReasonPhrase());
                        }
                        return sl.getStatusCode() == 200;
                    } else {
                        return false;
                    }
                }

            } else {
                logger.debug("Msg91SmsProvider - Some mandatory parameters are empty!");
                return false;
            }
        } catch (IOException e) {
            logger.error(e);
            return false;
        } finally {
            if (httpClient != null) {
                try {
                    httpClient.close();
                } catch (IOException ignore) {
                    // Ignore ...
                }
            }
        }
        return false;
    }

    private String removePlusFromMobileNumber(String mobileNumber) {
        logger.debug("Msg91SmsProvider - removePlusFromMobileNumber " + mobileNumber);

        if (mobileNumber.startsWith("+")) {
            return mobileNumber.substring(1);
        }
        return mobileNumber;
    }

    private String getCompletePath(String gateWayUrl, String sender, String smsRoute, String mobileNumber, String authKey, String country, String smsText) {
        String completeUrl = gateWayUrl
                + "sender=" + sender
                + "&route=" + smsRoute
                + "&mobiles=" + mobileNumber
                + "&authkey=" + authKey
                + "&country=" + country
                + "&message=" + smsText;
        return completeUrl;
    }

}
