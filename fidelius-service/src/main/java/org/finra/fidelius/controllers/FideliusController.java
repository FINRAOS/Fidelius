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

package org.finra.fidelius.controllers;


import org.finra.fidelius.exceptions.FideliusException;
import org.finra.fidelius.model.ActiveDirectory;
import org.finra.fidelius.model.Credential;
import org.finra.fidelius.model.HistoryEntry;
import org.finra.fidelius.model.Metadata;
import org.finra.fidelius.model.account.Account;
import org.finra.fidelius.services.CredentialsService;
import org.finra.fidelius.services.account.AccountsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
class FideliusController {

    @Inject
    private CredentialsService credentialsService;

    @Inject
    private AccountsService accountsService;

    @Value("${fidelius.validActiveDirectoryRegularExpression:(.*?)}")
    protected String validActiveDirectoryRegularExpression;

    @Value("${fidelius.validActiveDirectoryDescription:''}")
    protected String validActiveDirectoryDescription;

    @Value("${fidelius.dynamoTable}")
    private String tableName;

    @RequestMapping(value = "/heartbeat", method = RequestMethod.GET)
    public ResponseEntity heartbeatEndpoint() {
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = "/credentials", method = RequestMethod.GET)
    public ResponseEntity getCredentials(@RequestParam("account") String account,
                                                           @RequestParam("region") String region,
                                                           @RequestParam("application") String app) {
        List<Credential> foundCreds = new ArrayList<>();
        try {
            foundCreds = credentialsService.getAllCredentials(tableName, account, region, app);
        } catch (FideliusException fe) {
            return new ResponseEntity<>(fe, fe.getError());
        }
        return new ResponseEntity<>(foundCreds, HttpStatus.OK);
    }

    @ResponseBody
    @GetMapping(value="/credentials/{key:.+}/")
    public ResponseEntity getCredential(@PathVariable("key") String longKey,
                                        @RequestParam("account") String account,
                                        @RequestParam("region") String region,
                                        @RequestParam("application") String application) {

        final Credential credential = credentialsService.getCredential(account, region, application, longKey);

        if (credential != null)
            return new ResponseEntity<>(credential, HttpStatus.OK);

        return new ResponseEntity<>("Credential not found", HttpStatus.NOT_FOUND);
    }

    @RequestMapping(value = "/credentials/history", method = RequestMethod.GET)
    public ResponseEntity getCredentialHistory(@RequestParam("account") String account,
                                                                   @RequestParam("region") String region,
                                                                   @RequestParam("application") String app,
                                                                   @RequestParam("environment") String environment,
                                                                   @RequestParam(value = "component", required = false) String component,
                                                                   @RequestParam("shortKey") String key) {
        List<HistoryEntry> credHistory = new ArrayList<>();
        try {
            credHistory = credentialsService.getCredentialHistory(tableName, account, region, app, environment, component, key, false);
        } catch (FideliusException fe) {
            return new ResponseEntity<>(fe, fe.getError());
        }
        return new ResponseEntity<>(credHistory, credHistory.isEmpty() ? HttpStatus.NOT_FOUND : HttpStatus.OK);
    }

    @ResponseBody
    @GetMapping(value="/credentials/secret")
    public ResponseEntity getSecret(@RequestParam("account") String account,
                                      @RequestParam("region") String region,
                                      @RequestParam("application") String application,
                                      @RequestParam("environment") String environment,
                                      @RequestParam(value = "component", required = false) String component,
                                      @RequestParam("shortKey") String shortKey) {
        final Credential credentialSecret = credentialsService.getCredentialSecret(account, region, application, environment, component, shortKey);

        if (credentialSecret.getSecret() != null)
            return new ResponseEntity<>(credentialSecret, HttpStatus.OK);

        return new ResponseEntity<>("Credential not found", HttpStatus.NOT_FOUND);
    }

    @ResponseBody
    @PostMapping(value="/credentials/secret")
    public ResponseEntity createCredential(@Valid @RequestBody Credential credential) {
        final Credential credentialSecret;
        try {
            credentialSecret = credentialsService.createCredential(credential);
        } catch (FideliusException fe) {
            return new ResponseEntity<>(fe, fe.getError());
        }

        if (credentialSecret != null)
            return new ResponseEntity<>(credentialSecret, HttpStatus.CREATED);

        return new ResponseEntity<>("Credential not created", HttpStatus.BAD_REQUEST);
    }

    @ResponseBody
    @PutMapping(value="/credentials/secret")
    public ResponseEntity updateCredential(@Valid @RequestBody Credential credential) {
        final Credential credentialSecret = credentialsService.putCredential(credential);

        if (credentialSecret != null)
            return new ResponseEntity<>(credentialSecret, HttpStatus.CREATED);

        return new ResponseEntity<>("Credential not updated", HttpStatus.BAD_REQUEST);
    }

