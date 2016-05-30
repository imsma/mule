/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl;

import static org.mule.runtime.module.extension.internal.util.NameUtils.camel;
import static org.mule.runtime.module.extension.internal.util.NameUtils.hyphenize;
import org.mule.runtime.config.spring.dsl.api.ObjectFactory;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractExtensionObjectFactory<T> implements ObjectFactory<T>
{

    private Map<String, ValueResolver<?>> parameters = new HashMap<>();

    protected Map<String, ValueResolver<?>> getParameters()
    {
        validateParameters();
        return parameters;
    }

    public void setParameters(Map<String, ValueResolver<?>> parameters)
    {
        this.parameters = parameters;
    }

    protected ResolverSet getParametersAsResolverSet()
    {
        ResolverSet resolverSet = new ResolverSet();
        getParameters().forEach((key, valueResolver) -> resolverSet.add(key, valueResolver));

        return resolverSet;
    }

    private void validateParameters()
    {
        parameters.keySet().stream()
                .filter(key -> {
                    if (key.contains("-") && parameters.containsKey(camel(key)))
                    {
                        return true;
                    }
                    else
                    {
                        String hypenized = hyphenize(key);
                        return !hypenized.equals(key) && parameters.containsKey(hypenized);
                    }
                }).findFirst()
                .ifPresent(parameter -> {
                    throw new IllegalArgumentException(String.format("Parameter '%s' was specified as an attribute and as a child element at the same time.", parameter));
                });
    }
}
