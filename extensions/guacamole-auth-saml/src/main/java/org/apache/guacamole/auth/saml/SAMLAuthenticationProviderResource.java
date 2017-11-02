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
import com.onelogin.saml2.util.Util;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.FormParam;
import javax.ws.rs.Path;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.auth.saml.conf.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class that implements the REST API necessary for the
 * SAML Idp to POST back its response to Guacamole.
 */
@Produces(MediaType.APPLICATION_JSON)
public class SAMLAuthenticationProviderResource {

    /**
     * The Logger for the class.
     */
    private final Logger logger = LoggerFactory.getLogger(SAMLAuthenticationProviderResource.class); 

    /**
     * The configuration service for this module.
     */
    @Inject
    private ConfigurationService confService;

    /**
     * A REST endpoint that is POSTed to by the SAML Idp
     * with the results of the SAML SSO Authentication.
     */
    @POST
    @Path("callback")
    public Response processSamlResponse(@FormParam("SAMLResponse") String samlResponse)
            throws GuacamoleException {

        String guacBase = confService.getCallbackUrl().toString();
        try {
            Response redirectHome = Response.seeOther(
                new URI(guacBase + "?SAMLResponse=" + Util.urlEncoder(samlResponse))).build();
            logger.debug(">>>SAML<<< Response: {}", redirectHome.toString());
            return redirectHome;
        }
        catch (URISyntaxException e) {
            throw new GuacamoleServerException("Error processing SAML response.", e);
        }

        

    }

}
