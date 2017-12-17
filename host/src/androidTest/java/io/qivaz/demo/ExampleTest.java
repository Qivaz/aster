package io.qivaz.demo;


import android.support.test.uiautomator.UiAutomatorTestCase;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiScrollable;
import android.support.test.uiautomator.UiSelector;

/**
 *
 */
public class ExampleTest extends UiAutomatorTestCase {
    public void testDemo() throws UiObjectNotFoundException {
        getUiDevice().pressHome();
        // 进入设置菜单  
        UiObject settingApp = new UiObject(new UiSelector().text("Settings"));
        settingApp.click();
        //休眠3秒  
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e1) {
            // TODO Auto-generated catch block  
            e1.printStackTrace();
        }
        // 进入语言和输入法设置  
        UiScrollable settingItems = new UiScrollable(new UiSelector().scrollable(true));

        UiObject languageAndInputItem = settingItems.getChildByText(
                new UiSelector().text("Language & input"), "Language & input", true);
        languageAndInputItem.clickAndWaitForNewWindow();

    }
}