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
import org.jooq.InsertReturningStep;
import org.jooq.InsertValuesStep2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.omegizent.generated.jooq.Tables.EMBEDDING;

@ExtendWith( MockitoExtension.class )
class EmbeddingCacheServiceTest
{
    private final EmbeddingVector testEmbeddingVector =
            new EmbeddingVector( 42, new ImmutableDoubleArray( new double[] { -0.75, -0.5, 0.5, 0.75 } ));

    @Mock private DSLContext                                           mockDSLContext;
    @Mock private ConsoleLogger                                        mockConsoleLogger;
    @Mock private InsertValuesStep2< EmbeddingRecord, String, String > mockInsertValuesStep2;
    @Mock private InsertReturningStep< EmbeddingRecord >               mockReturningStep;

    @Test
    void testConstructor_nullDSLContext()
    {
        @SuppressWarnings( "DataFlowIssue" )
        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class,
                () -> new EmbeddingCacheService( false, null, this.mockConsoleLogger ));

        assertEquals( "DSL context must not be null.", exception.getMessage() );
    }

    @Test
    void testGetEmbedding_nullInput()
    {
        EmbeddingCacheService embeddingCacheService =
                new EmbeddingCacheService( false, this.mockDSLContext, this.mockConsoleLogger );

        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class,
                () -> embeddingCacheService.getEmbedding( null ));

        assertEquals( "Input must not be null.", exception.getMessage() );
    }

    @Test
    void testGetEmbedding_emptyInput()
    {
        EmbeddingCacheService embeddingCacheService =
                new EmbeddingCacheService( false, this.mockDSLContext, this.mockConsoleLogger );

        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class,
                () -> embeddingCacheService.getEmbedding( "" ));

        assertEquals( "Input must not be empty.", exception.getMessage() );
    }

    @Test
    void testGetEmbedding_cacheHit()
    {
        String testInput = "Test";

        EmbeddingRecord testEmbeddingRecord = new EmbeddingRecord(
                this.testEmbeddingVector.id(), testInput, this.testEmbeddingVector.vector().toString() );

        when( this.mockDSLContext.fetchOne( EMBEDDING, EMBEDDING.INPUT.eq( testInput )))
                .thenReturn( testEmbeddingRecord );

        EmbeddingCacheService embeddingCacheService =
                new EmbeddingCacheService( false, this.mockDSLContext, this.mockConsoleLogger );

        assertEquals( this.testEmbeddingVector, embeddingCacheService.getEmbedding( testInput ));
    }

    @Test
    void testGetEmbedding_cacheMiss()
    {
        String testInput = "Test";

        when( this.mockDSLContext.fetchOne( EMBEDDING, EMBEDDING.INPUT.eq( testInput ))).thenReturn( null );

        EmbeddingCacheService embeddingCacheService =
                new EmbeddingCacheService( false, this.mockDSLContext, this.mockConsoleLogger );

        assertNull( embeddingCacheService.getEmbedding( testInput ));
    }

    @Test
    void testCacheEmbedding_nullInput()
    {
        EmbeddingCacheService embeddingCacheService =
                new EmbeddingCacheService( false, this.mockDSLContext, this.mockConsoleLogger );

        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class,
                () -> embeddingCacheService.cacheEmbedding( null, this.testEmbeddingVector.vector() ));

        assertEquals( "Input must not be null.", exception.getMessage() );
    }

    @Test
    void testCacheEmbedding_emptyInput()
    {
        EmbeddingCacheService embeddingCacheService =
                new EmbeddingCacheService( false, this.mockDSLContext, this.mockConsoleLogger );

        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class,
                () -> embeddingCacheService.cacheEmbedding( "", this.testEmbeddingVector.vector() ));

        assertEquals( "Input must not be empty.", exception.getMessage() );
    }

    @Test
    void testCacheEmbedding_nullVector()
    {
        EmbeddingCacheService embeddingCacheService =
                new EmbeddingCacheService( false, this.mockDSLContext, this.mockConsoleLogger );

        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class,
                () -> embeddingCacheService.cacheEmbedding( "Test", null ));

        assertEquals( "Vector must not be null.", exception.getMessage() );
    }

    @Test
    void testCacheEmbedding_emptyVector()
    {
        EmbeddingCacheService embeddingCacheService =
                new EmbeddingCacheService( false, this.mockDSLContext, this.mockConsoleLogger );

        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class,
                () -> embeddingCacheService.cacheEmbedding( "Test", new ImmutableDoubleArray( new double[] {} )));

        assertEquals( "Vector must not be empty.", exception.getMessage() );
    }

    @Test
    void testCacheEmbedding_duplicate()
    {
        String testInput = "Test";

        this.mockCacheEmbedding( testInput );
        when( this.mockReturningStep.execute() ).thenReturn( 0 );

        EmbeddingCacheService embeddingCacheService =
                new EmbeddingCacheService( false, this.mockDSLContext, this.mockConsoleLogger );

        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class,
                () -> embeddingCacheService.cacheEmbedding( testInput, this.testEmbeddingVector.vector() ));

        assertEquals( "Input must not be a duplicate.", exception.getMessage() );
    }

    @Test
    void testCacheEmbedding_success()
    {
        String testInput = "Test";
        long testId = 1_042;

        this.mockCacheEmbedding( testInput );
        when( this.mockReturningStep.execute() ).thenReturn( 1 );
        when( this.mockDSLContext.lastID() ).thenReturn( BigInteger.valueOf( testId ));

        EmbeddingCacheService embeddingCacheService =
                new EmbeddingCacheService( true, this.mockDSLContext, this.mockConsoleLogger );

        assertEquals( testId, embeddingCacheService.cacheEmbedding( testInput, this.testEmbeddingVector.vector() ));

        verify( this.mockConsoleLogger ).println( "Cache New Embedding, ID: 1,042" );
        verifyNoMoreInteractions( this.mockConsoleLogger );
    }

    @Test
    void testGetInput_notFound()
    {
        long testId = 1_234;

        when( this.mockDSLContext.fetchValue( EMBEDDING.INPUT, EMBEDDING.ID.eq( testId ))).thenReturn( null );

        EmbeddingCacheService embeddingCacheService =
                new EmbeddingCacheService( false, this.mockDSLContext, this.mockConsoleLogger );

        RuntimeException exception = assertThrowsExactly( RuntimeException.class,
                () -> embeddingCacheService.getInput( testId ));

        assertEquals( "Unable to find embedding with id: 1,234", exception.getMessage() );
    }

    @Test
    void testGetInput_success()
    {
        String testInput = "Test Input";
        long testId = 42;

        when( this.mockDSLContext.fetchValue( EMBEDDING.INPUT, EMBEDDING.ID.eq( testId ))).thenReturn( testInput );

        EmbeddingCacheService embeddingCacheService =
                new EmbeddingCacheService( false, this.mockDSLContext, this.mockConsoleLogger );

        assertEquals( testInput, embeddingCacheService.getInput( testId ));
    }

    private void mockCacheEmbedding( String input )
    {
        when( this.mockDSLContext.insertInto( EMBEDDING, EMBEDDING.INPUT, EMBEDDING.VECTOR ))
                .thenReturn( this.mockInsertValuesStep2 );
        when( this.mockInsertValuesStep2.values( input, this.testEmbeddingVector.vector().toString() ))
                .thenReturn( this.mockInsertValuesStep2 );
        when( this.mockInsertValuesStep2.onDuplicateKeyIgnore() ).thenReturn( this.mockReturningStep );
    }
}
