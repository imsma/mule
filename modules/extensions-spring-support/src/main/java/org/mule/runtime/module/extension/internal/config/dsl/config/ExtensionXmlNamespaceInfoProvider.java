/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl.config;

import org.mule.runtime.config.spring.dsl.api.xml.XmlNamespaceInfoProvider;

public class ExtensionXmlNamespaceInfoProvider implements XmlNamespaceInfoProvider
{
    public static final String EXTENSION_NAMESPACE = "extension";

    @Override
    public String getNamespaceUriPrefix()
    {
        return "http//://www.mulesoft.org/schema/mule/extension";
    }

    @Override
    public String getNamespace()
    {
        return EXTENSION_NAMESPACE;
    }
}
