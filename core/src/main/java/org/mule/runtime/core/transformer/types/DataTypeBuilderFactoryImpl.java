/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transformer.types;

import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.DataTypeBuilder;
import org.mule.runtime.api.metadata.DataTypeBuilderFactory;
import org.mule.runtime.api.metadata.MimeTypes;

import javax.activation.DataHandler;
import javax.activation.DataSource;

public class DataTypeBuilderFactoryImpl extends DataTypeBuilderFactory
{

    @Override
    protected <T> DataTypeBuilder<T> builder()
    {
        return new DataTypeBuilderImpl<>();
    }

    /**
     * Will create a {@link DataType} object from an object instance. This method will check
     * if the object value is a {@link org.mule.runtime.core.api.MuleMessage} instance and will take the type from the message payload
     * and check if a mime type is set on the message and used that when constructing the {@link DataType}
     * object.
     *
     * @param value an object instance.  This can be a {@link org.mule.runtime.core.api.MuleMessage}, a collection, a proxy instance or any other
     *          object
     * @return a data type that represents the object type.
     */
    @Override
    protected <T> DataType<T> createFromObject(T value)
    {
        if (value instanceof DataType)
        {
            return (DataType<T>) value;
        }

        return DataType.builder((Class<T>) getObjectType(value)).mimeType(getObjectMimeType(value)).build();
    }

    private static String getObjectMimeType(Object value)
    {
        String mime = null;
        if (value instanceof MuleMessage)
        {
            MuleMessage mm = (MuleMessage) value;
            mime = mm.getDataType().getMimeType();
        }
        else if (value instanceof DataHandler)
        {
            mime = ((DataHandler) value).getContentType();
        }
        else if (value instanceof DataSource)
        {
            mime = ((DataSource) value).getContentType();
        }

        if (mime != null)
        {
            int i = mime.indexOf(";");
            mime = (i > -1 ? mime.substring(0, i) : mime);
            // TODO set the charset on the DataType when the field is introduced BL-140
        }
        else
        {
            mime = MimeTypes.ANY;
        }

        return mime;
    }

    private static Class<?> getObjectType(Object value)
    {
        Class<?> type;
        if (value == null)
        {
            type = Object.class;
        }
        else
        {
            if (value instanceof MuleMessage)
            {
                MuleMessage mm = (MuleMessage) value;
                type = mm.getPayload().getClass();
            }
            else
            {
                type = value.getClass();
            }
        }
        return type;
    }
}
