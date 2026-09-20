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

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonPointer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.dataformat.yaml.YAMLMapper;
import tools.jackson.dataformat.yaml.YAMLWriteFeature;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Random;
import java.util.regex.Pattern;

public class OpenAiApiCaller
{
    private final int                maxAttempts;
    private final long               minimumRetryDelay;
    private final String             apiKeyVarName;
    private final Environment        environment;
    private final HttpRequestBuilder httpRequestBuilder;
    private final HttpClient         httpClient;
    private final Random             random;
    private final OmegizentUtil      omegizentUtil;
    private final OmegizentLogger    omegizentLogger;
    private final TaskRunner         taskRunner;
    private final ObjectMapper       objectMapper;
    private final ObjectMapper       yamlObjectMapper;
    private final String             logDividerRequest;
    private final String             logDividerResponseHeaders;
    private final String             logDividerResponse;

    public OpenAiApiCaller()
    {
        this( 10,
              10_000,
              "OMEGIZENT_OPENAI_API_KEY",
              new Environment(),
              new HttpRequestBuilder(),
              HttpClient.newHttpClient(),
              new Random(),
              new OmegizentUtil(),
              new OmegizentLogger(),
              new TaskRunner( 200 ));
    }

    OpenAiApiCaller( int maxAttempts, long minimumRetryDelay, String apiKeyVarName, Environment environment,
                     HttpRequestBuilder httpRequestBuilder, HttpClient httpClient, Random random,
                     OmegizentUtil omegizentUtil, OmegizentLogger omegizentLogger, TaskRunner taskRunner )
    {
        this.maxAttempts        = maxAttempts;
        this.minimumRetryDelay  = minimumRetryDelay;
        this.apiKeyVarName      = apiKeyVarName;
        this.environment        = environment;
        this.httpRequestBuilder = httpRequestBuilder;
        this.httpClient         = httpClient;
        this.random             = random;
        this.omegizentUtil      = omegizentUtil;
        this.omegizentLogger    = omegizentLogger;
        this.taskRunner         = taskRunner;

        this.objectMapper     = new ObjectMapper();
        this.yamlObjectMapper = YAMLMapper.builder()
                .disable( YAMLWriteFeature.WRITE_DOC_START_MARKER )
                .enable( YAMLWriteFeature.LITERAL_BLOCK_STYLE )
                .enable( YAMLWriteFeature.SPLIT_LINES )
                .build();

        this.logDividerRequest         = "----- Request --------------------------------------------------------";
        this.logDividerResponseHeaders = "----- Response Headers -----------------------------------------------";
        this.logDividerResponse        = "----- Response -------------------------------------------------------";
    }

