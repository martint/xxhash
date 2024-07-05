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

class LongHashUnrolled
{
    public static long hash(byte[] input, int offset, int length)
    {
        long lane0 = PRIME32_3;
        long lane1 = PRIME64_1;
        long lane2 = PRIME64_2;
        long lane3 = PRIME64_3;
        long lane4 = PRIME64_4;
        long lane5 = PRIME32_2;
        long lane6 = PRIME64_5;
        long lane7 = PRIME32_1;

        int stripesPerBlock = (SECRET.length - STRIPE_LENGTH) / SECRET_CONSUME_RATE;
        int blockLength = STRIPE_LENGTH * stripesPerBlock;
        int blockCount = (length - 1) / blockLength;

        for (int block = 0; block < blockCount; block++) {
            for (int stripe = 0; stripe < stripesPerBlock; stripe++) {
                int off = offset + block * blockLength + stripe * STRIPE_LENGTH;
                int secretOffset = stripe * SECRET_CONSUME_RATE;

                long value0 = readLong(input, off + 0 * 8);
                long value1 = readLong(input, off + 1 * 8);
                long value2 = readLong(input, off + 2 * 8);
                long value3 = readLong(input, off + 3 * 8);
                long value4 = readLong(input, off + 4 * 8);
                long value5 = readLong(input, off + 5 * 8);
                long value6 = readLong(input, off + 6 * 8);
                long value7 = readLong(input, off + 7 * 8);

                lane0 += value1 + multiplyHighLow(value0 ^ readLong(SECRET, secretOffset + 0 * 8));
                lane1 += value0 + multiplyHighLow(value1 ^ readLong(SECRET, secretOffset + 1 * 8));
                lane2 += value3 + multiplyHighLow(value2 ^ readLong(SECRET, secretOffset + 2 * 8));
                lane3 += value2 + multiplyHighLow(value3 ^ readLong(SECRET, secretOffset + 3 * 8));
                lane4 += value5 + multiplyHighLow(value4 ^ readLong(SECRET, secretOffset + 4 * 8));
                lane5 += value4 + multiplyHighLow(value5 ^ readLong(SECRET, secretOffset + 5 * 8));
                lane6 += value7 + multiplyHighLow(value6 ^ readLong(SECRET, secretOffset + 6 * 8));
                lane7 += value6 + multiplyHighLow(value7 ^ readLong(SECRET, secretOffset + 7 * 8));
            }

            // scramble
            lane0 = (lane0 ^ (lane0 >>> 47) ^ readLong(SECRET, SECRET.length - STRIPE_LENGTH + 0 * 8)) * PRIME32_1;
            lane1 = (lane1 ^ (lane1 >>> 47) ^ readLong(SECRET, SECRET.length - STRIPE_LENGTH + 1 * 8)) * PRIME32_1;
            lane2 = (lane2 ^ (lane2 >>> 47) ^ readLong(SECRET, SECRET.length - STRIPE_LENGTH + 2 * 8)) * PRIME32_1;
            lane3 = (lane3 ^ (lane3 >>> 47) ^ readLong(SECRET, SECRET.length - STRIPE_LENGTH + 3 * 8)) * PRIME32_1;
            lane4 = (lane4 ^ (lane4 >>> 47) ^ readLong(SECRET, SECRET.length - STRIPE_LENGTH + 4 * 8)) * PRIME32_1;
            lane5 = (lane5 ^ (lane5 >>> 47) ^ readLong(SECRET, SECRET.length - STRIPE_LENGTH + 5 * 8)) * PRIME32_1;
            lane6 = (lane6 ^ (lane6 >>> 47) ^ readLong(SECRET, SECRET.length - STRIPE_LENGTH + 6 * 8)) * PRIME32_1;
            lane7 = (lane7 ^ (lane7 >>> 47) ^ readLong(SECRET, SECRET.length - STRIPE_LENGTH + 7 * 8)) * PRIME32_1;
        }

        int stripeCount = ((length - 1) - (blockLength * blockCount)) / STRIPE_LENGTH;
        for (int stripe = 0; stripe < stripeCount; stripe++) {
            int off = offset + blockCount * blockLength + stripe * STRIPE_LENGTH;
            int secretOffset = stripe * SECRET_CONSUME_RATE;

            long value0 = readLong(input, off + 0 * 8);
            long value1 = readLong(input, off + 1 * 8);
            long value2 = readLong(input, off + 2 * 8);
            long value3 = readLong(input, off + 3 * 8);
            long value4 = readLong(input, off + 4 * 8);
            long value5 = readLong(input, off + 5 * 8);
            long value6 = readLong(input, off + 6 * 8);
            long value7 = readLong(input, off + 7 * 8);

            lane0 += value1 + multiplyHighLow(value0 ^ readLong(SECRET, secretOffset + 0 * 8));
            lane1 += value0 + multiplyHighLow(value1 ^ readLong(SECRET, secretOffset + 1 * 8));
            lane2 += value3 + multiplyHighLow(value2 ^ readLong(SECRET, secretOffset + 2 * 8));
            lane3 += value2 + multiplyHighLow(value3 ^ readLong(SECRET, secretOffset + 3 * 8));
            lane4 += value5 + multiplyHighLow(value4 ^ readLong(SECRET, secretOffset + 4 * 8));
            lane5 += value4 + multiplyHighLow(value5 ^ readLong(SECRET, secretOffset + 5 * 8));
            lane6 += value7 + multiplyHighLow(value6 ^ readLong(SECRET, secretOffset + 6 * 8));
            lane7 += value6 + multiplyHighLow(value7 ^ readLong(SECRET, secretOffset + 7 * 8));
        }

        int off = offset + length - STRIPE_LENGTH;
        int secretOffset = SECRET.length - STRIPE_LENGTH - SECRET_LAST_ACCUMULATOR_START;

        long value0 = readLong(input, off + 0 * 8);
        long value1 = readLong(input, off + 1 * 8);
        long value2 = readLong(input, off + 2 * 8);
        long value3 = readLong(input, off + 3 * 8);
        long value4 = readLong(input, off + 4 * 8);
        long value5 = readLong(input, off + 5 * 8);
        long value6 = readLong(input, off + 6 * 8);
        long value7 = readLong(input, off + 7 * 8);

        lane0 += value1 + multiplyHighLow(value0 ^ readLong(SECRET, secretOffset + 0 * 8));
        lane1 += value0 + multiplyHighLow(value1 ^ readLong(SECRET, secretOffset + 1 * 8));
        lane2 += value3 + multiplyHighLow(value2 ^ readLong(SECRET, secretOffset + 2 * 8));
        lane3 += value2 + multiplyHighLow(value3 ^ readLong(SECRET, secretOffset + 3 * 8));
        lane4 += value5 + multiplyHighLow(value4 ^ readLong(SECRET, secretOffset + 4 * 8));
        lane5 += value4 + multiplyHighLow(value5 ^ readLong(SECRET, secretOffset + 5 * 8));
        lane6 += value7 + multiplyHighLow(value6 ^ readLong(SECRET, secretOffset + 6 * 8));
        lane7 += value6 + multiplyHighLow(value7 ^ readLong(SECRET, secretOffset + 7 * 8));

        // merge
        long result = length * PRIME64_1;
        result += mix(
                lane0,
                lane1,
                readLong(SECRET, SECRET_MERGE_ACCUMULATORS_START + 16 * 0),
                readLong(SECRET, SECRET_MERGE_ACCUMULATORS_START + 16 * 0 + 8));

        result += mix(
                lane2,
                lane3,
                readLong(SECRET, SECRET_MERGE_ACCUMULATORS_START + 16 * 1),
                readLong(SECRET, SECRET_MERGE_ACCUMULATORS_START + 16 * 1 + 8));

        result += mix(
                lane4,
                lane5,
                readLong(SECRET, SECRET_MERGE_ACCUMULATORS_START + 16 * 2),
                readLong(SECRET, SECRET_MERGE_ACCUMULATORS_START + 16 * 2 + 8));

        result += mix(
                lane6,
                lane7,
                readLong(SECRET, SECRET_MERGE_ACCUMULATORS_START + 16 * 3),
                readLong(SECRET, SECRET_MERGE_ACCUMULATORS_START + 16 * 3 + 8));

        return avalanche(result);
    }
}
