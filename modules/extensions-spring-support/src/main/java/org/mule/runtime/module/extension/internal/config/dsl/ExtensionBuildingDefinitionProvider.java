/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl;

import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromChildConfiguration;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromSimpleParameter;
import static org.mule.runtime.config.spring.dsl.processor.TypeDefinition.fromType;
import static org.mule.runtime.core.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.core.util.Preconditions.checkState;
import static org.mule.runtime.module.extension.internal.config.dsl.config.ExtensionXmlNamespaceInfoProvider.EXTENSION_NAMESPACE;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getClassLoader;
import static org.mule.runtime.module.extension.internal.util.NameUtils.getTopLevelTypeName;
import static org.mule.runtime.module.extension.internal.util.NameUtils.hyphenize;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.DictionaryType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.config.spring.dsl.api.ComponentBuildingDefinition;
import org.mule.runtime.config.spring.dsl.api.ComponentBuildingDefinitionProvider;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.extension.api.BaseExtensionWalker;
import org.mule.runtime.extension.api.ExtensionManager;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.RuntimeExtensionModel;
import org.mule.runtime.extension.api.introspection.config.ConfigurationModel;
import org.mule.runtime.extension.api.introspection.config.RuntimeConfigurationModel;
import org.mule.runtime.extension.api.introspection.connection.ConnectionProviderModel;
import org.mule.runtime.extension.api.introspection.connection.HasConnectionProviderModels;
import org.mule.runtime.extension.api.introspection.operation.HasOperationModels;
import org.mule.runtime.extension.api.introspection.operation.OperationModel;
import org.mule.runtime.extension.api.introspection.operation.RuntimeOperationModel;
import org.mule.runtime.extension.api.introspection.parameter.ParameterModel;
import org.mule.runtime.extension.api.introspection.parameter.ParameterizedModel;
import org.mule.runtime.extension.api.introspection.property.SubTypesModelProperty;
import org.mule.runtime.extension.api.introspection.property.XmlModelProperty;
import org.mule.runtime.extension.api.introspection.source.HasSourceModels;
import org.mule.runtime.extension.api.introspection.source.RuntimeSourceModel;
import org.mule.runtime.extension.api.introspection.source.SourceModel;
import org.mule.runtime.module.extension.internal.config.ExtensionConfig;
import org.mule.runtime.module.extension.internal.config.dsl.config.ConfigurationDefinitionParser;
import org.mule.runtime.module.extension.internal.config.dsl.config.ConnectionProviderDefinitionParser;
import org.mule.runtime.module.extension.internal.config.dsl.config.OperationDefinitionParser;
import org.mule.runtime.module.extension.internal.config.dsl.config.SourceDefinitionParser;
import org.mule.runtime.module.extension.internal.introspection.SubTypesMappingContainer;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.xml.BeanDefinitionParser;

public class ExtensionBuildingDefinitionProvider implements ComponentBuildingDefinitionProvider
{

    private final List<ComponentBuildingDefinition> definitions = new LinkedList<>();

    private final Map<String, ExtensionModel> handledExtensions = new HashMap<>();
    private final Multimap<ExtensionModel, String> topLevelParameters = HashMultimap.create();
    private final Map<String, BeanDefinitionParser> parsers = new HashMap<>();
    private ExtensionManager extensionManager;
    private MuleContext muleContext;

    /**
     * Attempts to get a hold on a {@link ExtensionManager}
     * instance
     *
     * @throws java.lang.IllegalStateException if no extension manager could be found
     */
    @Override
    public void init(MuleContext muleContext)
    {
        this.muleContext = muleContext;
        extensionManager = muleContext.getExtensionManager();
        checkState(extensionManager != null, "Could not obtain the ExtensionManager");

        extensionManager.getExtensions().forEach(this::registerExtensionParsers);

    }

