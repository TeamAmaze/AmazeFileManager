package com.amaze.filemanager.utils.theme;

import com.afollestad.materialdialogs.Theme;

import org.junit.Test;
import java.util.Calendar;

import static org.junit.Assert.*;

/**
 * Created by yuhalyn on 2018-04-02.
 */
public class AppThemeTest {

    @Test
    public void getThemeLightTest() {
        AppTheme apptheme = AppTheme.getTheme(AppTheme.LIGHT_INDEX);
        assertEquals(AppTheme.LIGHT, apptheme);
    }

    @Test
    public void getThemeDARKTest() {
        AppTheme apptheme = AppTheme.getTheme(AppTheme.DARK_INDEX);
        assertEquals(AppTheme.DARK, apptheme);
    }

    @Test
    public void getThemeTIMEDTest() {
        AppTheme apptheme = AppTheme.getTheme(AppTheme.TIME_INDEX);
        assertEquals(AppTheme.TIMED, apptheme);
    }

    @Test
    public void getThemeBLACKTest() {
        AppTheme apptheme = AppTheme.getTheme(AppTheme.BLACK_INDEX);
        assertEquals(AppTheme.BLACK, apptheme);
    }

    @Test
    public void getMaterialDialogThemeLIGHTTest() {
        AppTheme apptheme = AppTheme.getTheme(AppTheme.LIGHT_INDEX);
        assertEquals(Theme.LIGHT, apptheme.getMaterialDialogTheme());
    }

    @Test
    public void getMaterialDialogThemeDARKTest() {
        AppTheme apptheme = AppTheme.getTheme(AppTheme.DARK_INDEX);
        assertEquals(Theme.DARK, apptheme.getMaterialDialogTheme());
    }

    @Test
    public void getMaterialDialogThemeTIMEDTest() {
        AppTheme apptheme = AppTheme.getTheme(AppTheme.TIME_INDEX);
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour <= 6 || hour >= 18) {
            assertEquals(Theme.DARK, apptheme.getMaterialDialogTheme());
        }
        else
            assertEquals(Theme.LIGHT, apptheme.getMaterialDialogTheme());
    }

    @Test
    public void getMaterialDialogThemeBLACKTest() {
        AppTheme apptheme = AppTheme.getTheme(AppTheme.BLACK_INDEX);
        assertEquals(Theme.DARK, apptheme.getMaterialDialogTheme());
    }

    @Test
    public void getSimpleThemeLIGHTTest() {
        AppTheme apptheme  = AppTheme.getTheme(AppTheme.LIGHT_INDEX);
        assertEquals(AppTheme.LIGHT, apptheme.getSimpleTheme());
    }

    @Test
    public void getSimpleThemeDARKTest() {
        AppTheme apptheme  = AppTheme.getTheme(AppTheme.DARK_INDEX);
        assertEquals(AppTheme.DARK, apptheme.getSimpleTheme());
    }

    @Test
    public void getSimpleThemeTIMEDTest() {
        AppTheme apptheme  = AppTheme.getTheme(AppTheme.TIME_INDEX);
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour <= 6 || hour >= 18) {
            assertEquals(AppTheme.DARK, apptheme.getSimpleTheme());
        }
        else
            assertEquals(AppTheme.LIGHT, apptheme.getSimpleTheme());
    }

    @Test
    public void getSimpleThemeBLACKTest() {
        AppTheme apptheme  = AppTheme.getTheme(AppTheme.BLACK_INDEX);
        assertEquals(AppTheme.BLACK, apptheme.getSimpleTheme());
    }

    @Test
    public void getIdLIGHTTest() {
        int index = 0;
        AppTheme apptheme = AppTheme.getTheme(index);
        assertEquals(index , apptheme.getId()) ;
    }

    @Test
    public void getIdDARKTest() {
        int index = 1;
        AppTheme apptheme = AppTheme.getTheme(index);
        assertEquals(index , apptheme.getId()) ;
    }

    @Test
    public void getIdTIMEDTest() {
        int index = 2;
        AppTheme apptheme = AppTheme.getTheme(index);
        assertEquals(index , apptheme.getId()) ;
    }

    @Test
    public void getIdBLACKTest() {
        int index = 3;
        AppTheme apptheme = AppTheme.getTheme(index);
        assertEquals(index , apptheme.getId()) ;
    }
}
