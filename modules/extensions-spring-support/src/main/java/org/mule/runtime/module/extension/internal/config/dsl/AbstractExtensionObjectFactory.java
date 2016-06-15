/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl;

import static com.google.common.collect.ImmutableList.copyOf;
import org.mule.runtime.config.spring.dsl.api.ObjectFactory;
import org.mule.runtime.module.extension.internal.runtime.resolver.CollectionValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.MapValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.StaticValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractExtensionObjectFactory<T> implements ObjectFactory<T>
{

    private Map<String, Object> parameters = new HashMap<>();

    protected Map<String, Object> getParameters()
    {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters)
    {
        this.parameters = parameters;
    }

    protected ResolverSet getParametersAsResolverSet()
    {
        ResolverSet resolverSet = new ResolverSet();
        getParameters().forEach((key, value) -> resolverSet.add(key, toValueResolver(value)));

        return resolverSet;
    }

    protected ValueResolver<?> toValueResolver(Object value)
    {
        ValueResolver<?> resolver;
        if (value instanceof ValueResolver)
        {
            resolver = (ValueResolver<?>) value;
        }
        else if (value instanceof Collection)
        {
            resolver = CollectionValueResolver.of((Class<? extends Collection>) value.getClass(), copyOf((Iterable) value));
        }
        else if (value instanceof Map)
        {
            Map<Object, Object> map = (Map<Object, Object>) value;
            List<ValueResolver<Object>> keys = new ArrayList<>(map.size());
            List<ValueResolver<Object>> values = new ArrayList<>(map.size());
            map.forEach((key, entryValue) -> {
                keys.add((ValueResolver<Object>) toValueResolver(key));
                values.add((ValueResolver<Object>) toValueResolver(entryValue));
            });
            resolver = MapValueResolver.of(map.getClass(), keys, values);
        }
        else
        {
            resolver = new StaticValueResolver<>(value);
        }
        return resolver;
    }
}
