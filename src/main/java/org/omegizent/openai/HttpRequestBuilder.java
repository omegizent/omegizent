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

package org.omegizent.openai;

import java.net.URI;
import java.net.http.HttpRequest;

class HttpRequestBuilder
{
    private HttpRequest.Builder builder;

    HttpRequestBuilder()
    {
        this.builder = HttpRequest.newBuilder();
    }

    HttpRequestBuilder reset()
    {
        this.builder = HttpRequest.newBuilder();
        return this;
    }

    HttpRequestBuilder uri( String uri )
    {
        this.builder.uri( URI.create( uri ));
        return this;
    }

    HttpRequestBuilder header( String name, String value )
    {
        this.builder.header( name, value );
        return this;
    }

    HttpRequestBuilder POST( String body )
    {
        this.builder.POST( HttpRequest.BodyPublishers.ofString( body ));
        return this;
    }

    HttpRequest build()
    {
        return this.builder.build();
    }
}
