package com.amaze.filemanager.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Random;

/**
 * Created by Vishal on 12-05-2015.
 */
public class PreferenceUtils {
    static int primary=-1,accent=-1,folder=-1,theme=-1;
    public static int getStatusColor(String skin) {
        int c=darker(Color.parseColor(skin),0.6f);
        return c;
    }public static int darker (int color, float factor) {
        int a = Color.alpha(color);
        int r = Color.red( color );
        int g = Color.green( color );
        int b = Color.blue( color );

        return Color.argb( a,
                Math.max( (int)(r * factor), 0 ),
                Math.max( (int)(g * factor), 0 ),
                Math.max( (int)(b * factor), 0 ) );
    }
    public static String random(SharedPreferences Sp) {


        Random random = new Random();
        int[] pos =combinations[ random.nextInt(combinations.length - 1)];
        int primary=pos[0],accent=pos[1],icon=pos[2];
        Sp.edit().putInt("skin_color_position", primary).commit();
        Sp.edit().putInt("fab_skin_color_position", accent).commit();
        Sp.edit().putInt("icon_skin_color_position", icon).commit();
        return colors[primary];
    }
    public static int getAccent(SharedPreferences Sp){
        if(accent==-1)
        accent=Sp.getInt("fab_skin_color_position", 1);
        return accent;
    }
    public static int getPrimaryColor(SharedPreferences Sp){
        if(primary==-1)
        primary=Sp.getInt("skin_color_position", 4);
        return primary;
    }
    public static int getFolderColor(SharedPreferences Sp){
        if(folder==-1) {
            int icon = Sp.getInt("icon_skin_color_position", -1);
            folder = icon == -1 ? Sp.getInt("skin_color_position", 4) : icon;
        }
        return folder;
    }
    public static String getPrimaryColorString(SharedPreferences Sp) {
        return (colors[getPrimaryColor(Sp)]);
    }
    public static String getFolderColorString(SharedPreferences Sp) {
        return (colors[getFolderColor(Sp)]);
    }
    public static String getAccentString(SharedPreferences Sp) {
        return (colors[getAccent(Sp)]);
    }
    public static int getTheme(SharedPreferences Sp){
        if(theme==-1){
            int th = Integer.parseInt(Sp.getString("theme", "0"));
            theme = th == 2 ? PreferenceUtils.hourOfDay() : th;
        }
        return theme;
    }
    public static void reset(){
        primary=accent=folder=theme=-1;
    }
    public static int getPosition(String Sp){
        int i=-1;
        for(String s:colors){
            i++;
            if(s.equals(Sp))return i;
        }
        return -1;
    }
    public final static int[][] combinations=new int[][]{
            //primary,accent,folder
            {14,11,12},
            {4,1,4},
            {8,12,8},
            {17,11,12},
            {3,1,3},
            {16,14,16},
            {1,12,1},
            {16,0,16},
            {0,12,0},
            {6,1,6},
            {7,1,7}
    };


    public static float[] calculatevalues(String color) {
        int c = Color.parseColor(color);
        float r = (float) Color.red(c) / 255;
        float g = (float) Color.green(c) / 255;
        float b = (float) Color.blue(c) / 255;
        return new float[]{r, g, b};
    }

