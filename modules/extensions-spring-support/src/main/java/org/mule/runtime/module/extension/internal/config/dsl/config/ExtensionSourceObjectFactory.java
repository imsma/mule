/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl.config;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.core.api.config.ThreadingProfile.DEFAULT_THREADING_PROFILE;
import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.config.ThreadingProfile;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.core.api.retry.RetryPolicyTemplate;
import org.mule.runtime.core.config.ImmutableThreadingProfile;
import org.mule.runtime.core.internal.connection.ConnectionManagerAdapter;
import org.mule.runtime.extension.api.introspection.RuntimeExtensionModel;
import org.mule.runtime.extension.api.introspection.source.RuntimeSourceModel;
import org.mule.runtime.extension.api.introspection.source.SourceModel;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceFactory;
import org.mule.runtime.module.extension.internal.config.dsl.AbstractExtensionObjectFactory;
import org.mule.runtime.module.extension.internal.manager.ExtensionManagerAdapter;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.source.ExtensionMessageSource;
import org.mule.runtime.module.extension.internal.runtime.source.SourceConfigurer;

import com.google.common.base.Joiner;

import java.util.List;

public class ExtensionSourceObjectFactory extends AbstractExtensionObjectFactory<ExtensionMessageSource>
{

    private final RuntimeExtensionModel extensionModel;
    private final RuntimeSourceModel sourceModel;
    private final MuleContext muleContext;

    private String configurationProviderName;
    private RetryPolicyTemplate retryPolicyTemplate;
    private ConnectionManagerAdapter connectionManager;

    public ExtensionSourceObjectFactory(RuntimeExtensionModel extensionModel, RuntimeSourceModel sourceModel, MuleContext muleContext)
    {
        this.extensionModel = extensionModel;
        this.sourceModel = sourceModel;
        this.muleContext = muleContext;
    }

    @Override
    public ExtensionMessageSource getObject() throws ConfigurationException
    {
        ResolverSet resolverSet = getParametersAsResolverSet();
        if (resolverSet.isDynamic())
        {
            throw dynamicParameterException(resolverSet, sourceModel);
        }

        ExtensionMessageSource messageSource = new ExtensionMessageSource(extensionModel,
                                                                          sourceModel,
                                                                          getSourceFactory(resolverSet),
                                                                          configurationProviderName,
                                                                          getThreadingProfile(),
                                                                          getRetryPolicyTemplate(),
                                                                          (ExtensionManagerAdapter) muleContext.getExtensionManager());
        try
        {
            muleContext.getInjector().inject(messageSource);
        }
        catch (MuleException e)
        {
            throw new ConfigurationException(createStaticMessage("Could not inject dependencies into source of flow " + getFlowName()), e);
        }

        return messageSource;
    }

    private ThreadingProfile getThreadingProfile()
    {
        ThreadingProfile tp = new ImmutableThreadingProfile(DEFAULT_THREADING_PROFILE);
        tp.setMuleContext(muleContext);

        return tp;
    }


    private SourceFactory getSourceFactory(ResolverSet resolverSet)
    {
        return () -> {
            Source source = sourceModel.getSourceFactory().createSource();
            try
            {
                return new SourceConfigurer(sourceModel, resolverSet, muleContext).configure(source);
            }
            catch (Exception e)
            {
                throw new MuleRuntimeException(createStaticMessage(format("Could not create generator for source '%s' in flow '%s'",
                                                                          sourceModel.getName(), getFlowName())));
            }
        };
    }

    private RetryPolicyTemplate getRetryPolicyTemplate() throws ConfigurationException
    {
        return retryPolicyTemplate != null ? retryPolicyTemplate : getConnectionManagerAdapter().getDefaultRetryPolicyTemplate();
    }

    private ConfigurationException dynamicParameterException(ResolverSet resolverSet, SourceModel model)
    {
        List<String> dynamicParams = resolverSet.getResolvers().entrySet().stream()
                .filter(entry -> entry.getValue().isDynamic())
                .map(entry -> entry.getKey())
                .collect(toList());

        return new ConfigurationException(createStaticMessage(format("The '%s' message source on flow '%s' is using expressions, which are not allowed on message sources. " +
                                                                     "Offending parameters are: [%s]",
                                                                     model.getName(), getFlowName(), Joiner.on(',').join(dynamicParams))));
    }

    private String getFlowName()
    {
        return "todo";
    }

    private ConnectionManagerAdapter getConnectionManagerAdapter() throws ConfigurationException
    {
        if (connectionManager == null)
        {
            try
            {
                connectionManager = muleContext.getRegistry().lookupObject(ConnectionManagerAdapter.class);
            }
            catch (RegistrationException e)
            {
                throw new ConfigurationException(createStaticMessage("Could not obtain connection manager adapter"), e);
            }
        }

        return connectionManager;
    }
}
