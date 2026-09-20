/*

Copyright 2025-2026 Jeffrey J. Weston <jjweston@gmail.com>

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

*/

package org.omegizent.qdrant;

import org.omegizent.OmegizentTestUtil;
import org.omegizent.TaskRunner;
import org.omegizent.embedding.Embedding;
import org.omegizent.embedding.ImmutableDoubleArray;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class QdrantServiceIT
{
    private final String              collectionName      = "omegizent_chunks_test";
    private final QdrantClientFactory qdrantClientFactory = new QdrantClientFactory();
    private final TaskRunner          taskRunner          = new TaskRunner( 200 );

    @AfterEach
    void tearDown()
    {
        OmegizentTestUtil.deleteCollection( this.qdrantClientFactory, this.collectionName, this.taskRunner );
    }

    @Test
    void testSearch() throws Exception
    {
        String className           = this.getClass().getSimpleName();
        int    collectionSize      = 1_536;
        int    inputCount          = 5;
        float  scoreDeltaThreshold = 0.00001f;

        List< SearchResult > expectedResults = new LinkedList<>();
        expectedResults.add( new SearchResult( 2, 0.67939620f ));
        expectedResults.add( new SearchResult( 1, 0.11987578f ));
        expectedResults.add( new SearchResult( 3, 0.11237586f ));
        expectedResults.add( new SearchResult( 4, 0.07297595f ));
        expectedResults.add( new SearchResult( 5, 0.07286711f ));

        try ( QdrantService qdrantService = new QdrantService(
                this.collectionName, collectionSize, false, this.taskRunner, this.qdrantClientFactory ))
        {
            for ( int i = 0; i < inputCount; i++ )
            {
                String resourceName = String.format( "%s-input-%d.json", className, i );
                long id = i + 1;
                ImmutableDoubleArray inputVector = this.getVector( resourceName );
                Embedding embedding = new Embedding( id, inputVector );
                qdrantService.upsert( embedding );
            }

            String resourceName = className + "-query.json";
            ImmutableDoubleArray queryVector = this.getVector( resourceName );
            List< SearchResult > actualResults = qdrantService.search( queryVector );

            assertEquals( expectedResults.size(), actualResults.size() );
            for ( int i = 0; i < expectedResults.size(); i++ )
            {
                float scoreDelta = Math.abs( expectedResults.get( i ).score() - actualResults.get( i ).score() );
                System.out.printf(
                        "Search Result: %d, Expected ID: %d, Actual ID: %d, " +
                                "Expected Score: %.10f, Actual Score: %.10f, Score Delta: %.10f%n",
                        i,
                        expectedResults.get( i ).id(),
                        actualResults.get( i ).id(),
                        expectedResults.get( i ).score(),
                        actualResults.get( i ).score(),
                        scoreDelta );

                assertEquals( expectedResults.get( i ).id(), actualResults.get( i ).id(), "Search Result ID " + i );
                assertThat( scoreDelta ).as( "Score Delta " + i ).isLessThanOrEqualTo( scoreDeltaThreshold );
            }
        }
    }

    private ImmutableDoubleArray getVector( String resourceName ) throws Exception
    {
        try ( InputStream resourceStream = this.getClass().getResourceAsStream( resourceName ))
        {
            return new ImmutableDoubleArray( OmegizentTestUtil.readInputStream( resourceStream ));
        }
    }
}
