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

import org.omegizent.ConsoleLogger;
import org.omegizent.generated.jooq.tables.records.EmbeddingRecord;

import org.jooq.DSLContext;

import static org.omegizent.generated.jooq.Tables.EMBEDDING;

public class EmbeddingCacheService
{
    private final boolean       logSummary;
    private final DSLContext    dslContext;
    private final ConsoleLogger consoleLogger;

    public EmbeddingCacheService( DSLContext dslContext )
    {
        boolean logSummary = false;

        this( logSummary, dslContext, new ConsoleLogger() );
    }

    EmbeddingCacheService( boolean logSummary, DSLContext dslContext, ConsoleLogger consoleLogger )
    {
        if ( dslContext == null ) throw new IllegalArgumentException( "DSL context must not be null." );

        this.logSummary    = logSummary;
        this.dslContext    = dslContext;
        this.consoleLogger = consoleLogger;
    }

    EmbeddingVector getEmbedding( String input )
    {
        if ( input == null ) throw new IllegalArgumentException( "Input must not be null." );
        if ( input.isEmpty() ) throw new IllegalArgumentException( "Input must not be empty." );

        EmbeddingRecord embeddingRecord = this.dslContext.fetchOne(
                EMBEDDING,
                EMBEDDING.EMBEDDING_MODEL_ID.eq( EmbeddingModel.TEXT_EMBEDDING_3_SMALL.getId() ),
                EMBEDDING.INPUT.eq( input ));
        if ( embeddingRecord == null ) return null;
        return new EmbeddingVector( embeddingRecord.getId(), new ImmutableDoubleArray( embeddingRecord.getVector() ));
    }

    long cacheEmbedding( String input, ImmutableDoubleArray vector )
    {
        if ( input == null ) throw new IllegalArgumentException( "Input must not be null." );
        if ( input.isEmpty() ) throw new IllegalArgumentException( "Input must not be empty." );

        if ( vector == null ) throw new IllegalArgumentException( "Vector must not be null." );
        if ( vector.length() == 0 ) throw new IllegalArgumentException( "Vector must not be empty." );

        int rowCount = this.dslContext
                .insertInto( EMBEDDING, EMBEDDING.EMBEDDING_MODEL_ID, EMBEDDING.INPUT, EMBEDDING.VECTOR )
                .values( EmbeddingModel.TEXT_EMBEDDING_3_SMALL.getId(), input, vector.toString() )
                .onDuplicateKeyIgnore()
                .execute();

        if ( rowCount == 0 ) throw new IllegalArgumentException( "Input must not be a duplicate." );

        long id = this.dslContext.lastID().longValueExact();
        if ( this.logSummary ) this.consoleLogger.println( String.format( "Cache New Embedding, ID: %,d", id ));
        return id;
    }

    public String getInput( long id )
    {
        String input = this.dslContext.fetchValue( EMBEDDING.INPUT, EMBEDDING.ID.eq( id ));
        if ( input != null ) return input;
        throw new RuntimeException( String.format( "Unable to find embedding with id: %,d", id ));
    }
}
