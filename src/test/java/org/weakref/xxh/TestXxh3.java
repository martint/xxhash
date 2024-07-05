package org.weakref.xxh;/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.assertj.core.presentation.HexadecimalRepresentation;
import org.junit.jupiter.api.Test;

import java.lang.foreign.MemorySegment;

import static org.assertj.core.api.Assertions.assertThat;

public class TestXxh3
{
    @Test
    public void test()
    {
        byte[] input = new byte[10000];
        for (int i = 0; i < input.length; i++) {
            input[i] = (byte) (i * 0x9E3779B1L);
        }

        for (int length = 0; length < input.length; length++) {
            verifyEqual(input, 0, length);
        }

        for (int length = 0; length < input.length; length++) {
            verifyEqual(input, 57, Math.max(0, length - 57));
        }
    }

    private void verifyEqual(byte[] input, int offset, int length)
    {
        assertThat(Xxh3.hash64(input, offset, length))
                .withRepresentation(new HexadecimalRepresentation())
                .describedAs("Offset: %d, Length: %d", offset, length)
                .isEqualTo(XxhNative.xxh3(MemorySegment.ofArray(input).asSlice(offset, length)));
    }
}
