package org.sunbird.sms.msg91;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jboss.logging.Logger;
import org.sunbird.sms.SMSAuthenticatorUtil;
import org.sunbird.sms.SmsConfigurationType;
import org.sunbird.sms.provider.ISmsProvider;

import javax.ws.rs.HttpMethod;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

public class Msg91SmsProvider implements ISmsProvider {

    private static Logger logger = Logger.getLogger(Msg91SmsProvider.class);

    private Map<String, String> configurations;

    @Override
    public void configure(Map<String, String> configurations) {
        this.configurations = configurations;
    }

    @Override
    public boolean send(String phoneNumber, String smsText) {
       return sendSmsCode(phoneNumber, smsText);
    }

    private boolean sendSmsCode(String mobileNumber, String smsText) {
        // Send an SMS
        logger.debug("Sending " + smsText + "  to mobileNumber " + mobileNumber);

        String smsUrl = SMSAuthenticatorUtil.getConfigString(configurations, SmsConfigurationType.CONF_SMS_URL);
        String smsUsr = SMSAuthenticatorUtil.getConfigString(configurations, SmsConfigurationType.CONF_SMS_USERNAME);
        String smsPwd = SMSAuthenticatorUtil.getConfigString(configurations, SmsConfigurationType.CONF_SMS_PASSWORD);

        String proxyUrl = SMSAuthenticatorUtil.getConfigString(configurations, SmsConfigurationType.CONF_SMS_PROXY_URL);
        String proxyUsr = SMSAuthenticatorUtil.getConfigString(configurations, SmsConfigurationType.CONF_SMS_PROXY_USERNAME);
        String proxyPwd = SMSAuthenticatorUtil.getConfigString(configurations, SmsConfigurationType.CONF_SMS_PROXY_PASSWORD);
        String contentType = SMSAuthenticatorUtil.getConfigString(configurations, SmsConfigurationType.CONF_SMS_CONTENT_TYPE);

        CloseableHttpClient httpClient = null;
        try {
            URL smsURL = (smsUrl != null && smsUrl.length() > 0) ? new URL(smsUrl) : null;
            URL proxyURL = (proxyUrl != null && proxyUrl.length() > 0) ? new URL(proxyUrl) : null;

            if (smsURL == null) {
                logger.error("SMS gateway URL is not configured.");
                return false;
            }


            CredentialsProvider credsProvider;
            if (SMSAuthenticatorUtil.getConfigString(configurations, SmsConfigurationType.CONF_SMS_AUTHTYPE, "").equals(SmsConfigurationType.CONF_AUTH_METHOD_INMESSAGE)) {
                credsProvider = getCredentialsProvider(null, null, proxyUsr, proxyPwd, smsURL, proxyURL);
            } else {
                credsProvider = getCredentialsProvider(smsUsr, smsPwd, proxyUsr, proxyPwd, smsURL, proxyURL);
            }

            HttpHost target = new HttpHost(smsURL.getHost(), smsURL.getPort(), smsURL.getProtocol());
            HttpHost proxy = (proxyURL != null) ? new HttpHost(proxyURL.getHost(), proxyURL.getPort(), proxyURL.getProtocol()) : null;

            httpClient = HttpClients.custom()
                    .setDefaultCredentialsProvider(credsProvider)
                    .build();

            RequestConfig requestConfig;
            requestConfig = RequestConfig.custom()
                    .setProxy(proxy)
                    .build();

            String httpMethod = SMSAuthenticatorUtil.getConfigString(configurations, SmsConfigurationType.CONF_SMS_METHOD_TYPE);
            if (httpMethod.equals(HttpMethod.GET)) {

                String path = getPath(mobileNumber, smsURL, smsText);

                HttpGet httpGet = new HttpGet(path);
                httpGet.setConfig(requestConfig);
                if (isNotEmpty(contentType)) {
                    httpGet.addHeader("Content-type", contentType);
                }

                logger.debug("Executing request " + httpGet.getRequestLine() + " to " + target + " via " + proxy);

                CloseableHttpResponse response = httpClient.execute(target, httpGet);
                StatusLine sl = response.getStatusLine();
                response.close();
                if (sl.getStatusCode() != 200) {
                    logger.error("SMS code for " + mobileNumber + " could not be sent: " + sl.getStatusCode() + " - " + sl.getReasonPhrase());
                }
                return sl.getStatusCode() == 200;

            } else if (httpMethod.equals(HttpMethod.POST)) {

                String path = getPath(mobileNumber, smsURL, smsText);
                String uri = smsURL.getProtocol() + "://" + smsURL.getHost() + ":" + smsURL.getPort() + path;

                HttpPost httpPost = new HttpPost(uri);
                httpPost.setConfig(requestConfig);
                if (isNotEmpty(contentType)) {
                    httpPost.addHeader("Content-type", contentType);
                }

                HttpEntity entity = new ByteArrayEntity(smsText.getBytes("UTF-8"));
                httpPost.setEntity(entity);

                CloseableHttpResponse response = httpClient.execute(httpPost);
                StatusLine sl = response.getStatusLine();
                response.close();
                if (sl.getStatusCode() != 200) {
                    logger.error("SMS code for " + mobileNumber + " could not be sent: " + sl.getStatusCode() + " - " + sl.getReasonPhrase());
                }
                return sl.getStatusCode() == 200;
            }
            return true;
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
    }

    private CredentialsProvider getCredentialsProvider(String smsUsr, String smsPwd, String proxyUsr, String proxyPwd, URL smsURL, URL proxyURL) {
        CredentialsProvider credsProvider = new BasicCredentialsProvider();

        // If defined, add BASIC Authentication parameters
        if (isNotEmpty(smsUsr) && isNotEmpty(smsPwd)) {
            credsProvider.setCredentials(
                    new AuthScope(smsURL.getHost(), smsURL.getPort()),
                    new UsernamePasswordCredentials(smsUsr, smsPwd));

        }

        // If defined, add Proxy Authentication parameters
        if (isNotEmpty(proxyUsr) && isNotEmpty(proxyPwd)) {
            credsProvider.setCredentials(
                    new AuthScope(proxyURL.getHost(), proxyURL.getPort()),
                    new UsernamePasswordCredentials(proxyUsr, proxyPwd));

        }
        return credsProvider;
    }

    private String getPath(String mobileNumber, URL smsURL, String smsText) throws UnsupportedEncodingException {
        String path = smsURL.getPath();
        if(smsURL.getQuery() != null && smsURL.getQuery().length() > 0) {
            path += smsURL.getQuery();
        }
        path = path.replaceFirst("\\{message\\}", URLEncoder.encode(smsText, "UTF-8"));
        path = path.replaceFirst("\\{phonenumber\\}", URLEncoder.encode(mobileNumber, "UTF-8"));
        return path;
    }

    private boolean isNotEmpty(String s) {
        return (s != null && s.length() > 0);
    }
}
