/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl;

import static java.lang.String.format;
import static org.mule.metadata.java.utils.JavaTypeUtils.getGenericTypeAt;
import static org.mule.metadata.java.utils.JavaTypeUtils.getType;
import static org.mule.metadata.utils.MetadataTypeUtils.getSingleAnnotation;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromChildConfiguration;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromChildListConfiguration;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromMultipleDefinitions;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromSimpleParameter;
import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import static org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport.LITERAL;
import static org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport.REQUIRED;
import static org.mule.runtime.module.extension.internal.util.NameUtils.hyphenize;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.DateTimeType;
import org.mule.metadata.api.model.DateType;
import org.mule.metadata.api.model.DictionaryType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.metadata.java.annotation.GenericTypesAnnotation;
import org.mule.runtime.config.spring.dsl.api.AttributeDefinition;
import org.mule.runtime.config.spring.dsl.api.ComponentBuildingDefinition;
import org.mule.runtime.config.spring.dsl.api.ComponentBuildingDefinition.Builder;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.util.ClassUtils;
import org.mule.runtime.core.util.CollectionUtils;
import org.mule.runtime.core.util.TemplateParser;
import org.mule.runtime.core.util.ValueHolder;
import org.mule.runtime.extension.api.introspection.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport;
import org.mule.runtime.extension.api.introspection.parameter.ParameterModel;
import org.mule.runtime.module.extension.internal.introspection.BasicTypeMetadataVisitor;
import org.mule.runtime.module.extension.internal.runtime.resolver.ExpressionFunctionValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.RegistryLookupValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.StaticValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.TypeSafeExpressionValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;

public abstract class ExtensionDefinitionParser
{

    private static final String DATE_FORMAT = "yyyy-MM-dd'T'hh:mm:ss";
    private static final String CALENDAR_FORMAT = "yyyy-MM-dd'T'hh:mm:ssX";

    private final TemplateParser parser = TemplateParser.createMuleStyleParser();
    private final ConversionService conversionService = new DefaultConversionService();
    private final ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();
    private final List<AttributeDefinition.Builder> parameters = new LinkedList<>();
    private final Builder definition;

    protected ExtensionDefinitionParser(Builder definition)
    {
        this.definition = definition;
    }

    public final ComponentBuildingDefinition parse() throws ConfigurationException
    {
        final Builder builder = definition.copy();
        doParse(builder);

        if (!CollectionUtils.isEmpty(parameters))
        {
            AttributeDefinition[] attributeDefinitions = parameters.stream()
                    .map(AttributeDefinition.Builder::build)
                    .toArray(AttributeDefinition[]::new);

            builder.withSetterParameterDefinition("parameters", fromMultipleDefinitions(attributeDefinitions).build());
        }

        return builder.build();
    }

    protected abstract void doParse(Builder definition) throws ConfigurationException;

    protected void parseParameters(List<ParameterModel> parameters)
    {
        parameters.forEach(parameter -> {
            final String parameterName = parameter.getName();

            parameter.getType().accept(new MetadataTypeVisitor()
            {
                @Override
                protected void defaultVisit(MetadataType metadataType)
                {
                    parseAttributeParameter(parameterName, metadataType, parameter.getDefaultValue(), parameter.getExpressionSupport(), parameter.isRequired());
                }

                @Override
                public void visitObject(ObjectType objectType)
                {
                    parsePojoParameter(parameterName, objectType, parameter.getDefaultValue(), parameter.getExpressionSupport(), parameter.isRequired());
                }

                @Override
                public void visitDictionary(DictionaryType dictionaryType)
                {
                    // TODO
                }

                @Override
                public void visitArrayType(ArrayType arrayType)
                {
                    //TODO specify collection type
                    parseCollectionParameter(parameter, arrayType);
                }
            });
        });
    }

    protected void parseCollectionParameter(ParameterModel parameter, ArrayType arrayType)
    {
        parseAttributeParameter(parameter.getName(), parameter.getType(), parameter.getDefaultValue(), parameter.getExpressionSupport(), parameter.isRequired());
        addParameter(fromChildListConfiguration(getType(arrayType.getType())).withWrapperIdentifier(hyphenize(parameter.getName())));
    }

