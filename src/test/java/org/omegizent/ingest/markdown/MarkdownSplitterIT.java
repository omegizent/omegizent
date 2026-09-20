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

import org.omegizent.OmegizentTestUtil;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownSplitterIT
{
    @Test
    void testSplit( @TempDir Path tempDir ) throws Exception
    {
        List< String > expectedChunks = List.of(
                """
                # Test Markdown

                This is a test Markdown file.
                """,
                """
                ## License

                ```text
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
                ```
                """ );

        MarkdownSplitter markdownSplitter = new MarkdownSplitter();
        List< String > actualChunks = markdownSplitter.split(
                OmegizentTestUtil.copyResource( this.getClass().getSimpleName() + ".md", tempDir ));
        assertThat( actualChunks ).as( "Chunks" ).isEqualTo( expectedChunks );
    }
}
