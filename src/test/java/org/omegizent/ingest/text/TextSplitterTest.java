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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

public class TextSplitterTest
{
    @Test
    void testConstructor_zeroMaxSegmentLength()
    {
        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class,
                () -> new TextSplitter( 0 ));

        assertEquals( "Max segment length must be greater than 0.", exception.getMessage() );
    }

    @Test
    void testGetSegments_nullContent()
    {
        TextSplitter textSplitter = new TextSplitter( 500 );

        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class,
                () -> textSplitter.getSegments( null ));

        assertEquals( "Content must not be null.", exception.getMessage() );
    }

    @Test
    void testGetSegments_emptyContent()
    {
        TextSplitter textSplitter = new TextSplitter( 500 );

        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class,
                () -> textSplitter.getSegments( "" ));

        assertEquals( "Content must not be empty.", exception.getMessage() );
    }

    @Test
    void testGetSegments_recursive()
    {
        TextSplitter textSplitter = new TextSplitter( 50 );

        String content =
                """
                This is a line that fits in its own segment.
                This-is-a-very-long-line-that-must-be-split. This-is-the-second-half-of-this-line.
                """;

        List< StringSegment > expectedSegments = List.of(
                new StringSegment( content,  0,  45, 0,  0, 1,  0 ),
                new StringSegment( content, 45,  90, 1,  0, 1, 45 ),
                new StringSegment( content, 90, 128, 1, 45, 2,  0 ));

        assertEquals( expectedSegments, textSplitter.getSegments( content ));
    }

    @Test
    void testGetSegments_surrogatePair_maxSegmentLength_one()
    {
        TextSplitter textSplitter = new TextSplitter( 1 );

        String content = "-\uD83D\uDE0A";

        List< StringSegment > expectedSegments = List.of(
                new StringSegment( content, 0, 1, 0, 0, 0, 1 ),
                new StringSegment( content, 1, 3, 0, 1, 1, 0 ));

        assertEquals( expectedSegments, textSplitter.getSegments( content ));
    }

    @Test
    void testSplitContentBlockSegments()
    {
        TextSplitter textSplitter = new TextSplitter( 50 );

        String content =
                """



                This
                is
                the
                first
                block.


                This
                is
                the
                second
                block.

                """;

        List< StringSegment > expectedSegments = List.of(
                new StringSegment( content,  0, 30,  0, 0, 10, 0 ),
                new StringSegment( content, 30, 57, 10, 0, 16, 0 ));

        assertEquals( expectedSegments, textSplitter.getSegments( content ));
    }

    @Test
    void testSplitContentLineSegments()
    {
        TextSplitter textSplitter = new TextSplitter( 50 );

        String content =
                """



                This is the first line.


                This is the second line.

                """;

        List< StringSegment > expectedSegments = List.of(
                new StringSegment( content,  0, 29, 0, 0, 6, 0 ),
                new StringSegment( content, 29, 55, 6, 0, 8, 0 ));

        assertEquals( expectedSegments, textSplitter.getSegments( content ));
    }

    @Test
    void testSplitNonWhitespaceSegments()
    {
        TextSplitter textSplitter = new TextSplitter( 25 );

        String content = "   ---abc-123---  ---xyz-789--- ";

        List< StringSegment > expectedSegments = List.of(
                new StringSegment( content,  0, 18, 0,  0, 0, 18 ),
                new StringSegment( content, 18, 32, 0, 18, 1,  0 ));

        assertEquals( expectedSegments, textSplitter.getSegments( content ));
    }

    @Test
    void testSplitWordSegments()
    {
        TextSplitter textSplitter = new TextSplitter( 20 );

        String content = "   ---abc123--xyz456-  ";

        List< StringSegment > expectedSegments = List.of(
                new StringSegment( content,  0, 14, 0,  0, 0, 14 ),
                new StringSegment( content, 14, 23, 0, 14, 1,  0 ));

        assertEquals( expectedSegments, textSplitter.getSegments( content ));
    }

    @Test
    void testSplitHalfSegments_even()
    {
        TextSplitter textSplitter = new TextSplitter( 20 );

        String content = "   --abc123xyz456---  ";

        List< StringSegment > expectedSegments = List.of(
                new StringSegment( content,  0, 11, 0,  0, 0, 11 ),
                new StringSegment( content, 11, 22, 0, 11, 1,  0 ));

        assertEquals( expectedSegments, textSplitter.getSegments( content ));
    }

    @Test
    void testSplitHalfSegments_odd()
    {
        TextSplitter textSplitter = new TextSplitter( 20 );

        String content = "   --abc123xyz4567---  ";

        List< StringSegment > expectedSegments = List.of(
                new StringSegment( content,  0, 11, 0,  0, 0, 11 ),
                new StringSegment( content, 11, 23, 0, 11, 1,  0 ));

        assertEquals( expectedSegments, textSplitter.getSegments( content ));
    }

    @Test
    void testSplitHalfSegments_surrogatePair()
    {
        TextSplitter textSplitter = new TextSplitter( 5 );

        String content = "---\uD83D\uDE0A---";

        List< StringSegment > expectedSegments = List.of(
                new StringSegment( content, 0, 5, 0, 0, 0, 5 ),
                new StringSegment( content, 5, 8, 0, 5, 1, 0 ));

        assertEquals( expectedSegments, textSplitter.getSegments( content ));
    }

    @Test
    void testMergeSegments()
    {
        TextSplitter textSplitter = new TextSplitter( 10 );

        String content = "12345 12 123 123456 1234";

        List< StringSegment > expectedSegments = List.of(
                new StringSegment( content,  0,  6, 0,  0, 0,  6 ),
                new StringSegment( content,  6, 13, 0,  6, 0, 13 ),
                new StringSegment( content, 13, 20, 0, 13, 0, 20 ),
                new StringSegment( content, 20, 24, 0, 20, 1,  0 ));

        assertEquals( expectedSegments, textSplitter.getSegments( content ));
    }
}
