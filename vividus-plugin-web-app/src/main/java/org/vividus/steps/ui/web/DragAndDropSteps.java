/*
 * Copyright 2019-2023 the original author or authors.
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

package org.vividus.steps.ui.web;

import java.time.Duration;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.apache.commons.lang3.ObjectUtils;
import org.jbehave.core.annotations.When;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.vividus.annotation.Replacement;
import org.vividus.selenium.IWebDriverProvider;
import org.vividus.selenium.locator.Locator;
import org.vividus.steps.ui.validation.IBaseValidations;
import org.vividus.steps.ui.web.model.Location;
import org.vividus.ui.monitor.TakeScreenshotOnFailure;
import org.vividus.ui.web.action.WebJavascriptActions;

@TakeScreenshotOnFailure
public class DragAndDropSteps
{
    private static final int RIGHT_OFFSET = 10;
    private static final int LEFT_OFFSET = -10;
    private static final String DRAGGABLE_ELEMENT = "Draggable element";
    private static final String TARGET_ELEMENT = "Target element";
    private static final String SIMULATE_DRAG_AND_DROP_JS = "simulate-drag-and-drop.js";
    private final IWebDriverProvider webDriverProvider;
    private final WebJavascriptActions javascriptActions;
    private final IBaseValidations baseValidations;

    public DragAndDropSteps(IWebDriverProvider webDriverProvider, WebJavascriptActions javascriptActions,
            IBaseValidations baseValidations)
    {
        this.webDriverProvider = webDriverProvider;
        this.javascriptActions = javascriptActions;
        this.baseValidations = baseValidations;
    }

    /**
     * Drags the <b>draggable</b> element and moves it relatively to the <b>target</b> element in
     * accordance to provided <b>location</b>.
     * <br>
     * <i>Example</i>
     * <br>
     * <code>When I drag element located `By.xpath(//div[@class='draggable'])` and drop it at RIGHT_TOP of element
     * located `By.xpath(//div[@class='target'])`</code>
     * If this step doesn't work, try to use step simulating drag&amp;drop
     * @param draggable draggable element
     * @param location location relatively to the <b>target</b> element (<b>TOP</b>,<b>BOTTOM</b>,<b>LEFT</b>,
     * <b>RIGHT</b>,<b>CENTER</b>,<b>LEFT_TOP</b>,<b>RIGHT_TOP</b>,<b>LEFT_BOTTOM</b>,<b>RIGHT_BOTTOM</b>)
     * @param target target element
     * @deprecated Use step:
     * "When I drag element located by `$draggable` and drop it at $location of element located by `$target`" instead
     */
    @Deprecated(since = "0.6.5", forRemoval = true)
    @Replacement(versionToRemoveStep = "0.7.0", replacementFormatPattern =
            "When I drag element located by `%1$s` and drop it at %2$s of element located by `%3$s`")
    @When("I drag element located `$draggable` and drop it at $location of element located `$target`")
    @SuppressWarnings("checkstyle:MagicNumber")
    public void dragAndDropToTargetAtLocationDeprecated(Locator draggable, Location location, Locator target)
    {
        performDragAndDropDeprecated(draggable, target, (draggableElement, targetElement) ->
        {
            Point offsetPoint = location.getPoint(draggableElement.getRect(), targetElement.getRect());
            new Actions(webDriverProvider.get())
                    .clickAndHold(draggableElement)
                    // Selenium bug: https://github.com/SeleniumHQ/selenium/issues/1365#issuecomment-547786925
                    .moveByOffset(RIGHT_OFFSET, 0)
                    .moveByOffset(LEFT_OFFSET, 0)
                    .moveByOffset(offsetPoint.getX(), offsetPoint.getY())
                    .release()
                    // Wait for DOM stabilization
                    .pause(Duration.ofSeconds(1))
                    .perform();
        });
    }

    /**
     * Drags the <b>draggable</b> element and moves it relatively to the <b>target</b> element in
     * accordance to provided <b>location</b>.
     * <br>
     * <i>Example</i>
     * <br>
     * <code>When I drag element located by `By.xpath(//div[@class='draggable'])` and drop it at RIGHT_TOP of element
     * located by `By.xpath(//div[@class='target'])`</code>
     * If this step doesn't work, try to use step simulating drag&amp;drop
     * @param draggable draggable element
     * @param location location relatively to the <b>target</b> element (<b>TOP</b>,<b>BOTTOM</b>,<b>LEFT</b>,
     * <b>RIGHT</b>,<b>CENTER</b>,<b>LEFT_TOP</b>,<b>RIGHT_TOP</b>,<b>LEFT_BOTTOM</b>,<b>RIGHT_BOTTOM</b>)
     * @param target target element
     */
    @When("I drag element located by `$draggable` and drop it at $location of element located by `$target`")
    public void dragAndDropToTargetAtLocation(Locator draggable, Location location, Locator target)
    {
        performDragAndDrop(draggable, target, (draggableElement, targetElement) ->
        {
            Point offsetPoint = location.getPoint(draggableElement.getRect(), targetElement.getRect());
            new Actions(webDriverProvider.get())
                    .clickAndHold(draggableElement)
                    // Selenium bug: https://github.com/SeleniumHQ/selenium/issues/1365#issuecomment-547786925
                    .moveByOffset(RIGHT_OFFSET, 0)
                    .moveByOffset(LEFT_OFFSET, 0)
                    .moveByOffset(offsetPoint.getX(), offsetPoint.getY())
                    .release()
                    // Wait for DOM stabilization
                    .pause(Duration.ofSeconds(1))
                    .perform();
        });
    }

    /**
     * Simulates drag of the <b>draggable</b> element and its drop at the <b>target</b> element via JavaScript.
     * <br>
     * <i>Example</i>
     * <br>
     * <code>When I simulate drag of element located `By.xpath(//div[@class='draggable'])` and drop at element located
     * `By.xpath(//div[@class='target'])`</code>
     * <p>
     *   The reason of having this step is that Selenium WebDriver doesn't support HTML5 drag&amp;drop:
     * </p>
     * <ul>
     *   <li><a href="https://github.com/seleniumhq/selenium-google-code-issue-archive/issues/3604">
     *       Issue 3604: HTML5 Drag and Drop with Selenium WebDriver
     *       </a>
     *   </li>
     *   <li><a href="https://github.com/SeleniumHQ/selenium/issues/1365">
     *       Issue 1365: Actions drag and drop method
     *       </a>
     *   </li>
     * </ul>
     * <p>
     * As workaround for these issue the step simulates HTML5 drag&amp;drop via JavaScript. There is no difference in
     * actual drag&amp;drop and its simulation via JavaScript from the functional side.
     * </p>
     * @param draggable draggable element
     * @param target target element
     * @deprecated Use step:
     * "When I simulate drag of element located by `$draggable` and drop at element located by `$target`" instead
     */
    @Deprecated(since = "0.6.5", forRemoval = true)
    @Replacement(versionToRemoveStep = "0.7.0", replacementFormatPattern =
            "When I simulate drag of element located by `%1$s` and drop at element located by `%2$s`")
    @When("I simulate drag of element located `$draggable` and drop at element located `$target`")
    public void simulateDragAndDropDeprecated(Locator draggable, Locator target)
    {
        performDragAndDropDeprecated(draggable, target, (draggableElement, targetElement) ->
                // See gist for details: https://gist.github.com/valfirst/7f36c8755676cdf8943a8a8f08eab2e3
                javascriptActions.executeScriptFromResource(getClass(),
                        SIMULATE_DRAG_AND_DROP_JS, draggableElement,
                        targetElement));
    }

    /**
     * Simulates drag of the <b>draggable</b> element and its drop at the <b>target</b> element via JavaScript.
     * <br>
     * <i>Example</i>
     * <br>
     * <code>When I simulate drag of element located `By.xpath(//div[@class='draggable'])` and drop at element located
     * `By.xpath(//div[@class='target'])`</code>
     * <p>
     *   The reason of having this step is that Selenium WebDriver doesn't support HTML5 drag&amp;drop:
     * </p>
     * <ul>
     *   <li><a href="https://github.com/seleniumhq/selenium-google-code-issue-archive/issues/3604">
     *       Issue 3604: HTML5 Drag and Drop with Selenium WebDriver
     *       </a>
     *   </li>
     *   <li><a href="https://github.com/SeleniumHQ/selenium/issues/1365">
     *       Issue 1365: Actions drag and drop method
     *       </a>
     *   </li>
     * </ul>
     * <p>
     * As workaround for these issue the step simulates HTML5 drag&amp;drop via JavaScript. There is no difference in
     * actual drag&amp;drop and its simulation via JavaScript from the functional side.
     * </p>
     * @param draggable draggable element
     * @param target target element
     */
    @When("I simulate drag of element located by `$draggable` and drop at element located by `$target`")
    public void simulateDragAndDrop(Locator draggable, Locator target)
    {
        performDragAndDrop(draggable, target, (draggableElement, targetElement) ->
                // See gist for details: https://gist.github.com/valfirst/7f36c8755676cdf8943a8a8f08eab2e3
                javascriptActions.executeScriptFromResource(getClass(), SIMULATE_DRAG_AND_DROP_JS, draggableElement,
                        targetElement));
    }

    private void performDragAndDropDeprecated(Locator draggable, Locator target,
            BiConsumer<WebElement, WebElement> dragAndDropExecutor)
    {
        WebElement draggableElement = baseValidations.assertIfElementExists(DRAGGABLE_ELEMENT, draggable);
        WebElement targetElement = baseValidations.assertIfElementExists(TARGET_ELEMENT, target);

        if (ObjectUtils.allNotNull(draggableElement, targetElement))
        {
            dragAndDropExecutor.accept(draggableElement, targetElement);
        }
    }

    private void performDragAndDrop(Locator draggable, Locator target,
                  BiConsumer<WebElement, WebElement> dragAndDropExecutor)
    {
        Optional<WebElement> draggableElement = baseValidations.assertElementExists(DRAGGABLE_ELEMENT, draggable);
        Optional<WebElement> targetElement = baseValidations.assertElementExists(TARGET_ELEMENT, target);

        if (draggableElement.isPresent() && targetElement.isPresent())
        {
            dragAndDropExecutor.accept(draggableElement.get(), targetElement.get());
        }
    }
}
