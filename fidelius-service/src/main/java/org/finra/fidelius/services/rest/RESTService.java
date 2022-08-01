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

package org.finra.fidelius.services.rest;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.dmfs.httpessentials.client.HttpRequestExecutor;
import org.dmfs.httpessentials.httpurlconnection.HttpUrlConnectionExecutor;
import org.dmfs.oauth2.client.*;
import org.dmfs.oauth2.client.grants.ClientCredentialsGrant;
import org.dmfs.oauth2.client.scope.BasicScope;
import org.dmfs.rfc3986.encoding.Precoded;
import org.dmfs.rfc3986.uris.LazyUri;
import org.dmfs.rfc5545.Duration;
import org.finra.fidelius.services.CredentialsService;
import org.finra.fidelius.services.auth.FideliusRoleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.inject.Inject;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
public class RESTService {

    @Value("${fidelius.auth.oauth.clientId:}")
    private Optional<String> clientId;

    @Value("${fidelius.auth.oauth.clientSecret:}")
    private Optional<String> clientSecret;

    @Value("${fidelius.auth.oauth.tokenUrl:}")
    private Optional<String> tokenUrl;

    @Value("${fidelius.auth.oauth.tokenUri:}")
    private Optional<String> tokenUri;

    private Logger logger = LoggerFactory.getLogger(RESTService.class);

    private LoadingCache<String, Optional<String>> userOAuth2TokenCache = CacheBuilder.newBuilder()
            .maximumSize(1000L)
            .concurrencyLevel(10)
            .expireAfterWrite(60L, TimeUnit.MINUTES)
            .build(new CacheLoader<String, Optional<String>>() {
                public Optional<String> load(String user) throws Exception {
                    return Optional.ofNullable(getOAuth2Header(clientId.get(), clientSecret.get()));
                }
            });

    public <T> T makeCall(String url, String uri, Class<T> clazz) {
        RestTemplate restTemplate = new RestTemplate();
        String completeUrl = String.format("%s/%s", url, uri);
        return restTemplate.getForObject(completeUrl, clazz);
    }

    public <T> T makeCall(String url, String uri, Class<T> clazz, String userName) {
        if(oAuth2ConfigProvided()) {
            logger.info("OAuth config detected. Fetching token.");
            String bearerToken = getOAuth2Token(userName);
            return makeCallWithOAuthToken(url, uri, clazz, bearerToken);
        }
        return makeCall(url, uri, clazz);
    }

    public <T> T makeCallWithOAuthToken(String url, String uri, Class<T> clazz, String bearerToken) {
        RestTemplate restTemplate = new RestTemplate();
        String completeUrl = String.format("%s/%s", url, uri);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", bearerToken);
        HttpEntity<T> requestEntity = new HttpEntity<>(headers);
        return restTemplate.exchange(completeUrl, HttpMethod.GET, requestEntity, clazz).getBody();
    }

    private String getOAuth2Header(String username, String password) {
        String token = getOAuth2Token(username, password);
        if(token.isEmpty()) {
            logger.error("Unable to fetch access token.");
            return "";
        }
        logger.info("Access token fetched.");
        return String.format("Bearer %s", token);
    }

    private String getOAuth2Token(String username, String password) {
        HttpRequestExecutor executor = new HttpUrlConnectionExecutor();
        // Create OAuth2 provider
        OAuth2AuthorizationProvider provider = new BasicOAuth2AuthorizationProvider(
                URI.create(tokenUrl.get() + "/" + tokenUri.get()),
                URI.create(tokenUrl.get() + "/" + tokenUri.get()),
                new Duration(1,0,600)           //Default expiration time if server does not respond
        );
        // Create OAuth2 client credentials
        OAuth2ClientCredentials credentials = new BasicOAuth2ClientCredentials(username, password);
        //Create OAuth2 client
        OAuth2Client client = new BasicOAuth2Client(
                provider,
                credentials,
                new LazyUri(new Precoded("http://localhost"))
        );
        try {
            OAuth2AccessToken token = new ClientCredentialsGrant(client, new BasicScope("scope")).accessToken(executor);
            return token.accessToken().toString();
        } catch(Exception e) {
            logger.error("Exception occurred while fetching access token.");
        }
        return "";
    }

    public String getOAuth2Token(String user) {
        return userOAuth2TokenCache.getUnchecked(user).get();
    }

    public boolean oAuth2ConfigProvided() {
        return clientId.isPresent() && clientSecret.isPresent() && tokenUrl.isPresent() && tokenUri.isPresent()
                && !clientId.get().equals("") && !clientSecret.get().equals("") && !tokenUrl.get().equals("") && !tokenUri.get().equals("");
    }
}
