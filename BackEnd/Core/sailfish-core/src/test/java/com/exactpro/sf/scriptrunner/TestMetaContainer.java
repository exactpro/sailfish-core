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
package com.exactpro.sf.scriptrunner;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.exactpro.sf.aml.script.MetaContainer;

import junit.framework.Assert;

/**
 * @author sergey.vasiliev
 *
 */
public class TestMetaContainer {

    @Test
    public void testFailUnexpectedInheritance() {
        String failUnexpected = "Y";
        int capacity = 5;
        MetaContainer parent = SailFishAction.createMetaContainer(failUnexpected);
        List<MetaContainer> containers = new ArrayList<>();
        containers.add(parent);
        for (int i = 1; i <= capacity; i++) {
            containers.add(SailFishAction.createMetaContainer(containers.get(i - 1), "Children" + i, null));
        }
        for (int i = 0; i <= capacity; i++) {
            Assert.assertEquals("fail_unexpected isn't correct", failUnexpected, containers.get(i).getFailUnexpected());
        }

        containers = new ArrayList<>();
        containers.add(null);
        for (int i = 1; i <= capacity; i++) {
            containers.add(SailFishAction.createMetaContainer(containers.get(i - 1), "Children" + i, null));
        }
        for (int i = 1; i <= capacity; i++) {
            Assert.assertEquals("fail_unexpected isn't correct", null, containers.get(i).getFailUnexpected());
        }

    }

}
