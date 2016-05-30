/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl;

import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

public class ParsedParameter
{

    private final String name;
    private final ValueResolver<?> resolver;

    public ParsedParameter(String name, ValueResolver<?> resolver)
    {
        this.name = name;
        this.resolver = resolver;
    }

    public String getName()
    {
        return name;
    }

    public ValueResolver<?> getResolver()
    {
        return resolver;
    }
}
