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

package org.omegizent.ingest.text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextSplitter
{
    private final Pattern linePattern          = Pattern.compile( "(.*)(\\R|\\z)" );
    private final Pattern nonWhitespacePattern = Pattern.compile( "\\s*\\S*(\\s+|\\z)" );
    private final Pattern wordPattern          = Pattern.compile( "\\W*\\w*(\\W+|\\z)" );

    private final int maxSegmentLength;

    public TextSplitter( int maxSegmentLength )
    {
        if ( maxSegmentLength <= 0 ) throw new IllegalArgumentException( "Max segment length must be greater than 0." );

        this.maxSegmentLength = maxSegmentLength;
    }

    // split an input string into segments that fit within the maximum segment length
    public List< StringSegment > getSegments( String content )
    {
        if ( content == null ) throw new IllegalArgumentException( "Content must not be null." );
        if ( content.isEmpty() ) throw new IllegalArgumentException( "Content must not be empty." );

        StringSegment initialSegment = new StringSegment( content, 0, content.length(), 0, 0, 1, 0 );
        int endLine = this.getRawLines( initialSegment ).getLast().endLine();
        StringSegment segment = new StringSegment( content, 0, content.length(), 0, 0, endLine, 0 );

        List< Function< StringSegment, List< StringSegment >>> splitters = List.of(
                this::splitContentBlockSegments,
                this::splitContentLineSegments,
                this::splitNonWhitespaceSegments,
                this::splitWordSegments,
                this::splitHalfSegments );

        return this.getSegments( segment, splitters );
    }

    // recursively split an input segment into segments that fit within the maximum segment length
    private List< StringSegment > getSegments(
            StringSegment inputSegment, List< Function< StringSegment, List< StringSegment >>> splitters )
    {
        // if the input segment is within the maximum segment length, return a list of only the input segment
        List< StringSegment > result = List.of( inputSegment );
        if ( inputSegment.length() <= this.maxSegmentLength ) return result;

        // if the input segment contains only a Unicode surrogate pair, return a list of only the input segment
        // https://www.unicode.org/glossary/#surrogate_pair
        // https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/Character.html#unicode
        if ( inputSegment.length() == 2 )
        {
            char c = inputSegment.content().charAt( inputSegment.start() );
            if ( Character.isHighSurrogate( c )) return result;
        }

        // split the input segment using progressively smaller scales until we have more than one segment
        for ( Function< StringSegment, List< StringSegment >> splitter : splitters )
        {
            result = splitter.apply( result.getFirst() );
            if ( result.size() > 1 ) break;
        }

        // merge adjacent pairs of segments that fit within the maximum segment length
        result = this.mergeSegments( result );

        // recursively split segments
        List< StringSegment > finalResult = new ArrayList<>();
        for ( StringSegment segment : result ) finalResult.addAll( this.getSegments( segment, splitters ));

        return finalResult;
    }

    // merge adjacent pairs of input segments that fit within the maximum segment length
    private List< StringSegment > mergeSegments( List< StringSegment > segments )
    {
        while ( true )
        {
            // look for the smallest adjacent pair of segments
            int minLength = Integer.MAX_VALUE;
            int mergeIndex = 0;
            for ( int i = 0; i < segments.size() - 1; i++ )
            {
                int length = segments.get( i ).length() + segments.get( i + 1 ).length();
                if ( length < minLength )
                {
                    minLength = length;
                    mergeIndex = i;
                }
            }

            // stop if the minimum merge length is larger than the maximum segment length
            if ( minLength > this.maxSegmentLength ) break;

            // merge the smallest adjacent pair of segments
            StringSegment first  = segments.get( mergeIndex );
            StringSegment second = segments.get( mergeIndex + 1 );
            StringSegment merged = new StringSegment(
                    first.content(), first.start(), second.end(),
                    first.startLine(), first.startColumn(),
                    second.endLine(), second.endColumn() );

            // create a new list with the merged segment
            List< StringSegment > mergeResult = new ArrayList<>( segments.subList( 0, mergeIndex ));
            mergeResult.add( merged );
            mergeResult.addAll( segments.subList( mergeIndex + 2, segments.size() ));

            segments = mergeResult;
        }

        return segments;
    }

    // split an input segment into segments containing blocks of non-empty lines separated by empty lines
    // each segment will include all the empty lines between it and the next segment
    // the first segment will include all empty lines before it
    private List< StringSegment > splitContentBlockSegments( StringSegment segment )
    {
        return this.splitContentSegments( segment, true );
    }

    // split an input segment into segments containing non-empty lines
    // each segment will include all the empty lines between it and the next segment
    // the first segment will include all empty lines before it
    private List< StringSegment > splitContentLineSegments( StringSegment segment )
    {
        return this.splitContentSegments( segment, false );
    }

    // split an input segment into segments containing non-empty lines
    // each segment will include all the empty lines between it and the next segment
    // the first segment will include all empty lines before it
    // set `groupBlocks` to `true` to group non-empty lines separated by empty lines into blocks
    private List< StringSegment > splitContentSegments( StringSegment segment, boolean groupBlocks )
    {
        String content = segment.content();
        List< StringSegment > result = new ArrayList<>();
        List< StringSegment > lines = this.getRawLines( segment );
        StringSegment firstLine = null;
        StringSegment lastLine = null;
        boolean contentStarted = false;
        boolean contentEnded = false;

        for ( StringSegment line : lines )
        {
            if ( firstLine == null ) firstLine = line;
            lastLine = line;
            int lineLength = this.getLineLength( line );

            if ( lineLength > 0 )
            {
                if (( contentEnded ) || ( !groupBlocks && ( firstLine != lastLine )))
                {
                    result.add( new StringSegment(
                            content, firstLine.start(), lastLine.start(),
                            firstLine.startLine(), firstLine.startColumn(),
                            lastLine.startLine(), lastLine.startColumn() ));

                    firstLine = lastLine;
                    contentEnded = false;
                }

                contentStarted = true;
                continue;
            }

            if ( contentStarted ) contentEnded = true;
        }

        if ( firstLine != null )
        {
            result.add( new StringSegment(
                    content, firstLine.start(), lastLine.end(),
                    firstLine.startLine(), firstLine.startColumn(),
                    lastLine.endLine(), lastLine.endColumn() ));
        }

        return result;
    }

    // split an input segment into segments of non-whitespace characters separated by whitespace characters
    // each segment will include all the whitespace characters between it and the next segment
    // the first segment will include all the whitespace characters before it
    private List< StringSegment > splitNonWhitespaceSegments( StringSegment segment )
    {
        return this.splitRegexSegments( segment, this.nonWhitespacePattern );
    }

    // split an input segment into segments of words (alphanumeric characters separated by non-alphanumeric characters)
    // each segment will include all the non-alphanumeric characters between it and the next segment
    // the first segment will include all the non-alphanumeric characters before it
    private List< StringSegment > splitWordSegments( StringSegment segment )
    {
        return this.splitRegexSegments( segment, this.wordPattern );
    }

    // split an input segment into segments using a regular expression pattern
    private List< StringSegment > splitRegexSegments( StringSegment segment, Pattern pattern )
    {
        String content = segment.content();
        List< StringSegment > result = new ArrayList<>();

        Matcher matcher = pattern.matcher( content ).region( segment.start(), segment.end() );
        while ( matcher.find() )
        {
            if ( matcher.start() == matcher.end() ) break;

            int startDelta = matcher.start() - segment.start();
            int matcherDelta = matcher.end() - matcher.start();

            int startColumn = segment.startColumn() + startDelta;
            int endColumn = startColumn + matcherDelta;

            result.add( new StringSegment(
                    content, matcher.start(), matcher.end(),
                    segment.startLine(), startColumn,
                    segment.startLine(), endColumn ));
        }

        StringSegment last = result.removeLast();
        result.add( new StringSegment(
                content, last.start(), last.end(),
                last.startLine(), last.startColumn(),
                segment.endLine(), segment.endColumn() ));

        return result;
    }

    // split an input segment in half
    private List< StringSegment > splitHalfSegments( StringSegment segment )
    {
        String content = segment.content();

        int split = segment.length() / 2;

        // avoid splitting a Unicode surrogate pair
        // if the split point falls on a low surrogate, move the split point past the surrogate pair
        // https://www.unicode.org/glossary/#surrogate_pair
        // https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/Character.html#unicode
        if ( Character.isLowSurrogate( content.charAt( segment.start() + split ))) split++;

        StringSegment first = new StringSegment(
                content, segment.start(), segment.start() + split,
                segment.startLine(), segment.startColumn(),
                segment.startLine(), segment.startColumn() + split );

        StringSegment second = new StringSegment(
                content, segment.start() + split, segment.end(),
                segment.startLine(), segment.startColumn() + split,
                segment.endLine(), segment.endColumn() );

        return List.of( first, second );
    }

    // get lines from an input segment
    // each returned segment is one line, including the linebreak sequence at the end
    // the last segment will end with a linebreak sequence only if the input segment ends with a linebreak sequence
    private List< StringSegment > getRawLines( StringSegment segment )
    {
        String content = segment.content();
        List< StringSegment > result = new ArrayList<>();

        int index = segment.startLine();

        Matcher matcher = this.linePattern.matcher( content ).region( segment.start(), segment.end() );
        while ( matcher.find() )
        {
            if ( matcher.start() == matcher.end() ) break;
            result.add( new StringSegment( content, matcher.start(), matcher.end(), index, 0, index + 1, 0 ));
            index++;
        }

        // if we only find one line, return the input segment as is
        if ( result.size() == 1 ) return List.of( segment );

        return result;
    }

    // get the length of a segment representing a line, excluding the linebreak sequence
    private int getLineLength( StringSegment segment )
    {
        Matcher matcher = this.linePattern.matcher( segment.content() ).region( segment.start(), segment.end() );
        if  ( matcher.find() ) return matcher.end( 1 ) - matcher.start();
        return 0;
    }
}