    private ValueResolver<?> resolverOf(String parameterName, MetadataType expectedType, Object value, Object defaultValue, ExpressionSupport expressionSupport, boolean required)
    {
        ValueResolver resolver = null;
        if (expressionSupport == LITERAL)
        {
            return new StaticValueResolver<>(value);
        }

        if (isExpressionFunction(expectedType) && value != null)
        {
            resolver = new ExpressionFunctionValueResolver<>((String) value, getGenericTypeAt((ObjectType) expectedType, 1, typeLoader).get());
        }

        final Class<Object> expectedClass = getType(expectedType);
        if (resolver == null)
        {
            if (isExpression(value, parser))
            {
                resolver = new TypeSafeExpressionValueResolver((String) value, expectedClass);
            }
        }

        if (resolver == null && value != null)
        {
            final ValueHolder<ValueResolver> resolverValueHolder = new ValueHolder<>();
            expectedType.accept(new BasicTypeMetadataVisitor()
            {
                @Override
                protected void visitBasicType(MetadataType metadataType)
                {
                    if (conversionService.canConvert(value.getClass(), expectedClass))
                    {
                        resolverValueHolder.set(new StaticValueResolver(conversionService.convert(value, expectedClass)));
                    }
                    else
                    {
                        defaultVisit(metadataType);
                    }
                }

                @Override
                public void visitDateTime(DateTimeType dateTimeType)
                {
                    resolverValueHolder.set(parseCalendar(value, dateTimeType, defaultValue));
                }

                @Override
                public void visitDate(DateType dateType)
                {
                    resolverValueHolder.set(parseDate(value, dateType, defaultValue));
                }

                @Override
                protected void defaultVisit(MetadataType metadataType)
                {
                    resolverValueHolder.set(new RegistryLookupValueResolver(value.toString()));
                }
            });

            resolver = resolverValueHolder.get();
        }

        if (resolver == null)
        {
            resolver = new StaticValueResolver<>(defaultValue);
        }

        if (resolver.isDynamic() && expressionSupport == NOT_SUPPORTED)
        {
            throw new IllegalArgumentException(format("An expression value was given for parameter '%s' but it doesn't support expressions", parameterName));
        }

        if (!resolver.isDynamic() && expressionSupport == REQUIRED && required)
        {
            throw new IllegalArgumentException(format("A fixed value was given for parameter '%s' but it only supports expressions", parameterName));
        }

        return resolver;
    }

    private boolean isExpression(Object value, TemplateParser parser)
    {
        return value instanceof String && parser.isContainsTemplate((String) value);
    }

    protected AttributeDefinition.Builder parseAttributeParameter(String name, MetadataType type, Object defaultValue, ExpressionSupport expressionSupport, boolean required)
    {
        AttributeDefinition.Builder definitionBuilder = fromSimpleParameter(name, value -> resolverOf(name, type, value, defaultValue, expressionSupport, required));
        addParameter(definitionBuilder);

        return definitionBuilder;
    }

    protected void parsePojoParameter(String name, ObjectType type, Object defaultValue, ExpressionSupport expressionSupport, boolean required)
    {
        parseAttributeParameter(name, type, defaultValue, expressionSupport, required);
        addParameter(fromChildConfiguration(getType(type)).withWrapperIdentifier(hyphenize(name)).withDefaultValue(defaultValue));
    }

    private void addParameter(AttributeDefinition.Builder definitionBuilder)
    {
        parameters.add(definitionBuilder);
    }

    private boolean isExpressionFunction(MetadataType metadataType)
    {
        if (!Function.class.isAssignableFrom(getType(metadataType)))
        {
            return false;
        }

        GenericTypesAnnotation generics = getSingleAnnotation(metadataType, GenericTypesAnnotation.class).orElse(null);
        if (generics == null)
        {
            return false;
        }

        if (generics.getGenericTypes().size() != 2)
        {
            return false;
        }

        final String genericClassName = generics.getGenericTypes().get(0);
        try
        {
            return MuleEvent.class.isAssignableFrom(ClassUtils.getClass(genericClassName));
        }
        catch (ClassNotFoundException e)
        {
            throw new MuleRuntimeException(createStaticMessage("Could not load class " + genericClassName), e);
        }
    }

    private ValueResolver parseCalendar(Object value, DateTimeType dataType, Object defaultValue)
    {
        if (isExpression(value, parser))
        {
            return new TypeSafeExpressionValueResolver((String) value, getType(dataType));
        }

        Date date = doParseDate(value, CALENDAR_FORMAT, defaultValue);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        return new StaticValueResolver(calendar);
    }

    private ValueResolver parseDate(Object value, DateType dateType, Object defaultValue)
    {
        if (isExpression(value, parser))
        {
            return new TypeSafeExpressionValueResolver((String) value, getType(dateType));
        }
        else
        {
            return new StaticValueResolver(doParseDate(value, DATE_FORMAT, defaultValue));
        }
    }

    private Date doParseDate(Object value, String parseFormat, Object defaultValue)
    {
        if (value == null)
        {
            if (defaultValue == null)
            {
                return null;
            }

            value = defaultValue;
        }

        if (value instanceof String)
        {
            SimpleDateFormat format = new SimpleDateFormat(parseFormat);
            try
            {
                return format.parse((String) value);
            }
            catch (ParseException e)
            {
                throw new IllegalArgumentException(format("Could not transform value '%s' into a Date using pattern '%s'", value, parseFormat));
            }
        }

        if (value instanceof Date)
        {
            return (Date) value;
        }

        throw new IllegalArgumentException(format("Could not transform value of type '%s' to Date", value != null ? value.getClass().getName() : "null"));
    }
}

