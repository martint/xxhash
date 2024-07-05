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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;

import static org.weakref.xxh.Constants.PRIME_MX1;

class Util
{
    public static final VarHandle LONG_HANDLE = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);
    public static final VarHandle INT_HANDLE = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);

    public static long avalanche(long value)
    {
        value = value ^ (value >>> 37);
        value *= PRIME_MX1;
        value = value ^ (value >>> 32);
        return value;
    }

    public static long mix(long a, long b, long secretA, long secretB)
    {
        return multiplyAndFold(a ^ secretA, b ^ secretB);
    }

    public static long multiplyAndFold(long low, long high)
    {
        long resultLow = low * high;
        long resultHigh = Math.unsignedMultiplyHigh(low, high);
        return resultLow ^ resultHigh;
    }

    public static long readLong(byte[] data, int offset)
    {
        return (long) LONG_HANDLE.get(data, offset);
    }

    public static long readUnsignedInt(byte[] data, int offset)
    {
        return (int) INT_HANDLE.get(data, offset) & 0xFFFFFFFFL;
    }

    public static long multiplyHighLow(long value)
    {
        return (value & 0xFFFF_FFFFL) * (value >>> 32);
    }
}
