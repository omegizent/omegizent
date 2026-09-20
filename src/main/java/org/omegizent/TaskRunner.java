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

public class TaskRunner
{
    @FunctionalInterface public interface ThrowingRunnable { void run() throws Exception; }
    @FunctionalInterface public interface ThrowingSupplier< T > { T get() throws Exception; }

    private final long            rateLimitDelay;
    private final OmegizentUtil   omegizentUtil;
    private final OmegizentLogger omegizentLogger;

    private boolean runPreviously = false;
    private long    previousStart;

    public TaskRunner( long rateLimitDelay )
    {
        this( rateLimitDelay, new OmegizentUtil(), new OmegizentLogger() );
    }

    public TaskRunner( long rateLimitDelay, OmegizentUtil omegizentUtil, OmegizentLogger omegizentLogger )
    {
        this.rateLimitDelay  = rateLimitDelay;
        this.omegizentUtil   = omegizentUtil;
        this.omegizentLogger = omegizentLogger;
    }

    public < T > T get( String taskName, boolean logTaskSummary, ThrowingSupplier< T > task )
    {
        return this.get( taskName, null, logTaskSummary, task );
    }

    public < T > T get( String taskName, String startMessage, boolean logTaskSummary, ThrowingSupplier< T > task )
    {
        if ( taskName == null ) throw new IllegalArgumentException( "Task name must not be null." );
        if ( taskName.isEmpty() ) throw new IllegalArgumentException( "Task name must not be empty." );
        if ( task == null ) throw new IllegalArgumentException( "Task must not be null." );

        if ( this.runPreviously )
        {
            long initTime = this.omegizentUtil.nanoTime();
            long previousDeltaMs = ( initTime - this.previousStart ) / 1_000_000;
            long delayMs = this.rateLimitDelay - previousDeltaMs;

            if ( delayMs > 0 )
            {
                if ( logTaskSummary )
                {
                    this.omegizentLogger.println(
                            String.format( "%s, Sleeping, Duration: %,d ms", taskName, delayMs ));
                }

                try { this.omegizentUtil.sleepThread( delayMs ); }
                catch ( InterruptedException e )
                {
                    this.omegizentUtil.interruptThread();
                    throw new RuntimeException( taskName + ", Sleep Interrupted", e );
                }
            }
        }
        else this.runPreviously = true;

        if ( logTaskSummary )
        {
            String message = taskName + ", Starting";
            if (( startMessage != null ) && ( !startMessage.isEmpty() )) message += ", " + startMessage;
            this.omegizentLogger.println( message );
        }

        long startTime = this.omegizentUtil.nanoTime();
        this.previousStart = startTime;

        T result;
        try { result = task.get(); }
        catch ( InterruptedException e )
        {
            this.omegizentUtil.interruptThread();
            throw new RuntimeException( taskName + ", Task Interrupted", e );
        }
        catch ( RuntimeException e ) { throw e; }
        catch ( Exception e ) { throw new RuntimeException( taskName + ", Exception Occurred", e ); }

        long stopTime = this.omegizentUtil.nanoTime();
        long deltaMs = ( stopTime - startTime ) / 1_000_000;

        if ( logTaskSummary )
        {
            this.omegizentLogger.println( String.format( "%s, Complete, Duration: %,d ms", taskName, deltaMs ));
        }

        return result;
    }

    public void run( String taskName, boolean logTaskSummary, ThrowingRunnable task )
    {
        this.run( taskName, null, logTaskSummary, task );
    }

    public void run( String taskName, String startMessage, boolean logTaskSummary, ThrowingRunnable task )
    {
        this.get( taskName, startMessage, logTaskSummary, () -> { task.run(); return null; } );
    }
}
