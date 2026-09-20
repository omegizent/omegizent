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

import io.github.cdimascio.dotenv.Dotenv;

public class Environment
{
    private final Dotenv dotenv;

    public Environment()
    {
        this( Dotenv.load() );
    }

    Environment( Dotenv dotenv )
    {
        this.dotenv = dotenv;
    }

    public String getString( String name )
    {
        if ( name == null ) throw new IllegalArgumentException( "Name must not be null." );

        String result = this.dotenv.get( name );
        if ( result == null )
        {
            throw new RuntimeException( "Missing required environment variable. Name: " + name );
        }

        return result;
    }

    public int getInt( String name )
    {
        String value = this.getString( name );

        try { return Integer.parseInt( value ); }
        catch ( NumberFormatException e )
        {
            String message = "Cannot convert environment variable to integer. Name: " + name + ", Value: " + value;
            throw new RuntimeException( message, e );
        }
    }
}
