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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class StringSegmentTest
{
    @Test
    void testConstructor_nullContent()
    {
        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class,
                () -> new StringSegment( null, 0, 4, 0, 0, 1, 0 ));

        assertEquals( "Content must not be null.", exception.getMessage() );
    }

    @Test
    void testConstructor_emptyContent()
    {
        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class,
                () -> new StringSegment( "", 0, 4, 0, 0, 1, 0 ));

        assertEquals( "Content must not be empty.", exception.getMessage() );
    }

    @Test
    void testConstructor_negativeStart()
    {
        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class,
                () -> new StringSegment( "test", -1, 4, 0, 0, 1, 0 ));

        assertEquals( "Start must be greater than or equal to zero.", exception.getMessage() );
    }

    @Test
    void testConstructor_endEqualToStart()
    {
        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class,
                () -> new StringSegment( "test", 0, 0, 0, 0, 1, 0 ));

        assertEquals( "End must be greater than start.", exception.getMessage() );
    }

    @Test
    void testConstructor_endGreaterThanContentLength()
    {
        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class,
                () -> new StringSegment( "test", 0, 10, 0, 0, 1, 0 ));

        assertEquals( "End must be less than or equal to content length.", exception.getMessage() );
    }

    @Test
    void testConstructor_negativeStartLine()
    {
        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class,
                () -> new StringSegment( "test", 0, 4, -1, 0, 1, 0 ));

        assertEquals( "Start line must be greater than or equal to zero.", exception.getMessage() );
    }

    @Test
    void testConstructor_negativeStartColumn()
    {
        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class,
                () -> new StringSegment( "test", 0, 4, 0, -1, 1, 0 ));

        assertEquals( "Start column must be greater than or equal to zero.", exception.getMessage() );
    }

    @Test
    void testConstructor_endLineLessThanStartLine()
    {
        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class,
                () -> new StringSegment( "test", 0, 4, 0, 0, -1, 0 ));

        assertEquals( "End line must be greater than or equal to start line.", exception.getMessage() );
    }

    @Test
    void testConstructor_negativeEndColumn()
    {
        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class,
                () -> new StringSegment( "test", 0, 4, 0, 0, 1, -1 ));

        assertEquals( "End column must be greater than or equal to zero.", exception.getMessage() );
    }

    @Test
    void testConstructor_startLineEqualEndLine_endColumnEqualStartColumn()
    {
        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class,
                () -> new StringSegment( "test", 0, 4, 0, 0, 0, 0 ));

        assertEquals( "End column must be greater than start column, when start line and end line are equal.",
                exception.getMessage() );
    }

    @Test
    void testConstructor_startLineEqualEndLine_columnLengthGreaterThanSegmentLength()
    {
        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class,
                () -> new StringSegment( "test", 0, 4, 0, 0, 0, 10 ));

        assertEquals(
                "The length between start column and end column " +
                "must be less than or equal to the length between start and end, " +
                "when start line and end line are equal.",
                exception.getMessage() );
    }

    @Test
    void testConstructor_success()
    {
        StringSegment stringSegment = new StringSegment( "This is a test string.", 10, 14, 0, 10, 0, 14 );
        assertEquals( 4, stringSegment.length() );
        assertEquals( "test", stringSegment.segment() );
    }
}