    @ResponseBody
    @DeleteMapping(value="/credentials/secret")
    public ResponseEntity deleteCredential(@RequestParam("account") String account,
                                                       @RequestParam("region") String region,
                                                       @RequestParam("application") String application,
                                                       @RequestParam("environment") String environment,
                                                       @RequestParam(value = "component", required = false) String component,
                                                       @RequestParam(value = "source", required = false) String source,
                                                       @RequestParam(value = "sourceType", required = false) String sourceType,
                                                       @RequestParam("shortKey") String shortKey) {
        Credential credentialToDelete = new Credential(shortKey, null, account, region, application, environment, component, null, null, source, sourceType);

        final Credential credentialSecret = credentialsService.deleteCredential(credentialToDelete);

        if (credentialSecret != null)
            return new ResponseEntity<>(credentialSecret, HttpStatus.ACCEPTED);

        return new ResponseEntity<>("Credential not deleted", HttpStatus.NOT_FOUND);
    }

    @ResponseBody
    @PostMapping(value="/credentials/rotate")
    public ResponseEntity rotateSecret(@RequestBody Map<String, String> request){
       String account = request.get("account");
       String sourceType = request.get("sourceType");
       String sourceName = request.get("source");
       String shortKey = request.get("shortKey");
       String component = request.get("component");
       String region = request.get("region");
       String application = request.get("application");
       String environment= request.get("environment");

       ResponseEntity response = credentialsService.rotateCredential(account, sourceType, sourceName, region, application, environment, component, shortKey);
       return response;
    }

    @ResponseBody
    @GetMapping(value="/sources")
    public ResponseEntity getSourceNames(@RequestParam("account") String account,
                                      @RequestParam("region") String region,
                                      @RequestParam("sourceType") String sourceType,
                                      @RequestParam("application") String application) throws Exception {
        final List<String> metadataInfo = credentialsService.getMetadataInfo(account, region, sourceType, application);

        if (metadataInfo != null)
            return new ResponseEntity<>(metadataInfo, HttpStatus.OK);

        return new ResponseEntity<>("Unable to describe source names", HttpStatus.NOT_FOUND);
    }

    @ResponseBody
    @GetMapping(value="/credentials/metadata")
    public ResponseEntity getMetadata(@RequestParam("account") String account,
                                    @RequestParam("region") String region,
                                    @RequestParam("application") String application,
                                    @RequestParam("environment") String environment,
                                    @RequestParam(value = "component", required = false) String component,
                                    @RequestParam("shortKey") String shortKey) {
        final Metadata metadataGet = credentialsService.getMetadata(account, region, application, environment, component, shortKey);

        return new ResponseEntity<>(metadataGet, HttpStatus.OK);
    }

    @ResponseBody
    @PostMapping(value="/credentials/metadata")
    public ResponseEntity createMetadata(@Valid @RequestBody Metadata metadata) {
        final Metadata metadataToAdd;

        try {
            metadataToAdd = credentialsService.createMetadata(metadata);
        } catch (FideliusException fe) {
            return new ResponseEntity<>(fe, fe.getError());
        }

        if (metadataToAdd != null)
            return new ResponseEntity<>(metadata, HttpStatus.CREATED);

        return new ResponseEntity<>("Metadata is not created", HttpStatus.BAD_REQUEST);
    }

    @ResponseBody
    @PutMapping(value="/credentials/metadata")
    public ResponseEntity updateMetadata(@Valid @RequestBody Metadata metadata) {
        final Metadata metadataToUpdate = credentialsService.putMetadata(metadata);

        if (metadataToUpdate != null)
            return new ResponseEntity<>(metadataToUpdate, HttpStatus.CREATED);

        return new ResponseEntity<>("Metadata is not updated", HttpStatus.BAD_REQUEST);
    }

    @ResponseBody
    @DeleteMapping(value="/credentials/metadata")
    public ResponseEntity deleteMetadata(@RequestParam("account") String account,
                                           @RequestParam("region") String region,
                                           @RequestParam("application") String application,
                                           @RequestParam("environment") String environment,
                                           @RequestParam(value = "component", required = false) String component,
                                           @RequestParam("shortKey") String shortKey) {
        Metadata metadataToDelete = new Metadata(shortKey, null, account, region, application, environment, component, null, null);

        final Metadata metadataSecret = credentialsService.deleteMetadata(metadataToDelete);

        if (metadataSecret != null)
            return new ResponseEntity<>(metadataSecret, HttpStatus.ACCEPTED);

        return new ResponseEntity<>("Metadata is not deleted", HttpStatus.NOT_FOUND);
    }

    @ResponseBody
    @GetMapping(value="/validActiveDirectoryRegularExpression")
    public ResponseEntity activeDirectory() {
        return new ResponseEntity<>(new ActiveDirectory(validActiveDirectoryRegularExpression, validActiveDirectoryDescription.replaceAll("\\\\n", "\n")), HttpStatus.OK);
    }

    @ResponseBody
    @GetMapping(value="/accounts")
    public ResponseEntity getAccounts() {
        final List<Account> accounts = accountsService.getAccounts();

        if(accounts != null && accounts.size() > 0)
            return new ResponseEntity<>(accounts, HttpStatus.OK);

        return new ResponseEntity<>("No Accounts found", HttpStatus.NOT_FOUND);
    }
}
