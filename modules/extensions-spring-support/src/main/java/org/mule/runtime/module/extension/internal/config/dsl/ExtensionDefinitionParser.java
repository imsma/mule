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
import static org.mule.metadata.utils.MetadataTypeUtils.getDefaultValue;
import static org.mule.metadata.utils.MetadataTypeUtils.getSingleAnnotation;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromChildConfiguration;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromChildMapConfiguration;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromFixedValue;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromMultipleDefinitions;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromSimpleParameter;
import static org.mule.runtime.config.spring.dsl.api.KeyAttributeDefinitionPair.newBuilder;
import static org.mule.runtime.config.spring.dsl.api.TypeDefinition.fromMapEntryType;
import static org.mule.runtime.config.spring.dsl.api.TypeDefinition.fromType;
import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import static org.mule.runtime.extension.api.introspection.declaration.type.TypeUtils.getExpressionSupport;
import static org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport.LITERAL;
import static org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport.REQUIRED;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getMemberName;
import static org.mule.runtime.module.extension.internal.util.NameUtils.hyphenize;
import static org.mule.runtime.module.extension.internal.util.NameUtils.pluralize;
import static org.mule.runtime.module.extension.internal.util.NameUtils.singularize;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.DateTimeType;
import org.mule.metadata.api.model.DateType;
import org.mule.metadata.api.model.DictionaryType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.metadata.java.annotation.GenericTypesAnnotation;
import org.mule.runtime.config.spring.dsl.api.AttributeDefinition;
import org.mule.runtime.config.spring.dsl.api.ComponentBuildingDefinition;
import org.mule.runtime.config.spring.dsl.api.ComponentBuildingDefinition.Builder;
import org.mule.runtime.config.spring.dsl.api.KeyAttributeDefinitionPair;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.util.ClassUtils;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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
    private final Map<String, AttributeDefinition.Builder> parameters = new HashMap<>();
    private final Builder baseDefinitionBuilder;
    private final List<ComponentBuildingDefinition> parsedDefinitions = new ArrayList<>();

    protected ExtensionDefinitionParser(Builder baseDefinitionBuilder)
    {
        this.baseDefinitionBuilder = baseDefinitionBuilder;
    }

    public final List<ComponentBuildingDefinition> parse() throws ConfigurationException
    {
        final Builder builder = baseDefinitionBuilder.copy();
        doParse(builder);

        AttributeDefinition parametersDefinition = fromFixedValue(new HashMap<>()).build();
        if (!parameters.isEmpty())
        {
            KeyAttributeDefinitionPair[] attributeDefinitions = parameters.entrySet().stream()
                    .map(entry -> newBuilder().withAttributeDefinition(entry.getValue().build()).withKey(entry.getKey()).build())
                    .toArray(KeyAttributeDefinitionPair[]::new);
            parametersDefinition = fromMultipleDefinitions(attributeDefinitions).build();
        }

        builder.withSetterParameterDefinition("parameters", parametersDefinition);

        addDefinition(builder.build());
        return parsedDefinitions;
    }

    protected abstract void doParse(Builder definitionBuilder) throws ConfigurationException;

    protected void parseParameters(List<ParameterModel> parameters)
    {
        parameters.forEach(parameter -> {
            parameter.getType().accept(new MetadataTypeVisitor()
            {
                @Override
                protected void defaultVisit(MetadataType metadataType)
                {
                    parseAttributeParameter(parameter);
                }

                @Override
                public void visitObject(ObjectType objectType)
                {
                    if (isExpressionFunction(objectType))
                    {
                        defaultVisit(objectType);
                        return;
                    }

                    parsePojoParameter(parameter);
                }

                @Override
                public void visitDictionary(DictionaryType dictionaryType)
                {
                    parseMapParameters(parameter, dictionaryType);
                }

                @Override
                public void visitArrayType(ArrayType arrayType)
                {
                    parseCollectionParameter(parameter, arrayType);
                }
            });
        });
    }

    private void parseMapParameters(ParameterModel parameter, DictionaryType dictionaryType)
    {
        parseAttributeParameter(parameter);

        Class<? extends Map> mapType = getType(dictionaryType);
        if (ConcurrentMap.class.equals(mapType))
        {
            mapType = ConcurrentHashMap.class;
        }
        else if (Map.class.equals(mapType))
        {
            mapType = HashMap.class;
        }

        final MetadataType keyType = dictionaryType.getKeyType();
        final MetadataType valueType = dictionaryType.getValueType();
        final Class<?> keyClass = getType(keyType);
        final Class<?> valueClass = getType(valueType);
        final String parameterName = parameter.getName();
        final String mapElementName = hyphenize(pluralize(parameterName));

        addParameter(parameterName, fromChildMapConfiguration(keyClass, valueClass)
                .withWrapperIdentifier(mapElementName)
                .withDefaultValue(parameter.getDefaultValue()));

        addDefinition(baseDefinitionBuilder.copy()
                              .withIdentifier(mapElementName)
                              .withTypeDefinition(fromType(mapType))
                              .build());

        String entryElementName = hyphenize(singularize(parameterName));

        valueType.accept(new MetadataTypeVisitor()
        {

            @Override
            protected void defaultVisit(MetadataType metadataType)
            {
                addDefinition(baseDefinitionBuilder.copy()
                                      .withIdentifier(entryElementName)
                                      .withTypeDefinition(fromMapEntryType(keyClass, valueClass))
                                      .withKeyTypeConverter(value -> resolverOf(parameterName, keyType, value, null, parameter.getExpressionSupport(), true))
                                      .withTypeConverter(value -> resolverOf(parameterName, valueType, value, null, parameter.getExpressionSupport(), true))
                                      .build());
            }

            @Override
            public void visitArrayType(ArrayType arrayType)
            {
                defaultVisit(arrayType);
                final String itemElementName = entryElementName + "-item";
                arrayType.getType().accept(new BasicTypeMetadataVisitor()
                {
                    @Override
                    protected void visitBasicType(MetadataType metadataType)
                    {
                        addDefinition(baseDefinitionBuilder.copy()
                                              .withIdentifier(itemElementName)
                                              .withTypeDefinition(fromType(getType(metadataType)))
                                              .withTypeConverter(value -> resolverOf(parameterName, metadataType, value, getDefaultValue(metadataType), getExpressionSupport(metadataType), false))
                                              .build());
                    }

                    @Override
                    protected void defaultVisit(MetadataType metadataType)
                    {
                        addDefinition(baseDefinitionBuilder.copy()
                                              .withIdentifier(itemElementName)
                                              .withTypeDefinition(fromType(ValueResolver.class))
                                              .withObjectFactoryType(TopLevelParameterObjectFactory.class)
                                              .withConstructorParameterDefinition(fromFixedValue(arrayType.getType()).build())
                                              .build());
                    }
                });
            }
        });
    }

    private void parseCollectionParameter(ParameterModel parameter, ArrayType arrayType)
    {
        parseAttributeParameter(parameter);

        Class<? extends Iterable> collectionType = getType(arrayType);

        if (Set.class.equals(collectionType))
        {
            collectionType = HashSet.class;
        }
        else if (Collection.class.equals(collectionType) || Iterable.class.equals(collectionType) || collectionType == null)
        {
            collectionType = List.class;
        }

        final String collectionElementName = hyphenize(parameter.getName());
        addParameter(parameter.getName(), fromChildConfiguration(collectionType).withWrapperIdentifier(collectionElementName));
        addDefinition(baseDefinitionBuilder.copy()
                              .withIdentifier(collectionElementName)
                              .withTypeDefinition(fromType(collectionType))
                              .build());

        Builder itemDefinitionBuilder = baseDefinitionBuilder.copy().withIdentifier(hyphenize(singularize(parameter.getName())));

        arrayType.getType().accept(new BasicTypeMetadataVisitor()
        {
            @Override
            protected void visitBasicType(MetadataType metadataType)
            {
                itemDefinitionBuilder.withTypeDefinition(fromType(getType(metadataType)))
                        .withTypeConverter(value -> resolverOf(parameter.getName(),
                                                               metadataType,
                                                               value,
                                                               getDefaultValue(metadataType).orElse(null),
                                                               getExpressionSupport(metadataType),
                                                               false));
            }

            @Override
            public void visitObject(ObjectType objectType)
            {
                itemDefinitionBuilder.withTypeDefinition(fromType(ValueResolver.class))
                        .withObjectFactoryType(TopLevelParameterObjectFactory.class)
                        .withConstructorParameterDefinition(fromFixedValue(objectType).build());
            }
        });

        addDefinition(itemDefinitionBuilder.build());
    }

    private ValueResolver<?> resolverOf(String parameterName, MetadataType expectedType, Object value, Object defaultValue, ExpressionSupport expressionSupport, boolean required)
    {
        if (value instanceof ValueResolver)
        {
            return (ValueResolver<?>) value;
        }

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

    protected AttributeDefinition.Builder parseAttributeParameter(ParameterModel parameterModel)
    {
        return parseAttributeParameter(getMemberName(parameterModel, parameterModel.getName()),
                                       parameterModel.getName(),
                                       parameterModel.getType(),
                                       parameterModel.getDefaultValue(),
                                       parameterModel.getExpressionSupport(),
                                       parameterModel.isRequired());
    }

    protected AttributeDefinition.Builder parseAttributeParameter(String key, String name, MetadataType type, Object defaultValue, ExpressionSupport expressionSupport, boolean required)
    {
        AttributeDefinition.Builder definitionBuilder = fromSimpleParameter(name, value -> resolverOf(name, type, value, defaultValue, expressionSupport, required));
        addParameter(key, definitionBuilder);

        return definitionBuilder;
    }

    protected void parsePojoParameter(ParameterModel parameterModel)
    {
        parsePojoParameter(getMemberName(parameterModel, parameterModel.getName()),
                           parameterModel.getName(),
                           (ObjectType) parameterModel.getType(),
                           parameterModel.getDefaultValue(),
                           parameterModel.getExpressionSupport(),
                           parameterModel.isRequired());
    }

    protected void parsePojoParameter(String key, String name, ObjectType type, Object defaultValue, ExpressionSupport expressionSupport, boolean required)
    {
        parseAttributeParameter(key, name, type, defaultValue, expressionSupport, required);
        addParameter(name, fromChildConfiguration(ValueResolver.class).withWrapperIdentifier(hyphenize(name)));
        for (ObjectFieldType field : type.getFields())
        {
            field.getValue().accept(new MetadataTypeVisitor()
            {
                @Override
                public void visitObject(ObjectType objectType)
                {
                    addDefinition(baseDefinitionBuilder.copy()
                                          .withIdentifier(hyphenize(field.getKey().getName().getLocalPart()))
                                          .withTypeDefinition(fromType(ValueResolver.class))
                                          .withObjectFactoryType(TopLevelParameterObjectFactory.class)
                                          .withConstructorParameterDefinition(fromFixedValue(objectType).build())
                                          .build());
                }
            });
        }
    }

    protected void addDefinition(ComponentBuildingDefinition definition)
    {
        parsedDefinitions.add(definition);
    }

    private void addParameter(String key, AttributeDefinition.Builder definitionBuilder)
    {
        parameters.put(key, definitionBuilder);
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

