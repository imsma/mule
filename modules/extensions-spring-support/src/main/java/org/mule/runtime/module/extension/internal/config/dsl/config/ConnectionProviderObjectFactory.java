/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl.config;

import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.core.api.retry.RetryPolicyTemplate;
import org.mule.runtime.core.internal.connection.ConnectionManagerAdapter;
import org.mule.runtime.extension.api.introspection.connection.RuntimeConnectionProviderModel;
import org.mule.runtime.module.extension.internal.config.dsl.AbstractExtensionObjectFactory;
import org.mule.runtime.module.extension.internal.runtime.config.ConnectionProviderObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;

public class ConnectionProviderObjectFactory extends AbstractExtensionObjectFactory<ConnectionProviderResolver>
{
    private final RuntimeConnectionProviderModel providerModel;
    private final ConnectionManagerAdapter connectionManager;

    private PoolingProfile poolingProfile = null;
    private RetryPolicyTemplate retryPolicyTemplate = null;
    private boolean disableValidation = false;

    public ConnectionProviderObjectFactory(RuntimeConnectionProviderModel providerModel, ConnectionManagerAdapter connectionManager)
    {
        this.providerModel = providerModel;
        this.connectionManager = connectionManager;
    }

    @Override
    public ConnectionProviderResolver getObject() throws Exception
    {
        ResolverSet resolverSet = getParametersAsResolverSet();
        return new ConnectionProviderResolver(new ConnectionProviderObjectBuilder(providerModel, resolverSet, poolingProfile, disableValidation, retryPolicyTemplate, connectionManager));
    }

    public void setPoolingProfile(PoolingProfile poolingProfile)
    {
        this.poolingProfile = poolingProfile;
    }

    public void setRetryPolicyTemplate(RetryPolicyTemplate retryPolicyTemplate)
    {
        this.retryPolicyTemplate = retryPolicyTemplate;
    }

    public void setDisableValidation(boolean disableValidation)
    {
        this.disableValidation = disableValidation;
    }
}
