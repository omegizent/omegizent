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

package org.omegizent;

import org.omegizent.qdrant.QdrantClientFactory;

import io.qdrant.client.QdrantClient;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class OmegizentTestUtil
{
    private OmegizentTestUtil() {}

    public static String readInputStream( InputStream inputStream )
    {
        if ( inputStream == null ) throw new IllegalArgumentException( "Input stream must not be null." );

        try { return new String( inputStream.readAllBytes() ); }
        catch ( IOException e ) { throw new RuntimeException( "Failed to read from input stream.", e ); }
    }

    public static Path copyResource( String resourceName, Path destination ) throws IOException
    {
        Path resourcePath = destination.resolve( resourceName );

        try ( InputStream resourceStream = OmegizentTestUtil.class.getResourceAsStream( resourceName ))
        {
            assertNotNull( resourceStream, "Missing Resource: " + resourceName );
            Files.copy( resourceStream, resourcePath );
        }

        return resourcePath;
    }

    public static void deleteCollection(
            QdrantClientFactory qdrantClientFactory, String collectionName, TaskRunner taskRunner )
    {
        try( QdrantClient qdrantClient = qdrantClientFactory.create() )
        {
            taskRunner.run( "Test Util - Delete Collection", false,
                    () -> qdrantClient.deleteCollectionAsync( collectionName ).get() );
        }
    }
}
