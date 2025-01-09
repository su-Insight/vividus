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

package org.vividus.ui.web.playwright;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType.LaunchOptions;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Tracing;
import com.microsoft.playwright.Tracing.StartOptions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.vividus.testcontext.SimpleTestContext;

@SuppressWarnings("PMD.CloseResource")
@ExtendWith(MockitoExtension.class)
class BrowserContextProviderTests
{
    private static final long TIMEOUT_MILLIS = 100;

    @Spy private final SimpleTestContext testContext = new SimpleTestContext();
    @Spy private final StartOptions tracingOptions = new StartOptions().setScreenshots(false).setSnapshots(false);
    @Mock private BrowserType browserType;
    @Mock private LaunchOptions launchOptions;
    @Mock private Path tracesOutputDirectory;
    private BrowserContextProvider browserContextProvider;

    @BeforeEach
    void setUp()
    {
        Duration duration = Duration.ofMillis(TIMEOUT_MILLIS);
        BrowserContextConfiguration browserContextConfiguration = new BrowserContextConfiguration(tracingOptions,
                duration, tracesOutputDirectory);
        browserContextProvider = new BrowserContextProvider(browserType, launchOptions, testContext,
                browserContextConfiguration);
    }

    @Test
    void shouldCreateBrowserContextAtFirstRetrievalOnly()
    {
        try (var playwrightStaticMock = mockStatic(Playwright.class))
        {
            // First retrieval
            Playwright playwright = mock();
            playwrightStaticMock.when(Playwright::create).thenReturn(playwright);
            Browser browser = mock();
            when(browserType.launchBrowser(playwright, launchOptions)).thenReturn(browser);
            BrowserContext browserContext = mock();
            when(browser.newContext()).thenReturn(browserContext);

            var actual = browserContextProvider.get();

            assertSame(browserContext, actual);
            verify(browserContext).setDefaultTimeout(TIMEOUT_MILLIS);
            verifyNoMoreInteractions(browserContext);
            playwrightStaticMock.reset();
            reset(playwright, browser);

            // Second retrieval
            var actual2 = browserContextProvider.get();

            assertSame(browserContext, actual2);
            verify(browserContext).setDefaultTimeout(TIMEOUT_MILLIS);
            verifyNoMoreInteractions(browserContext);
            verifyNoInteractions(playwright, browser);
            playwrightStaticMock.verifyNoInteractions();

            // Close context
            when(browserContext.pages()).thenReturn(List.of());
            browserContextProvider.closeCurrentContext();

            verify(browserContext).close();
            verifyNoMoreInteractions(browserContext);
            verifyNoInteractions(tracesOutputDirectory);
        }
    }

    static Stream<Arguments> tracingConfigurations()
    {
        return Stream.of(
                arguments(true, true),
                arguments(false, true),
                arguments(true, false)
        );
    }

    @ParameterizedTest
    @MethodSource("tracingConfigurations")
    void shouldCreateTracesArchiveWhenAnyTracingOptionIsEnabled(boolean tracingScreenshots, boolean tracingSnapshots)
    {
        tracingOptions.setScreenshots(tracingScreenshots).setSnapshots(tracingSnapshots);
        try (var playwrightStaticMock = mockStatic(Playwright.class))
        {
            // Retrieve context
            Playwright playwright = mock();
            playwrightStaticMock.when(Playwright::create).thenReturn(playwright);
            Browser browser = mock();
            when(browserType.launchBrowser(playwright, launchOptions)).thenReturn(browser);
            BrowserContext browserContext = mock();
            when(browser.newContext()).thenReturn(browserContext);
            Tracing tracing = mock();
            when(browserContext.tracing()).thenReturn(tracing);

            var actual = browserContextProvider.get();

            assertSame(browserContext, actual);
            verify(browserContext).setDefaultTimeout(TIMEOUT_MILLIS);
            verifyNoMoreInteractions(browserContext);
            verify(tracing).start(tracingOptions);
            playwrightStaticMock.reset();
            reset(playwright, browser);

            // Close context
            var fileNameArgumentCaptor = ArgumentCaptor.forClass(String.class);
            Path tracesArchivePath = mock();
            when(tracesOutputDirectory.resolve(fileNameArgumentCaptor.capture())).thenReturn(tracesArchivePath);

            Page page = mock();
            when(browserContext.pages()).thenReturn(List.of(page));
            browserContextProvider.closeCurrentContext();

            var ordered = inOrder(tracing, page, browserContext);
            var stopOptionsArgumentCaptor = ArgumentCaptor.forClass(Tracing.StopOptions.class);
            ordered.verify(tracing).stop(stopOptionsArgumentCaptor.capture());
            ordered.verify(page).close();
            ordered.verify(browserContext).close();

            verifyNoInteractions(browser, playwright);
            playwrightStaticMock.verifyNoInteractions();

            assertThat(fileNameArgumentCaptor.getValue(), matchesPattern("traces-.*-\\d{13}\\.zip"));
            assertEquals(tracesArchivePath, stopOptionsArgumentCaptor.getValue().path);

            // Open new context
            BrowserContext newBrowserContext = mock();
            when(browser.newContext()).thenReturn(newBrowserContext);
            Tracing newTracing = mock();
            when(newBrowserContext.tracing()).thenReturn(newTracing);

            var anotherActual = browserContextProvider.get();

            assertSame(newBrowserContext, anotherActual);
            assertNotSame(actual, anotherActual);
            verify(newBrowserContext).setDefaultTimeout(TIMEOUT_MILLIS);
            verifyNoMoreInteractions(newBrowserContext);
            verify(tracing).start(tracingOptions);
            verifyNoInteractions(playwright);
            playwrightStaticMock.verifyNoInteractions();
        }
    }

    @Test
    void shouldCloseAllInstancesOnDestroy()
    {
        try (var playwrightStaticMock = mockStatic(Playwright.class))
        {
            Playwright playwright = mock();
            playwrightStaticMock.when(Playwright::create).thenReturn(playwright);
            Browser browser = mock();
            when(browserType.launchBrowser(playwright, launchOptions)).thenReturn(browser);
            BrowserContext browserContext = mock();
            when(browser.newContext()).thenReturn(browserContext);

            var actual = browserContextProvider.get();

            assertSame(browserContext, actual);
            verify(browserContext).setDefaultTimeout(TIMEOUT_MILLIS);
            verifyNoMoreInteractions(browserContext);
            playwrightStaticMock.reset();
            reset(playwright, browser);

            browserContextProvider.destroy();

            var ordered = inOrder(browser, playwright);
            ordered.verify(browser).close();
            ordered.verify(playwright).close();
        }
    }

    @Test
    void shouldDoNothingOnDestroyIfNoBrowserWasStarted()
    {
        try (var playwrightStaticMock = mockStatic(Playwright.class))
        {
            browserContextProvider.destroy();
            playwrightStaticMock.verifyNoInteractions();
        }
    }
}
