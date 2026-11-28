/*

Copyright 2026 Jeffrey J. Weston <jjweston@gmail.com>

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

import org.omegizent.generated.jooq.tables.records.EmbeddingModelRecord;

import org.jooq.DSLContext;
import org.jooq.Query;
import org.jooq.exception.IntegrityConstraintViolationException;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sqlite.SQLiteErrorCode;
import org.sqlite.SQLiteException;

import java.nio.file.Path;
import java.sql.Connection;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.omegizent.generated.jooq.Tables.EMBEDDING;
import static org.omegizent.generated.jooq.Tables.EMBEDDING_MODEL;

public class EmbeddingModelIT
{
    @Test
    void testCollection( @TempDir Path tempDir ) throws Exception
    {
        List< EmbeddingModelRecord > expectedEmbeddingModels = Arrays
                .stream( EmbeddingModel.values() )
                .map( embeddingModel -> new EmbeddingModelRecord( embeddingModel.getId(), embeddingModel.getName() ))
                .toList();

        SQLiteConnectionFactory sqLiteConnectionFactory = new SQLiteConnectionFactory( tempDir );

        try ( Connection connection = sqLiteConnectionFactory.create() )
        {
            DSLContext dslContext = DSL.using( connection );

            List< EmbeddingModelRecord > actualEmbeddingModels = dslContext.fetch( EMBEDDING_MODEL );

            assertThat( actualEmbeddingModels )
                    .as( "Embedding Models" )
                    .containsExactlyInAnyOrderElementsOf( expectedEmbeddingModels );
        }
    }

    @Test
    void testForeignKeyEnforced( @TempDir Path tempDir ) throws Exception
    {
        SQLiteConnectionFactory sqLiteConnectionFactory = new SQLiteConnectionFactory( tempDir );

        try ( Connection connection = sqLiteConnectionFactory.create() )
        {
            DSLContext dslContext = DSL.using( connection );

            Query query = dslContext
                    .insertInto( EMBEDDING, EMBEDDING.EMBEDDING_MODEL_ID, EMBEDDING.INPUT, EMBEDDING.VECTOR )
                    .values( -1L, "Test Input", "Test Vector" );

            assertThatThrownBy( query::execute )
                    .isInstanceOf( IntegrityConstraintViolationException.class )
                    .rootCause()
                    .isInstanceOfSatisfying( SQLiteException.class, e ->
                            assertThat( e.getResultCode() )
                                    .isEqualTo( SQLiteErrorCode.SQLITE_CONSTRAINT_FOREIGNKEY ));
        }
    }
}
