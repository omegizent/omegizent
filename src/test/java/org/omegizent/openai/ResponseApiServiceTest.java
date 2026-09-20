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
import org.omegizent.embedding.Embedding;
import org.omegizent.embedding.EmbeddingCacheService;
import org.omegizent.embedding.EmbeddingService;
import org.omegizent.embedding.ImmutableDoubleArray;
import org.omegizent.qdrant.QdrantService;
import org.omegizent.qdrant.SearchResult;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith( MockitoExtension.class )
class ResponseApiServiceTest
{
    private final int testIterationLimit = 5;

    @Mock private OpenAiApiCaller       mockOpenAiApiCaller;
    @Mock private EmbeddingCacheService mockEmbeddingCacheService;
    @Mock private EmbeddingService      mockEmbeddingService;
    @Mock private QdrantService         mockQdrantService;
    @Mock private OmegizentLogger       mockOmegizentLogger;

    @Test
    void testConstructor_nullEmbeddingCacheService()
    {
        @SuppressWarnings( "DataFlowIssue" )
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class, () -> new ResponseApiService(
                        this.testIterationLimit, false, false, false, false, false,
                        null, this.mockEmbeddingService, this.mockQdrantService,
                        this.mockOpenAiApiCaller, this.mockOmegizentLogger ));

