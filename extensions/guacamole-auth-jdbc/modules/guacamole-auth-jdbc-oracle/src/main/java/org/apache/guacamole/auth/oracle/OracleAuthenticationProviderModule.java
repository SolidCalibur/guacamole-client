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

package org.apache.guacamole.auth.oracle;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.name.Names;
import java.util.Properties;
import org.apache.guacamole.GuacamoleException;
import org.mybatis.guice.datasource.helper.JdbcHelper;

/**
 * Guice module which configures Oracle-specific injections.
 */
public class OracleAuthenticationProviderModule implements Module {

    /**
     * MyBatis-specific configuration properties.
     */
    private final Properties myBatisProperties = new Properties();

    /**
     * Oracle-specific driver configuration properties.
     */
    private final Properties driverProperties = new Properties();
    
    /**
     * Creates a new Oracle authentication provider module that configures
     * driver and MyBatis properties using the given environment.
     *
     * @param environment
     *     The environment to use when configuring MyBatis and the underlying
     *     JDBC driver.
     *
     * @throws GuacamoleException
     *     If a required property is missing, or an error occurs while parsing
     *     a property.
     */
    public OracleAuthenticationProviderModule(OracleEnvironment environment)
            throws GuacamoleException {

        // Set the Oracle-specific properties for MyBatis.
        myBatisProperties.setProperty("mybatis.environment.id", "guacamole");
        myBatisProperties.setProperty("JDBC.host", environment.getOracleHostname());
        myBatisProperties.setProperty("JDBC.port", String.valueOf(environment.getOraclePort()));
        myBatisProperties.setProperty("oracle.sid", environment.getOracleDatabase());
        myBatisProperties.setProperty("JDBC.username", environment.getOracleUsername());
        myBatisProperties.setProperty("JDBC.password", environment.getOraclePassword());
        myBatisProperties.setProperty("JDBC.autoCommit", "false");
        myBatisProperties.setProperty("mybatis.pooled.pingEnabled", "true");
        myBatisProperties.setProperty("mybatis.pooled.pingQuery", "SELECT 1 FROM DUAL");

        // Use UTF-8 in database
        driverProperties.setProperty("characterEncoding", "UTF-8");

    }

    @Override
    public void configure(Binder binder) {

        // Bind Oracle-specific properties
        JdbcHelper.Oracle_Thin.configure(binder);
        
        // Bind MyBatis properties
        Names.bindProperties(binder, myBatisProperties);

        // Bind JDBC driver properties
        binder.bind(Properties.class)
            .annotatedWith(Names.named("JDBC.driverProperties"))
            .toInstance(driverProperties);

    }

}
