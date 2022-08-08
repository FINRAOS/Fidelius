/*
 * Copyright (c) 2019. Fidelius Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.finra.fidelius.config;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.retry.PredefinedRetryPolicies;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient;
import org.finra.fidelius.authfilter.UserHeaderFilter;
import org.finra.fidelius.authfilter.parser.IFideliusUserProfile;
import org.finra.fidelius.authfilter.parser.SSOParser;
import org.finra.fidelius.services.auth.FideliusActiveDirectoryLDAPAuthorizationService;
import org.finra.fidelius.services.auth.FideliusAuthProperties;
import org.finra.fidelius.services.auth.FideliusAuthorizationService;
import org.finra.fidelius.services.auth.FideliusOpenLDAPAuthorizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.*;
import org.springframework.core.Ordered;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Configuration file used to set up beans like ClientConfiguration
 * or any other AWS clients such as S3, SQS, etc.
 */
@Configuration
@EnableAutoConfiguration
@ComponentScan("org.finra.fidelius")
public class AppConfig {

    @Value("${fidelius.aws.proxyHost:}")
    private Optional<String> proxyHost;

    @Value("${fidelius.aws.proxyPort:}")
    private Optional<Integer> proxyPort;

    @Value("${fidelius.javax.contentSecurityPolicy:}")
    private Optional<String> contentSecurityPolicy;

    private final Logger logger = LoggerFactory.getLogger(AppConfig.class);

    private final String userIdHeader;
    private final String userFullNameHeader;
    private final String userEmailHeader;
    private final String userMembershipsHeader;
    private final String userMembershipsPattern;
    private final String userBase;
    private final String base;

    @Autowired
    private final FideliusAuthProperties fideliusAuthProperties;

    @Autowired
    private ClientConfiguration clientConfiguration;

    public AppConfig(FideliusAuthProperties fideliusAuthProperties){
        //LDAP
        this.fideliusAuthProperties = fideliusAuthProperties;
        this.userIdHeader = fideliusAuthProperties.getUserIdHeader();
        this.userFullNameHeader = fideliusAuthProperties.getUserFullNameHeader();
        this.userEmailHeader = fideliusAuthProperties.getUserEmailHeader();
        this.userMembershipsHeader = fideliusAuthProperties.getUserMembershipsHeader();
        this.userMembershipsPattern = fideliusAuthProperties.getUserMembershipsPattern();
        this.base = fideliusAuthProperties.getLdap().getBase();
        this.userBase = fideliusAuthProperties.getLdap().getUsersBase();
    }

    @Bean
    public ClientConfiguration clientConfiguration() {
        final ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setRetryPolicy(PredefinedRetryPolicies.DYNAMODB_DEFAULT);
        if (this.proxyHost.isPresent() && this.proxyPort.isPresent()) {
            clientConfiguration.setProxyHost(this.proxyHost.get());
            clientConfiguration.setProxyPort(this.proxyPort.get());
        }

        return clientConfiguration;
    }

    @Bean
    public AWSSecurityTokenServiceClient awsSecurityTokenServiceClient() {
        return new AWSSecurityTokenServiceClient(this.clientConfiguration);
    }

    @Configuration
    @EnableSwagger2
    class SwaggerConfig {
        @Bean
        public Docket api() {
            return new Docket(DocumentationType.SWAGGER_2)
                    .select()
                    .apis(RequestHandlerSelectors.basePackage("org.finra.fidelius.controllers"))
                    .paths(PathSelectors.any())
                    .build();
        }
    }

    /* Creating UserProfileFilter with order of 0 to ensure it happens first */
    @Bean
    @AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
    public FilterRegistrationBean userProfileFilterRegistration() {
        FilterRegistrationBean userProfileFilterRegistration = new FilterRegistrationBean();
        if(contentSecurityPolicy.isPresent() && !contentSecurityPolicy.get().isEmpty()) {
            userProfileFilterRegistration.setFilter(new UserHeaderFilter(new SSOParser(userIdHeader), contentSecurityPolicy.get()));
        } else {
            userProfileFilterRegistration.setFilter(new UserHeaderFilter(new SSOParser(userIdHeader)));
        }
        userProfileFilterRegistration.setOrder(0);
        return userProfileFilterRegistration;
    }

    /* Request scoped bean to create autowireable UserProfile object */
    @Bean
    @Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
    public IFideliusUserProfile userProfile() {
        HttpServletRequest req = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        Principal p = req.getUserPrincipal();
        return (IFideliusUserProfile) p;
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.ldap.contextSource")
    public LdapContextSource contextSource() {
        LdapContextSource contextSource = new LdapContextSource();
        contextSource.setBase(userBase);
        return contextSource;
    }

    @Bean
    public LdapTemplate ldapTemplate(ContextSource contextSource) {
        return new LdapTemplate(contextSource);
    }

    @Bean
    public FideliusAuthorizationService fideliusLDAPAuthorizationService(LdapTemplate ldapTemplate,
                                                                         Supplier<IFideliusUserProfile> fideliusUserProfileSupplier) {
        //Sets to AD if true
        if(fideliusAuthProperties.getLdap().getIsActiveDirectory()) {
        logger.info("Setting Authorization to work with Active Directory");
        return new FideliusActiveDirectoryLDAPAuthorizationService(ldapTemplate,
                fideliusUserProfileSupplier,
                fideliusAuthProperties);
        }

        logger.info("Setting Authorization to work with OpenLDAP");
        //Defaults to OpenLDAP otherwise
        return new FideliusOpenLDAPAuthorizationService(ldapTemplate,
                fideliusUserProfileSupplier,
                fideliusAuthProperties);
    }
}
