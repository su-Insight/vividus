/*
 * Copyright 2019-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.vividus.ui.variable;

import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vividus.selenium.IWebDriverProvider;
import org.vividus.variable.DynamicVariableCalculationResult;

public class SourceCodeDynamicVariable extends AbstractWebDriverDynamicVariable
{
    private static final Logger LOGGER = LoggerFactory.getLogger(SourceCodeDynamicVariable.class);

    public SourceCodeDynamicVariable(IWebDriverProvider webDriverProvider)
    {
        super(webDriverProvider, WebDriver::getPageSource);
    }

    @Override
    public DynamicVariableCalculationResult calculateValue()
    {
        LOGGER.warn("The \"${source-code}\" dynamic variable is deprecated and will be removed in VIVIDUS 0.7.0. "
                + "Please use \"${context-source-code}\" dynamic variable instead.");
        return super.calculateValue();
    }
}
