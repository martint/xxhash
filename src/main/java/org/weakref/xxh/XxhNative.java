/*
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
package org.weakref.xxh;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

public class XxhNative
{
    private final static MethodHandle XXH64;
    private final static MethodHandle XXH3;

    static {
        SymbolLookup lookup = SymbolLookup.libraryLookup("libxxhash.0.8.3.dylib", Arena.ofAuto());

        // XXH_PUBLIC_API XXH64_hash_t XXH64(const void* input, size_t length, unsigned long long seed);
        XXH64 = lookup.find("XXH64")
                .map(location -> Linker.nativeLinker()
                        .downcallHandle(
                                location,
                                FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG),
                                Linker.Option.critical(true)
                        ))
                .get();

        // XXH_PUBLIC_API XXH64_hash_t XXH3_64bits(XXH_NOESCAPE const void* input, size_t length)
        XXH3 = lookup.find("XXH3_64bits")
                .map(location -> Linker.nativeLinker()
                        .downcallHandle(
                                location,
                                FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG),
                                Linker.Option.critical(true)))
                .get();
    }

    public static long xxh64(MemorySegment data)
    {
        try {
            return (long) XXH64.invokeExact(data, data.byteSize(), 0L);
        }
        catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static long xxh3(MemorySegment data)
    {
        try {
            return (long) XXH3.invokeExact(data, data.byteSize());
        }
        catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args)
    {
        MemorySegment buffer = MemorySegment.ofArray(new byte[100]);

        System.out.println(XxhNative.xxh64(buffer));
    }
}