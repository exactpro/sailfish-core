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

import com.exactpro.sf.actions.NTGMatrixUtil;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class TestNTGActionFunctions //extends TestToolsProject
{
    private static final Logger logger = LoggerFactory.getLogger(TestNTGActionFunctions.class);

	private static final int ERROR = 1; // 1s

    private NTGMatrixUtil ntgMatrixUtil = new NTGMatrixUtil();

    @Test
	public void testExpireDateTime() throws Exception
	{
        int current = ntgMatrixUtil.ExpireDateTime();

        int date = ntgMatrixUtil.ExpireDateTime("SECOND", "1");
        Assert.assertTrue("date1", date == current + 1 || date == current + 1 + ERROR);
        date = ntgMatrixUtil.ExpireDateTime("MINUTE", "1");
        Assert.assertTrue("date2", date == current + 60 || date == current + 60 + ERROR);
        date = ntgMatrixUtil.ExpireDateTime("DAY", "1");
        Assert.assertTrue("date3", date == current + 60 * 60 * 24 || date == current + 60 * 60 * 24 + ERROR);

        date = ntgMatrixUtil.ExpireDateTime("SECOND", "-1");
        Assert.assertTrue("date11", date == current - 1 || date == current - 1 + ERROR);
        date = ntgMatrixUtil.ExpireDateTime("MINUTE", "-1");
        Assert.assertTrue("date12", date == current - 60 || date == current - 60 + ERROR);
        date = ntgMatrixUtil.ExpireDateTime("DAY", "-1");
        Assert.assertTrue("date13", date == current - 60 * 60 * 24 || date == current - 60 * 60 * 24 + ERROR);

        date = ntgMatrixUtil.ExpireDateTime((String) null, "1");
        Assert.assertTrue("date33", date == current || date == current + ERROR);

        date = ntgMatrixUtil.ExpireDateTime("", "1");
        Assert.assertTrue("date34", date == current || date == current + ERROR);

        date = ntgMatrixUtil.ExpireDateTime((String) null, "sdds");
        Assert.assertTrue("date42", date == current || date == current + ERROR);

        date = ntgMatrixUtil.ExpireDateTime("", "sdds");
        Assert.assertTrue("date43", date == current || date == current + ERROR);

        date = ntgMatrixUtil.ExpireDateTime("SECOND", "");
        Assert.assertTrue("date44", date == current || date == current + ERROR);

        date = ntgMatrixUtil.ExpireDateTime("SECOND", null);
        Assert.assertTrue("date45", date == current || date == current + ERROR);

        boolean exceptionCaught = false;
        try {
            String date32 = "" + ntgMatrixUtil.ExpireDateTime("SECOND", "sdds");
            System.out.println( "date32 = " + date32);
        } catch (NumberFormatException e) {
            exceptionCaught = true;
        }
        Assert.assertTrue("Exception was not caught", exceptionCaught);
	}

    @Test
	public void testExpireDateTimePosix() throws Exception
	{
        List<Exception> list = new ArrayList<Exception>();
        try
		{
            String date1 = "" + ntgMatrixUtil.ExpireDateTimePOSIX("SECOND", "1");

			System.out.println( "date1 = " + date1);

            String date2 = "" + ntgMatrixUtil.ExpireDateTimePOSIX("MINUTE", "1");
			System.out.println( "date2 = " + date2);

            String date3 = "" + ntgMatrixUtil.ExpireDateTimePOSIX("DAY", "1");
			System.out.println( "date3 = " + date3);

            String date11 = "" + ntgMatrixUtil.ExpireDateTimePOSIX("SECOND", "-1");
			System.out.println( "date11 = " + date11);

            String date12 = "" + ntgMatrixUtil.ExpireDateTimePOSIX("MINUTE", "-1");
			System.out.println( "date12 = " + date12);

            String date13 = "" + ntgMatrixUtil.ExpireDateTimePOSIX("DAY", "-1");
			System.out.println( "date13 = " + date13);


            try {
                String date31 = "" + ntgMatrixUtil.ExpireDateTimePOSIX("SE__COND", "1");
                System.out.println( "date31 = " + date31);
            } catch (Exception e) {
                list.add(e);
            }

            try {
                String date32 = "" + ntgMatrixUtil.ExpireDateTimePOSIX("SECOND", "sdds");
                System.out.println( "date32 = " + date32);
            } catch (Exception e) {
                list.add(e);
            }

            String date33 = "" + ntgMatrixUtil.ExpireDateTimePOSIX(null, "1");
			System.out.println( "date33 = " + date33);

            String date34 = "" + ntgMatrixUtil.ExpireDateTimePOSIX("", "1");
			System.out.println( "date34 = " + date34);


            String date42 = "" + ntgMatrixUtil.ExpireDateTimePOSIX(null, "sdds");
			System.out.println( "date42 = " + date42);

            String date43 = "" + ntgMatrixUtil.ExpireDateTimePOSIX("", "sdds");
			System.out.println( "date43 = " + date43);

            String date44 = "" + ntgMatrixUtil.ExpireDateTimePOSIX("SECOND", "");
			System.out.println( "date44 = " + date44);

            String date45 = "" + ntgMatrixUtil.ExpireDateTimePOSIX("SECOND", null);
			System.out.println( "date45 = " + date45);
		}
		catch(Exception e)
		{
			logger.error( "Error", e );
			Assert.fail();
		}
        Assert.assertEquals("Some Exception was not caught", 2, list.size());
	}
}