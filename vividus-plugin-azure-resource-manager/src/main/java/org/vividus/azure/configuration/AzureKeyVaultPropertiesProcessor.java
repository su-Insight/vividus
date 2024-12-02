/*
 * Copyright 2019-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.vividus.azure.configuration;

import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import com.azure.core.http.HttpMethod;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;

import org.apache.commons.lang3.Validate;
import org.vividus.azure.client.AzureResourceManagerClient;
import org.vividus.azure.identity.CredentialFactory;
import org.vividus.configuration.AbstractPropertiesProcessor;
import org.vividus.util.json.JsonPathUtils;

public class AzureKeyVaultPropertiesProcessor extends AbstractPropertiesProcessor
{
    private static final String ENVIRONMENT_PROPERTY = "azure.environment";
    private static final String KEY_VAULT_NAME_PROPERTY = "secrets-manager.azure-key-vault.name";
    private static final String KEY_VAULT_API_VERSION_PROPERTY = "secrets-manager.azure-key-vault.api-version";

    private static final Map<String, AzureEnvironment> ENVIRONMENT_MAP = Map.of(
            "AZURE", AzureEnvironment.AZURE,
            "AZURE_CHINA", AzureEnvironment.AZURE_CHINA,
            "AZURE_US_GOVERNMENT", AzureEnvironment.AZURE_US_GOVERNMENT
    );

    private AzureEnvironment azureEnvironment;
    private String keyVaultName;
    private String apiVersion;
    private AzureResourceManagerClient client;

    public AzureKeyVaultPropertiesProcessor()
    {
        super("AZURE_KEY_VAULT");
    }

    @Override
    public Properties processProperties(Properties properties)
    {
        this.azureEnvironment = Optional.ofNullable(ENVIRONMENT_MAP.get(properties.getProperty(ENVIRONMENT_PROPERTY)))
                .orElse(AzureEnvironment.AZURE);
        this.keyVaultName = properties.getProperty(KEY_VAULT_NAME_PROPERTY);
        this.apiVersion = properties.getProperty(KEY_VAULT_API_VERSION_PROPERTY);
        return super.processProperties(properties);
    }

    @Override
    protected String processValue(String propertyName, String partOfPropertyValueToProcess)
    {
        return getKeyVaultSecretValue(partOfPropertyValueToProcess);
    }

    private String getKeyVaultSecretValue(String secretName)
    {
        if (client == null)
        {
            Validate.notEmpty(keyVaultName, "Secrets can't be read from Azure Key Vault. "
                    + "Please, provide Azure Key Vault name as value into property `%s`", KEY_VAULT_NAME_PROPERTY);
            this.client = new AzureResourceManagerClient(new AzureProfile(azureEnvironment),
                    CredentialFactory.createTokenCredential());
        }
        String[] value = new String[1];
        client.executeHttpRequest(HttpMethod.GET,
                String.format("https://%s%s/secrets/%s?api-version=%s", keyVaultName,
                        azureEnvironment.getKeyVaultDnsSuffix(), secretName, apiVersion), Optional.empty(),
                response -> value[0] = JsonPathUtils.getData(response, "$.value"),
                error -> { throw new RuntimeException(error); });
        return value[0];
    }
}
