/*

Copyright 2025 Jeffrey J. Weston <jjweston@gmail.com>

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

package org.omegizent.embedding;

import org.omegizent.openai.EmbeddingApiService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.Mockito.when;

@ExtendWith( MockitoExtension.class )
class EmbeddingServiceTest
{
    private final String testString = "Test";

    private final Embedding testEmbedding =
            new Embedding( 42, new ImmutableDoubleArray( new double[] { -0.75, -0.5, 0.5, 0.75 } ));

    @Mock private EmbeddingCacheService mockEmbeddingCacheService;
    @Mock private EmbeddingApiService   mockEmbeddingApiService;

    @Test
    void testConstructor_nullEmbeddingCacheService()
    {
        @SuppressWarnings( "DataFlowIssue" )
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class, () -> new EmbeddingService( null, this.mockEmbeddingApiService ));

        assertEquals( "Embedding cache service must not be null.", exception.getMessage() );
    }

    @Test
    void testConstructor_nullEmbeddingApiService()
    {
        @SuppressWarnings( "DataFlowIssue" )
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class, () -> new EmbeddingService( this.mockEmbeddingCacheService, null ));

        assertEquals( "Embedding API service must not be null.", exception.getMessage() );
    }

    @Test
    void testGetEmbedding_cacheHit()
    {
        EmbeddingService embeddingService =
                new EmbeddingService( this.mockEmbeddingCacheService, this.mockEmbeddingApiService );

        when( this.mockEmbeddingCacheService.getEmbedding( this.testString )).thenReturn( this.testEmbedding );
        Embedding actualEmbedding = embeddingService.getEmbedding( this.testString );
        assertEquals( this.testEmbedding, actualEmbedding );
    }

    @Test
    void testGetEmbedding_cacheMiss()
    {
        EmbeddingService embeddingService =
                new EmbeddingService( this.mockEmbeddingCacheService, this.mockEmbeddingApiService );

        when( this.mockEmbeddingCacheService.getEmbedding( this.testString )).thenReturn( null );
        when( this.mockEmbeddingApiService.getEmbeddingVector( this.testString )).
                thenReturn( this.testEmbedding.vector() );
        when( this.mockEmbeddingCacheService.cacheEmbedding( this.testString, this.testEmbedding.vector() ))
                .thenReturn( this.testEmbedding.id() );
        Embedding actualEmbedding = embeddingService.getEmbedding( this.testString );
        assertEquals( this.testEmbedding, actualEmbedding );
    }
}
