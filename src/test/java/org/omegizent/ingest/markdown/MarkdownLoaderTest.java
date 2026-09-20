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

package org.omegizent.ingest.markdown;

import org.omegizent.embedding.Embedding;
import org.omegizent.embedding.EmbeddingService;
import org.omegizent.embedding.ImmutableDoubleArray;
import org.omegizent.qdrant.QdrantService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith( MockitoExtension.class )
class MarkdownLoaderTest
{
    @Mock private MarkdownSplitter mockMarkdownSplitter;
    @Mock private EmbeddingService mockEmbeddingService;
    @Mock private QdrantService    mockQdrantService;

    @Test
    void testConstructor_nullEmbeddingService()
    {
        @SuppressWarnings( "DataFlowIssue" )
        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class, () ->
                new MarkdownLoader( this.mockMarkdownSplitter, null, this.mockQdrantService ));

        assertEquals( "Embedding service must not be null.", exception.getMessage() );
    }

    @Test
    void testConstructor_nullQdrantService()
    {
        @SuppressWarnings( "DataFlowIssue" )
        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class, () ->
                new MarkdownLoader( this.mockMarkdownSplitter, this.mockEmbeddingService, null ));

        assertEquals( "Qdrant service must not be null.", exception.getMessage() );
    }

    @Test
    void testLoad_nullPath()
    {
        MarkdownLoader markdownLoader =
                new MarkdownLoader( this.mockMarkdownSplitter, this.mockEmbeddingService, this.mockQdrantService );

        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class, () ->
                markdownLoader.load( null ));

        assertEquals( "Path must not be null.", exception.getMessage() );
    }


    @Test
    void testLoad_success()
    {
        Path testPath = Paths.get( "test.md" );

        List< String > testChunks = new LinkedList<>();
        testChunks.add( "Chunk 1" );
        testChunks.add( "Chunk 2" );
        testChunks.add( "Chunk 3" );

        Embedding testEmbedding1 = new Embedding( 42, new ImmutableDoubleArray( new double[] { 0.1, -0.1 } ));
        Embedding testEmbedding2 = new Embedding( 13, new ImmutableDoubleArray( new double[] { 0.2, -0.2 } ));
        Embedding testEmbedding3 = new Embedding( 67, new ImmutableDoubleArray( new double[] { 0.3, -0.3 } ));

        MarkdownLoader markdownLoader = new MarkdownLoader(
                this.mockMarkdownSplitter, this.mockEmbeddingService, this.mockQdrantService );

        when( this.mockMarkdownSplitter.split( testPath )).thenReturn( testChunks );
        when( this.mockEmbeddingService.getEmbedding( testChunks.get( 0 ))).thenReturn( testEmbedding1 );
        when( this.mockEmbeddingService.getEmbedding( testChunks.get( 1 ))).thenReturn( testEmbedding2 );
        when( this.mockEmbeddingService.getEmbedding( testChunks.get( 2 ))).thenReturn( testEmbedding3 );

        markdownLoader.load( testPath );

        verify( this.mockQdrantService ).upsert( testEmbedding1 );
        verify( this.mockQdrantService ).upsert( testEmbedding2 );
        verify( this.mockQdrantService ).upsert( testEmbedding3 );
    }
}
