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

import java.util.List;

import com.exactpro.sf.aml.Description;
import com.exactpro.sf.comparison.Convention;
import com.exactpro.sf.configuration.ResourceAliases;
import com.exactpro.sf.scriptrunner.AbstractCaller;
import com.exactpro.sf.scriptrunner.utilitymanager.UtilityMethod;
import com.exactpro.sf.util.AbstractBugsChecker;
import com.exactpro.sf.util.BugsCheckerBuilder;
import com.exactpro.sf.util.BugsListCheckerBuilder;

/**
 * @author sergey.vasiliev
 *
 */
@MatrixUtils
@ResourceAliases({"BugsUtils"})
public class BugsUtils extends AbstractCaller {
    @UtilityMethod
    @Description("This method compares the actual value against the expected value and also takes into account<br/>"
            + "the values of the known bugs which can be added by calling the Bug(description, value) / BugEmpty(description) method after calling the function.<br/>"
            + "Considering the above said, the final syntax is:<br/>"
            + "#{Expected(value)}[.Bug(String description, Object alternativeValue) ...][.Actual(x)].<br/>"
            + "If one of the alternative values must be empty, then use:<br/>"
            + "#{Expected(value)}[.BugEmpty(String description) ...][.Actual(x)].<br/>" )
    public BugsCheckerBuilder Expected(Object originValue) {
        return new BugsCheckerBuilder(originValue);
    }

    @UtilityMethod
    @Description("This method compares the size of actual values against the expected size of values and also takes into account<br/>"
            + "the list value sizes of the known bugs which can be added by calling the Bug(description, [value]) / BugEmpty(description) / BugAny(description) method after calling the function.<br/>"
            + "User should specify a list of values with unique size in every node.<br/>"
            + "Expected or first matching combination by size with the actual value will be applied for deep check.<br/>"
            + "Considering the above said, the final syntax is:<br/>"
            + "#{ExpectedList([value])}[.Bug(String description, Object [value, value]) ...].<br/>"
            + "If one of the alternative value lists must be empty, then use:<br/>"
            + "#{ExpectedList(value)}[.BugEmpty(String description) ...].<br/>" )
    public BugsListCheckerBuilder ExpectedList(Object originValue) {
        return new BugsListCheckerBuilder(originValue);
    }

    @UtilityMethod
    @Description("This method compares the actual value against the expected empty value and also takes into account<br/>"
            + "the values of the known bugs which can be added by calling the Bug(description, value) / BugAny(description) method after calling the function.<br/>"
            + "Considering the above said, the final syntax is:<br/>"
            + "#{ExpectedEmpty()}[.Bug(String description, Object alternativeValue) ...][.Actual(x)].<br/>"
            + "If the alternative value can’t be empty, then use:<br/>"
            + "#{ExpectedEmpty(value)}.BugAny(String description)[.Actual(x)].<br/>")
    public BugsCheckerBuilder ExpectedEmpty() {
        return new BugsCheckerBuilder(Convention.CONV_MISSED_OBJECT);
    }

    @UtilityMethod
    @Description("This method compares the size of actual values against the expected empty size of values and also takes into account<br/>"
            + "the list value sizes of the known bugs which can be added by calling the Bug(description, value) / BugEmpty(description) method after calling the function.<br/>"
            + "User should specify a list of values with unique size in every node.<br/>"
            + "Expected or first matching combination by size with the actual value will be applied for deep check.<br/>"
            + "Considering the above said, the final syntax is:<br/>"
            + "#{ExpectedEmptyList()}[.Bug(String description, Object alternativeValue) ...][.Actual(x)].<br/>"
            + "If the alternative value list can’t be empty, then use:<br/>"
            + "#{ExpectedEmptyList(value)}.BugAny(String description)[.Actual(x)].<br/>")
    public BugsListCheckerBuilder ExpectedEmptyList() {
        return new BugsListCheckerBuilder(Convention.CONV_MISSED_OBJECT);
    }

    @UtilityMethod
    @Description("This method compares the actual value against the expected not empty value and also takes into account<br/>"
            + "the values of the known bugs which can be added by calling the BugEmpty(description) method after calling the function.<br/>"
            + "Considering the above said, the final syntax is:<br/>"
            + "#{ExpectedAny()}.BugEmpty(String description)[.Actual(x)].<br/>")
    public BugsCheckerBuilder ExpectedAny() {
        return new BugsCheckerBuilder(Convention.CONV_PRESENT_OBJECT);
    }

    @UtilityMethod
    @Description("This method compares the size of actual values against the expected not empty value list and also takes into account<br/>"
            + "the list value sizes of the known bugs which can be added by calling the BugEmpty(description) method after calling the function.<br/>"
            + "User should specify a list of values with unique size in every node.<br/>"
            + "Considering the above said, the final syntax is:<br/>"
            + "#{ExpectedAnyList()}.BugEmpty(String description)[.Actual(x)].<br/>")
    public BugsListCheckerBuilder ExpectedAnyList() {
        return new BugsListCheckerBuilder(Convention.CONV_PRESENT_OBJECT);
    }
}
