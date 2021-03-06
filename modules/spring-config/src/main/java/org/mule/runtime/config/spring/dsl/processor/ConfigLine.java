/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.processor;

import static com.google.common.collect.ImmutableMap.copyOf;
import static org.mule.runtime.core.util.Preconditions.checkState;
import org.mule.runtime.core.util.Preconditions;

import com.google.common.collect.ImmutableMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Node;

/**
 * A configuration line represents the data within a line in a configuration file
 *
 * @since 4.0
 */
public class ConfigLine
{

    /**
     * Provides access to the parent configuration line of this config line
     */
    private ConfigLineProvider parent;
    /**
     * Namespace which defines the config line definition
     */
    private String namespace;
    /**
     * Identifier of the configuration entry
     */
    private String identifier;

    /**
     * The identifier attributes defined in the configuration
     */
    private Map<String, SimpleConfigAttribute> configAttributes = new HashMap<>();

    /**
     * Generic set of attributes to be used for custom configuration file formats attributes
     */
    private Map<String, Object> customAttributes = new HashMap<>();

    /**
     * Config lines embedded inside this config line
     */
    private List<ConfigLine> childrenConfigLines = new LinkedList<>();

    //TODO MULE-9638 remove once we don't need the old parsing mechanism anymore.
    private Node node;
    private String textContent;

    public ConfigLine()
    {
    }

    public String getNamespace() {
        return namespace;
    }

    public String getIdentifier() {
        return identifier;
    }

    public Map<String, SimpleConfigAttribute> getConfigAttributes() {
        return copyOf(configAttributes);
    }

    public Map<String, Object> getCustomAttributes()
    {
        return Collections.unmodifiableMap(customAttributes);
    }

    public List<ConfigLine> getChildren() {
        return childrenConfigLines;
    }

    public ConfigLine getParent() {
        return parent.getConfigLine();
    }

    public Node getNode()
    {
        return node;
    }

    public String getTextContent()
    {
        return textContent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConfigLine that = (ConfigLine) o;

        if (parent != null ? !parent.equals(that.parent) : that.parent != null) return false;
        if (namespace != null ? !namespace.equals(that.namespace) : that.namespace != null) return false;
        if (identifier != null ? !identifier.equals(that.identifier) : that.identifier != null) return false;
        if (configAttributes != null ? !configAttributes.equals(that.configAttributes) : that.configAttributes != null)
            return false;
        return childrenConfigLines != null ? childrenConfigLines.equals(that.childrenConfigLines) : that.childrenConfigLines == null;

    }

    @Override
    public int hashCode() {
        int result = parent != null ? parent.hashCode() : 0;
        result = 31 * result + (namespace != null ? namespace.hashCode() : 0);
        result = 31 * result + (identifier != null ? identifier.hashCode() : 0);
        result = 31 * result + (configAttributes != null ? configAttributes.hashCode() : 0);
        result = 31 * result + (childrenConfigLines != null ? childrenConfigLines.hashCode() : 0);
        return result;
    }

    public static class Builder
    {
        private ConfigLine configLine = new ConfigLine();
        private boolean alreadyBuild;

        public Builder setNamespace(String namespace) {
            checkState(!alreadyBuild, "builder already build an object, you cannot modify it");
            configLine.namespace = namespace;
            return this;
        }

        public Builder setIdentifier(String operation) {
            checkState(!alreadyBuild, "builder already build an object, you cannot modify it");
            configLine.identifier = operation;
            return this;
        }

        public Builder addConfigAttribute(String name, String value)
        {
            checkState(!alreadyBuild, "builder already build an object, you cannot modify it");
            configLine.configAttributes.put(name, new SimpleConfigAttribute(name, value));
            return this;
        }

        public Builder addCustomAttribute(String name, Object value)
        {
            checkState(!alreadyBuild, "builder already build an object, you cannot modify it");
            configLine.customAttributes.put(name, value);
            return this;
        }

        public Builder addChild(ConfigLine line)
        {
            checkState(!alreadyBuild, "builder already build an object, you cannot modify it");
            configLine.childrenConfigLines.add(line);
            return this;
        }

        public Builder setParent(ConfigLineProvider parent)
        {
            checkState(!alreadyBuild, "builder already build an object, you cannot modify it");
            configLine.parent = parent;
            return this;
        }

        public Builder setNode(Node node)
        {
            configLine.node = node;
            return this;
        }

        public Builder setTextContent(String textContent)
        {
            configLine.textContent = textContent;
            return this;
        }

        public ConfigLine build()
        {
            alreadyBuild = true;
            return configLine;
        }
    }

}
