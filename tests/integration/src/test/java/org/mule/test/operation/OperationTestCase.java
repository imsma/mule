/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.operation;

import static org.junit.Assert.assertThat;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleEvent;

import org.hamcrest.core.Is;
import org.junit.Assert;
import org.junit.Test;

public class OperationTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "operation-config.xml";
    }

    @Test
    public void test() throws Exception
    {
        MuleEvent muleEvent = flowRunner("flow").run();
        assertThat(muleEvent.getMessage().getPayload(), Is.is("hola amigoooooo"));
        assertThat(muleEvent.getFlowVariable("testVar"), Is.is("testVarValue"));

        muleEvent = flowRunner("flow2").run();
        assertThat(muleEvent.getMessage().getPayload(), Is.is("hola amigoooooo2"));
        assertThat(muleEvent.getFlowVariable("testVar"), Is.is("testVarValue2"));

        muleEvent = flowRunner("flow3").run();
        assertThat(muleEvent.getMessage().getPayload(), Is.is("hola amigoooooo3"));
        assertThat(muleEvent.getFlowVariable("testVar"), Is.is("testVarDefault"));

        muleEvent = flowRunner("flow4").run();
        assertThat(muleEvent.getMessage().getPayload(), Is.is("hola amigoooooo4"));
        assertThat(muleEvent.getFlowVariable("testVar"), Is.is("testVarConfigValue"));
    }
}