    public static float[] calculatefilter(float[] values) {
        float[] src = {

                values[0], 0, 0, 0, 0,
                0, values[1], 0, 0, 0,
                0, 0, values[2], 0, 0,
                0, 0, 0, 1, 0
        };
        return src;
    }
    public static String getSelectionColor(String skin) {

        String[] colors = new String[]{
                "#F44336", "#74e84e40",
                "#e91e63", "#74ec407a",
                "#9c27b0", "#74ab47bc",
                "#673ab7", "#747e57c2",
                "#3f51b5", "#745c6bc0",
                "#2196F3", "#74738ffe",
                "#03A9F4", "#7429b6f6",
                "#00BCD4", "#7426c6da",
                "#009688", "#7426a69a",
                "#4CAF50", "#742baf2b",
                "#8bc34a", "#749ccc65",
                "#FFC107", "#74ffca28",
                "#FF9800", "#74ffa726",
                "#FF5722", "#74ff7043",
                "#795548", "#748d6e63",
                "#212121", "#79bdbdbd",
                "#607d8b", "#7478909c",
                "#004d40", "#740E5D50"
        };
        return colors[Arrays.asList(colors).indexOf(skin) + 1];
    }
        public final static String[] colors = new String[]{
                "#F44336",
                "#e91e63",
                "#9c27b0",
                "#673ab7",
                "#3f51b5",
                "#2196F3",
                "#03A9F4",
                "#00BCD4",
                "#009688",
                "#4CAF50",
                "#8bc34a",
                "#FFC107",
                "#FF9800",
                "#FF5722",
                "#795548",
                "#212121",
                "#607d8b",
                "#004d40"
        };
        public static final String LICENCE_TERMS = "<html><body>" +
            "<h3>Notices for files:</h3>" +
            "<ul><li>nineoldandroids-2.4.0.jar</ul></li>" +	//nineoldandroids
            "<p style = 'background-color:#eeeeee;padding-left:1em'><code>" +
            "<br>/*<br>" +
            "&nbsp;* Copyright 2012 Jake Wharton<br>" +
            "&nbsp;* <br>" +
            "&nbsp;* Licensed under the Apache License, Version 2.0 (the \"License\");<br>" +
            "&nbsp;* you may not use this file except in compliance with the License.<br>" +
            "&nbsp;* You may obtain a copy of the License at<br>" +
            "&nbsp;* <br>" +
            "&nbsp;* &nbsp;&nbsp;&nbsp;http://www.apache.org/licenses/LICENSE-2.0<br>" +
            "&nbsp;* <br>" +
            "&nbsp;* Unless required by applicable law or agreed to in writing, software<br>" +
            "&nbsp;* distributed under the License is distributed on an \"AS IS\" BASIS,<br>" +
            "&nbsp;* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.<br>" +
            "&nbsp;* See the License for the specific language governing permissions and<br>" +
            "&nbsp;* limitations under the License.<br>" +
            "&nbsp;*/ " +
            "<br><br></code></p>" +
            "<h3>Notices for files:</h3> " +
            "<ul><li>RootTools.jar</ul></li>" +	//RootTools
            "<p style = 'background-color:#eeeeee;padding-left:1em'><code>" +
            "<br>/*<br>" +
            "&nbsp;* This file is part of the RootTools Project: http://code.google.com/p/roottools/<br>" +
            "&nbsp;*<br>" +
            "&nbsp;* Copyright (c) 2012 Stephen Erickson, Chris Ravenscroft, Dominik Schuermann, Adam Shanks<br>" +
            "&nbsp;*<br>" +
            "&nbsp;* This code is dual-licensed under the terms of the Apache License Version 2.0 and<br>" +
            "&nbsp;* the terms of the General Public License (GPL) Version 2.<br>" +
            "&nbsp;* You may use this code according to either of these licenses as is most appropriate<br>" +
            "&nbsp;* for your project on a case-by-case basis.<br>" +
            "&nbsp;*<br>" +
            "&nbsp;* The terms of each license can be found in the root directory of this project's repository as well as at:<br>" +
            "&nbsp;*<br>" +
            "&nbsp;* &nbsp;&nbsp;&nbsp;http://www.apache.org/licenses/LICENSE-2.0<br>" +
            "&nbsp;* &nbsp;&nbsp;&nbsp;http://www.gnu.org/licenses/gpl-2.0.txt<br>" +
            "&nbsp;*<br>" +
            "&nbsp;* Unless required by applicable law or agreed to in writing, software<br>" +
            "&nbsp;* distributed under these Licenses is distributed on an \"AS IS\" BASIS,<br>" +
            "&nbsp;* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.<br>" +
            "&nbsp;* See each License for the specific language governing permissions and<br>" +
            "&nbsp;* limitations under that License.<br>" +
            "&nbsp;*/ " +
            "<br><br></code></p>" +
            "<h3>Notices for libraries:</h3> " +
            "<ul><li>CircularImageView</ul></li>" +	//CircularImageView
            "<p style = 'background-color:#eeeeee;padding-left:1em'><code>" +
            "<br>/*<br>" +
            "&nbsp;* The MIT License (MIT)<br>" +
            "&nbsp;*<br>" +
            "&nbsp;* Copyright (c) 2014 Pkmmte Xeleon<br>" +
            "&nbsp;*<br>" +
            "&nbsp;* Permission is hereby granted, free of charge, to any person obtaining a copy<br>" +
            "&nbsp;* of this software and associated documentation files (the \"Software\"), to deal<br>" +
            "&nbsp;* in the Software without restriction, including without limitation the rights<br>" +
            "&nbsp;* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell<br>" +
            "&nbsp;* copies of the Software, and to permit persons to whom the Software is<br>" +
            "&nbsp;* furnished to do so, subject to the following conditions:" +
            "&nbsp;*<br>" +
            "&nbsp;* The above copyright notice and this permission notice shall be included in<br>" +
            "&nbsp;* all copies or substantial portions of the Software.<br>" +
            "&nbsp;* THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR<br>" +
            "&nbsp;* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,<br>" +
            "&nbsp;* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE<br>" +
            "&nbsp;* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER<br>" +
            "&nbsp;* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,<br>" +
            "&nbsp;* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN<br>" +
            "&nbsp;* THE SOFTWARE.<br>" +
            "&nbsp;*/ " +
            "<br><br></code></p>" +
            "<h3>Notices for libraries:</h3> " +
            "<ul><li>FloatingActionButton</ul></li>" +	//FloatingActionBar
            "<p style = 'background-color:#eeeeee;padding-left:1em'><code>" +
            "<br>/*<br>" +
            "&nbsp;* The MIT License (MIT)<br>" +
            "&nbsp;*<br>" +
            "&nbsp;* Copyright (c) 2014 Oleksandr Melnykov<br>" +
            "&nbsp;*<br>" +
            "&nbsp;* Permission is hereby granted, free of charge, to any person obtaining a copy<br>" +
            "&nbsp;* of this software and associated documentation files (the \"Software\"), to deal<br>" +
            "&nbsp;* in the Software without restriction, including without limitation the rights<br>" +
            "&nbsp;* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell<br>" +
            "&nbsp;* copies of the Software, and to permit persons to whom the Software is<br>" +
            "&nbsp;* furnished to do so, subject to the following conditions:" +
            "&nbsp;*<br>" +
            "&nbsp;* The above copyright notice and this permission notice shall be included in<br>" +
            "&nbsp;* all copies or substantial portions of the Software.<br>" +
            "&nbsp;* THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR<br>" +
            "&nbsp;* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,<br>" +
            "&nbsp;* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE<br>" +
            "&nbsp;* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER<br>" +
            "&nbsp;* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,<br>" +
            "&nbsp;* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN<br>" +
            "&nbsp;* THE SOFTWARE.<br>" +
            "&nbsp;*/ " +
            "<br><br></code></p>" +
            "<h3>Notices for libraries:</h3>" +
            "<ul><li>Android System Bar Tint</ul></li>" +	// SystemBar tint
            "<p style = 'background-color:#eeeeee;padding-left:1em'><code>" +
            "<br>/*<br>" +
            "&nbsp;* Copyright 2013 readyState Software Limited<br>" +
            "&nbsp;* <br>" +
            "&nbsp;* Licensed under the Apache License, Version 2.0 (the \"License\");<br>" +
            "&nbsp;* you may not use this file except in compliance with the License.<br>" +
            "&nbsp;* You may obtain a copy of the License at<br>" +
            "&nbsp;* <br>" +
            "&nbsp;* &nbsp;&nbsp;&nbsp;http://www.apache.org/licenses/LICENSE-2.0<br>" +
            "&nbsp;* <br>" +
            "&nbsp;* Unless required by applicable law or agreed to in writing, software<br>" +
            "&nbsp;* distributed under the License is distributed on an \"AS IS\" BASIS,<br>" +
            "&nbsp;* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.<br>" +
            "&nbsp;* See the License for the specific language governing permissions and<br>" +
            "&nbsp;* limitations under the License.<br>" +
            "&nbsp;*/ " +
            "<br><br></code></p>" +
            "<h3>Notices for libraries:</h3> " +
            "<ul><li>Material Dialogs</ul></li>" +	//Material Dialogs
            "<p style = 'background-color:#eeeeee;padding-left:1em'><code>" +
            "<br>/*<br>" +
            "&nbsp;* The MIT License (MIT)<br>" +
            "&nbsp;*<br>" +
            "&nbsp;* Copyright (c) 2014 Aidan Michael Follestad<br>" +
            "&nbsp;*<br>" +
            "&nbsp;* Permission is hereby granted, free of charge, to any person obtaining a copy<br>" +
            "&nbsp;* of this software and associated documentation files (the \"Software\"), to deal<br>" +
            "&nbsp;* in the Software without restriction, including without limitation the rights<br>" +
            "&nbsp;* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell<br>" +
            "&nbsp;* copies of the Software, and to permit persons to whom the Software is<br>" +
            "&nbsp;* furnished to do so, subject to the following conditions:" +
            "&nbsp;*<br>" +
            "&nbsp;* The above copyright notice and this permission notice shall be included in<br>" +
            "&nbsp;* all copies or substantial portions of the Software.<br>" +
            "&nbsp;* THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR<br>" +
            "&nbsp;* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,<br>" +
            "&nbsp;* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE<br>" +
            "&nbsp;* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER<br>" +
            "&nbsp;* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,<br>" +
            "&nbsp;* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN<br>" +
            "&nbsp;* THE SOFTWARE.<br>" +
            "&nbsp;*/ " +
            "<br><br></code></p>" +
            "<h3>Notices for libraries:</h3>" +
            "<ul><li>UnRAR</ul></li>" +	// junRar
            "<p style = 'background-color:#eeeeee;padding-left:1em'><code>" +
            "<br>/*<br>" +
            "&nbsp;* UnRAR - free utility for RAR archives<br>" +
            "&nbsp;* License for use and distribution of<br>" +
            "&nbsp;* FREE portable version<br>" +
            "&nbsp;*/ " +
            "<br><br>" +
            "https://raw.githubusercontent.com/junrar/junrar/master/license.txt" +
            "<br><br></code></p>" +
            "<h3>Notices for libraries:</h3>" +
            "<ul><li>commons-compress</ul></li>" +	// commons-compress
            "<p style = 'background-color:#eeeeee;padding-left:1em'><code>" +
            "<br>/*<br>" +
            "&nbsp;* Copyright [yyyy] [name of copyright owner]<br>" +
            "&nbsp;* <br>" +
            "&nbsp;* Licensed under the Apache License, Version 2.0 (the \"License\");<br>" +
            "&nbsp;* you may not use this file except in compliance with the License.<br>" +
            "&nbsp;* You may obtain a copy of the License at<br>" +
            "&nbsp;* <br>" +
            "&nbsp;* &nbsp;&nbsp;&nbsp;http://www.apache.org/licenses/LICENSE-2.0<br>" +
            "&nbsp;* <br>" +
            "&nbsp;* Unless required by applicable law or agreed to in writing, software<br>" +
            "&nbsp;* distributed under the License is distributed on an \"AS IS\" BASIS,<br>" +
            "&nbsp;* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.<br>" +
            "&nbsp;* See the License for the specific language governing permissions and<br>" +
            "&nbsp;* limitations under the License.<br>" +
            "&nbsp;*/ " +
            "<br><br></code></p>" +
            "<h3>Notices for libraries:</h3>" +
            "<ul><li>sticky-headers-recyclerview</ul></li>" +	// stickyHeadersRecyclerView
            "<p style = 'background-color:#eeeeee;padding-left:1em'><code>" +
            "<br>/*<br>" +
            "&nbsp;* Copyright 2014 Jacob Tabak - Timehop<br>" +
            "&nbsp;* <br>" +
            "&nbsp;* Licensed under the Apache License, Version 2.0 (the \"License\");<br>" +
            "&nbsp;* you may not use this file except in compliance with the License.<br>" +
            "&nbsp;* You may obtain a copy of the License at<br>" +
            "&nbsp;* <br>" +
            "&nbsp;* &nbsp;&nbsp;&nbsp;http://www.apache.org/licenses/LICENSE-2.0<br>" +
            "&nbsp;* <br>" +
            "&nbsp;* Unless required by applicable law or agreed to in writing, software<br>" +
            "&nbsp;* distributed under the License is distributed on an \"AS IS\" BASIS,<br>" +
            "&nbsp;* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.<br>" +
            "&nbsp;* See the License for the specific language governing permissions and<br>" +
            "&nbsp;* limitations under the License.<br>" +
            "&nbsp;*/ " +
            "<br><br></code></p>" +
            "<h3>Notices for libraries:</h3>" +
            "<ul><li>Universal Image Loader</ul></li>" +	// universalImageLoader
            "<p style = 'background-color:#eeeeee;padding-left:1em'><code>" +
            "<br>/*<br>" +
            "&nbsp;* Copyright 2011-2015 Sergey Tarasevich<br>" +
            "&nbsp;* <br>" +
            "&nbsp;* Licensed under the Apache License, Version 2.0 (the \"License\");<br>" +
            "&nbsp;* you may not use this file except in compliance with the License.<br>" +
            "&nbsp;* You may obtain a copy of the License at<br>" +
            "&nbsp;* <br>" +
            "&nbsp;* &nbsp;&nbsp;&nbsp;http://www.apache.org/licenses/LICENSE-2.0<br>" +
            "&nbsp;* <br>" +
            "&nbsp;* Unless required by applicable law or agreed to in writing, software<br>" +
            "&nbsp;* distributed under the License is distributed on an \"AS IS\" BASIS,<br>" +
            "&nbsp;* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.<br>" +
            "&nbsp;* See the License for the specific language governing permissions and<br>" +
            "&nbsp;* limitations under the License.<br>" +
            "&nbsp;*/ " +
            "<br><br></code></p>" +
            "</body></html>";

    public static int hourOfDay() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        if (hour <= 6 || hour >= 18) {
            return 1;
        } else
            return 0;
    }
}
