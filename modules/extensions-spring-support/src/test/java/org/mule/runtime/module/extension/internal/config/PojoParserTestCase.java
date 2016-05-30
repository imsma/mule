/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;
import org.mule.test.heisenberg.extension.HeisenbergExtension;
import org.mule.test.heisenberg.extension.model.KnockeableDoor;

import org.junit.Test;

public class PojoParserTestCase extends ExtensionFunctionalTestCase
{

    @Override
    protected Class<?>[] getAnnotatedExtensionClasses()
    {
        return new Class[]{HeisenbergExtension.class};
    }

    @Override
    protected String getConfigFile()
    {
        return "heisenberg-pojo-parser.xml";
    }

    @Test
    public void parseDoor() throws Exception {
        ValueResolver<KnockeableDoor> resolver = muleContext.getRegistry().lookupObject("door");
        KnockeableDoor door = resolver.resolve(getTestEvent(""));

        assertThat(door.getVictim(), equalTo("Gustavo Fring"));
        assertThat(door.getAddress(), equalTo("pollos hermanos"));


    }
}
