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
import org.omegizent.TaskRunner;
import org.omegizent.embedding.EmbeddingCacheService;
import org.omegizent.embedding.EmbeddingService;
import org.omegizent.ingest.markdown.MarkdownLoader;
import org.omegizent.qdrant.QdrantClientFactory;
import org.omegizent.qdrant.QdrantService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sqlite.SQLiteDataSource;

import java.nio.file.Path;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResponseApiServiceIT
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
    void testGetResponse( @TempDir Path tempDir ) throws Exception
    {
        int collectionSize = 1_536;

        String query1 =
                """
                This is an automated integration test verifying successful integration with the OpenAI Responses API. \
                In order for this test to pass \
                you must respond with the correct answer to the question I ask you below as follows:

                Answer: [answer]

                Replace `[answer]` with your answer. \
                Do not add anything else to your response otherwise the test will fail.

                What is the name of this software project?\
                """;

        String query2 =
                """
                This is a continuation of the automated integration test \
                verifying successful integration with the OpenAI Responses API. \
                This test verifies that our API integration \
                correctly handles conversation state in multi-turn conversations.

                In order for this test to pass \
                you must respond according to the same instructions I gave you in my previous message.

                What question did I ask in my previous message?\
                """;

        String response1 = "Answer: Magic Carpet";
        String response2 = "Answer: What is the name of this software project?";

        String databaseUrl = "jdbc:sqlite::memory:";
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl( databaseUrl );

        try ( Connection connection = dataSource.getConnection();
              QdrantService qdrantService = new QdrantService(
                      this.collectionName, collectionSize, false, this.taskRunner, this.qdrantClientFactory ))
        {
            OpenAiApiCaller openAiApiCaller = new OpenAiApiCaller();
            EmbeddingCacheService embeddingCacheService = new EmbeddingCacheService( connection );
            EmbeddingApiService embeddingApiService = new EmbeddingApiService( openAiApiCaller );
            EmbeddingService embeddingService = new EmbeddingService( embeddingCacheService, embeddingApiService );
            ResponseApiService responseApiService =
                    new ResponseApiService( embeddingCacheService, embeddingService, qdrantService, openAiApiCaller );

            MarkdownLoader markdownLoader = new MarkdownLoader( embeddingService, qdrantService );
            markdownLoader.load( OmegizentTestUtil.copyResource( this.getClass(), ".md", tempDir ));

            assertEquals( response1, responseApiService.getResponse( query1 ));
            assertEquals( response2, responseApiService.getResponse( query2 ));
        }
    }
}
