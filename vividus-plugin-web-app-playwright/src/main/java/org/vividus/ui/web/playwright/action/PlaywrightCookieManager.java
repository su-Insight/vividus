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

package org.vividus.ui.web.playwright.action;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.options.Cookie;

import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.impl.cookie.BasicClientCookie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vividus.http.client.CookieStoreCollector;
import org.vividus.ui.web.action.CookieManager;
import org.vividus.ui.web.action.InetAddressUtils;
import org.vividus.ui.web.playwright.BrowserContextProvider;

public class PlaywrightCookieManager implements CookieManager<Cookie>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PlaywrightCookieManager.class);

    private final BrowserContextProvider browserContextProvider;

    public PlaywrightCookieManager(BrowserContextProvider browserContextProvider)
    {
        this.browserContextProvider = browserContextProvider;
    }

    @Override
    public void addCookie(String cookieName, String cookieValue, String path, String urlAsString)
    {
        String domain = InetAddressUtils.getDomain(urlAsString);
        Cookie cookie = new Cookie(cookieName, cookieValue)
                .setPath(path)
                .setDomain(domain);
        LOGGER.debug("Adding cookie: {}", cookie);
        getBrowserContext().addCookies(List.of(cookie));
    }

    @Override
    public void deleteAllCookies()
    {
        getBrowserContext().clearCookies();
    }

    @Override
    public void deleteCookie(String cookieName)
    {
        getBrowserContext().clearCookies(new BrowserContext.ClearCookiesOptions().setName(cookieName));
    }

    @Override
    public Cookie getCookie(String cookieName)
    {
        return getBrowserContext().cookies().stream()
                .filter(cookie -> cookie.name.equals(cookieName))
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<Cookie> getCookies()
    {
        return getBrowserContext().cookies();
    }

    @Override
    public CookieStore getCookiesAsHttpCookieStore()
    {
        return getCookies().stream().map(PlaywrightCookieManager::createHttpClientCookie).collect(
                new CookieStoreCollector());
    }

    private static org.apache.hc.client5.http.cookie.Cookie createHttpClientCookie(Cookie cookie)
    {
        BasicClientCookie httpClientCookie = new BasicClientCookie(cookie.name, cookie.value);
        httpClientCookie.setDomain(cookie.domain);
        httpClientCookie.setPath(cookie.path);
        Optional.ofNullable(cookie.expires).map(val -> Instant.ofEpochSecond(val.longValue()))
                .ifPresent(httpClientCookie::setExpiryDate);
        httpClientCookie.setSecure(cookie.secure);
        httpClientCookie.setAttribute(org.apache.hc.client5.http.cookie.Cookie.DOMAIN_ATTR, cookie.domain);
        httpClientCookie.setAttribute(org.apache.hc.client5.http.cookie.Cookie.PATH_ATTR, cookie.path);
        return httpClientCookie;
    }

    private BrowserContext getBrowserContext()
    {
        return browserContextProvider.get();
    }
}
