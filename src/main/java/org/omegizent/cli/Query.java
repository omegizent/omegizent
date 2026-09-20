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
import org.omegizent.embedding.SQLiteConnectionFactory;
import org.omegizent.ingest.markdown.MarkdownLoader;
import org.omegizent.openai.EmbeddingApiService;
import org.omegizent.openai.OpenAiApiCaller;
import org.omegizent.openai.ResponseApiService;
import org.omegizent.qdrant.QdrantService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;

class Query
{
    private Query() {}

    static void main()
    {
        SQLiteConnectionFactory sqLiteConnectionFactory = new SQLiteConnectionFactory();

        try ( Connection connection = sqLiteConnectionFactory.create();
              QdrantService qdrantService = new QdrantService() )
        {
            OpenAiApiCaller openAiApiCaller = new OpenAiApiCaller();
            EmbeddingCacheService embeddingCacheService = new EmbeddingCacheService( connection );
            EmbeddingApiService embeddingApiService = new EmbeddingApiService( openAiApiCaller );
            EmbeddingService embeddingService = new EmbeddingService( embeddingCacheService, embeddingApiService );
            ResponseApiService responseApiService =
                    new ResponseApiService( embeddingCacheService, embeddingService, qdrantService, openAiApiCaller );

            MarkdownLoader markdownLoader = new MarkdownLoader( embeddingService, qdrantService );
            markdownLoader.load( Paths.get( "readme.md" ));

            Query.queryLoop( responseApiService );
        }
        catch ( SQLException e ) { throw new RuntimeException( "Failed to close database connection.", e ); }
    }

    private static void queryLoop( ResponseApiService responseApiService )
    {
        System.out.println();
        System.out.println( "Omegizent - Command-Line Query Interface" );
        System.out.println();
        System.out.println( "Enter your query. Press enter on an empty line when you are finished." );

        BufferedReader reader = new BufferedReader( new InputStreamReader( System.in ));
        while ( true )
        {
            System.out.println();
            System.out.print( "> " );

            String query;
            try { query = reader.readLine(); }
            catch ( IOException e ) { throw new RuntimeException( "Failed to read query.", e ); }
            System.out.println();

            if ( query == null ) break;
            query = query.trim();
            if ( query.isEmpty() ) break;

            String response = responseApiService.getResponse( query );
            System.out.println();
            System.out.println( "Response:" );
            System.out.println();
            System.out.println( response );
        }

        System.out.println( "Exiting" );
    }
}
