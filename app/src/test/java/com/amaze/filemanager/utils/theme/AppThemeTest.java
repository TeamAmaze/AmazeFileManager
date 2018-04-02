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
    public void getThemeLightTest() throws Exception {
        AppTheme apptheme = AppTheme.getTheme(AppTheme.LIGHT_INDEX);
        assertEquals(AppTheme.LIGHT, apptheme);
    }

    @Test
    public void getThemeDARKTest() throws Exception {
        AppTheme apptheme = AppTheme.getTheme(AppTheme.DARK_INDEX);
        assertEquals(AppTheme.DARK, apptheme);
    }

    @Test
    public void getThemeTIMEDTest() throws Exception {
        AppTheme apptheme = AppTheme.getTheme(AppTheme.TIME_INDEX);
        assertEquals(AppTheme.TIMED, apptheme);
    }

    @Test
    public void getThemeBLACKTest() throws Exception {
        AppTheme apptheme = AppTheme.getTheme(AppTheme.BLACK_INDEX);
        assertEquals(AppTheme.BLACK, apptheme);
    }

    @Test
    public void getMaterialDialogThemeLIGHTTest() throws Exception {
        AppTheme apptheme = AppTheme.getTheme(AppTheme.LIGHT_INDEX);
        assertEquals(Theme.LIGHT, apptheme.getMaterialDialogTheme());
    }

    @Test
    public void getMaterialDialogThemeDARKTest() throws Exception {
        AppTheme apptheme = AppTheme.getTheme(AppTheme.DARK_INDEX);
        assertEquals(Theme.DARK, apptheme.getMaterialDialogTheme());
    }

    @Test
    public void getMaterialDialogThemeTIMEDTest() throws Exception {
        AppTheme apptheme = AppTheme.getTheme(AppTheme.TIME_INDEX);
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour <= 6 || hour >= 18) {
            assertEquals(Theme.DARK, apptheme.getMaterialDialogTheme());
        }
        else
            assertEquals(Theme.LIGHT, apptheme.getMaterialDialogTheme());
    }

    @Test
    public void getMaterialDialogThemeBLACKTest() throws Exception {
        AppTheme apptheme = AppTheme.getTheme(AppTheme.BLACK_INDEX);
        assertEquals(Theme.DARK, apptheme.getMaterialDialogTheme());
    }

    @Test
    public void getSimpleThemeLIGHTTest() throws Exception {
        AppTheme apptheme  = AppTheme.getTheme(AppTheme.LIGHT_INDEX);
        assertEquals(AppTheme.LIGHT, apptheme.getSimpleTheme());
    }

    @Test
    public void getSimpleThemeDARKTest() throws Exception {
        AppTheme apptheme  = AppTheme.getTheme(AppTheme.DARK_INDEX);
        assertEquals(AppTheme.DARK, apptheme.getSimpleTheme());
    }

    @Test
    public void getSimpleThemeTIMEDTest() throws Exception {
        AppTheme apptheme  = AppTheme.getTheme(AppTheme.TIME_INDEX);
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour <= 6 || hour >= 18) {
            assertEquals(AppTheme.DARK, apptheme.getSimpleTheme());
        }
        else
            assertEquals(AppTheme.LIGHT, apptheme.getSimpleTheme());
    }

    @Test
    public void getSimpleThemeBLACKTest() throws Exception {
        AppTheme apptheme  = AppTheme.getTheme(AppTheme.BLACK_INDEX);
        assertEquals(AppTheme.BLACK, apptheme.getSimpleTheme());
    }

    @Test
    public void getIdLIGHTTest() throws Exception {
        int index = 0;
        AppTheme apptheme = AppTheme.getTheme(index);
        assertEquals(index , apptheme.getId()) ;
    }

    @Test
    public void getIdDARKTest() throws Exception {
        int index = 1;
        AppTheme apptheme = AppTheme.getTheme(index);
        assertEquals(index , apptheme.getId()) ;
    }

    @Test
    public void getIdTIMEDTest() throws Exception {
        int index = 2;
        AppTheme apptheme = AppTheme.getTheme(index);
        assertEquals(index , apptheme.getId()) ;
    }

    @Test
    public void getIdBLACKTest() throws Exception {
        int index = 3;
        AppTheme apptheme = AppTheme.getTheme(index);
        assertEquals(index , apptheme.getId()) ;
    }
}
