/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl.config;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_TIME_SUPPLIER;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.lifecycle.LifecycleUtils;
import org.mule.runtime.core.time.TimeSupplier;
import org.mule.runtime.extension.api.introspection.config.RuntimeConfigurationModel;
import org.mule.runtime.extension.api.runtime.ConfigurationProvider;
import org.mule.runtime.module.extension.internal.config.dsl.AbstractExtensionObjectFactory;
import org.mule.runtime.module.extension.internal.runtime.DynamicConfigPolicy;
import org.mule.runtime.module.extension.internal.runtime.config.ConfigurationProviderFactory;
import org.mule.runtime.module.extension.internal.runtime.config.DefaultConfigurationProviderFactory;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.StaticValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ConfigurationProviderObjectFactory extends AbstractExtensionObjectFactory<ConfigurationProvider<Object>> implements Lifecycle
{

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationProviderObjectFactory.class);

    private final String name;
    private final RuntimeConfigurationModel configurationModel;
    private final ConfigurationProviderFactory configurationProviderFactory = new DefaultConfigurationProviderFactory();
    private final MuleContext muleContext;

    private DynamicConfigPolicy dynamicConfigPolicy;
    private Optional<ValueResolver<ConnectionProvider>> connectionProviderResolver = empty();
    private ConfigurationProvider<Object> instance;

    ConfigurationProviderObjectFactory(String name,
                                       RuntimeConfigurationModel configurationModel,
                                       MuleContext muleContext)
    {
        this.name = name;
        this.configurationModel = configurationModel;
        this.muleContext = muleContext;
    }

    @Override
    public ConfigurationProvider<Object> getObject() throws Exception
    {
        return instance;
    }

    private ConfigurationProvider<Object> createInnerInstance()
    {
        ResolverSet resolverSet = getParametersAsResolverSet();
        final ValueResolver<ConnectionProvider> connectionProviderResolver = getConnectionProviderResolver();

        ConfigurationProvider<Object> configurationProvider;
        try
        {
            if (resolverSet.isDynamic() || connectionProviderResolver.isDynamic())
            {
                configurationProvider = configurationProviderFactory.createDynamicConfigurationProvider(
                        name,
                        configurationModel,
                        resolverSet,
                        connectionProviderResolver,
                        getDynamicConfigPolicy());
            }
            else
            {
                configurationProvider = configurationProviderFactory.createStaticConfigurationProvider(
                        name,
                        configurationModel,
                        resolverSet,
                        connectionProviderResolver,
                        muleContext);
            }

            muleContext.getInjector().inject(configurationProvider);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }

        return configurationProvider;
    }

    private DynamicConfigPolicy getDynamicConfigPolicy()
    {
        if (dynamicConfigPolicy == null)
        {
            TimeSupplier timeSupplier = muleContext.getRegistry().lookupObject(OBJECT_TIME_SUPPLIER);
            dynamicConfigPolicy = DynamicConfigPolicy.getDefault(timeSupplier);
        }

        return dynamicConfigPolicy;
    }

    private ValueResolver<ConnectionProvider> getConnectionProviderResolver()
    {
        return connectionProviderResolver.orElse(new StaticValueResolver<>(null));
    }

    public void setDynamicConfigPolicy(DynamicConfigPolicy dynamicConfigPolicy)
    {
        this.dynamicConfigPolicy = dynamicConfigPolicy;
    }

    public void setConnectionProviderResolver(ConnectionProviderResolver connectionProviderResolver)
    {
        this.connectionProviderResolver = ofNullable(connectionProviderResolver);
    }

    @Override
    public void initialise() throws InitialisationException
    {
        instance = createInnerInstance();
        try
        {
            muleContext.getInjector().inject(instance);
        }
        catch (MuleException e)
        {
            throw new MuleRuntimeException(e);
        }
        LifecycleUtils.initialiseIfNeeded(instance);
    }


    @Override
    public void dispose()
    {
        LifecycleUtils.disposeIfNeeded(instance, logger);
    }

    @Override
    public void start() throws MuleException
    {
        LifecycleUtils.startIfNeeded(instance);
    }

    @Override
    public void stop() throws MuleException
    {
        LifecycleUtils.stopIfNeeded(instance);
    }
}
