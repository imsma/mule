/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl;

import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromFixedValue;
import static org.mule.runtime.config.spring.dsl.processor.TypeDefinition.fromType;
import static org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport.SUPPORTED;
import static org.mule.runtime.module.extension.internal.util.NameUtils.getTopLevelTypeName;
import static org.mule.runtime.module.extension.internal.util.NameUtils.hyphenize;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.config.spring.dsl.api.ComponentBuildingDefinition.Builder;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

final class TopLevelParameterParser extends ExtensionDefinitionParser
{

    private final ObjectType type;

    TopLevelParameterParser(Builder definition, ObjectType type)
    {
        super(definition);
        this.type = type;
    }

    @Override
    protected void doParse(Builder definition) throws ConfigurationException
    {
        definition.withIdentifier(hyphenize(getTopLevelTypeName(type)))
                .withTypeDefinition(fromType(ValueResolver.class))
                .withObjectFactoryType(TopLevelParameterObjectFactory.class)
                .withConstructorParameterDefinition(fromFixedValue(type).build());

        for (ObjectFieldType objectField : type.getFields())
        {
            final MetadataType fieldType = objectField.getValue();
            final String parameterName = objectField.getKey().getName().getLocalPart();

            fieldType.accept(new MetadataTypeVisitor()
            {

                @Override
                protected void defaultVisit(MetadataType metadataType)
                {
                    parseAttributeParameter(parameterName, metadataType, null, SUPPORTED, false);
                }

                @Override
                public void visitObject(ObjectType objectType)
                {
                    parsePojoParameter(parameterName, objectType, null, SUPPORTED, false);
                }
            });
        }
    }
}
