/**
 * Copyright 2013 Crypto Workshop Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cryptoworkshop.ximix.node.mixnet.challenge;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A basic odd/even challenger based on the step number.
 */
public class SimpleOddsEvensChallenger
    extends OddsEvensChallenger
{
    private AtomicInteger counter = new AtomicInteger(0);
    private AtomicBoolean isFirst = new AtomicBoolean(true);

    /**
     * Base constructor.
     *
     * @param size
     * @param stepNo
     */
    public SimpleOddsEvensChallenger(Integer size, Integer stepNo)
    {
        super(size, stepNo);
    }

    @Override
    public boolean hasNext()
    {
        return counter.get() < range;
    }

    @Override
    public int nextIndex()
    {
        //
        // we guarantee the last message is always checked.
        //
        if (isOddRange && isFirst.getAndSet(false))
        {
            return range * 2;
        }

        if (isOddStepNumber)
        {
            return counter.getAndIncrement() * 2;
        }
        else
        {
            return (counter.getAndIncrement() * 2) + 1;
        }
    }
}
