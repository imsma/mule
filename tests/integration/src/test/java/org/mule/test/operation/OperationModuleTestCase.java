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
import org.junit.Test;

public class OperationModuleTestCase extends FunctionalTestCase
{


    //TODO missing tests:
    //1) define a config with less parameters than expected
    //2) define a config with at least one more parameter than expected
    //3) define a module without config
    //4) define a module without operations
    //5) use a module inside another module
    //6) create 2 modules with the same operation name and show they can co-exist

    @Override
    protected String getConfigFile()
    {
        return "operation-module-flow.xml";
    }

    @Test
    public void testSetPayloadHardcodedFlow() throws Exception{
        MuleEvent muleEvent = flowRunner("testSetPayloadHardcodedFlow").run();
        assertThat(muleEvent.getMessage().getPayload(), Is.is("hardcoded value"));
    }

    @Test
    public void testSetPayloadParamFlow() throws Exception{
        MuleEvent muleEvent = flowRunner("testSetPayloadParamFlow").run();
        assertThat(muleEvent.getMessage().getPayload(), Is.is("new payload"));
    }

    @Test
    public void testSetPayloadConfigParamFlow() throws Exception{
        MuleEvent muleEvent = flowRunner("testSetPayloadConfigParamFlow").run();
        assertThat(muleEvent.getMessage().getPayload(), Is.is("some config-value-parameter"));
    }

    @Test
    public void testSetPayloadConfigDefaultParamFlow() throws Exception{
        MuleEvent muleEvent = flowRunner("testSetPayloadConfigDefaultParamFlow").run();
        assertThat(muleEvent.getMessage().getPayload(), Is.is("some default-config-value-parameter"));
    }
}
