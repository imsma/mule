/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl;

import static org.mule.runtime.module.extension.internal.config.dsl.ExtensionDefinitionParser.CHILD_ELEMENT_KEY_PREFIX;
import static org.mule.runtime.module.extension.internal.config.dsl.ExtensionDefinitionParser.CHILD_ELEMENT_KEY_SUFFIX;
import org.mule.runtime.config.spring.dsl.api.ObjectFactory;
import org.mule.runtime.core.util.collection.ImmutableListCollector;
import org.mule.runtime.module.extension.internal.runtime.resolver.CollectionValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.MapValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.StaticValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

import com.google.common.collect.ImmutableMap;

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
        this.parameters = normalize(parameters);
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
            resolver = CollectionValueResolver.of((Class<? extends Collection>) value.getClass(), (List) ((Collection) value).stream().map(this::toValueResolver).collect(new ImmutableListCollector()));
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

    private Map<String, Object> normalize(Map<String, Object> parameters)
    {
        ImmutableMap.Builder<String, Object> normalized = ImmutableMap.builder();
        parameters.forEach((key, value) -> {
            String normalizedKey = key;

            if (isChildKey(key))
            {
                normalizedKey = unwrapChildKey(key);
                if (parameters.containsKey(normalizedKey))
                {
                    throw new IllegalArgumentException(String.format("Parameter '%s' was specified as an attribute and as a child element at the same time.", normalizedKey));
                }
            }

            normalized.put(normalizedKey, value);
        });

        return normalized.build();
    }

    private boolean isChildKey(String key)
    {
        return key.startsWith(CHILD_ELEMENT_KEY_PREFIX) && key.endsWith(CHILD_ELEMENT_KEY_SUFFIX);
    }

    private String unwrapChildKey(String key)
    {
        return key.replaceAll(CHILD_ELEMENT_KEY_PREFIX, "").replaceAll(CHILD_ELEMENT_KEY_SUFFIX, "");
    }
}
