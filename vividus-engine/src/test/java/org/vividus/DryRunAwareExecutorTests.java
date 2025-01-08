/*
 * Copyright 2019-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.vividus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.jbehave.core.embedder.StoryControls;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DryRunAwareExecutorTests
{
    @ParameterizedTest
    @CsvSource({
            "true, 17",
            "false, 19"
    })
    void shouldReturnCorrectValue(boolean dryRun, int expectedValue)
    {
        var storyControls = mock(StoryControls.class);
        when(storyControls.dryRun()).thenReturn(dryRun);
        var executor = new DryRunAwareExecutor()
        {
            @Override
            public StoryControls getStoryControls()
            {
                return storyControls;
            }
        };
        assertEquals(expectedValue, executor.execute(() -> 19, 17));
    }
}
