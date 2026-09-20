/*

Copyright 2025 Jeffrey J. Weston <jjweston@gmail.com>

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

import org.omegizent.openai.EmbeddingApiService;

public class EmbeddingService
{
    private final EmbeddingCacheService embeddingCacheService;
    private final EmbeddingApiService   embeddingApiService;

    public EmbeddingService( EmbeddingCacheService embeddingCacheService, EmbeddingApiService embeddingApiService )
    {
        if ( embeddingCacheService == null )
            throw new IllegalArgumentException( "Embedding cache service must not be null." );
        if ( embeddingApiService == null )
            throw new IllegalArgumentException( "Embedding API service must not be null." );

        this.embeddingCacheService = embeddingCacheService;
        this.embeddingApiService   = embeddingApiService;
    }

    public Embedding getEmbedding( String input )
    {
        Embedding embedding = this.embeddingCacheService.getEmbedding( input );
        if ( embedding != null ) return embedding;

        ImmutableDoubleArray vector = this.embeddingApiService.getEmbeddingVector( input );
        long id = this.embeddingCacheService.cacheEmbedding( input, vector );
        return new Embedding( id, vector );
    }
}
