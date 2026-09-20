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

package org.omegizent.cli;

import org.omegizent.ingest.text.StringSegment;
import org.omegizent.ingest.text.TextSplitter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

class SplitTextFile
{
    private static final TextSplitter textSplitter = new TextSplitter( 2_000 );

    private SplitTextFile() {}

    static void main( String[] args )
    {
        System.out.println( "Split Text File" );

        for ( String arg : args )
        {
            System.out.println( arg );
        }

        if ( args.length < 1 )
        {
            System.err.println( "Error: Filename not specified." );
            System.err.println( "Usage: mvn exec:exec -P split-text -D exec.filename=[filename]" );
            System.err.println( "Replace [filename] with the name of the text file to split." );
            System.exit( 1 );
        }

        Path path = Paths.get( args[ 0 ] );
        if ( !path.toFile().exists() ) throw new IllegalArgumentException( "File not found: " + path );

        String content;
        try { content = Files.readString( path ); }
        catch ( IOException e ) { throw new RuntimeException( "Exception reading file: " + path, e ); }

        List< StringSegment > segments = SplitTextFile.textSplitter.getSegments( content );
        SplitTextFile.printSegments( segments );
    }

    private static void printSegments( List< StringSegment > segments )
    {
        String indexLabel       = "Index";
        String startLabel       = "Start";
        String endLabel         = "End";
        String startLineLabel   = "Start Line";
        String startColumnLabel = "Start Column";
        String endLineLabel     = "End Line";
        String endColumnLabel   = "End Column";
        String lengthLabel      = "Length";

        StringSegment lastSegment = segments.getLast();

        int maxIndexLength     = String.format( "%,d", segments.size() - 1     ).length();
        int maxStartLength     = String.format( "%,d", lastSegment.start()     ).length();
        int maxEndLength       = String.format( "%,d", lastSegment.end()       ).length();
        int maxStartLineLength = String.format( "%,d", lastSegment.startLine() ).length();
        int maxEndLineLength   = String.format( "%,d", lastSegment.endLine()   ).length();

        int maxStartColumnLength = segments
                .stream()
                .map( StringSegment::startColumn )
                .mapToInt( length -> String.format( "%,d", length ).length() )
                .max()
                .orElse( 1 );

        int maxEndColumnLength = segments
                .stream()
                .map( StringSegment::endColumn )
                .mapToInt( length -> String.format( "%,d", length ).length() )
                .max()
                .orElse( 1 );

        int maxLengthLength = segments
                .stream()
                .map( StringSegment::length )
                .mapToInt( length -> String.format( "%,d", length ).length() )
                .max()
                .orElse( 1 );

        String headerFormat = String.format(
                "%s: %%,%dd, " +
                "%s: %%,%dd, " +
                "%s: %%,%dd, " +
                "%s: %%,%dd, " +
                "%s: %%,%dd, " +
                "%s: %%,%dd, " +
                "%s: %%,%dd, " +
                "%s: %%,%dd",
                indexLabel,       maxIndexLength,
                startLabel,       maxStartLength,
                endLabel,         maxEndLength,
                startLineLabel,   maxStartLineLength,
                startColumnLabel, maxStartColumnLength,
                endLineLabel,     maxEndLineLength,
                endColumnLabel,   maxEndColumnLength,
                lengthLabel,      maxLengthLength );

        // 8 labels with " :" before their value = 16 characters
        // 7 labels with ", " after their value = 14 characters
        // total of above = 30 characters
        // add the length of all labels and values to the above
        int headerLength = 30 +
                indexLabel      .length() + maxIndexLength       +
                startLabel      .length() + maxStartLength       +
                endLabel        .length() + maxEndLength         +
                startLineLabel  .length() + maxStartLineLength   +
                startColumnLabel.length() + maxStartColumnLength +
                endLineLabel    .length() + maxEndLineLength     +
                endColumnLabel  .length() + maxEndColumnLength   +
                lengthLabel     .length() + maxLengthLength;

        String divider = "-".repeat( headerLength );

        int index = 0;
        for ( StringSegment segment : segments )
        {
            StringBuilder content = new StringBuilder();
            segment.segment().lines().forEach( s -> content.append( String.format( "%s|%n", s )));

            String header = String.format(
                    headerFormat, index,
                    segment.start(), segment.end(),
                    segment.startLine(), segment.startColumn(),
                    segment.endLine(), segment.endColumn(),
                    segment.length() );

            System.out.println( header );
            System.out.println( divider );
            System.out.print( content );
            System.out.println( divider );

            index++;
        }
    }
}
