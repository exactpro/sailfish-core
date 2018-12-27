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
package com.exactpro.sf.services.ntg;

import java.math.BigDecimal;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.util.ErrorUtil;
import com.exactpro.sf.util.AbstractTest;

public class TestConvertFloatToInteger extends AbstractTest {
    private static final Logger logger = LoggerFactory.getLogger(TestConvertFloatToInteger.class);

	@Test
	public void testEncode_FloatValue() throws Exception
	{
		try
		{
			float floatValue_1 = 33f;
			float floatValue_2 = 33.01f;
			float floatValue_3 = 33.0099f;
			float floatValue_4 = 33.00009f;
			float floatValue_5 = 33.00005f;
			float floatValue_6 = 33.00003f;

			BigDecimal baseValue = new BigDecimal( floatValue_1 );
			BigDecimal baseScaled  = baseValue.setScale( 4, BigDecimal.ROUND_HALF_UP );
			BigDecimal multiplied = baseScaled.multiply( new BigDecimal(10000.0)) ;
			Assert.assertTrue( multiplied.intValue() == 330000 );

			baseValue = new BigDecimal( floatValue_2 );
			baseScaled  = baseValue.setScale( 4, BigDecimal.ROUND_HALF_UP );
			multiplied = baseScaled.multiply( new BigDecimal(10000.0)) ;
			Assert.assertTrue( multiplied.intValue() == 330100 );

			baseValue = new BigDecimal( floatValue_3 );
			baseScaled  = baseValue.setScale( 4, BigDecimal.ROUND_HALF_UP );
			multiplied = baseScaled.multiply( new BigDecimal(10000.0)) ;
			Assert.assertTrue( multiplied.intValue() == 330099 );

			baseValue = new BigDecimal( floatValue_4 );
			baseScaled  = baseValue.setScale( 4, BigDecimal.ROUND_HALF_UP );
			multiplied = baseScaled.multiply( new BigDecimal(10000.0)) ;
			Assert.assertTrue( multiplied.intValue() == 330001 );

			baseValue = new BigDecimal( floatValue_5 );
			baseScaled  = baseValue.setScale( 4, BigDecimal.ROUND_HALF_UP );
			multiplied = baseScaled.multiply( new BigDecimal(10000.0)) ;
			Assert.assertTrue( multiplied.intValue() == 330000 );

			baseValue = new BigDecimal( floatValue_6 );
			baseScaled  = baseValue.setScale( 4, BigDecimal.ROUND_HALF_UP );
			multiplied = baseScaled.multiply( new BigDecimal(10000.0)) ;
			Assert.assertTrue( multiplied.intValue() == 330000 );
		}
		catch (Exception e)
		{
			logger.error( ErrorUtil.formatException( e ));
			Assert.fail();
		}
	}
}