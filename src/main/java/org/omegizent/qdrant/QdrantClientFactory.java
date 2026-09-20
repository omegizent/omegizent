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

import org.omegizent.Environment;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;

public class QdrantClientFactory
{
    private final String      qdrantHostVarName;
    private final String      qdrantGrpcPortVarName;
    private final Environment environment;

    public QdrantClientFactory()
    {
        this.qdrantHostVarName     = "OMEGIZENT_QDRANT_HOST";
        this.qdrantGrpcPortVarName = "OMEGIZENT_QDRANT_GRPC_PORT";
        this.environment           = new Environment();
    }

    public QdrantClient create()
    {
        String host = this.environment.getString( this.qdrantHostVarName );
        int port = this.environment.getInt( this.qdrantGrpcPortVarName );
        return new QdrantClient( QdrantGrpcClient.newBuilder( host, port, false ).build() );
    }
}