    JsonNode getResponse( String taskName, String apiEndpoint, ObjectNode requestNode, String startMessage,
                          boolean logSummary, boolean logRequest, boolean logResponseHeaders, boolean logResponse,
                          List< Pattern > embeddedJsonPatterns, Map< String, Integer > arraysToTrim )
    {
        if ( taskName == null ) throw new IllegalArgumentException( "Task name must not be null." );
        if ( apiEndpoint == null ) throw new IllegalArgumentException( "API endpoint must not be null." );
        if ( requestNode == null ) throw new IllegalArgumentException( "Request node must not be null." );

        String requestString = this.objectMapper.writeValueAsString( requestNode );

        if ( logRequest )
        {
            String debugRequestString = this.yamlObjectMapper.writer().writeValueAsString(
                    this.prepareJsonForLogging(
                            JsonPointer.compile( "/request" ),
                            requestNode, embeddedJsonPatterns, arraysToTrim )).trim();

            this.omegizentLogger.println( this.logDividerRequest );
            this.omegizentLogger.println( debugRequestString );
            this.omegizentLogger.println( this.logDividerRequest );
        }

        HttpRequest request = this.httpRequestBuilder.reset()
                .uri( apiEndpoint )
                .header( "Content-Type", "application/json" )
                .header( "Authorization", "Bearer " + this.environment.getString( this.apiKeyVarName ))
                .POST( requestString )
                .build();

        long     previousRetryDelay = 0;
        int      statusCode         = 0;
        JsonNode responseNode       = JsonNodeFactory.instance.objectNode();

        for ( int attempt = 1; attempt <= this.maxAttempts; attempt++ )
        {
            HttpResponse< String > response = this.taskRunner.get( taskName, startMessage, logSummary,
                    () -> this.httpClient.send( request, HttpResponse.BodyHandlers.ofString() ));

            statusCode = response.statusCode();
            String responseString = response.body();

            if ( logResponseHeaders )
            {
                String debugResponseHeadersString =
                        this.yamlObjectMapper.writer().writeValueAsString( response.headers().map() ).trim();

                this.omegizentLogger.println( this.logDividerResponseHeaders );
                this.omegizentLogger.println( debugResponseHeadersString );
                this.omegizentLogger.println( this.logDividerResponseHeaders );
            }

            try { responseNode = this.objectMapper.readTree( responseString ); }
            catch ( JacksonException e )
            {
                throw new RuntimeException(
                        String.format( "%s, Failed to deserialize response. Status Code: %d, Response:%n%s",
                                taskName, statusCode, responseString ), e );
            }

            if ( logResponse )
            {
                String debugResponseString = this.yamlObjectMapper.writer().writeValueAsString(
                        this.prepareJsonForLogging(
                                JsonPointer.compile( "/response" ),
                                responseNode, embeddedJsonPatterns, arraysToTrim )).trim();

                this.omegizentLogger.println( this.logDividerResponse );
                this.omegizentLogger.println( "Status Code: " + statusCode );
                this.omegizentLogger.println( "Response:" );
                this.omegizentLogger.println( debugResponseString );
                this.omegizentLogger.println( this.logDividerResponse );
            }

            if ( statusCode != 429 ) break;

            OptionalLong retryDelayHeader = response.headers().firstValueAsLong( "retry-after-ms" );
            if ( retryDelayHeader.isEmpty() ) break;

            long rawRetryDelay = retryDelayHeader.getAsLong();
            if ( rawRetryDelay <= 0 ) break;

            if ( attempt == this.maxAttempts ) break;

            long retryDelay = Math.max( rawRetryDelay, this.minimumRetryDelay );
            if ( attempt > 1 ) retryDelay = Math.max( retryDelay, previousRetryDelay );
            float jitter = attempt == 1 ? this.random.nextFloat() / 10 + 1.1f : this.random.nextFloat() + 1.5f;
            long sleepDelay = (long) ( retryDelay * jitter );
            previousRetryDelay = sleepDelay;

            if ( logSummary )
            {
                this.omegizentLogger.println( String.format(
                        "%s, Rate Limit Exceeded, Attempt: %,d, " +
                        "Raw Retry Delay: %,d ms, Retry Delay: %,d ms, Jitter: %.3f, Sleeping: %,d ms",
                        taskName, attempt, rawRetryDelay, retryDelay, jitter, sleepDelay ));
            }

            try { this.omegizentUtil.sleepThread( sleepDelay ); }
            catch ( InterruptedException e )
            {
                this.omegizentUtil.interruptThread();
                throw new RuntimeException( taskName + ", Retry Sleep Interrupted", e );
            }
        }

        if ( statusCode != 200 )
        {
            String errorMessage = responseNode.path( "error" ).path( "message" ).asString();
            String exceptionMessage = taskName + ", Error Returned, Status Code: " + statusCode;
            if ( !errorMessage.isEmpty() ) exceptionMessage += ", Error Message: " + errorMessage;
            throw new RuntimeException( exceptionMessage );
        }

        return responseNode;
    }

    private JsonNode prepareJsonForLogging(
            JsonPointer path, JsonNode node, List< Pattern > embeddedJsonPatterns, Map< String, Integer > arraysToTrim )
    {
        String pathString = path.toString();
        if ( embeddedJsonPatterns.stream().anyMatch( pattern -> pattern.matcher( pathString ).matches() ))
        {
            if ( node.isString() )
            {
                String nodeString = node.asString();
                try
                {
                    node = this.objectMapper.readTree( nodeString );
                }
                catch ( JacksonException _ )
                {
                    this.omegizentLogger.println(
                            "Failed to deserialize embedded JSON for path: " + pathString + ", JSON: " + nodeString );
                }
            }
        }

        if ( node.isObject() )
        {
            ObjectNode copy = this.objectMapper.createObjectNode();

            for ( String name : node.propertyNames() )
            {
                copy.set( name, this.prepareJsonForLogging(
                        path.appendProperty( name ), node.path( name ), embeddedJsonPatterns, arraysToTrim ));
            }

            return copy;
        }

        if ( node.isArray() )
        {
            ArrayNode copy = this.objectMapper.createArrayNode();

            int startIndex = 0;

            if ( arraysToTrim.containsKey( pathString ))
            {
                startIndex = arraysToTrim.get( pathString );
                if ( startIndex > 0 )
                {
                    copy.add( this.objectMapper.createObjectNode()
                            .put( "type", "debugging" )
                            .put( "message", "Elements Trimmed" )
                            .put( "count", startIndex ));
                }
            }

            for ( int i = startIndex; i < node.size(); i++ )
            {
                copy.add( this.prepareJsonForLogging(
                        path.appendIndex( i ), node.get( i ), embeddedJsonPatterns, arraysToTrim ));
            }

            return copy;
        }

        return node;
    }
}
