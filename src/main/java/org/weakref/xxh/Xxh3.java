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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import static org.weakref.xxh.Constants.PRIME64_1;
import static org.weakref.xxh.Constants.PRIME_MX2;
import static org.weakref.xxh.Constants.SECRET;
import static org.weakref.xxh.Constants.PRIME64_2;
import static org.weakref.xxh.Constants.PRIME64_3;
import static org.weakref.xxh.Util.mix;
import static org.weakref.xxh.Util.multiplyAndFold;
import static org.weakref.xxh.Util.readLong;
import static org.weakref.xxh.Util.readUnsignedInt;
import static org.weakref.xxh.Util.avalanche;

public class Xxh3
{
    private static final MethodHandle HASH_LONG;

    static {
        try {
//            HASH_LONG = MethodHandles.lookup().findStatic(LongHashScalar.class, "hash", MethodType.methodType(long.class, byte[].class, int.class, int.class));
//            HASH_LONG = MethodHandles.lookup().findStatic(LongHashUnrolled.class, "hash", MethodType.methodType(long.class, byte[].class, int.class, int.class));
            HASH_LONG = MethodHandles.lookup().findStatic(LongHashVector128.class, "hash", MethodType.methodType(long.class, byte[].class, int.class, int.class));
        }
        catch (NoSuchMethodException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    public static long hash64(byte[] input, int offset, int length)
    {
        if (length == 0) {
            return xxh64Avalanche(readLong(SECRET, 56) ^ readLong(SECRET, 64));
        }
        else if (length < 4) {
            return hash1to3(input, offset, length);
        }
        else if (length <= 8) {
            return hash4to8(input, offset, length);
        }
        else if (length <= 16) {
            return hash9to16(input, offset, length);
        }
        else if (length <= 128) {
            return hash17to128(input, offset, length);
        }
        else if (length <= 240) {
            return hash129to240(input, offset, length);
        }

        try {
            return (long) HASH_LONG.invokeExact(input, offset, length);
        }
        catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static long hash1to3(byte[] input, int offset, int length)
    {
        long c1 = input[offset] & 0xFF;
        long c2 = input[offset + (length >>> 1)] & 0xFF;
        long c3 = input[offset + (length - 1)] & 0xFF;
        long combined = (c1 << 16) | (c2 << 24) | c3 | ((long) length << 8);
        return xxh64Avalanche(combined ^ readUnsignedInt(SECRET, 0) ^ readUnsignedInt(SECRET, 4));
    }

    private static long hash4to8(byte[] input, int offset, int length)
    {
        long value = readUnsignedInt(input, offset + length - 4) + (readUnsignedInt(input, offset) << 32);
        return strongAvalanche(value ^ readLong(SECRET, 8) ^ readLong(SECRET, 16), length);
    }

    private static long hash9to16(byte[] input, int offset, int length)
    {
        long low = readLong(input, offset) ^ (readLong(SECRET, 24) ^ readLong(SECRET, 32));
        long high = readLong(input, offset + length - 8) ^ (readLong(SECRET, 40) ^ readLong(SECRET, 48));

        return avalanche(length + Long.reverseBytes(low) + high + multiplyAndFold(low, high));
    }

    private static long hash17to128(byte[] input, int offset, int length)
    {
        long accumulator = length * PRIME64_1;

//        int i = (length - 1) / 32;
//        do {
//            accumulator += mix(
//                    readLong(input, offset + (16 * i)),
//                    readLong(input, offset + (16 * i) + 8),
//                    readLong(SECRET, 32 * i),
//                    readLong(SECRET, 32 * i + 8));
//            accumulator += mix(
//                    readLong(input, offset + length - 16 * (i + 1)),
//                    readLong(input, offset + length - 16 * (i + 1) + 8),
//                    readLong(SECRET, 32 * i + 16),
//                    readLong(SECRET, 32 * i + 16 + 8));
//        }
//        while (i-- != 0);

        int forward = offset;
        int backward = offset + length - 8;

        long first = readLong(input, forward);
        forward += 8;
        long second = readLong(input, forward);
        forward += 8;

        long second2 = readLong(input, backward);
        backward -= 8;
        long first2 = readLong(input, backward);
        backward -= 8;

        accumulator += mix(first, second, readLong(SECRET, 0), readLong(SECRET, 8));
        accumulator += mix(first2, second2, readLong(SECRET, 16), readLong(SECRET, 24));
        if (length > 32) {
            first = readLong(input, forward);
            forward += 8;
            second = readLong(input, forward);
            forward += 8;

            second2 = readLong(input, backward);
            backward -= 8;
            first2 = readLong(input, backward);
            backward -= 8;

            accumulator += mix(first, second, readLong(SECRET, 32), readLong(SECRET, 40));
            accumulator += mix(first2, second2, readLong(SECRET, 48), readLong(SECRET, 56));
            if (length > 64) {
                first = readLong(input, forward);
                forward += 8;
                second = readLong(input, forward);
                forward += 8;

                second2 = readLong(input, backward);
                backward -= 8;
                first2 = readLong(input, backward);
                backward -= 8;

                accumulator += mix(first, second, readLong(SECRET, 64), readLong(SECRET, 72));
                accumulator += mix(first2, second2, readLong(SECRET, 80), readLong(SECRET, 88));
                if (length > 96) {
                    first = readLong(input, forward);
                    forward += 8;
                    second = readLong(input, forward);
                    forward += 8;

                    second2 = readLong(input, backward);
                    backward -= 8;
                    first2 = readLong(input, backward);
                    backward -= 8;

                    accumulator += mix(first, second, readLong(SECRET, 96), readLong(SECRET, 104));
                    accumulator += mix(first2, second2, readLong(SECRET, 112), readLong(SECRET, 120));
                }
            }
        }

//        accumulator += mix(readLong(input, offset + 0), readLong(input, offset + 0 + 8), readLong(SECRET, 0), readLong(SECRET, 0 + 8));
//        accumulator += mix(readLong(input, offset + length - 16), readLong(input, offset + length - 16 + 8), readLong(SECRET, 16), readLong(SECRET, 16 + 8));
//        if (length > 32) {
//            accumulator += mix(readLong(input, offset + 16), readLong(input, offset + 16 + 8), readLong(SECRET, 32), readLong(SECRET, 32 + 8));
//            accumulator += mix(readLong(input, offset + length - 32), readLong(input, offset + length - 32 + 8), readLong(SECRET, 48), readLong(SECRET, 48 + 8));
//            if (length > 64) {
//                accumulator += mix(readLong(input, offset + 32), readLong(input, offset + 32 + 8), readLong(SECRET, 64), readLong(SECRET, 64 + 8));
//                accumulator += mix(readLong(input, offset + length - 48), readLong(input, offset + length - 48 + 8), readLong(SECRET, 80), readLong(SECRET, 80 + 8));
//                if (length > 96) {
//                    accumulator += mix(readLong(input, offset + 48), readLong(input, offset + 48 + 8), readLong(SECRET, 96), readLong(SECRET, 96 + 8));
//                    accumulator += mix(readLong(input, offset + length - 64), readLong(input, offset + length - 64 + 8), readLong(SECRET, 112), readLong(SECRET, 112 + 8));
//                }
//            }
//        }

        return avalanche(accumulator);
    }

    private static final int SECRET_MIN_SIZE = 136;
    private static final int MIDSIZE_LAST_OFFSET = 17;
    private static final int MIDSIZE_STARTOFFSET = 3;

    private static long hash129to240(byte[] data, int offset, int length)
    {
        long accumulator = length * PRIME64_1;
        for (int i = 0; i < 8; i++) {
            accumulator += mix(
                    readLong(data, offset + 16 * i),
                    readLong(data, offset + 16 * i + 8),
                    readLong(SECRET, 16 * i),
                    readLong(SECRET, 16 * i + 8));
        }
        accumulator = avalanche(accumulator);
        long end = mix(
                readLong(data, offset + length - 16),
                readLong(data, offset + length - 8),
                readLong(SECRET, SECRET_MIN_SIZE - MIDSIZE_LAST_OFFSET),
                readLong(SECRET, SECRET_MIN_SIZE - MIDSIZE_LAST_OFFSET + 8));

        int rounds = length / 16;
        for (int i = 8; i < rounds; i++) {
            end += mix(
                    readLong(data, offset + 16 * i),
                    readLong(data, offset + 16 * i + 8),
                    readLong(SECRET, 16 * (i - 8) + MIDSIZE_STARTOFFSET),
                    readLong(SECRET, 16 * (i - 8) + MIDSIZE_STARTOFFSET + 8));
        }

        return avalanche(accumulator + end);
    }

    private static long xxh64Avalanche(long value)
    {
        value = value ^ (value >>> 33);
        value *= PRIME64_2;
        value = value ^ (value >>> 29);
        value *= PRIME64_3;
        value = value ^ (value >>> 32);
        return value;
    }

    /*
     * This is a stronger avalanche, inspired by Pelle Evensen's rrmxmx
     * preferable when input has not been previously mixed
     */
    private static long strongAvalanche(long value, int length)
    {
        value ^= Long.rotateLeft(value, 49) ^ Long.rotateLeft(value, 24);
        value *= PRIME_MX2;
        value ^= (value >>> 35) + length;
        value *= PRIME_MX2;
        return value ^ (value >>> 28);
    }
}
