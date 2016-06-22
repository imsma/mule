/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.junit4.runners;

import java.util.function.Predicate;

/**
 * {@link Predicate} to exclude {@link MavenArtifact} based on groupId, artifactId and type.
 * It support wildcard (any) for any of GAT fields. It is not supported partial wildcard startWith, endsWith for a GAT.
 */
public class MavenArtifactExclusionPredicate implements Predicate<MavenArtifact>
{

    private String groupId;
    private String artifactId;
    private String type;

    public MavenArtifactExclusionPredicate(String groupId, String artifactId, String type)
    {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.type = type;
    }

    @Override
    public boolean test(MavenArtifact mavenArtifact)
    {
        if (groupId.equals("*") || groupId.equals(mavenArtifact.getGroupId()))
        {
            if (artifactId.equals("*") || artifactId.equals(mavenArtifact.getArtifactId()))
            {
                if (type == null)
                {
                    return true;
                }
                else
                {
                    if (type.equals("*") || type.equals(mavenArtifact.getType()))
                    {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
