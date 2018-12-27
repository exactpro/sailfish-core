/******************************************************************************
 * Copyright 2009-2018 Exactpro (Exactpro Systems Limited)
 *
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
 ******************************************************************************/
package com.exactpro.sf.util;

import java.math.BigDecimal;

import com.exactpro.sf.actions.MatrixUtils;
import com.exactpro.sf.aml.Description;
import com.exactpro.sf.comparison.conversion.MultiConverter;
import com.exactpro.sf.configuration.ResourceAliases;
import com.exactpro.sf.scriptrunner.AbstractCaller;
import com.exactpro.sf.scriptrunner.utilitymanager.UtilityMethod;

/**
 * @author anatoly.ivanov
 *
 */
@MatrixUtils
@ResourceAliases({ "CompareUtil" })
public class CompareUtil extends AbstractCaller {

    @Description("Tests if a number is in the neighbourhood of base +/- delta (inclusive / non-strict) Usage: #{inRangeBased(actual, base, delta)}")
    @UtilityMethod
    public boolean inRangeBased(Number actual, Number base, Number delta) {
        return compare(actual, base, delta, false);
    }

    @Description("Tests if a number is in the neighbourhood of base +/- delta (exclusive / strict) Usage: #{inRangeBasedStrict(actual, base, delta)}")
    @UtilityMethod
    public boolean inRangeBasedStrict(Number actual, Number base, Number delta) {
        return compare(actual, base, delta, true);
    }

    private boolean compare(Number actual, Number base, Number delta, boolean strict) {
        BigDecimal deltaValue = MultiConverter.convert(delta, BigDecimal.class);
        BigDecimal actualValue = MultiConverter.convert(actual, BigDecimal.class);

        BigDecimal minRangeLimit = MultiConverter.convert(base, BigDecimal.class).subtract(deltaValue);
        BigDecimal maxRangeLimit = MultiConverter.convert(base, BigDecimal.class).add(deltaValue);

        return strict
                ? (actualValue.compareTo(minRangeLimit) > 0 && actualValue.compareTo(maxRangeLimit) < 0)
                : (actualValue.compareTo(minRangeLimit) >= 0 && actualValue.compareTo(maxRangeLimit) <= 0);
    }
}
