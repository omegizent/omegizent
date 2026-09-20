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

package org.omegizent.embedding;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.sqlite.SQLiteDataSource;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class EmbeddingCacheServiceIT
{
    @Test
    void testCache() throws Exception
    {
        String testInput = "This is a test input.";
        ImmutableDoubleArray testVector = new ImmutableDoubleArray( new double[] { -0.75, -0.5, 0.5, 0.75 } );

        String databaseUrl = "jdbc:sqlite::memory:";
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl( databaseUrl );

        try ( Connection connection = dataSource.getConnection() )
        {
            EmbeddingCacheService embeddingCacheService = new EmbeddingCacheService( connection );
            Assertions.assertNull( embeddingCacheService.getEmbedding( testInput ));
            long id = embeddingCacheService.cacheEmbedding( testInput, testVector );
            assertEquals( testInput, embeddingCacheService.getInput( id ));
            Embedding expectedEmbedding = new Embedding( id, testVector );
            Embedding cachedEmbedding = embeddingCacheService.getEmbedding( testInput );
            assertEquals( expectedEmbedding, cachedEmbedding );

            IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class,
                    () -> embeddingCacheService.cacheEmbedding( testInput, testVector ) );

            assertEquals( "Input must not be a duplicate.", exception.getMessage() );
        }
    }
}
