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

import static org.weakref.xxh.Constants.SECRET;
import static org.weakref.xxh.Constants.SECRET_CONSUME_RATE;
import static org.weakref.xxh.Constants.SECRET_LAST_ACCUMULATOR_START;
import static org.weakref.xxh.Constants.STRIPE_LENGTH;
import static org.weakref.xxh.Constants.PRIME32_1;
import static org.weakref.xxh.Constants.PRIME32_2;
import static org.weakref.xxh.Constants.PRIME32_3;
import static org.weakref.xxh.Constants.PRIME64_1;
import static org.weakref.xxh.Constants.PRIME64_2;
import static org.weakref.xxh.Constants.PRIME64_3;
import static org.weakref.xxh.Constants.PRIME64_4;
import static org.weakref.xxh.Constants.PRIME64_5;
import static org.weakref.xxh.Constants.SECRET_MERGE_ACCUMULATORS_START;
import static org.weakref.xxh.Util.avalanche;
import static org.weakref.xxh.Util.mix;
import static org.weakref.xxh.Util.multiplyHighLow;
import static org.weakref.xxh.Util.readLong;

class LongHashScalar
{
    public static long hash(byte[] input, int offset, int length)
    {
        long[] accumulators = new long[] {PRIME32_3, PRIME64_1, PRIME64_2, PRIME64_3, PRIME64_4, PRIME32_2, PRIME64_5, PRIME32_1};

        int stripesPerBlock = (SECRET.length - STRIPE_LENGTH) / SECRET_CONSUME_RATE;
        int blockLength = STRIPE_LENGTH * stripesPerBlock;
        int blockCount = (length - 1) / blockLength;

        for (int block = 0; block < blockCount; block++) {
            for (int stripe = 0; stripe < stripesPerBlock; stripe++) {
                accumulate(accumulators, input, offset + block * blockLength + stripe * STRIPE_LENGTH, stripe * SECRET_CONSUME_RATE);
            }

            scramble(accumulators);
        }

        int stripeCount = ((length - 1) - (blockLength * blockCount)) / STRIPE_LENGTH;
        for (int stripe = 0; stripe < stripeCount; stripe++) {
            accumulate(accumulators, input, offset + blockCount * blockLength + stripe * STRIPE_LENGTH, stripe * SECRET_CONSUME_RATE);
        }

        accumulate(accumulators, input, offset + length - STRIPE_LENGTH, SECRET.length - STRIPE_LENGTH - SECRET_LAST_ACCUMULATOR_START);

        // merge
        long result = length * PRIME64_1;
        for (int i = 0; i < 4; i++) {
            result += mix(
                    accumulators[2 * i],
                    accumulators[2 * i + 1],
                    readLong(SECRET, SECRET_MERGE_ACCUMULATORS_START + 16 * i),
                    readLong(SECRET, SECRET_MERGE_ACCUMULATORS_START + 16 * i + 8));
        }
        return avalanche(result);
    }

    static void accumulate(long[] accumulators, byte[] input, int offset, int secretOffset)
    {
        long value = readLong(input, offset);
        accumulators[0] += multiplyHighLow(value ^ readLong(SECRET, secretOffset));
        accumulators[1] += value;

        value = readLong(input, offset + 8);
        accumulators[1] += multiplyHighLow(value ^ readLong(SECRET, secretOffset + 8));
        accumulators[0] += value;

        value = readLong(input, offset + 16);
        accumulators[2] += multiplyHighLow(value ^ readLong(SECRET, secretOffset + 16));
        accumulators[3] += value;

        value = readLong(input, offset + 24);
        accumulators[3] += multiplyHighLow(value ^ readLong(SECRET, secretOffset + 24));
        accumulators[2] += value;

        value = readLong(input, offset + 32);
        accumulators[4] += multiplyHighLow(value ^ readLong(SECRET, secretOffset + 32));
        accumulators[5] += value;

        value = readLong(input, offset + 40);
        accumulators[5] += multiplyHighLow(value ^ readLong(SECRET, secretOffset + 40));
        accumulators[4] += value;

        value = readLong(input, offset + 48);
        accumulators[6] += multiplyHighLow(value ^ readLong(SECRET, secretOffset + 48));
        accumulators[7] += value;

        value = readLong(input, offset + 56);
        accumulators[7] += multiplyHighLow(value ^ readLong(SECRET, secretOffset + 56));
        accumulators[6] += value;
    }

    private static void scramble(long[] accumulators)
    {
        for (int lane = 0; lane < accumulators.length; lane++) {
            long accumulator = accumulators[lane];
            accumulator ^= (accumulator >>> 47);
            accumulator ^= readLong(SECRET, SECRET.length - STRIPE_LENGTH + lane * 8);
            accumulator *= PRIME32_1;
            accumulators[lane] = accumulator;
        }
    }
}
