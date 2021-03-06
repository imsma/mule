/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transformer.simple;

import org.mule.runtime.api.metadata.SimpleDataType;
import org.mule.runtime.core.api.transformer.DiscoverableTransformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.transformer.AbstractTransformer;
import org.mule.runtime.core.transformer.types.DataTypeFactory;

import java.text.NumberFormat;

/**
 * <code>NumberToString</code> converts a Number to a String.  A NumberFormat is used if one is provided.
 */
public class NumberToString extends AbstractTransformer implements DiscoverableTransformer
{

    private NumberFormat numberFormat;

    /**
     * Give core transformers a slighty higher priority
     */
    private int priorityWeighting = DiscoverableTransformer.DEFAULT_PRIORITY_WEIGHTING + 1;

    public NumberToString()
    {
        registerSourceType(new SimpleDataType<>(Number.class));
        setReturnDataType(DataTypeFactory.STRING);
    }

    public NumberToString(NumberFormat numberFormat)
    {
        this();
        this.numberFormat = numberFormat;
    }

    @Override
    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        if (src == null)
        {
            return "";
        }

        if (numberFormat != null)
        {
            return numberFormat.format(src);
        }
        else
        {
            return ((Number) src).toString();
        }
    }

    @Override
    public int getPriorityWeighting()
    {
        return priorityWeighting;
    }

    @Override
    public void setPriorityWeighting(int priorityWeighting)
    {
        this.priorityWeighting = priorityWeighting;
    }

}
