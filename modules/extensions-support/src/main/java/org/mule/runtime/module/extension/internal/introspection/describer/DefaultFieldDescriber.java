/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.describer;

import static org.mule.runtime.module.extension.internal.introspection.describer.MuleExtensionAnnotationParser.parseDisplayAnnotations;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getExpressionSupport;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getFieldMetadataType;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getDefaultValue;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ParameterDeclarer;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ParameterizedDeclarer;
import org.mule.runtime.extension.api.introspection.property.DisplayModelProperty;
import org.mule.runtime.module.extension.internal.model.property.DeclaringMemberModelProperty;

import java.lang.reflect.Field;

/**
 * Default implementation of {@link FieldDescriber}, capable
 * of handling all {@link Field}s in a generic way.
 * <p>
 * Although it can handle pretty much any field, it's not suitable
 * for those which need special treatment.
 *
 * @since 4.0
 */
final class DefaultFieldDescriber implements FieldDescriber
{

    private final ClassTypeLoader typeLoader;

    DefaultFieldDescriber(ClassTypeLoader typeLoader)
    {
        this.typeLoader = typeLoader;
    }

    /**
     * Always returns {@code true}
     *
     * @return {@code true}
     */
    @Override
    public boolean accepts(Field field)
    {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParameterDeclarer describe(Field field, ParameterizedDeclarer declarer)
    {
        String parameterName = MuleExtensionAnnotationParser.getAliasName(field);
        ParameterDeclarer parameterDeclarer;
        MetadataType fieldType = getFieldMetadataType(field, typeLoader);

        Optional optional = field.getAnnotation(Optional.class);
        parameterDeclarer = optional == null ? declarer.withRequiredParameter(parameterName)
                                             : declarer.withOptionalParameter(parameterName).defaultingTo(getDefaultValue(optional));

        parameterDeclarer.ofType(fieldType)
                .withExpressionSupport(getExpressionSupport(field))
                .withModelProperty(new DeclaringMemberModelProperty(field));

        DisplayModelProperty displayModelProperty = parseDisplayAnnotations(field, field.getName());
        if (displayModelProperty != null)
        {
            parameterDeclarer.withModelProperty(displayModelProperty);
        }

        return parameterDeclarer;
    }
}
