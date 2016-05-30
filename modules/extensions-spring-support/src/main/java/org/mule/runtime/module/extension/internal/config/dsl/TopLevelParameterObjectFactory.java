/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl;

import static org.mule.metadata.java.utils.JavaTypeUtils.getType;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getFieldByAlias;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.module.extension.internal.runtime.DefaultObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.ObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.resolver.ObjectBuilderValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

import java.lang.reflect.Field;

import org.springframework.beans.factory.FactoryBean;

public class TopLevelParameterObjectFactory extends AbstractExtensionObjectFactory<ValueResolver<Object>> implements FactoryBean
{

    private final Class<Object> objectClass;
    final ObjectBuilder builder;

    public TopLevelParameterObjectFactory(ObjectType type)
    {
        objectClass = getType(type);
        builder = new DefaultObjectBuilder(objectClass);
    }

    @Override
    public ValueResolver<Object> getObject() throws Exception
    {

        getParameters().forEach((key, value) -> {
            Field field = getFieldByAlias(objectClass, key);
            if (field != null)
            {
                builder.addPropertyResolver(field, value);
            }
        });

        return new ObjectBuilderValueResolver<>(builder);
    }

    @Override
    public Class<?> getObjectType()
    {
        return objectClass;
    }

    @Override
    public boolean isSingleton()
    {
        return true;
    }
}
