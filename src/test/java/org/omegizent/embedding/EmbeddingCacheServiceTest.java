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

import org.omegizent.OmegizentLogger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith( MockitoExtension.class )
class EmbeddingCacheServiceTest
{
    private final Embedding testEmbedding =
            new Embedding( 42, new ImmutableDoubleArray( new double[] { -0.75, -0.5, 0.5, 0.75 } ));

    @Mock private OmegizentLogger   mockOmegizentLogger;
    @Mock private Connection        mockConnection;
    @Mock private PreparedStatement mockPreparedStatement;
    @Mock private ResultSet         mockResultSet;

    @Test
    void testConstructor_nullConnection()
    {
        @SuppressWarnings( "DataFlowIssue" )
        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class,
                () -> new EmbeddingCacheService( false, null, this.mockOmegizentLogger ));

        assertEquals( "Connection must not be null.", exception.getMessage() );
    }

    @Test
    void testGetEmbedding_nullInput() throws Exception
    {
        EmbeddingCacheService embeddingCacheService = this.getEmbeddingCacheService( false );

        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class,
                () -> embeddingCacheService.getEmbedding( null ));

        assertEquals( "Input must not be null.", exception.getMessage() );
    }

    @Test
    void testGetEmbedding_emptyInput() throws Exception
    {
        EmbeddingCacheService embeddingCacheService = this.getEmbeddingCacheService( false );

        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class,
                () -> embeddingCacheService.getEmbedding( "" ));

        assertEquals( "Input must not be empty.", exception.getMessage() );
    }

    @Test
    void testGetEmbedding_cacheHit() throws Exception
    {
        EmbeddingCacheService embeddingCacheService = this.getEmbeddingCacheService( false );

        when( this.mockPreparedStatement.executeQuery() ).thenReturn( this.mockResultSet );
        when( this.mockResultSet.next() ).thenReturn( true );
        when ( this.mockResultSet.getLong( "Id" )).thenReturn( this.testEmbedding.id() );
        when ( this.mockResultSet.getString( "Vector" )).thenReturn( this.testEmbedding.vector().toString() );

        Embedding actualEmbedding = embeddingCacheService.getEmbedding( "Test" );
        assertEquals( this.testEmbedding, actualEmbedding );
    }

    @Test
    void testGetEmbedding_cacheMiss() throws Exception
    {
        EmbeddingCacheService embeddingCacheService = this.getEmbeddingCacheService( false );

        when( this.mockPreparedStatement.executeQuery() ).thenReturn( this.mockResultSet );
        when( this.mockResultSet.next() ).thenReturn( false );

        Embedding actualEmbedding = embeddingCacheService.getEmbedding( "Test" );
        assertNull( actualEmbedding );
    }

    @Test
    void testCacheEmbedding_nullInput() throws Exception
    {
        EmbeddingCacheService embeddingCacheService = this.getEmbeddingCacheService( false );

        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class,
                () -> embeddingCacheService.cacheEmbedding( null, this.testEmbedding.vector() ));

        assertEquals( "Input must not be null.", exception.getMessage() );
    }

    @Test
    void testCacheEmbedding_emptyInput() throws Exception
    {
        EmbeddingCacheService embeddingCacheService = this.getEmbeddingCacheService( false );

        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class,
                () -> embeddingCacheService.cacheEmbedding( "", this.testEmbedding.vector() ));

        assertEquals( "Input must not be empty.", exception.getMessage() );
    }

    @Test
    void testCacheEmbedding_nullVector() throws Exception
    {
        EmbeddingCacheService embeddingCacheService = this.getEmbeddingCacheService( false );

        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class,
                () -> embeddingCacheService.cacheEmbedding( "Test", null ));

        assertEquals( "Vector must not be null.", exception.getMessage() );
    }

    @Test
    void testCacheEmbedding_emptyVector() throws Exception
    {
        EmbeddingCacheService embeddingCacheService = this.getEmbeddingCacheService( false );

        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class,
                () -> embeddingCacheService.cacheEmbedding( "Test", new ImmutableDoubleArray( new double[] {} )));

        assertEquals( "Vector must not be empty.", exception.getMessage() );
    }

    @Test
    @SuppressWarnings( "MagicConstant" )
    void testCacheEmbedding_duplicate() throws Exception
    {
        EmbeddingCacheService embeddingCacheService = this.getEmbeddingCacheService( false );

        when( this.mockConnection.prepareStatement( any(), anyInt() )).thenReturn( this.mockPreparedStatement );
        when( this.mockPreparedStatement.executeUpdate() ).thenReturn( 0 );

        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class,
                () -> embeddingCacheService.cacheEmbedding( "Test", this.testEmbedding.vector() ));

        assertEquals( "Input must not be a duplicate.", exception.getMessage() );
    }

    @Test
    @SuppressWarnings( "MagicConstant" )
    void testCacheEmbedding_success() throws Exception
    {
        EmbeddingCacheService embeddingCacheService = this.getEmbeddingCacheService( true );

        when( this.mockConnection.prepareStatement( any(), anyInt() )).thenReturn( this.mockPreparedStatement );
        when( this.mockPreparedStatement.executeUpdate() ).thenReturn( 1 );
        when( this.mockPreparedStatement.getGeneratedKeys() ).thenReturn( this.mockResultSet );
        when( this.mockResultSet.next() ).thenReturn( true );
        when( this.mockResultSet.getLong( 1 )).thenReturn( 1_042L );
        assertEquals( 1_042, embeddingCacheService.cacheEmbedding( "Test", this.testEmbedding.vector() ));

        verify( this.mockOmegizentLogger ).println( "Cache New Embedding, ID: 1,042" );
        verifyNoMoreInteractions( this.mockOmegizentLogger );
    }

    @Test
    void testGetInput_notFound() throws Exception
    {
        EmbeddingCacheService embeddingCacheService = this.getEmbeddingCacheService( false );

        when( this.mockPreparedStatement.executeQuery() ).thenReturn( this.mockResultSet );
        when( this.mockResultSet.next() ).thenReturn( false );

        RuntimeException exception = assertThrowsExactly( RuntimeException.class,
                () -> embeddingCacheService.getInput( 1_234 ));

        assertEquals( "Unable to find embedding with id: 1,234", exception.getMessage() );
    }

    @Test
    void testGetInput_success() throws Exception
    {
        EmbeddingCacheService embeddingCacheService = this.getEmbeddingCacheService( false );

        String testInput = "Test Input";

        when( this.mockPreparedStatement.executeQuery() ).thenReturn( this.mockResultSet );
        when( this.mockResultSet.next() ).thenReturn( true );
        when ( this.mockResultSet.getString( "Input" )).thenReturn( testInput );

        assertEquals( testInput, embeddingCacheService.getInput( 42 ));
    }

    private EmbeddingCacheService getEmbeddingCacheService( boolean logSummary ) throws Exception
    {
        when( this.mockConnection.prepareStatement( any() )).thenReturn( this.mockPreparedStatement );
        return new EmbeddingCacheService( logSummary, this.mockConnection, this.mockOmegizentLogger );
    }
}
