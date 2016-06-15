/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.message;

import static java.lang.String.format;
import static org.mule.runtime.core.util.UUID.getUUID;

import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.api.message.MuleMessageBuilder;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.DefaultMuleMessage;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Provides a way to build immutable {@link MuleMessage} objects.
 *
 * @since 4.0
 */
public class MuleMessageBuilderImpl<T> implements MuleMessageBuilder<T>
{

    private String id;
    private T payload;
    private DataType<T> dataType;
    private Map<String, Serializable> properties = new HashMap<>();

    @Override
    public MuleMessage<T, ? extends Serializable> build()
    {
        if (payload == null)
        {
            throw new IllegalArgumentException("MuleMessage cannot have 'null' payload.");
        }

        final DataType<T> payloadDataType = DataType.<T> createFromObject(payload);
        if (dataType == null)
        {
            dataType = payloadDataType;
        }
        else
        {
            if (!dataType.isCompatibleWith(payloadDataType))
            {
                throw new IllegalArgumentException(format("Given 'dataType' (%s) is not compatible with the given payload type (%s).", dataType.toString(), payloadDataType.toString()));
            }
        }

        final DefaultMuleMessage message = new DefaultMuleMessage(payload, dataType);
        message.setUniqueId(id != null ? id : getUUID());
        for (Entry<String, Serializable> propertyEntry : properties.entrySet())
        {
            message.setOutboundProperty(propertyEntry.getKey(), propertyEntry.getValue());
        }

        return (MuleMessage<T, ? extends Serializable>) message;
    }

    @Override
    public MuleMessageBuilder<T> setPayload(T payload)
    {
        this.payload = payload;
        return this;
    }

    @Override
    public MuleMessageBuilder<T> setDataType(DataType<T> dataType)
    {
        this.dataType = dataType;
        return this;
    }

    @Override
    public MuleMessageBuilder<T> setId(String id)
    {
        this.id = id;
        return this;
    }

    @Override
    public MuleMessageBuilder<T> addProperties(Map<String, Serializable> properties)
    {
        this.properties.putAll(properties);
        return this;
    }

    @Override
    public MuleMessageBuilder<T> addProperty(String key, Serializable value)
    {
        properties.put(key, value);
        return this;
    }

    @Override
    public MuleMessageBuilder<T> removeProperty(String key)
    {
        properties.remove(key);
        return this;
    }

    @Override
    public MuleMessageBuilder<T> copy(MuleMessage<T, ? extends Serializable> copyFrom)
    {
        // properties.clear();
        // properties.putAll(copyFrom.getAttributes());

        // id = copyFrom.getId();

        payload = copyFrom.getPayload();
        dataType = (DataType<T>) copyFrom.getDataType();

        return this;
    }
}