    @Override
    public List<ComponentBuildingDefinition> getComponentBuildingDefinitions()
    {
        ComponentBuildingDefinition.Builder baseDefinition = new ComponentBuildingDefinition.Builder().withNamespace(EXTENSION_NAMESPACE);
        definitions.add(baseDefinition.copy()
                                .withIdentifier("extensions-config")
                                .withTypeDefinition(fromType(ExtensionConfig.class))
                                .withObjectFactoryType(ExtensionConfigObjectFactory.class)
                                .withSetterParameterDefinition("dynamicConfigurationExpiration", fromChildConfiguration(DynamicConfigurationExpirationObjectFactory.class).build())
                                .build());
        definitions.add(baseDefinition.copy()
                                .withIdentifier("dynamic-configuration-expiration")
                                .withTypeDefinition(fromType(DynamicConfigurationExpiration.class))
                                .withObjectFactoryType(DynamicConfigurationExpirationObjectFactory.class)
                                .withConstructorParameterDefinition(fromSimpleParameter("frequency").build())
                                .withConstructorParameterDefinition(fromSimpleParameter("timeUnit", value -> TimeUnit.valueOf((String) value)).build())
                                .build());

        return definitions;
    }

    private void registerExtensionParsers(ExtensionModel extensionModel)
    {
        XmlModelProperty xmlModelProperty = extensionModel.getModelProperty(XmlModelProperty.class).orElse(null);
        if (xmlModelProperty == null)
        {
            return;
        }

        final ComponentBuildingDefinition.Builder definition = new ComponentBuildingDefinition.Builder().withNamespace(xmlModelProperty.getNamespace());
        Optional<SubTypesModelProperty> subTypesProperty = extensionModel.getModelProperty(SubTypesModelProperty.class);
        SubTypesMappingContainer typeMapping = new SubTypesMappingContainer(subTypesProperty.isPresent() ? subTypesProperty.get().getSubTypesMapping() : Collections.emptyMap());

        withContextClassLoader(getClassLoader(extensionModel), () -> {

            new BaseExtensionWalker()
            {
                @Override
                public void onConfiguration(ConfigurationModel model)
                {
                    parseWith(new ConfigurationDefinitionParser(definition, (RuntimeConfigurationModel) model, muleContext));
                }

                @Override
                public void onOperation(HasOperationModels owner, OperationModel model)
                {
                    parseWith(new OperationDefinitionParser(definition, (RuntimeExtensionModel) extensionModel, (RuntimeOperationModel) model, muleContext));
                }

                @Override
                public void onConnectionProvider(HasConnectionProviderModels owner, ConnectionProviderModel model)
                {
                    parseWith(new ConnectionProviderDefinitionParser(definition, model, muleContext));
                }

                @Override
                public void onSource(HasSourceModels owner, SourceModel model)
                {
                    parseWith(new SourceDefinitionParser(definition, (RuntimeExtensionModel) extensionModel, (RuntimeSourceModel) model, muleContext));
                }

                @Override
                public void onParameter(ParameterizedModel owner, ParameterModel model)
                {
                    typeMapping.getSubTypes(model.getType()).forEach(subtype -> registerTopLevelParameter(extensionModel, subtype, definition));
                    registerTopLevelParameter(extensionModel, model.getType(), definition);
                }
            }.walk(extensionModel);

            handledExtensions.put(xmlModelProperty.getNamespace(), extensionModel);
        });
    }

    private void parseWith(ExtensionDefinitionParser parser)
    {
        try
        {
            definitions.add(parser.parse());
        }
        catch (ConfigurationException e)
        {
            throw new MuleRuntimeException(e);
        }
    }

    private void registerTopLevelParameter(final ExtensionModel extensionModel, final MetadataType parameterType, ComponentBuildingDefinition.Builder definition)
    {
        parameterType.accept(new MetadataTypeVisitor()
        {
            @Override
            public void visitObject(ObjectType objectType)
            {
                String name = hyphenize(getTopLevelTypeName(objectType));
                if (topLevelParameters.put(extensionModel, name))
                {
                    parseWith(new TopLevelParameterParser(definition, objectType));
                }
            }

            @Override
            public void visitArrayType(ArrayType arrayType)
            {
                registerTopLevelParameter(extensionModel, arrayType.getType(), definition.copy());
            }

            @Override
            public void visitDictionary(DictionaryType dictionaryType)
            {
                MetadataType keyType = dictionaryType.getKeyType();
                keyType.accept(this);
                registerTopLevelParameter(extensionModel, keyType, definition.copy());
            }
        });
    }

}
