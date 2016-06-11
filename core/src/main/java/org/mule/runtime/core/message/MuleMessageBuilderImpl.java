/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.message;

import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.api.message.MuleMessageBuilder;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.DefaultMuleMessage;

/**
 * Provides a way to build immutable {@link MuleMessage} objects.
 *
 * @since 4.0
 */
public class MuleMessageBuilderImpl<T> implements MuleMessageBuilder<T>
{

    private Object payload;
    private DataType<T> dataType;

    @Override
    public MuleMessage build()
    {
        return new DefaultMuleMessage(payload, dataType);
    }

    @Override
    public MuleMessageBuilder<T> setPayload(Object payload)
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
}
