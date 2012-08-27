package org.springframework.social.openidconnect.api.impl;

import java.net.URI;

import org.apache.log4j.Logger;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.social.oauth2.AbstractOAuth2ApiBinding;
import org.springframework.social.openidconnect.PayPalConnectionProperties;
import org.springframework.social.openidconnect.api.PayPal;
import org.springframework.social.openidconnect.api.PayPalProfile;
import org.springframework.social.support.URIBuilder;

/**
 * Templates which binds provider to spring social API. This template is also used to get {@code PayPalProfile} from
 * userinfo endpoint.
 * 
 * @author abprabhakar
 * 
 */
public class PayPalTemplate extends AbstractOAuth2ApiBinding implements PayPal {

    /**
     * Logger for PayPalTemplate
     */
    private Logger logger = Logger.getLogger(PayPalTemplate.class);

    /**
     * Access token given by PayPal Access.
     */
    private String accessToken;

    /**
     * User Info end point.
     */
    private String userInfoUrl;

    /**
     * Default constructor.
     */
    public PayPalTemplate() {
    } // Used if token was not obtained

    /**
     * Constructors which accept acess token
     * 
     * @param accessToken - Access token given by PayPal Access.
     * 
     * @param userInfoUrl - User Info endpoint.
     */
    public PayPalTemplate(String accessToken, String userInfoUrl) {
        super(accessToken);
        this.accessToken = accessToken;
        this.userInfoUrl = userInfoUrl;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.social.openidconnect.api.PayPal#getUserProfile()
     */
    @Override
    public PayPalProfile getUserProfile() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", this.accessToken);
        ResponseEntity<PayPalProfile> jsonResponse = getRestTemplate().exchange(buildURI(), HttpMethod.GET,
                new HttpEntity<byte[]>(headers), PayPalProfile.class);
        PayPalProfile profile = jsonResponse.getBody();
        // Password cannot be blank for Spring security. Setting it to access token rather keeping it as "N/A".
        profile.setPassword(this.accessToken);
        if (logger.isDebugEnabled()) {
            logger.debug("access token  " + accessToken);
            logger.debug("PayPal Profile receieved  " + profile);
        }
        return profile;
    }

    /**
     * Builds uri for user info service endpoint. Default one given by {@linkplain PayPalConnectionProperties} will be
     * {@code userInfoUrl} if null.
     * 
     * @return - Uri with parameter
     */
    private URI buildURI() {
        URIBuilder uriBuilder;
        if (userInfoUrl != null) {
            uriBuilder = URIBuilder.fromUri(this.userInfoUrl);
        } else {
            logger.debug("Using default user info url  " + userInfoUrl);
            uriBuilder = URIBuilder.fromUri(PayPalConnectionProperties.getUserInfoEndpoint());
        }

        return uriBuilder.queryParam("schema", "openid").build();
    }

    /**
     * Sets the access token which is internally used as password.
     * 
     * @param accessToken - Token given by token service.
     */
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    /**
     * Sets user info endpoint for this template.
     * 
     * @param userInfoUrl - User Info Endpoint
     */
    public void setUserInfoUrl(String userInfoUrl) {
        this.userInfoUrl = userInfoUrl;
    }

}
