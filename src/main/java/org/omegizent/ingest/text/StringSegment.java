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

public record StringSegment(
        String content, int start, int end, int startLine, int startColumn, int endLine, int endColumn )
{
    public StringSegment
    {
        if ( content == null )
            throw new IllegalArgumentException( "Content must not be null." );
        if ( content.isEmpty() )
            throw new IllegalArgumentException( "Content must not be empty." );
        if ( start < 0 )
            throw new IllegalArgumentException( "Start must be greater than or equal to zero." );
        if ( end <= start )
            throw new IllegalArgumentException( "End must be greater than start." );
        if ( end > content.length() )
            throw new IllegalArgumentException( "End must be less than or equal to content length." );
        if ( startLine < 0 )
            throw new IllegalArgumentException( "Start line must be greater than or equal to zero." );
        if ( startColumn < 0 )
            throw new IllegalArgumentException( "Start column must be greater than or equal to zero." );
        if ( endLine < startLine )
            throw new IllegalArgumentException( "End line must be greater than or equal to start line." );
        if ( endColumn < 0 )
            throw new IllegalArgumentException( "End column must be greater than or equal to zero." );

        if ( startLine == endLine )
        {
            if ( endColumn <= startColumn ) throw new IllegalArgumentException(
                    "End column must be greater than start column, when start line and end line are equal." );
            if (( endColumn - startColumn ) > ( end - start )) throw new IllegalArgumentException(
                    "The length between start column and end column " +
                    "must be less than or equal to the length between start and end, " +
                    "when start line and end line are equal." );
        }
    }

    public int length()
    {
        return this.end() - this.start();
    }

    public String segment()
    {
        return this.content().substring( this.start(), this.end() );
    }
}
