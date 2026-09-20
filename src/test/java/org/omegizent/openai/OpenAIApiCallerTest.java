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

import org.omegizent.Environment;
import org.omegizent.OmegizentLogger;
import org.omegizent.OmegizentUtil;
import org.omegizent.TaskRunner;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith( MockitoExtension.class )
class OpenAIApiCallerTest
{
    private final String     testTaskName      = "OpenAIApiCallerTest";
    private final String     testApiEndpoint   = "https://example.org/v1/test";
    private final String     testApiKeyVarName = "OMEGIZENT_TEST_API_KEY";
    private final ObjectNode testRequestNode   = JsonNodeFactory.instance.objectNode();

    @Mock private Environment            mockEnvironment;
    @Mock private HttpRequestBuilder     mockHttpRequestBuilder;
    @Mock private HttpClient             mockHttpClient;
    @Mock private Random                 mockRandom;
    @Mock private OmegizentUtil          mockOmegizentUtil_OpenAiApiCaller;
    @Mock private OmegizentUtil          mockOmegizentUtil_TaskRunner;
    @Mock private OmegizentLogger        mockOmegizentLogger;
    @Mock private HttpResponse< String > mockHttpResponse;
    @Mock private HttpHeaders            mockHttpHeaders;

    @Captor private ArgumentCaptor< String > requestBodyCaptor;

    @Test
    void testGetResponse_nullTaskName()
    {
        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class,
                () -> this.createOpenAiApiCaller().getResponse(
                        null, this.testApiEndpoint, this.testRequestNode, null,
                        false, false, false, false, List.of(), Map.of() ));