        assertEquals( "Embedding cache service must not be null.", exception.getMessage() );
    }

    @Test
    void testConstructor_nullEmbeddingService()
    {
        @SuppressWarnings( "DataFlowIssue" )
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class, () -> new ResponseApiService(
                        this.testIterationLimit, false, false, false, false, false,
                        this.mockEmbeddingCacheService, null, this.mockQdrantService,
                        this.mockOpenAiApiCaller, this.mockOmegizentLogger ));

        assertEquals( "Embedding service must not be null.", exception.getMessage() );
    }

    @Test
    void testConstructor_nullQdrantService()
    {
        @SuppressWarnings( "DataFlowIssue" )
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class, () -> new ResponseApiService(
                        this.testIterationLimit, false, false, false, false, false,
                        this.mockEmbeddingCacheService, this.mockEmbeddingService, null,
                        this.mockOpenAiApiCaller, this.mockOmegizentLogger ));

        assertEquals( "Qdrant service must not be null.", exception.getMessage() );
    }

    @Test
    void testConstructor_nullOpenAiCaller()
    {
        @SuppressWarnings( "DataFlowIssue" )
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class, () -> new ResponseApiService(
                        this.testIterationLimit, false, false, false, false, false,
                        this.mockEmbeddingCacheService, this.mockEmbeddingService, this.mockQdrantService,
                        null, this.mockOmegizentLogger ));

        assertEquals( "OpenAI API caller must not be null.", exception.getMessage() );
    }

    @Test
    void getResponse_nullQuery()
    {
        ResponseApiService responseApiService = new ResponseApiService(
                this.testIterationLimit, false, false, false, false, false,
                this.mockEmbeddingCacheService, this.mockEmbeddingService, this.mockQdrantService,
                this.mockOpenAiApiCaller, this.mockOmegizentLogger );

        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class,
                () -> responseApiService.getResponse( null ));

        assertEquals( "Query must not be null.", exception.getMessage() );
    }

    @Test
    @SuppressWarnings( "ExtractMethodRecommender" )
    void getResponse_iterationLimit()
    {
        int iterationLimit = 1024;

        ResponseApiService responseApiService = new ResponseApiService(
                iterationLimit, true, false, false, false, false,
                this.mockEmbeddingCacheService, this.mockEmbeddingService, this.mockQdrantService,
                this.mockOpenAiApiCaller, this.mockOmegizentLogger );

        String userQuery = "What is a problem with an infinite solution?";
        String functionQuery = "What is infinity plus one?";
        ImmutableDoubleArray queryVector = new ImmutableDoubleArray( new double[] { 0.5, 0.4, 0.3, 0.2, 0.1 } );
        Embedding queryEmbedding = new Embedding( 42, queryVector );
        SearchResult searchResult = new SearchResult( 7, 0.5f );
        List< SearchResult > searchResults = List.of( searchResult );
        String testSearchResult = "Infinity";

        String responseString = String.format(
                """
                {
                  "output":
                  [
                    {
                      "type" : "function_call",
                      "arguments" : "{\\"query\\":\\"%s\\"}",
                      "call_id" : "test_call_id",
                      "name" : "search_readme"
                    }
                  ],
                  "usage":
                  {
                    "input_tokens": 2000,
                    "output_tokens": 1000,
                    "total_tokens": 3000
                  }
                }
                """, functionQuery );

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode responseNode = objectMapper.readTree( responseString );

        when( this.mockOpenAiApiCaller.getResponse(
                        any(), any(), any(), any(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(),
                        any(), any() ))
                .thenReturn( responseNode );

        when( this.mockEmbeddingService.getEmbedding( functionQuery )).thenReturn( queryEmbedding );
        when( this.mockQdrantService.search( queryVector )).thenReturn( searchResults );
        when( this.mockEmbeddingCacheService.getInput( searchResult.id() )).thenReturn( testSearchResult );

        RuntimeException exception = assertThrowsExactly( RuntimeException.class,
                () -> responseApiService.getResponse( userQuery ));

        assertEquals( "Failed to get response within 1,024 iterations.", exception.getMessage() );

        InOrder inOrder = inOrder( this.mockOmegizentLogger );

        inOrder.verify( this.mockOmegizentLogger ).println(
                "Response API Call, Iteration: 1, New Input Tokens: 2,000, Total Input Tokens: 2,000, " +
                        "Output Tokens: 1,000, Total Tokens: 3,000" );

        for  ( int i = 1; i < iterationLimit; i++ )
        {
            inOrder.verify( this.mockOmegizentLogger ).println( String.format(
                    "Response API Call, Iteration: %,d, New Input Tokens: 0, Total Input Tokens: 2,000, " +
                            "Output Tokens: 1,000, Total Tokens: 3,000", i + 1 ));
        }

        inOrder.verifyNoMoreInteractions();
        verifyNoMoreInteractions( this.mockOmegizentLogger );
    }

    @Test
    void getResponse_success()
    {
        ResponseApiService responseApiService = new ResponseApiService(
                this.testIterationLimit, true, false, false, false, true,
                this.mockEmbeddingCacheService, this.mockEmbeddingService, this.mockQdrantService,
                this.mockOpenAiApiCaller, this.mockOmegizentLogger );

        String expectedUserQuery1         = "What is your quest?";
        String expectedUserResponse1      = "To seek the Holy Grail!";
        String expectedFunctionQuery1     = "What is my quest?";
        String expectedCallId1            = "test_call_id_1";
        String expectedSearchResultText1a = "My quest is to seek the Holy Grail!";
        String expectedSearchResultText1b = "I am looking for brave knights to join my Knights of the Round Table!";
        long   expectedSearchResultId1a   = 7;
        long   expectedSearchResultId1b   = 1_024;
        float  expectedSearchResulScore1a = 0.500f;
        float  expectedSearchResulScore1b = 0.250f;

        String expectedUserQuery2         = "What is your favorite color?";
        String expectedUserResponse2      = "Blue. No, yel-- aah!";
        String expectedFunctionQuery2     = "What is my favorite color?";
        String expectedCallId2            = "test_call_id_2";
        String expectedSearchResultText2a = "My favorite color is blue. Or maybe it's yellow...";
        String expectedSearchResultText2b = "I am looking for brave knights to join my Knights of the Round Table!";
        long   expectedSearchResultId2a   = 13;
        long   expectedSearchResultId2b   = 1_024;
        float  expectedSearchResulScore2a = 0.625f;
        float  expectedSearchResulScore2b = 0.125f;

        ImmutableDoubleArray queryVector1 = new ImmutableDoubleArray( new double[] { 0.5, 0.4, 0.3, 0.2, 0.1 } );
        Embedding queryEmbedding1 = new Embedding( 42, queryVector1 );
        List< SearchResult > searchResults1 = List.of(
                new SearchResult( expectedSearchResultId1a, expectedSearchResulScore1a ),
                new SearchResult( expectedSearchResultId1b, expectedSearchResulScore1b ));

        ImmutableDoubleArray queryVector2 = new ImmutableDoubleArray( new double[] { 0.6, 0.5, 0.4, 0.3, 0.2 } );
        Embedding queryEmbedding2 = new Embedding( 67, queryVector2 );
        List< SearchResult > searchResults2 = List.of(
                new SearchResult( expectedSearchResultId2a, expectedSearchResulScore2a ),
                new SearchResult( expectedSearchResultId2b, expectedSearchResulScore2b ));

        String responseStringFunctionCall =
                """
                {
                  "output":
                  [
                    {
                      "type" : "function_call",
                      "arguments" : "{\\"query\\":\\"%s\\"}",
                      "call_id" : "%s",
                      "name" : "search_readme"
                    }
                  ],
                  "usage":
                  {
                    "input_tokens": %d,
                    "output_tokens": %d,
                    "total_tokens": %d
                  }
                }
                """;

        String responseStringAnswer =
                """
                {
                  "output":
                  [
                    {
                      "type": "message",
                      "content":
                      [
                        {
                          "text": "%s"
                        }
                      ],
                      "role": "assistant"
                    }
                  ],
                  "usage":
                  {
                    "input_tokens": %d,
                    "output_tokens": %d,
                    "total_tokens": %d
                  }
                }
                """;

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode responseNode1 = objectMapper.readTree(
                String.format(
                        responseStringFunctionCall, expectedFunctionQuery1, expectedCallId1, 2_000, 1_000, 3_000 ));
        JsonNode responseNode2 = objectMapper.readTree(
                String.format(
                        responseStringAnswer, expectedUserResponse1, 3_500, 1_500, 5_000 ));
        JsonNode responseNode3 = objectMapper.readTree(
                String.format(
                        responseStringFunctionCall, expectedFunctionQuery2, expectedCallId2, 4_500, 2_000, 6_500 ));
        JsonNode responseNode4 = objectMapper.readTree(
                String.format(
                        responseStringAnswer, expectedUserResponse2, 7_000, 2_500, 9_500 ));

        List< JsonNode > responses = List.of( responseNode1, responseNode2, responseNode3, responseNode4 );
        AtomicInteger responseIndex = new AtomicInteger( 0 );
        List< JsonNode > inputNodeList = new LinkedList<>();

        when( this.mockOpenAiApiCaller.getResponse(
                        any(), any(), any(), any(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(),
                        any(), any() ))
                .then( invocation ->
                {
                    inputNodeList.add( invocation.getArgument( 2, ObjectNode.class ).path( "input" ).deepCopy() );
                    return responses.get( responseIndex.getAndIncrement() );
                } );

        when( this.mockEmbeddingService.getEmbedding( expectedFunctionQuery1 )).thenReturn( queryEmbedding1 );
        when( this.mockQdrantService.search( queryVector1 )).thenReturn( searchResults1 );
        when( this.mockEmbeddingCacheService.getInput( expectedSearchResultId1a ))
                .thenReturn( expectedSearchResultText1a );
        when( this.mockEmbeddingCacheService.getInput( expectedSearchResultId1b ))
                .thenReturn( expectedSearchResultText1b );

        when( this.mockEmbeddingService.getEmbedding( expectedFunctionQuery2 )).thenReturn( queryEmbedding2 );
        when( this.mockQdrantService.search( queryVector2 )).thenReturn( searchResults2 );
        when( this.mockEmbeddingCacheService.getInput( expectedSearchResultId2a ))
                .thenReturn( expectedSearchResultText2a );
        when( this.mockEmbeddingCacheService.getInput( expectedSearchResultId2b ))
                .thenReturn( expectedSearchResultText2b );

        assertEquals( expectedUserResponse1, responseApiService.getResponse( expectedUserQuery1 ));
        assertEquals( expectedUserResponse2, responseApiService.getResponse( expectedUserQuery2 ));

        InOrder inOrder = inOrder( this.mockOmegizentLogger );

        inOrder.verify( this.mockOmegizentLogger ).println( "Response API Call, Iteration: 1, " +
                "New Input Tokens: 2,000, Total Input Tokens: 2,000, Output Tokens: 1,000, Total Tokens: 3,000" );
        inOrder.verify( this.mockOmegizentLogger ).println( "Response API Call, Search Readme: What is my quest?" );
        inOrder.verify( this.mockOmegizentLogger ).println( "Chunk:     7, Score: 0.5000000000" );
        inOrder.verify( this.mockOmegizentLogger ).println( "Chunk: 1,024, Score: 0.2500000000" );
        inOrder.verify( this.mockOmegizentLogger ).println( "Response API Call, Iteration: 2, " +
                "New Input Tokens: 1,500, Total Input Tokens: 3,500, Output Tokens: 1,500, Total Tokens: 5,000" );
        inOrder.verify( this.mockOmegizentLogger ).println( "Response API Call, Iteration: 1, " +
                "New Input Tokens: 1,000, Total Input Tokens: 4,500, Output Tokens: 2,000, Total Tokens: 6,500" );
        inOrder.verify( this.mockOmegizentLogger ).println(
                "Response API Call, Search Readme: What is my favorite color?" );
        inOrder.verify( this.mockOmegizentLogger ).println( "Chunk:    13, Score: 0.6250000000" );
        inOrder.verify( this.mockOmegizentLogger ).println( "Chunk: 1,024, Score: 0.1250000000, Duplicate" );
        inOrder.verify( this.mockOmegizentLogger ).println( "Response API Call, Iteration: 2, " +
                "New Input Tokens: 2,500, Total Input Tokens: 7,000, Output Tokens: 2,500, Total Tokens: 9,500" );

        inOrder.verifyNoMoreInteractions();
        verifyNoMoreInteractions( this.mockOmegizentLogger );

        assertEquals( 4, inputNodeList.size() );
        assertEquals( 2, inputNodeList.get( 0 ).size() );
        assertEquals( 4, inputNodeList.get( 1 ).size() );
        assertEquals( 6, inputNodeList.get( 2 ).size() );
        assertEquals( 8, inputNodeList.get( 3 ).size() );

        assertEquals( expectedUserQuery1, inputNodeList.get( 0 ).path( 1 ).path( "content" ).asString() );

        assertEquals( expectedCallId1, inputNodeList.get( 1 ).path( 2 ).path( "call_id" ).asString() );
        assertEquals( expectedCallId1, inputNodeList.get( 1 ).path( 3 ).path( "call_id" ).asString() );

        String functionResponseJson1 = inputNodeList.get( 1 ).path( 3 ).path( "output" ).asString();
        JsonNode functionResponseNode1 = objectMapper.readTree( functionResponseJson1 );
        assertEquals( 2, functionResponseNode1.size() );

        assertEquals( expectedSearchResultId1a, functionResponseNode1.path( 0 ).path( "id" ).asLong() );
        assertEquals( expectedSearchResultId1b, functionResponseNode1.path( 1 ).path( "id" ).asLong() );

        assertEquals( expectedSearchResulScore1a, functionResponseNode1.path( 0 ).path( "score" ).asFloat() );
        assertEquals( expectedSearchResulScore1b, functionResponseNode1.path( 1 ).path( "score" ).asFloat() );

        assertEquals( expectedSearchResultText1a, functionResponseNode1.path( 0 ).path( "text" ).asString() );
        assertEquals( expectedSearchResultText1b, functionResponseNode1.path( 1 ).path( "text" ).asString() );

        assertFalse( functionResponseNode1.path( 0 ).path( "duplicate" ).asBoolean() );
        assertFalse( functionResponseNode1.path( 1 ).path( "duplicate" ).asBoolean() );

        assertEquals( expectedUserQuery2, inputNodeList.get( 2 ).path( 5 ).path( "content" ).asString() );

        assertEquals( expectedCallId2, inputNodeList.get( 3 ).path( 6 ).path( "call_id" ).asString() );
        assertEquals( expectedCallId2, inputNodeList.get( 3 ).path( 7 ).path( "call_id" ).asString() );

        String functionResponseJson2 = inputNodeList.get( 3 ).path( 7 ).path( "output" ).asString();
        JsonNode functionResponseNode2 = objectMapper.readTree( functionResponseJson2 );
        assertEquals( 2, functionResponseNode2.size() );

        assertEquals( expectedSearchResultId2a, functionResponseNode2.path( 0 ).path( "id" ).asLong() );
        assertEquals( expectedSearchResultId2b, functionResponseNode2.path( 1 ).path( "id" ).asLong() );

        assertEquals( expectedSearchResulScore2a, functionResponseNode2.path( 0 ).path( "score" ).asFloat() );
        assertEquals( expectedSearchResulScore2b, functionResponseNode2.path( 1 ).path( "score" ).asFloat() );

        assertEquals( expectedSearchResultText2a, functionResponseNode2.path( 0 ).path( "text" ).asString() );
        assertEquals( "",                         functionResponseNode2.path( 1 ).path( "text" ).asString() );

        assertFalse( functionResponseNode2.path( 0 ).path( "duplicate" ).asBoolean() );
        assertTrue(  functionResponseNode2.path( 1 ).path( "duplicate" ).asBoolean() );
    }

    @Test
    void handleOutput_noMessage_and_noFunctionCall()
    {
        ResponseApiService responseApiService = new ResponseApiService(
                this.testIterationLimit, false, false, false, false, false,
                this.mockEmbeddingCacheService, this.mockEmbeddingService, this.mockQdrantService,
                this.mockOpenAiApiCaller, this.mockOmegizentLogger );

        String queryString = "What is your quest?";

        String responseString =
                """
                {
                  "output":
                  [
                  ],
                  "usage":
                  {
                    "input_tokens": 2000,
                    "output_tokens": 1000,
                    "total_tokens": 3000
                  }
                }
                """;

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode responseNode = objectMapper.readTree( responseString );

        when( this.mockOpenAiApiCaller.getResponse(
                        any(), any(), any(), any(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(),
                        any(), any() ))
                .thenReturn( responseNode );

        RuntimeException exception = assertThrowsExactly( RuntimeException.class,
                () -> responseApiService.getResponse( queryString ));

        String expectedMessage =
                "Failed to find response message or function call:" +
                System.lineSeparator() +
                responseNode.path( "output" ).toPrettyString();

        assertEquals( expectedMessage, exception.getMessage() );
    }

    @Test
    @SuppressWarnings( "ExtractMethodRecommender" )
    void handleOutput_message_and_functionCall()
    {
        ResponseApiService responseApiService = new ResponseApiService(
                this.testIterationLimit, false, false, false, false, false,
                this.mockEmbeddingCacheService, this.mockEmbeddingService, this.mockQdrantService,
                this.mockOpenAiApiCaller, this.mockOmegizentLogger );

        String queryString = "What is your quest?";
        String testQuery = "Test Query";
        ImmutableDoubleArray queryVector = new ImmutableDoubleArray( new double[] { 0.5, 0.4, 0.3, 0.2, 0.1 } );
        Embedding queryEmbedding = new Embedding( 42, queryVector );
        SearchResult searchResult = new SearchResult( 7, 0.5f );
        List< SearchResult > searchResults = List.of( searchResult );
        String testSearchResult = "Test Search Result";

        String responseString = String.format(
                """
                {
                  "output":
                  [
                    {
                      "type": "message",
                      "content":
                      [
                        {
                          "text": "Test Message"
                        }
                      ],
                      "role": "assistant"
                    },
                    {
                        "type" : "function_call",
                        "arguments" : "{\\"query\\":\\"%s\\"}",
                        "call_id" : "test_call_id",
                        "name" : "search_readme"
                    }
                  ],
                  "usage":
                  {
                    "input_tokens": 2000,
                    "output_tokens": 1000,
                    "total_tokens": 3000
                  }
                }
                """, testQuery );

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode responseNode = objectMapper.readTree( responseString );

        when( this.mockOpenAiApiCaller.getResponse(
                        any(), any(), any(), any(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(),
                        any(), any() ))
                .thenReturn( responseNode );

        when( this.mockEmbeddingService.getEmbedding( testQuery )).thenReturn( queryEmbedding );
        when( this.mockQdrantService.search( queryVector )).thenReturn( searchResults );
        when( this.mockEmbeddingCacheService.getInput( searchResult.id() )).thenReturn( testSearchResult );

        RuntimeException exception = assertThrowsExactly( RuntimeException.class,
                () -> responseApiService.getResponse( queryString ));

        String expectedMessage =
                "Received response message with function call:" +
                System.lineSeparator() +
                responseNode.path( "output" ).toPrettyString();

        assertEquals( expectedMessage, exception.getMessage() );
    }

    @Test
    @SuppressWarnings( "ExtractMethodRecommender" )
    void handleOutput_noAssistantRole()
    {
        ResponseApiService responseApiService = new ResponseApiService(
                this.testIterationLimit, false, false, false, false, false,
                this.mockEmbeddingCacheService, this.mockEmbeddingService, this.mockQdrantService,
                this.mockOpenAiApiCaller, this.mockOmegizentLogger );

        String queryString = "What is your quest?";

        String responseString =
                """
                {
                  "output":
                  [
                    {
                      "type": "message",
                      "content":
                      [
                        {
                          "text": "Test Message"
                        }
                      ],
                      "role": "broken"
                    }
                  ],
                  "usage":
                  {
                    "input_tokens": 2000,
                    "output_tokens": 1000,
                    "total_tokens": 3000
                  }
                }
                """;

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode responseNode = objectMapper.readTree( responseString );

        when( this.mockOpenAiApiCaller.getResponse(
                        any(), any(), any(), any(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(),
                        any(), any() ))
                .thenReturn( responseNode );

        RuntimeException exception = assertThrowsExactly( RuntimeException.class,
                () -> responseApiService.getResponse( queryString ));

        String expectedMessage =
                "Found response message from unexpected role:" +
                System.lineSeparator() +
                responseNode.path( "output" ).toPrettyString();

        assertEquals( expectedMessage, exception.getMessage() );
    }

    @Test
    @SuppressWarnings( "ExtractMethodRecommender" )
    void handleOutput_multipleMessages()
    {
        ResponseApiService responseApiService = new ResponseApiService(
                this.testIterationLimit, false, false, false, false, false,
                this.mockEmbeddingCacheService, this.mockEmbeddingService, this.mockQdrantService,
                this.mockOpenAiApiCaller, this.mockOmegizentLogger );

        String queryString = "What is your quest?";

        String responseString =
                """
                {
                  "output":
                  [
                    {
                      "type": "message",
                      "content":
                      [
                        {
                          "text": "First Message"
                        }
                      ],
                      "role": "assistant"
                    },
                    {
                      "type": "message",
                      "content":
                      [
                        {
                          "text": "Second Message"
                        }
                      ],
                      "role": "assistant"
                    }
                  ],
                  "usage":
                  {
                    "input_tokens": 2000,
                    "output_tokens": 1000,
                    "total_tokens": 3000
                  }
                }
                """;

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode responseNode = objectMapper.readTree( responseString );

        when( this.mockOpenAiApiCaller.getResponse(
                        any(), any(), any(), any(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(),
                        any(), any() ))
                .thenReturn( responseNode );

        RuntimeException exception = assertThrowsExactly( RuntimeException.class,
                () -> responseApiService.getResponse( queryString ));

        String expectedMessage =
                "Found more than one response message:" +
                System.lineSeparator() +
                responseNode.path( "output" ).toPrettyString();

        assertEquals( expectedMessage, exception.getMessage() );
    }

    @Test
    @SuppressWarnings( "ExtractMethodRecommender" )
    void handleOutput_multipleContentElements()
    {
        ResponseApiService responseApiService = new ResponseApiService(
                this.testIterationLimit, false, false, false, false, false,
                this.mockEmbeddingCacheService, this.mockEmbeddingService, this.mockQdrantService,
                this.mockOpenAiApiCaller, this.mockOmegizentLogger );

        String queryString = "What is your quest?";

        ArrayNode contentNode = JsonNodeFactory.instance.arrayNode();
        for ( int i = 0; i < 1_024; i++ ) contentNode.add( JsonNodeFactory.instance.objectNode().put( "text", "foo" ));

        String responseString = String.format(
                """
                {
                  "output":
                  [
                    {
                      "type": "message",
                      "content": %s,
                      "role": "assistant"
                    }
                  ],
                  "usage":
                  {
                    "input_tokens": 2000,
                    "output_tokens": 1000,
                    "total_tokens": 3000
                  }
                }
                """, contentNode );

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode responseNode = objectMapper.readTree( responseString );

        when( this.mockOpenAiApiCaller.getResponse(
                        any(), any(), any(), any(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(),
                        any(), any() ))
                .thenReturn( responseNode );

        RuntimeException exception = assertThrowsExactly( RuntimeException.class,
                () -> responseApiService.getResponse( queryString ));

        String expectedMessage =
                "Expected 1 content element, but received 1,024:" +
                System.lineSeparator() +
                responseNode.path( "output" ).toPrettyString();

        assertEquals( expectedMessage, exception.getMessage() );
    }

    @Test
    @SuppressWarnings( "ExtractMethodRecommender" )
    void handleFunctionCall_invalidArguments()
    {
        ResponseApiService responseApiService = new ResponseApiService(
                this.testIterationLimit, false, false, false, false, false,
                this.mockEmbeddingCacheService, this.mockEmbeddingService, this.mockQdrantService,
                this.mockOpenAiApiCaller, this.mockOmegizentLogger );

        String queryString = "What is your quest?";

        String responseString =
                """
                {
                  "output":
                  [
                    {
                        "type" : "function_call",
                        "arguments" : "This is not valid JSON.",
                        "call_id" : "test_call_id",
                        "name" : "search_readme"
                    }
                  ],
                  "usage":
                  {
                    "input_tokens": 2000,
                    "output_tokens": 1000,
                    "total_tokens": 3000
                  }
                }
                """;

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode responseNode = objectMapper.readTree( responseString );

        when( this.mockOpenAiApiCaller.getResponse(
                        any(), any(), any(), any(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(),
                        any(), any() ))
                .thenReturn( responseNode );

        RuntimeException exception = assertThrowsExactly( RuntimeException.class,
                () -> responseApiService.getResponse( queryString ));

        String expectedMessage =
                "Failed to deserialize arguments:" +
                System.lineSeparator() +
                "This is not valid JSON.";

        assertEquals( expectedMessage, exception.getMessage() );
    }

    @Test
    @SuppressWarnings( "ExtractMethodRecommender" )
    void handleFunctionCall_invalidFunction()
    {
        ResponseApiService responseApiService = new ResponseApiService(
                this.testIterationLimit, false, false, false, false, false,
                this.mockEmbeddingCacheService, this.mockEmbeddingService, this.mockQdrantService,
                this.mockOpenAiApiCaller, this.mockOmegizentLogger );

        String queryString = "What is your quest?";

        String responseString =
                """
                {
                  "output":
                  [
                    {
                        "type" : "function_call",
                        "arguments" : "{\\"query\\":\\"Test Query\\"}",
                        "call_id" : "test_call_id",
                        "name" : "invalid_function"
                    }
                  ],
                  "usage":
                  {
                    "input_tokens": 2000,
                    "output_tokens": 1000,
                    "total_tokens": 3000
                  }
                }
                """;

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode responseNode = objectMapper.readTree( responseString );

        when( this.mockOpenAiApiCaller.getResponse(
                        any(), any(), any(), any(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(),
                        any(), any() ))
                .thenReturn( responseNode );

        RuntimeException exception = assertThrowsExactly( RuntimeException.class,
                () -> responseApiService.getResponse( queryString ));

        String expectedMessage =
                "Unrecognized function:" +
                System.lineSeparator() +
                responseNode.path( "output" ).path( 0 ).toPrettyString();

        assertEquals( expectedMessage, exception.getMessage() );
    }

    @Test
    @SuppressWarnings( "ExtractMethodRecommender" )
    void handleSearchReadme_emptyQuery()
    {
        ResponseApiService responseApiService = new ResponseApiService(
                this.testIterationLimit, false, false, false, false, false,
                this.mockEmbeddingCacheService, this.mockEmbeddingService, this.mockQdrantService,
                this.mockOpenAiApiCaller, this.mockOmegizentLogger );

        String queryString = "What is your quest?";

        String responseString =
                """
                {
                  "output":
                  [
                    {
                        "type" : "function_call",
                        "arguments" : "{\\"query\\":\\"\\"}",
                        "call_id" : "test_call_id",
                        "name" : "search_readme"
                    }
                  ],
                  "usage":
                  {
                    "input_tokens": 2000,
                    "output_tokens": 1000,
                    "total_tokens": 3000
                  }
                }
                """;

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode responseNode = objectMapper.readTree( responseString );

        when( this.mockOpenAiApiCaller.getResponse(
                        any(), any(), any(), any(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(),
                        any(), any() ))
                .thenReturn( responseNode );

        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class,
                () -> responseApiService.getResponse( queryString ));

        assertEquals( "Query must not be empty.", exception.getMessage() );
    }
}
