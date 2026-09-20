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

import org.omegizent.OmegizentLogger;
import org.omegizent.embedding.ImmutableDoubleArray;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith( MockitoExtension.class )
class EmbeddingApiServiceTest
{
    @Mock private OmegizentLogger mockOmegizentLogger;
    @Mock private OpenAiApiCaller mockOpenAiApiCaller;

    @Captor private ArgumentCaptor< String >     startMessageCaptor;
    @Captor private ArgumentCaptor< ObjectNode > requestNodeCaptor;

    @Test
    void testConstructor_nullOpenAiApiCaller()
    {
        @SuppressWarnings( "DataFlowIssue" )
        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class,
                () -> new EmbeddingApiService( false, false, false, false, null, this.mockOmegizentLogger ));

        assertEquals( "OpenAI API caller must not be null.", exception.getMessage() );
    }

    @Test
    void testGetEmbeddingVector_nullInput()
    {
        EmbeddingApiService embeddingApiService = new EmbeddingApiService(
                false, false, false, false, this.mockOpenAiApiCaller, this.mockOmegizentLogger );

        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class, () -> embeddingApiService.getEmbeddingVector( null ));

        assertEquals( "Input must not be null.", exception.getMessage() );
    }

    @Test
    void testGetEmbeddingVector_emptyInput()
    {
        EmbeddingApiService embeddingApiService = new EmbeddingApiService(
                false, false, false, false, this.mockOpenAiApiCaller, this.mockOmegizentLogger );

        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class, () -> embeddingApiService.getEmbeddingVector( "" ));

        assertEquals( "Input must not be empty.", exception.getMessage() );
    }

    @Test
    void testGetEmbeddingVector_longInput()
    {
        EmbeddingApiService embeddingApiService = new EmbeddingApiService(
                false, false, false, false, this.mockOpenAiApiCaller, this.mockOmegizentLogger );

        String input = "a".repeat( 32_768 );

        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class, () -> embeddingApiService.getEmbeddingVector( input ));

        assertEquals( "Input length must not be greater than 20,000. Actual Length: 32,768", exception.getMessage() );
    }

    @Test
    @SuppressWarnings( "ExtractMethodRecommender" )
    void testGetEmbeddingVector_success()
    {
        String expectedInput = "This is a test with \"quote\" characters included in it. ".repeat( 20 ).trim();
        ImmutableDoubleArray expectedVector = new ImmutableDoubleArray( new double[] { -0.75, -0.5, 0.5, 0.75 } );

        String responseString = String.format(
                """
                {
                  "object" : "list",
                  "data" : [ {
                    "object" : "embedding",
                    "index" : 0,
                    "embedding" : %s
                  } ],
                  "usage" : {
                    "total_tokens" : 1024
                  }
                }
                """, expectedVector );

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode responseNode = objectMapper.readTree( responseString );

        when( this.mockOpenAiApiCaller
                .getResponse(
                        any(), any(), this.requestNodeCaptor.capture(), this.startMessageCaptor.capture(),
                        anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(), any(), any() ))
                .thenReturn( responseNode );

        EmbeddingApiService embeddingApiService = new EmbeddingApiService(
                true, false, false, false, this.mockOpenAiApiCaller, this.mockOmegizentLogger );

        ImmutableDoubleArray actualVector = embeddingApiService.getEmbeddingVector( expectedInput );

        ObjectNode requestNode = this.requestNodeCaptor.getValue();
        String actualInput = requestNode.get( "input" ).asString();
        String actualStartMessage = this.startMessageCaptor.getValue();

        assertEquals( expectedInput, actualInput );
        assertEquals( expectedVector, actualVector );
        assertEquals( "Input Length: 1,099", actualStartMessage );

        verify( this.mockOmegizentLogger ).println( "Embedding API Call, Tokens: 1,024" );
        verifyNoMoreInteractions( this.mockOmegizentLogger );
    }
}
