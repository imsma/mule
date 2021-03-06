/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher;

import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;

import java.util.List;

public interface ServerPluginClassLoaderManager
{

    /**
     * Adds a new classloader
     *
     * @param classLoader plugin classloader to add. Non null
     */
    void addPluginClassLoader(ArtifactClassLoader classLoader);

    /**
     * Immutable list of current plugin classloaders
     *
     * @return list of classloaders. Non null
     */
    List<ArtifactClassLoader> getPluginClassLoaders();
}
