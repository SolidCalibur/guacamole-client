/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.guacamole.auth.saml;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.onelogin.saml2.authn.AuthnRequest;
import com.onelogin.saml2.authn.SamlResponse;
import com.onelogin.saml2.exception.SettingsException;
import com.onelogin.saml2.exception.ValidationError;
import com.onelogin.saml2.http.HttpRequest;
import com.onelogin.saml2.servlet.ServletUtils;
import com.onelogin.saml2.settings.Saml2Settings;
import com.onelogin.saml2.util.Util;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import org.apache.guacamole.auth.saml.conf.ConfigurationService;
import org.apache.guacamole.auth.saml.form.SAMLRedirectField;
import org.apache.guacamole.auth.saml.user.AuthenticatedUser;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.form.Field;
import org.apache.guacamole.net.auth.Credentials;
import org.apache.guacamole.net.auth.credentials.CredentialsInfo;
import org.apache.guacamole.net.auth.credentials.GuacamoleInvalidCredentialsException;
import org.apache.guacamole.net.auth.credentials.GuacamoleInsufficientCredentialsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * Class that provides services for use by the SAML Authentication Provider class.
 */
public class AuthenticationProviderService {

    /**
     * Logger for this class.
     */
    private final Logger logger = LoggerFactory.getLogger(AuthenticationProviderService.class);

    /**
     * Service for retrieving SAML configuration information.
     */
    @Inject
    private ConfigurationService confService;

    /**
     * Provider for AuthenticatedUser objects.
     */
    @Inject
    private Provider<AuthenticatedUser> authenticatedUserProvider;

    /**
     * Returns an AuthenticatedUser representing the user authenticated by the
     * given credentials.
     *
     * @param credentials
     *     The credentials to use for authentication.
     *
     * @return
     *     An AuthenticatedUser representing the user authenticated by the
     *     given credentials.
     *
     * @throws GuacamoleException
     *     If an error occurs while authenticating the user, or if access is
     *     denied.
     */
    public AuthenticatedUser authenticateUser(Credentials credentials)
            throws GuacamoleException {

        HttpServletRequest request = credentials.getRequest();

        // Initialize and configure SAML client.
        Saml2Settings samlSettings = confService.getSamlSettings();

        if (request != null) {
            logger.debug(">>>SAML<<< Looking for SAML response.");
            String samlResponseParam = request.getParameter("SAMLResponse");
            if (samlResponseParam != null) {
                logger.debug(">>>SAML<<< Processing SAML response.");
                // Do SAML Response stuff here.
                HttpRequest httpRequest = ServletUtils.makeHttpRequest(request);
                try {
                    SamlResponse samlResponse = new SamlResponse(samlSettings, httpRequest);
                    logger.debug(">>>SAML<<< Username: {}", samlResponse.getNameId());
                    String username = samlResponse.getNameId().toLowerCase();
                    if (username != null) {
                        credentials.setUsername(username);
                        AuthenticatedUser authenticatedUser = authenticatedUserProvider.get();
                        authenticatedUser.init(username, credentials);
                        return authenticatedUser;
                    }
                }
                catch (IOException e) {
                    logger.warn("Error during I/O while parsing SAML response: {}", e.getMessage());
                    logger.debug("Received IOException when trying to parse SAML response.", e);
                    throw new GuacamoleInvalidCredentialsException("Error during SAML login.",
                                CredentialsInfo.USERNAME_PASSWORD);
                }
                catch (ParserConfigurationException e) {
                    logger.warn("Error configuring XML parser: {}", e.getMessage());
                    logger.debug("Received ParserConfigurationException when trying to parse SAML response.", e);
                    throw new GuacamoleInvalidCredentialsException("Error during SAML login.",
                                CredentialsInfo.USERNAME_PASSWORD);
                }
                catch (SAXException e) {
                    logger.warn("Bad XML when parsing SAML response: {}", e.getMessage());
                    logger.debug("Received SAXException while parsing SAML response.", e);
                    throw new GuacamoleInvalidCredentialsException("Error during SAML login.",
                                CredentialsInfo.USERNAME_PASSWORD);
                }
                catch (SettingsException e) {
                    logger.warn("Error with SAML settings while parsing response: {}", e.getMessage());
                    logger.debug("Received SettingsException while parsing SAML response.", e);
                    throw new GuacamoleInvalidCredentialsException("Error during SAML login.",
                                CredentialsInfo.USERNAME_PASSWORD);
                }
                catch (ValidationError e) {
                    logger.warn("Error validating SAML response: {}", e.getMessage());
                    logger.debug("Received ValidationError while parsing SAML response.", e);
                    throw new GuacamoleInvalidCredentialsException("Error during SAML login.",
                                CredentialsInfo.USERNAME_PASSWORD);
                }
                catch (XPathExpressionException e) {
                    logger.warn("Problem with XML parsing response: {}", e.getMessage());
                    logger.debug("Received XPathExpressionException while processing SAML response.", e);
                    throw new GuacamoleInvalidCredentialsException("Error during SAML login.",
                                CredentialsInfo.USERNAME_PASSWORD);
                }
                catch (Exception e) {
                    logger.warn("Exception while getting name from SAML response: {}", e.getMessage());
                    logger.debug("Received Exception while retrieving name from SAML response.", e);
                    throw new GuacamoleInvalidCredentialsException("Error during SAML login.",
                                CredentialsInfo.USERNAME_PASSWORD);
                }
            }
        }

        AuthnRequest samlReq = new AuthnRequest(samlSettings);
        String reqString;
        try {
            reqString = samlSettings.getIdpSingleSignOnServiceUrl() + "?SAMLRequest=" +
                    Util.urlEncoder(samlReq.getEncodedAuthnRequest());
        }
        catch (IOException e) {
            logger.error("Error encoding authentication request to string: {}", e.getMessage());
            logger.debug("Got IOException encoding authentication request.", e);
            throw new GuacamoleInvalidCredentialsException("Error during SAML authentication.", CredentialsInfo.USERNAME_PASSWORD);
        }
        logger.debug(">>>SAML<<< Request URL: {}", request.getRequestURL().toString());
        logger.debug(">>>SAML<<< Request: {}", reqString);

        throw new GuacamoleInsufficientCredentialsException("Redirecting to SAML IdP.",
                new CredentialsInfo(Arrays.asList(new Field[] {
                    new SAMLRedirectField(reqString)
                }))
        );


    }

}
