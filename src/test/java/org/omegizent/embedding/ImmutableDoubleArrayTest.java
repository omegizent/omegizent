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

package org.omegizent.embedding;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class ImmutableDoubleArrayTest
{
    @Test
    void constructor_nullArray()
    {
        double[] nullArray = null;

        @SuppressWarnings( { "ConstantValue", "DataFlowIssue" } )
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class, () -> new ImmutableDoubleArray( nullArray ));

        assertEquals( "Array must not be null.", exception.getMessage() );
    }

    @Test
    void constructor_nullString()
    {
        String nullString = null;

        @SuppressWarnings( { "ConstantValue", "DataFlowIssue" } )
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class, () -> new ImmutableDoubleArray( nullString ));

        assertEquals( "String must not be null.", exception.getMessage() );
    }
}
