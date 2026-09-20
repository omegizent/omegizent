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

import org.omegizent.embedding.EmbeddingService;
import org.omegizent.qdrant.QdrantService;

import java.nio.file.Path;
import java.util.List;

public class MarkdownLoader
{
    private final MarkdownSplitter markdownSplitter;
    private final EmbeddingService embeddingService;
    private final QdrantService    qdrantService;

    public MarkdownLoader( EmbeddingService embeddingService, QdrantService qdrantService )
    {
        this( new MarkdownSplitter(), embeddingService, qdrantService );
    }

    MarkdownLoader( MarkdownSplitter markdownSplitter, EmbeddingService embeddingService, QdrantService qdrantService )
    {
        if ( embeddingService == null ) throw new IllegalArgumentException( "Embedding service must not be null." );
        if ( qdrantService == null ) throw new IllegalArgumentException( "Qdrant service must not be null." );

        this.markdownSplitter = markdownSplitter;
        this.embeddingService = embeddingService;
        this.qdrantService    = qdrantService;
    }

    public void load( Path path )
    {
        if ( path == null ) throw new IllegalArgumentException( "Path must not be null." );

        List< String > chunks = this.markdownSplitter.split( path );
        for ( String chunk : chunks ) this.qdrantService.upsert( this.embeddingService.getEmbedding( chunk ));
    }
}