        assertEquals( "Task name must not be null.", exception.getMessage() );
    }

    @Test
    void testGetResponse_nullApiEndpoint()
    {
        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class,
                () -> this.createOpenAiApiCaller().getResponse(
                        this.testTaskName, null, this.testRequestNode, null,
                        false, false, false, false, List.of(), Map.of() ));

        assertEquals( "API endpoint must not be null.", exception.getMessage() );
    }

    @Test
    void testGetResponse_nullRequestNode()
    {
        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class,
                () -> this.createOpenAiApiCaller().getResponse(
                        this.testTaskName, this.testApiEndpoint, null, null,
                        false, false, false, false, List.of(), Map.of() ));

        assertEquals( "Request node must not be null.", exception.getMessage() );
    }

    @Test
    void testGetResponse_rateLimit_noRetry() throws Exception
    {
        String response =
                """
                {
                    "error":
                    {
                        "message": "Request too large.",
                        "type": "tokens",
                        "code": "rate_limit_exceeded",
                        "param": null
                    }
                }
                """;

        this.mockApiCall( response, 429 );
        when( this.mockHttpResponse.headers() ).thenReturn( this.mockHttpHeaders );

        RuntimeException exception = assertThrowsExactly( RuntimeException.class,
                () -> this.createOpenAiApiCaller().getResponse(
                        this.testTaskName, this.testApiEndpoint, this.testRequestNode, null,
                        false, false, false, false, List.of(), Map.of() ));

        String expectedMessage =
                "OpenAIApiCallerTest, Error Returned, Status Code: 429, Error Message: Request too large.";

        assertEquals( expectedMessage, exception.getMessage() );
    }

    @Test
    void testGetResponse_rateLimit_invalidRetryDelay() throws Exception
    {
        String response =
                """
                {
                    "error":
                    {
                        "message": "Rate limit reached. Please try again in -5 ms.",
                        "type": "tokens",
                        "code": "rate_limit_exceeded",
                        "param": null
                    }
                }
                """;

        this.mockApiCall( response, 429 );
        when( this.mockHttpResponse.headers() ).thenReturn( this.mockHttpHeaders );
        when( this.mockHttpHeaders.firstValueAsLong( "retry-after-ms" )).thenReturn( OptionalLong.of( -5 ));

        RuntimeException exception = assertThrowsExactly( RuntimeException.class,
                () -> this.createOpenAiApiCaller().getResponse(
                        this.testTaskName, this.testApiEndpoint, this.testRequestNode, null,
                        false, false, false, false, List.of(), Map.of() ));

        String expectedMessage =
                "OpenAIApiCallerTest, Error Returned, Status Code: 429, " +
                "Error Message: Rate limit reached. Please try again in -5 ms.";

        assertEquals( expectedMessage, exception.getMessage() );

        verifyNoMoreInteractions( this.mockOmegizentUtil_OpenAiApiCaller );
    }

    @Test
    void testGetResponse_rateLimit_maxAttemptsExceeded() throws Exception
    {
        String response =
                """
                {
                    "error":
                    {
                        "message": "Rate limit reached. Please try again in 20000 ms.",
                        "type": "tokens",
                        "code": "rate_limit_exceeded",
                        "param": null
                    }
                }
                """;

        this.mockApiCall( response, 429 );
        when( this.mockHttpResponse.headers() ).thenReturn( this.mockHttpHeaders );
        when( this.mockHttpHeaders.firstValueAsLong( "retry-after-ms" )).thenReturn( OptionalLong.of( 20_000 ));
        when( this.mockRandom.nextFloat() ).thenReturn( 0.5f );

        RuntimeException exception = assertThrowsExactly( RuntimeException.class,
                () -> this.createOpenAiApiCaller().getResponse(
                        this.testTaskName, this.testApiEndpoint, this.testRequestNode, null,
                        true, false, false, false, List.of(), Map.of() ));

        String expectedMessage =
                "OpenAIApiCallerTest, Error Returned, Status Code: 429, " +
                        "Error Message: Rate limit reached. Please try again in 20000 ms.";

        assertEquals( expectedMessage, exception.getMessage() );

        InOrder inOrder = inOrder( this.mockOmegizentLogger, this.mockOmegizentUtil_OpenAiApiCaller );
        inOrder.verify( this.mockOmegizentLogger ).println( "OpenAIApiCallerTest, Starting" );
        inOrder.verify( this.mockOmegizentLogger ).println( "OpenAIApiCallerTest, Complete, Duration: 0 ms" );
        inOrder.verify( this.mockOmegizentLogger ).println( "OpenAIApiCallerTest, Rate Limit Exceeded, " +
                "Attempt: 1, Raw Retry Delay: 20,000 ms, Retry Delay: 20,000 ms, Jitter: 1.150, Sleeping: 23,000 ms" );
        inOrder.verify( this.mockOmegizentUtil_OpenAiApiCaller ).sleepThread( 23_000 );
        inOrder.verify( this.mockOmegizentLogger ).println( "OpenAIApiCallerTest, Starting" );
        inOrder.verify( this.mockOmegizentLogger ).println( "OpenAIApiCallerTest, Complete, Duration: 0 ms" );
        inOrder.verify( this.mockOmegizentLogger ).println( "OpenAIApiCallerTest, Rate Limit Exceeded, " +
                "Attempt: 2, Raw Retry Delay: 20,000 ms, Retry Delay: 23,000 ms, Jitter: 2.000, Sleeping: 46,000 ms" );
        inOrder.verify( this.mockOmegizentUtil_OpenAiApiCaller ).sleepThread( 46_000 );
        inOrder.verify( this.mockOmegizentLogger ).println( "OpenAIApiCallerTest, Starting" );
        inOrder.verify( this.mockOmegizentLogger ).println( "OpenAIApiCallerTest, Complete, Duration: 0 ms" );
        inOrder.verifyNoMoreInteractions();
        verifyNoMoreInteractions( this.mockOmegizentLogger, this.mockOmegizentUtil_OpenAiApiCaller );
    }

    @Test
    void testGetResponse_rateLimit_sleepInterrupted() throws Exception
    {
        String response =
                """
                {
                    "error":
                    {
                        "message": "Rate limit reached. Please try again in 30000 ms.",
                        "type": "tokens",
                        "code": "rate_limit_exceeded",
                        "param": null
                    }
                }
                """;

        InterruptedException interruptedException = new InterruptedException( "Test Interruption" );

        this.mockApiCall( response, 429 );
        when( this.mockHttpResponse.headers() ).thenReturn( this.mockHttpHeaders );
        when( this.mockHttpHeaders.firstValueAsLong( "retry-after-ms" )).thenReturn( OptionalLong.of( 30_000 ));
        when( this.mockRandom.nextFloat() ).thenReturn( 0.25f );
        doThrow( interruptedException ).when( this.mockOmegizentUtil_OpenAiApiCaller ).sleepThread( 33_750 );

        RuntimeException exception = assertThrowsExactly( RuntimeException.class,
                () -> this.createOpenAiApiCaller().getResponse(
                        this.testTaskName, this.testApiEndpoint, this.testRequestNode, null,
                        false, false, false, false, List.of(), Map.of() ));

        String expectedMessage = "OpenAIApiCallerTest, Retry Sleep Interrupted";

        assertEquals( expectedMessage, exception.getMessage() );
        assertEquals( interruptedException, exception.getCause() );

        verify( this.mockOmegizentUtil_OpenAiApiCaller ).interruptThread();
        verifyNoMoreInteractions( this.mockOmegizentUtil_OpenAiApiCaller );
    }

    @Test
    void testGetResponse_rateLimit_retrySuccess() throws Exception
    {
        String response =
                """
                {
                  "color": "yellow",
                  "noun": "snow"
                }
                """;

        JsonNode expectedResponseNode = JsonNodeFactory.instance.objectNode()
                .put( "color", "yellow" )
                .put( "noun", "snow" );

        this.mockApiCall( response, 429, 200 );
        when( this.mockHttpResponse.headers() ).thenReturn( this.mockHttpHeaders );
        when( this.mockHttpHeaders.firstValueAsLong( "retry-after-ms" )).thenReturn( OptionalLong.of( 5_000 ));
        when( this.mockRandom.nextFloat() ).thenReturn( 0.75f );

        JsonNode actualResponseNode = this.createOpenAiApiCaller().getResponse(
                this.testTaskName, this.testApiEndpoint, this.testRequestNode, "Start Message",
                true, false, false, false, List.of(), Map.of() );

        assertEquals( expectedResponseNode, actualResponseNode );

        InOrder inOrder = inOrder( this.mockOmegizentLogger, this.mockOmegizentUtil_OpenAiApiCaller );
        inOrder.verify( this.mockOmegizentLogger ).println( "OpenAIApiCallerTest, Starting, Start Message" );
        inOrder.verify( this.mockOmegizentLogger ).println( "OpenAIApiCallerTest, Complete, Duration: 0 ms" );
        inOrder.verify( this.mockOmegizentLogger ).println( "OpenAIApiCallerTest, Rate Limit Exceeded, " +
                "Attempt: 1, Raw Retry Delay: 5,000 ms, Retry Delay: 10,000 ms, Jitter: 1.175, Sleeping: 11,750 ms" );
        inOrder.verify( this.mockOmegizentUtil_OpenAiApiCaller ).sleepThread( 11_750 );
        inOrder.verify( this.mockOmegizentLogger ).println( "OpenAIApiCallerTest, Starting, Start Message" );
        inOrder.verify( this.mockOmegizentLogger ).println( "OpenAIApiCallerTest, Complete, Duration: 0 ms" );
        inOrder.verifyNoMoreInteractions();
        verifyNoMoreInteractions( this.mockOmegizentLogger, this.mockOmegizentUtil_OpenAiApiCaller );
    }

    @Test
    void testGetResponse_error_withoutMessage() throws Exception
    {
        String response =
                """
                {
                }
                """;

        this.mockApiCall( response, 500 );

        RuntimeException exception = assertThrowsExactly( RuntimeException.class,
                () -> this.createOpenAiApiCaller().getResponse(
                        this.testTaskName, this.testApiEndpoint, this.testRequestNode, null,
                        false, false, false, false, List.of(),  Map.of() ));

        assertEquals( "OpenAIApiCallerTest, Error Returned, Status Code: 500", exception.getMessage() );
    }

    @Test
    void testGetResponse_error_withMessage() throws Exception
    {
        String response =
                """
                {
                    "error":
                    {
                        "message": "Incorrect API key provided.",
                        "type": "invalid_request_error",
                        "code": "invalid_api_key",
                        "param": null
                    }
                }
                """;

        this.mockApiCall( response, 401 );

        RuntimeException exception = assertThrowsExactly( RuntimeException.class,
                () -> this.createOpenAiApiCaller().getResponse(
                        this.testTaskName, this.testApiEndpoint, this.testRequestNode, null,
                        false, false, false, false, List.of(), Map.of() ));

        String expectedMessage =
                "OpenAIApiCallerTest, Error Returned, Status Code: 401, Error Message: Incorrect API key provided.";

        assertEquals( expectedMessage, exception.getMessage() );
    }

    @Test
    void testGetResponse_invalidResponse() throws Exception
    {
        String response = "This is not valid JSON.";

        this.mockApiCall( response, 402 );

        RuntimeException exception = assertThrowsExactly( RuntimeException.class,
                () -> this.createOpenAiApiCaller().getResponse(
                        this.testTaskName, this.testApiEndpoint, this.testRequestNode, null,
                        false, false, false, false, List.of(), Map.of() ));

        String expectedMessage =
                "OpenAIApiCallerTest, Failed to deserialize response. Status Code: 402, Response:" +
                System.lineSeparator() +
                "This is not valid JSON.";

        assertEquals( expectedMessage, exception.getMessage() );
    }

    @Test
    void testGetResponse_success() throws Exception
    {
        ObjectMapper objectMapper = new ObjectMapper();

        String response =
                """
                {
                  "adjective": "frozen",
                  "noun": "yogurt"
                }
                """;

        ObjectNode expectedRequestNode = JsonNodeFactory.instance.objectNode()
                .put( "query", "What is your favorite food?" );

        ObjectNode expectedResponseNode = JsonNodeFactory.instance.objectNode()
                .put( "adjective", "frozen" )
                .put( "noun", "yogurt" );

        this.mockApiCall( response, 200 );

        JsonNode actualResponseNode = this.createOpenAiApiCaller().getResponse(
                this.testTaskName, this.testApiEndpoint, expectedRequestNode, "Start Message",
                true, false, false, false, List.of(), Map.of() );

        String actualRequestString = this.requestBodyCaptor.getValue();
        JsonNode actualRequestNode = objectMapper.readTree( actualRequestString );

        assertEquals( expectedRequestNode, actualRequestNode );
        assertEquals( expectedResponseNode, actualResponseNode );

        InOrder inOrder = inOrder( this.mockOmegizentLogger );
        inOrder.verify( this.mockOmegizentLogger ).println( "OpenAIApiCallerTest, Starting, Start Message" );
        inOrder.verify( this.mockOmegizentLogger ).println( "OpenAIApiCallerTest, Complete, Duration: 0 ms" );
        inOrder.verifyNoMoreInteractions();
        verifyNoMoreInteractions( this.mockOmegizentLogger );
    }

    private OpenAiApiCaller createOpenAiApiCaller()
    {
        int  testMaxAttempts       = 3;
        long testMinimumRetryDelay = 10_000;

        TaskRunner testTaskRunner = new TaskRunner( 0, this.mockOmegizentUtil_TaskRunner, this.mockOmegizentLogger );

        return new OpenAiApiCaller(
                testMaxAttempts, testMinimumRetryDelay, this.testApiKeyVarName, this.mockEnvironment,
                this.mockHttpRequestBuilder, this.mockHttpClient, this.mockRandom,
                this.mockOmegizentUtil_OpenAiApiCaller, this.mockOmegizentLogger, testTaskRunner );
    }

    private void mockApiCall( String response, int... statusCodes ) throws Exception
    {
        String testApiKey = "Test API Key";

        when( this.mockEnvironment.getString( this.testApiKeyVarName )).thenReturn( testApiKey );
        when( this.mockHttpRequestBuilder.reset() ).thenReturn( this.mockHttpRequestBuilder );
        when( this.mockHttpRequestBuilder.uri( this.testApiEndpoint )).thenReturn( this.mockHttpRequestBuilder );
        when( this.mockHttpRequestBuilder.header( "Content-Type", "application/json" ))
                .thenReturn( this.mockHttpRequestBuilder );
        when( this.mockHttpRequestBuilder.header( "Authorization", "Bearer " + testApiKey ))
                .thenReturn( this.mockHttpRequestBuilder );
        when( this.mockHttpRequestBuilder.POST( this.requestBodyCaptor.capture() ))
                .thenReturn( this.mockHttpRequestBuilder );
        when( this.mockHttpClient.< String >send( any(), any() )).thenReturn( this.mockHttpResponse );
        if ( response != null ) when( this.mockHttpResponse.body() ).thenReturn( response );

        if ( statusCodes.length > 0 )
        {
            Integer firstStatusCode = statusCodes[ 0 ];
            Integer[] remainingStatusCodes = Arrays
                    .stream( statusCodes, 1, statusCodes.length )
                    .boxed()
                    .toArray( Integer[]::new );

            when( this.mockHttpResponse.statusCode() ).thenReturn( firstStatusCode, remainingStatusCodes );
        }
    }
}
