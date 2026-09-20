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

package org.omegizent.openai;

import org.omegizent.OmegizentTestUtil;
import org.omegizent.embedding.ImmutableDoubleArray;

import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EmbeddingApiServiceIT
{
    @Test
    void testGetEmbeddingVector() throws Exception
    {
        String input = "This is an automated integration test for calling the OpenAI Embedding API.";
        double similarityThreshold = 0.99999;

        String resourceName = this.getClass().getSimpleName() + ".json";

        ImmutableDoubleArray expectedVector;
        try ( InputStream resourceStream = this.getClass().getResourceAsStream( resourceName ))
        {
            expectedVector = new ImmutableDoubleArray( OmegizentTestUtil.readInputStream( resourceStream ));
        }

        OpenAiApiCaller openAiApiCaller = new OpenAiApiCaller();
        EmbeddingApiService embeddingApiService = new EmbeddingApiService( openAiApiCaller );
        ImmutableDoubleArray actualVector = embeddingApiService.getEmbeddingVector( input );

        assertEquals( expectedVector.length(), actualVector.length(), "Vector Length" );

        double similarity = this.cosineSimilarity( expectedVector.getArray(), actualVector.getArray() );
        System.out.printf( "Cosine Similarity: %.10f%n", similarity );
        assertThat( similarity ).as( "Cosine Similarity" ).isGreaterThanOrEqualTo( similarityThreshold );
    }

    private double cosineSimilarity( double[] a, double[] b )
    {
        double dot  = 0.0;
        double magA = 0.0;
        double magB = 0.0;

        for ( int i = 0; i < a.length; i++ )
        {
            double x = a[ i ];
            double y = b[ i ];

            dot  += x * y;
            magA += x * x;
            magB += y * y;
        }

        if ( magA == 0.0 || magB == 0.0 ) return 0.0;

        return dot / ( Math.sqrt( magA ) * Math.sqrt( magB ));
    }
}
