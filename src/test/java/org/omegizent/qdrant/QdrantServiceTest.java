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

package org.omegizent.qdrant;

import org.omegizent.OmegizentLogger;
import org.omegizent.OmegizentUtil;
import org.omegizent.TaskRunner;
import org.omegizent.embedding.Embedding;
import org.omegizent.embedding.ImmutableDoubleArray;

import com.google.common.util.concurrent.ListenableFuture;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.VectorsFactory;
import io.qdrant.client.grpc.Collections;
import io.qdrant.client.grpc.Common;
import io.qdrant.client.grpc.Points;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedList;
import java.util.List;

import static io.qdrant.client.PointIdFactory.id;
import static io.qdrant.client.QueryFactory.nearest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith( MockitoExtension.class )
class QdrantServiceTest
{
    private final String testCollectionName = "test";
    private final int    testCollectionSize = 5;

    @Mock private OmegizentUtil                                               mockOmegizentUtil;
    @Mock private OmegizentLogger                                             mockOmegizentLogger;
    @Mock private QdrantClientFactory                                         mockQdrantClientFactory;
    @Mock private QdrantClient                                                mockQdrantClient;
    @Mock private ListenableFuture< Boolean >                                 mockBooleanListenableFuture;
    @Mock private ListenableFuture< Collections.CollectionOperationResponse > mockCollectionResponseListenableFuture;
    @Mock private ListenableFuture< Points.UpdateResult >                     mockUpdateResultListenableFuture;
    @Mock private ListenableFuture< List< Points.ScoredPoint >>               mockScoredPointsListenableFuture;

    @Test
    void init_collectionExists_returnsNull_closeException()
    {
        Exception closeException = new Exception( "Close Exception" );

        when( this.mockQdrantClientFactory.create() ).thenReturn( this.mockQdrantClient );
        when( this.mockQdrantClient.collectionExistsAsync( this.testCollectionName ))
                .thenReturn( this.mockBooleanListenableFuture );
        doThrow( closeException ).when( this.mockQdrantClient ).close();

        RuntimeException exception = assertThrowsExactly(
                RuntimeException.class, () ->
                {
                    try ( QdrantService qdrantService = this.createQdrantService( this.testCollectionSize, false ))
                    {
                        fail( "Should not get here. qdrantService: " + qdrantService );
                    }
                } );

        assertEquals( "Qdrant - Check Collection Exists, Null Returned", exception.getMessage() );
        assertEquals( 1, exception.getSuppressed().length, "Suppressed Exception Array Length" );
        assertEquals( closeException, exception.getSuppressed()[ 0 ] );
    }

    @Test
    void upsert_nullEmbedding() throws Exception
    {
        this.mockInit( this.testCollectionSize );

        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class, () ->
                {
                    try ( QdrantService qdrantService = this.createQdrantService( this.testCollectionSize, false ))
                    {
                        qdrantService.upsert( null );
                    }
                } );

