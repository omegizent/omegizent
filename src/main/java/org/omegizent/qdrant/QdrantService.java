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

import org.omegizent.TaskRunner;
import org.omegizent.embedding.Embedding;
import org.omegizent.embedding.ImmutableDoubleArray;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.VectorsFactory;
import io.qdrant.client.grpc.Collections;
import io.qdrant.client.grpc.Points;

import java.util.List;

import static io.qdrant.client.PointIdFactory.id;
import static io.qdrant.client.QueryFactory.nearest;

public class QdrantService implements AutoCloseable
{
    private final String       collectionName;
    private final int          collectionSize;
    private final boolean      logSummary;
    private final TaskRunner   taskRunner;
    private final QdrantClient qdrantClient;

    public QdrantService()
    {
        String              collectionName      = "omegizent_chunks";
        int                 collectionSize      = 1_536;
        boolean             logSummary          = false;
        TaskRunner          taskRunner          = new TaskRunner( 200 );
        QdrantClientFactory qdrantClientFactory = new QdrantClientFactory();

        this( collectionName, collectionSize, logSummary, taskRunner, qdrantClientFactory );
    }

    public QdrantService( String collectionName, int collectionSize, boolean logSummary,
                          TaskRunner taskRunner, QdrantClientFactory qdrantClientFactory )
    {
        this.collectionName = collectionName;
        this.collectionSize = collectionSize;
        this.logSummary     = logSummary;
        this.taskRunner     = taskRunner;
        this.qdrantClient   = qdrantClientFactory.create();

        try { this.init(); }
        catch ( Exception initException )
        {
            try { this.close(); }
            catch ( Exception closeException ) { initException.addSuppressed( closeException ); }
            throw initException;
        }
    }

    public void close()
    {
        this.qdrantClient.close();
    }

    public void upsert( Embedding embedding )
    {
        if ( embedding == null ) throw new IllegalArgumentException( "Embedding must not be null." );
        this.validateVector( embedding.vector() );

        String taskName = "Qdrant - Upsert Point";
        String startMessage = String.format( "Point ID: %,d", embedding.id() );

        Points.PointStruct point = Points.PointStruct.newBuilder()
                .setId( id( embedding.id() ))
                .setVectors( VectorsFactory.vectors( embedding.vector().toFloatArray() ))
                .build();

        this.taskRunner.run(
                taskName, startMessage, this.logSummary,
                () -> this.qdrantClient.upsertAsync( this.collectionName, List.of( point )).get() );
    }

    public List< SearchResult > search( ImmutableDoubleArray vector )
    {
        this.validateVector( vector );

        String taskName = "Qdrant - Search";

        Points.QueryPoints query = Points.QueryPoints.newBuilder()
                .setCollectionName( this.collectionName )
                .setQuery( nearest( vector.toFloatArray() ))
                .build();

        List< Points.ScoredPoint > points = this.taskRunner.get(
                taskName, this.logSummary,
                () -> this.qdrantClient.queryAsync( query ).get() );
        if ( points == null ) throw new RuntimeException( taskName + ", Null Returned" );

        return points.stream().map( point -> new SearchResult( point.getId().getNum(), point.getScore() )).toList();
    }

    private void init()
    {
        if ( this.collectionExists() ) this.deleteCollection();
        this.createCollection();
    }

    private boolean collectionExists()
    {
        String taskName = "Qdrant - Check Collection Exists";

        Boolean exists = this.taskRunner.get(
                taskName, this.logSummary,
                () -> this.qdrantClient.collectionExistsAsync( this.collectionName ).get() );
        if ( exists == null ) throw new RuntimeException( taskName + ", Null Returned" );
        return exists;
    }

    private void deleteCollection()
    {
        String taskName = "Qdrant - Delete Collection";
        this.taskRunner.run(
                taskName, this.logSummary,
                () -> this.qdrantClient.deleteCollectionAsync( this.collectionName ).get() );
    }

    private void createCollection()
    {
        String taskName = "Qdrant - Create Collection";

        Collections.VectorParams vectorParams = Collections.VectorParams.newBuilder()
                .setDistance( Collections.Distance.Cosine )
                .setSize( this.collectionSize )
                .build();

        this.taskRunner.run(
                taskName, this.logSummary,
                () -> this.qdrantClient.createCollectionAsync( this.collectionName, vectorParams ).get() );
    }

    private void validateVector( ImmutableDoubleArray vector )
    {
        if ( vector == null ) throw new IllegalArgumentException( "Vector must not be null." );

        if ( vector.length() != this.collectionSize )
        {
            throw new IllegalArgumentException( String.format(
                    "Vector length must be %,d. Actual Length: %,d", this.collectionSize, vector.length() ));
        }
    }
}
