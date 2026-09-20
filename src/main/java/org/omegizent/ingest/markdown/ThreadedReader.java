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

package org.omegizent.ingest.markdown;

import java.io.BufferedReader;
import java.util.LinkedList;
import java.util.List;

class ThreadedReader implements AutoCloseable
{
    // synchronization is unnecessary; this is only accessed by one thread at a time
    private final List< String > lines = new LinkedList<>();

    private Thread thread;
    private Exception exception;

    ThreadedReader() {}

    public void close()
    {
        if ( this.thread != null ) this.join();
    }

    void start( BufferedReader reader )
    {
        if ( this.thread != null ) throw new IllegalStateException( "Thread is currently running." );

        this.exception = null;
        this.lines.clear();

        this.thread = new Thread( () ->
        {
            try
            {
                String line;
                while (( line = reader.readLine() ) != null ) this.lines.add( line );
            }
            catch ( Exception e ) { this.exception = e; }
        } );

        this.thread.start();
    }

    void join()
    {
        if ( this.thread == null ) throw new IllegalStateException( "Thread is not running." );

        try
        {
            this.thread.join();
            this.thread = null;
        }
        catch ( InterruptedException e )
        {
            Thread.currentThread().interrupt();
            throw new RuntimeException( e );
        }
    }

    List< String > getLines()
    {
        if ( this.thread != null ) throw new IllegalStateException( "Thread is currently running." );
        return List.copyOf( this.lines );
    }

    Exception getException()
    {
        if ( this.thread != null ) throw new IllegalStateException( "Thread is currently running." );
        return this.exception;
    }
}
