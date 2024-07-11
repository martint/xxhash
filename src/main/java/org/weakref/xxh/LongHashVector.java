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

import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.LongVector;
import jdk.incubator.vector.VectorShuffle;

import static jdk.incubator.vector.LongVector.SPECIES_PREFERRED;
import static jdk.incubator.vector.VectorOperators.LSHR;
import static jdk.incubator.vector.VectorOperators.XOR;
import static org.weakref.xxh.Constants.PRIME32_1;
import static org.weakref.xxh.Constants.PRIME32_2;
import static org.weakref.xxh.Constants.PRIME32_3;
import static org.weakref.xxh.Constants.PRIME64_1;
import static org.weakref.xxh.Constants.PRIME64_2;
import static org.weakref.xxh.Constants.PRIME64_3;
import static org.weakref.xxh.Constants.PRIME64_4;
import static org.weakref.xxh.Constants.PRIME64_5;
import static org.weakref.xxh.Constants.SECRET;
import static org.weakref.xxh.Constants.SECRET_CONSUME_RATE;
import static org.weakref.xxh.Constants.SECRET_LAST_ACCUMULATOR_START;
import static org.weakref.xxh.Constants.SECRET_MERGE_ACCUMULATORS_START;
import static org.weakref.xxh.Constants.STRIPE_LENGTH;
import static org.weakref.xxh.Util.avalanche;
import static org.weakref.xxh.Util.mix;
import static org.weakref.xxh.Util.readLong;

class LongHashVector
{
    private static final VectorShuffle<Byte> BYTE_SHUFFLE = VectorShuffle.fromOp(ByteVector.SPECIES_PREFERRED, i -> (i / 8 ^ 1) * 8 + i % 8);

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

        return merge(length, accumulators);
    }

    static void accumulate(long[] accumulators, byte[] input, int offset, int secretOffset)
    {
        for (int i = 0; i < SPECIES_PREFERRED.loopBound(accumulators.length); i += SPECIES_PREFERRED.length()) {
            LongVector accumulatorsVector = LongVector.fromArray(SPECIES_PREFERRED, accumulators, i);
            ByteVector inputVector = ByteVector.fromArray(ByteVector.SPECIES_PREFERRED, input, offset + i * 8);
            ByteVector secretVector = ByteVector.fromArray(ByteVector.SPECIES_PREFERRED, SECRET, secretOffset + i * 8);

            LongVector key = inputVector
                    .lanewise(XOR, secretVector)
                    .reinterpretAsLongs();

            LongVector low = key.and(0xFFFF_FFFFL);
            LongVector high = key.lanewise(LSHR, 32);
            LongVector product = high.mul(low);

            LongVector swapped = inputVector
                    .rearrange(BYTE_SHUFFLE)
                    .reinterpretAsLongs();

            accumulatorsVector
                    .add(swapped)
                    .add(product)
                    .intoArray(accumulators, i);
        }
    }

    private static void scramble(long[] accumulators)
    {
        for (int i = 0; i < SPECIES_PREFERRED.loopBound(accumulators.length); i += SPECIES_PREFERRED.length()) {
            LongVector vector = LongVector.fromArray(SPECIES_PREFERRED, accumulators, i);
            LongVector secret = ByteVector.fromArray(ByteVector.SPECIES_PREFERRED, SECRET, (SECRET.length - STRIPE_LENGTH) + (i * 8)).reinterpretAsLongs();

            vector.lanewise(XOR, vector.lanewise(LSHR, 47))
                    .lanewise(XOR, secret)
                    .mul(PRIME32_1)
                    .intoArray(accumulators, i);
        }
    }

    private static long merge(int length, long[] accumulators)
    {
        long result = length * PRIME64_1;
        result += mix(
                accumulators[2 * 0],
                accumulators[2 * 0 + 1],
                readLong(SECRET, SECRET_MERGE_ACCUMULATORS_START + 16 * 0),
                readLong(SECRET, SECRET_MERGE_ACCUMULATORS_START + 16 * 0 + 8));
        result += mix(
                accumulators[2 * 1],
                accumulators[2 * 1 + 1],
                readLong(SECRET, SECRET_MERGE_ACCUMULATORS_START + 16 * 1),
                readLong(SECRET, SECRET_MERGE_ACCUMULATORS_START + 16 * 1 + 8));
        result += mix(
                accumulators[2 * 2],
                accumulators[2 * 2 + 1],
                readLong(SECRET, SECRET_MERGE_ACCUMULATORS_START + 16 * 2),
                readLong(SECRET, SECRET_MERGE_ACCUMULATORS_START + 16 * 2 + 8));
        result += mix(
                accumulators[2 * 3],
                accumulators[2 * 3 + 1],
                readLong(SECRET, SECRET_MERGE_ACCUMULATORS_START + 16 * 3),
                readLong(SECRET, SECRET_MERGE_ACCUMULATORS_START + 16 * 3 + 8));

        return avalanche(result);
    }

    public static void main(String[] args)
    {
        byte[] data = new byte[100000];
        for (int i = 0; i < 1000000; i++) {
            hash(data, 0, data.length);
        }
    }
}
