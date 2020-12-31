/*
 * Copyright (C) 2014-2020 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com> and Contributors.
 *
 * This file is part of Amaze File Manager.
 *
 * Amaze File Manager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.amaze.filemanager.filesystem.smb;

import static android.os.Build.VERSION_CODES.JELLY_BEAN;
import static android.os.Build.VERSION_CODES.KITKAT;
import static android.os.Build.VERSION_CODES.P;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import jcifs.ResolverType;
import jcifs.context.BaseContext;

@Config(sdk = {JELLY_BEAN, KITKAT, P})
@RunWith(AndroidJUnit4.class)
public class CifsContextsTest {

  @BeforeClass
  public static void bootstrap() {
    jcifs.Config.registerSmbURLHandler();
  }

  @After
  public void tearDown() {
    CifsContexts.clearBaseContexts();
  }

  @Test
  public void testCreateUsingEmptyPropeties() {
    BaseContext ctx = CifsContexts.create("smb://user:user@1.2.3.4", new Properties());
    verifyCommonProperties(ctx);
  }

  @Test
  public void testCreateWithNull() {
    BaseContext ctx = CifsContexts.create("smb://user:user@1.2.3.4", null);
    verifyCommonProperties(ctx);
  }

  @Test
  public void testCreateUsingCustomProperties() {
    Properties p = new Properties();
    p.setProperty("jcifs.smb.client.ipcSigningEnforced", "false");
    BaseContext ctx = CifsContexts.create("smb://user:user@1.2.3.4", p);
    verifyCommonProperties(ctx);
    assertFalse(ctx.getConfig().isIpcSigningEnforced());
  }

  @Test
  public void testRepeatedCreateWithDifferentCustomProperties() {
    Properties p = new Properties();
    p.setProperty("jcifs.smb.client.ipcSigningEnforced", "false");
    BaseContext ctx = CifsContexts.create("smb://user:user@1.2.3.4", p);
    verifyCommonProperties(ctx);
    assertFalse(ctx.getConfig().isIpcSigningEnforced());

    CifsContexts.clearBaseContexts();

    p = new Properties();
    p.setProperty("jcifs.smb.client.ipcSigningEnforced", "true");
    ctx = CifsContexts.create("smb://user:user@1.2.3.4", p);
    verifyCommonProperties(ctx);
    assertTrue(ctx.getConfig().isIpcSigningEnforced());
  }

  @Test
  public void testGetBaseContextFromCachedBaseContexts() {
    BaseContext ctx1 = CifsContexts.create("smb://user:user@1.2.3.4", null);
    verifyCommonProperties(ctx1);

    BaseContext ctx2 = CifsContexts.create("smb://user:user@1.2.3.4", null);
    verifyCommonProperties(ctx2);
    assertEquals(ctx1.hashCode(), ctx2.hashCode());

    BaseContext ctx3 = CifsContexts.create("smb://foo:bar@5.6.7.8", null);
    verifyCommonProperties(ctx3);
    assertEquals(ctx1.hashCode(), ctx2.hashCode());
    assertNotEquals(ctx1.hashCode(), ctx3.hashCode());
    assertNotEquals(ctx2.hashCode(), ctx3.hashCode());
  }

  /*
   * CifsContexts doesn't parse query parameters, no use check
   */
  @Test
  public void testGetBaseContextShouldDifferIfSpecifyQueryParameterInPath() {
    BaseContext ctx1 = CifsContexts.create("smb://user:user@1.2.3.4", null);
    verifyCommonProperties(ctx1);

    BaseContext ctx2 = CifsContexts.create("smb://user:user@1.2.3.4?foo=bar", null);
    verifyCommonProperties(ctx2);
    assertNotEquals(ctx1.hashCode(), ctx2.hashCode());

    BaseContext ctx3 = CifsContexts.create("smb://user:user@1.2.3.4?foo=baz", null);
    verifyCommonProperties(ctx3);
    assertNotEquals(ctx1.hashCode(), ctx2.hashCode());
    assertNotEquals(ctx2.hashCode(), ctx3.hashCode());

    BaseContext ctx4 = CifsContexts.create("smb://user:user@1.2.3.4?foo=bar", null);
    verifyCommonProperties(ctx4);
    assertNotEquals(ctx1.hashCode(), ctx4.hashCode());
    assertNotEquals(ctx3.hashCode(), ctx4.hashCode());
    assertEquals(ctx2.hashCode(), ctx4.hashCode());
  }

  private void verifyCommonProperties(@NonNull BaseContext ctx) {
    assertNotNull(ctx);
    assertEquals(ResolverType.RESOLVER_BCAST, ctx.getConfig().getResolveOrder().get(0));
    assertEquals(-1 * 60, ctx.getConfig().getNetbiosCachePolicy());
    assertEquals(30000, ctx.getConfig().getResponseTimeout());
    assertEquals(5000, ctx.getConfig().getNetbiosRetryTimeout());
  }
}
