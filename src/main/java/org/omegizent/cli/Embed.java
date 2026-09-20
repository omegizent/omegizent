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

package org.omegizent.cli;

import org.omegizent.embedding.EmbeddingCacheService;
import org.omegizent.embedding.EmbeddingService;
import org.omegizent.embedding.ImmutableDoubleArray;
import org.omegizent.embedding.SQLiteConnectionFactory;
import org.omegizent.openai.EmbeddingApiService;
import org.omegizent.openai.OpenAiApiCaller;

import java.sql.Connection;
import java.sql.SQLException;

class Embed
{
    private Embed() {}

    static void main()
    {
        int vectorStringLimit = 50;
        String input = "Omegizent is an AI assistant for developers.";
        System.out.println( "Input: " + input );

        ImmutableDoubleArray vector = Embed.getEmbeddingVector( input );
        String vectorString = vector.toString();

        if ( vectorString.length() > vectorStringLimit )
        {
            vectorString = vectorString.substring( 0, vectorStringLimit ) + "...";
        }

        System.out.println( "Vector: " + vectorString );
    }

    private static ImmutableDoubleArray getEmbeddingVector( String input )
    {
        SQLiteConnectionFactory sqLiteConnectionFactory = new SQLiteConnectionFactory();
        try ( Connection connection = sqLiteConnectionFactory.create() )
        {
            OpenAiApiCaller openAiApiCaller = new OpenAiApiCaller();
            EmbeddingCacheService embeddingCacheService = new EmbeddingCacheService( connection );
            EmbeddingApiService embeddingApiService = new EmbeddingApiService( openAiApiCaller );
            EmbeddingService embeddingService = new EmbeddingService( embeddingCacheService, embeddingApiService );
            return embeddingService.getEmbedding( input ).vector();
        }
        catch ( SQLException e ) { throw new RuntimeException( "Failed to close database connection.", e ); }
    }
}
