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
package com.exactpro.sf.statictesting;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.mockito.Mockito;

import io.qameta.allure.junit4.AllureJunit4;

public class ExtendedAllureJunit4 extends AllureJunit4 {
    @Override
    public void testRunStarted(Description description) throws Exception {
        super.testRunStarted(mockDescription(description));
    }

    @Override
    public void testRunFinished(Result result) throws Exception {
        super.testRunFinished(mockResult(result));
    }

    @Override
    public void testStarted(Description description) throws Exception {
        super.testStarted(mockDescription(description));
    }

    @Override
    public void testFinished(Description description) throws Exception {
        super.testFinished(mockDescription(description));
    }

    @Override
    public void testFailure(Failure failure) throws Exception {
        super.testFailure(mockFailure(failure));
    }

    @Override
    public void testAssumptionFailure(Failure failure) {
        super.testAssumptionFailure(mockFailure(failure));
    }

    @Override
    public void testIgnored(Description description) throws Exception {
        super.testIgnored(mockDescription(description));
    }

    private Description mockDescription(Description description) {
        String path = StringUtils.substringBetween(description.getDisplayName(), "[", "]");
        description = Mockito.spy(description);
        Mockito.when(description.getClassName()).thenReturn(StringUtils.substringBeforeLast(path, File.separator));
        Mockito.when(description.getMethodName()).thenReturn(StringUtils.substringAfterLast(path, File.separator));
        return description;
    }

    private Failure mockFailure(Failure failure) {
        Description description = mockDescription(failure.getDescription());
        failure = Mockito.spy(failure);
        Mockito.when(failure.getDescription()).thenReturn(description);
        return failure;
    }

    private Result mockResult(Result result) {
        List<Failure> failures = result.getFailures().stream().map(this::mockFailure).collect(Collectors.toList());
        result = Mockito.spy(result);
        Mockito.when(result.getFailures()).thenReturn(failures);
        return result;
    }
}