        assertEquals( "Embedding must not be null.", exception.getMessage() );
    }

    @Test
    void upsert_incorrectVectorLength() throws Exception
    {
        int                  expectedCollectionSize = 1_234;
        int                  actualCollectionSize   = 1_024;
        long                 testId                 = 42;
        ImmutableDoubleArray testVector             = new ImmutableDoubleArray( new double[ actualCollectionSize ] );
        Embedding            testEmbedding          = new Embedding( testId, testVector );

        this.mockInit( expectedCollectionSize );

        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class, () ->
                {
                    try ( QdrantService qdrantService = this.createQdrantService( expectedCollectionSize, false ))
                    {
                        qdrantService.upsert( testEmbedding );
                    }
                } );

        assertEquals( "Vector length must be 1,234. Actual Length: 1,024", exception.getMessage() );
    }

    @Test
    void upsert_success() throws Exception
    {
        long                 testId        = 1024;
        ImmutableDoubleArray testVector    = new ImmutableDoubleArray( new double[]{ 0.1f, 0.2f, 0.3f, 0.4f, 0.5f } );
        Embedding            testEmbedding = new Embedding( testId, testVector );

        Points.PointStruct testPoint = Points.PointStruct.newBuilder()
                .setId( id( testId ))
                .setVectors( VectorsFactory.vectors( testVector.toFloatArray() ))
                .build();

        this.mockInit( this.testCollectionSize );

        when( this.mockQdrantClient.upsertAsync( this.testCollectionName, List.of( testPoint )))
                .thenReturn( this.mockUpdateResultListenableFuture );

        try ( QdrantService qdrantService = this.createQdrantService( this.testCollectionSize, true ))
        {
            clearInvocations( this.mockOmegizentLogger );
            qdrantService.upsert( testEmbedding );
        }

        InOrder inOrder = inOrder( this.mockOmegizentLogger );

        inOrder.verify( this.mockOmegizentLogger ).println( "Qdrant - Upsert Point, Starting, Point ID: 1,024" );
        inOrder.verify( this.mockOmegizentLogger ).println( "Qdrant - Upsert Point, Complete, Duration: 0 ms" );

        inOrder.verifyNoMoreInteractions();
        verifyNoMoreInteractions( this.mockOmegizentLogger );
    }

    @Test
    void search_nullVector() throws Exception
    {
        this.mockInit( this.testCollectionSize );

        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class, () ->
                {
                    try ( QdrantService qdrantService = this.createQdrantService( this.testCollectionSize, false ))
                    {
                        qdrantService.search( null );
                    }
                } );

        assertEquals( "Vector must not be null.", exception.getMessage() );
    }

    @Test
    void search_incorrectVectorLength() throws Exception
    {
        int                  expectedCollectionSize = 5_678;
        int                  actualCollectionSize   = 2_048;
        ImmutableDoubleArray testVector             = new ImmutableDoubleArray( new double[ actualCollectionSize ] );

        this.mockInit( expectedCollectionSize );

        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class, () ->
                {
                    try ( QdrantService qdrantService = this.createQdrantService( expectedCollectionSize, false ))
                    {
                        qdrantService.search( testVector );
                    }
                } );

        assertEquals( "Vector length must be 5,678. Actual Length: 2,048", exception.getMessage() );
    }

    @Test
    void search_success() throws Exception
    {
        ImmutableDoubleArray testVector = new ImmutableDoubleArray( new double[]{ 0.1f, 0.2f, 0.3f, 0.4f, 0.5f } );

        long testId1 = 42;
        long testId2 = 13;
        long testId3 = 74;

        float testScore1 = 0.6f;
        float testScore2 = 0.5f;
        float testScore3 = 0.4f;

        List< SearchResult > expectedResults = new LinkedList<>();
        expectedResults.add( new SearchResult( testId1, testScore1 ));
        expectedResults.add( new SearchResult( testId2, testScore2 ));
        expectedResults.add( new SearchResult( testId3, testScore3 ));

        Points.QueryPoints testQuery = Points.QueryPoints.newBuilder()
                .setCollectionName( this.testCollectionName )
                .setQuery( nearest( testVector.toFloatArray() ))
                .build();

        List< Points.ScoredPoint > testScoredPoints = new LinkedList<>();
        testScoredPoints.add( this.mockScoredPoint( testId1, testScore1 ));
        testScoredPoints.add( this.mockScoredPoint( testId2, testScore2 ));
        testScoredPoints.add( this.mockScoredPoint( testId3, testScore3 ));

        this.mockInit( this.testCollectionSize );

        when( this.mockQdrantClient.queryAsync( testQuery )).thenReturn( this.mockScoredPointsListenableFuture );
        when( this.mockScoredPointsListenableFuture.get() ).thenReturn( testScoredPoints );

        try ( QdrantService qdrantService = this.createQdrantService( this.testCollectionSize, false ))
        {
            List< SearchResult > actualResults = qdrantService.search( testVector );
            assertThat( actualResults ).as( "Search Results" ).containsExactlyElementsOf( expectedResults );
        }
    }

    private void mockInit( int testCollectionSize ) throws Exception
    {
        Collections.VectorParams vectorParams = Collections.VectorParams.newBuilder()
                .setDistance( Collections.Distance.Cosine )
                .setSize( testCollectionSize )
                .build();

        when( this.mockQdrantClientFactory.create() ).thenReturn( this.mockQdrantClient );
        when( this.mockQdrantClient.collectionExistsAsync( this.testCollectionName ))
                .thenReturn( this.mockBooleanListenableFuture );
        when( this.mockBooleanListenableFuture.get() ).thenReturn( true );
        when( this.mockQdrantClient.deleteCollectionAsync( this.testCollectionName ))
                .thenReturn( this.mockCollectionResponseListenableFuture );
        when( this.mockQdrantClient.createCollectionAsync( this.testCollectionName, vectorParams ))
                .thenReturn( this.mockCollectionResponseListenableFuture );
    }

    private QdrantService createQdrantService( int collectionSize, boolean logSummary )
    {
        TaskRunner taskRunner = new TaskRunner( 0, this.mockOmegizentUtil, this.mockOmegizentLogger );
        return new QdrantService(
                this.testCollectionName, collectionSize, logSummary, taskRunner, this.mockQdrantClientFactory );
    }

    private Points.ScoredPoint mockScoredPoint( long id, float score )
    {
        Points.ScoredPoint scoredPoint = Mockito.mock( Points.ScoredPoint.class );
        Common.PointId pointId = Mockito.mock( Common.PointId.class );
        when( scoredPoint.getId() ).thenReturn( pointId );
        when( pointId.getNum() ).thenReturn( id );
        when ( scoredPoint.getScore() ).thenReturn( score );
        return scoredPoint;
    }
}
