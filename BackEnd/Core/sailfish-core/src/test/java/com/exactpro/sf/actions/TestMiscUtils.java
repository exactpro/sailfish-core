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
package com.exactpro.sf.actions;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.exactpro.sf.common.impl.messages.DefaultMessageFactory;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.aml.scriptutil.StaticUtil;
import com.exactpro.sf.center.impl.SFLocalContext;
import com.exactpro.sf.util.AbstractTest;
import com.exactpro.sf.util.FieldKnownBugException;

public class TestMiscUtils extends AbstractTest {

    private final String intFiled = "int";
    private final String stringFiled = "str";
    private MiscUtils miscUtils = null;

    @Before
    public void before() {
        this.miscUtils = new MiscUtils();
    }

    @Test
    public void testIntCheckCount() throws Exception {
        Collection<Integer> intCollection = Arrays.asList(4, 1, 2, 3, 4);

        Assert.assertTrue(this.miscUtils.checkCount(intCollection, 5, 0));
        Assert.assertFalse(this.miscUtils.checkCount(intCollection, 5, 1));

        Assert.assertTrue(this.miscUtils.checkCount(intCollection, 2, 1));
        Assert.assertFalse(this.miscUtils.checkCount(intCollection, 2, 2));

        Assert.assertTrue(this.miscUtils.checkCount(intCollection, 4, 2));
        Assert.assertFalse(this.miscUtils.checkCount(intCollection, 4, 0));
    }

    @Test
    public void testIMessageCheckCount() throws Exception {
        IMessage empty = DefaultMessageFactory.getFactory().createMessage("name", "namespace");
        IMessage fullItem = empty.cloneMessage();
        fullItem.addField(intFiled, 1);
        IMessage partItem = fullItem.cloneMessage();
        fullItem.addField(stringFiled, "str");

        int count = 2;
        Collection<IMessage> msgCollection = Collections.nCopies(count, fullItem);

        Assert.assertTrue(this.miscUtils.checkCount(msgCollection, fullItem, count));
        Assert.assertTrue(this.miscUtils.checkCount(msgCollection, partItem, count));
        Assert.assertTrue(this.miscUtils.checkCount(msgCollection, empty, 0));
        Assert.assertFalse(this.miscUtils.checkCount(msgCollection, fullItem, 0));
        Assert.assertFalse(this.miscUtils.checkCount(msgCollection, partItem, 0));
        Assert.assertFalse(this.miscUtils.checkCount(msgCollection, empty, count));

        IMessage knownBugItem = empty.cloneMessage();
        knownBugItem.addField(intFiled,
                StaticUtil.filter(0, null,
                        "um.call(SailfishURI.parse(\"BugsUtils.Expected\"), 2).Bug(\"int bug\", 1 )",
                        "um", SFLocalContext.getDefault().getUtilityManager()));

        FieldKnownBugException fieldKnownBugException = null;
        try {
            this.miscUtils.checkCount(msgCollection, knownBugItem, count);
        } catch (FieldKnownBugException e) {
            fieldKnownBugException = e;
        }
        Assert.assertEquals(1, fieldKnownBugException.getActualDescriptions().size());
        String subject = fieldKnownBugException.getActualDescriptions().toString();
        Assert.assertThat(subject, subject, containsString("int bug"));
        Assert.assertThat(subject, subject, not(containsString("string bug")));

        knownBugItem.addField(stringFiled,
                StaticUtil.filter(0, null,
                        "um.call(SailfishURI.parse(\"BugsUtils.Expected\"), \"str2\").Bug(\"string bug\", \"str\" )",
                        "um", SFLocalContext.getDefault().getUtilityManager()));
        try {
            this.miscUtils.checkCount(msgCollection, knownBugItem, count);
        } catch (FieldKnownBugException e) {
            fieldKnownBugException = e;
        }
        
        Assert.assertEquals(1, fieldKnownBugException.getActualDescriptions().size());
        subject = fieldKnownBugException.getActualDescriptions().toString();
        Assert.assertThat(subject, subject, containsString("int bug"));
        Assert.assertThat(subject, subject, containsString("string bug"));
    }
}
