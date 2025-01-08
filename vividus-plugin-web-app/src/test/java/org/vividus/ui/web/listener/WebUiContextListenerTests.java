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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriver.TargetLocator;
import org.vividus.ui.context.IUiContext;
import org.vividus.ui.web.listener.WebUiContextListener.Factory;

@ExtendWith(MockitoExtension.class)
class WebUiContextListenerTests
{
    private static final String WINDOW_NAME1 = "windowName1";
    private static final String WINDOW_NAME2 = "windowName2";

    @Mock private IUiContext uiContext;

    @Test
    void shouldResetContextBeforeAnyNavigationAction()
    {
        var listener = new Factory(uiContext).createListener(null);
        listener.beforeAnyNavigationCall(null, null, null);
        verify(uiContext).reset();
    }

    @Test
    void testBeforeWindow()
    {
        WebDriver webDriver = mock();
        var listener = new Factory(uiContext).createListener(webDriver);
        listener.beforeWindow(null, WINDOW_NAME1);
        verifyNoInteractions(uiContext, webDriver);
    }

    @Test
    void testBeforeWindowWindowExists()
    {
        WebDriver webDriver = mock();
        var listener = new Factory(uiContext).createListener(webDriver);
        listener.afterWindow(null, WINDOW_NAME1, webDriver);
        when(webDriver.getWindowHandles()).thenReturn(new LinkedHashSet<>(List.of(WINDOW_NAME1, WINDOW_NAME2)));
        listener.beforeWindow(null, WINDOW_NAME2);
        verifyNoInteractions(uiContext);
    }

    @Test
    void testBeforeWindowWindowNotExists()
    {
        WebDriver webDriver = mock();
        var listener = new Factory(uiContext).createListener(webDriver);
        listener.afterWindow(null, WINDOW_NAME1, webDriver);
        TargetLocator targetLocator = mock();
        when(webDriver.switchTo()).thenReturn(targetLocator);
        when(webDriver.getWindowHandles()).thenReturn(Set.of(WINDOW_NAME2));

        listener.beforeWindow(null, WINDOW_NAME2);

        verify(uiContext).reset();
        verify(targetLocator).window(WINDOW_NAME2);
    }

    @Test
    void testSwitchToNewWindow()
    {
        WebDriver webDriver = mock();
        when(webDriver.getWindowHandle()).thenReturn(WINDOW_NAME1);
        var listener = new Factory(uiContext).createListener(webDriver);
        listener.afterWindow(null, null, webDriver);
        when(webDriver.getWindowHandles()).thenReturn(new LinkedHashSet<>(List.of(WINDOW_NAME1, WINDOW_NAME2)));
        listener.beforeWindow(null, WINDOW_NAME2);
        verifyNoInteractions(uiContext);
    }
}
