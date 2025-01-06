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

package org.vividus.ui.web.listener;

import java.lang.reflect.Method;

import org.openqa.selenium.WebDriver.Navigation;
import org.openqa.selenium.support.events.WebDriverListener;
import org.vividus.selenium.logging.BrowserLogManager;

public final class BrowserLogCleaningListener implements WebDriverListener
{
    private final BrowserLogManager browserLogManager;

    private BrowserLogCleaningListener(BrowserLogManager browserLogManager)
    {
        this.browserLogManager = browserLogManager;
    }

    @Override
    public void beforeAnyNavigationCall(Navigation navigation, Method method, Object[] args)
    {
        browserLogManager.resetBuffer(true);
    }

    public static class Factory implements WebDriverListenerFactory
    {
        private final BrowserLogManager browserLogManager;

        public Factory(BrowserLogManager browserLogManager)
        {
            this.browserLogManager = browserLogManager;
        }

        @Override
        public WebDriverListener createListener()
        {
            return new BrowserLogCleaningListener(browserLogManager);
        }
    }
}
