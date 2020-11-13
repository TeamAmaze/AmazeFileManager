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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import org.junit.BeforeClass;
import org.junit.Test;

import androidx.annotation.NonNull;

import jcifs.Config;
import jcifs.ResolverType;
import jcifs.context.BaseContext;

public class CifsContextFactoryTest {

  @BeforeClass
  public static void bootstrap() {
    Config.registerSmbURLHandler();
  }

  @Test
  public void testCreateUsingEmptyPropeties() {
    BaseContext ctx = CifsContextFactory.create(new Properties());
    verifyCommonProperties(ctx);
  }

  @Test
  public void testCreateWithNull() {
    BaseContext ctx = CifsContextFactory.create(null);
    verifyCommonProperties(ctx);
  }

  @Test
  public void testCreateUsingCustomProperties() {
    Properties p = new Properties();
    p.setProperty("jcifs.smb.client.ipcSigningEnforced", "false");
    BaseContext ctx = CifsContextFactory.create(p);
    verifyCommonProperties(ctx);
    assertFalse(ctx.getConfig().isIpcSigningEnforced());
  }

  @Test
  public void testRepeatedCreateWithCustomProperties() {
    Properties p = new Properties();
    p.setProperty("jcifs.smb.client.ipcSigningEnforced", "false");
    BaseContext ctx = CifsContextFactory.create(p);
    verifyCommonProperties(ctx);
    assertFalse(ctx.getConfig().isIpcSigningEnforced());

    p = new Properties();
    p.setProperty("jcifs.smb.client.ipcSigningEnforced", "true");
    ctx = CifsContextFactory.create(p);
    verifyCommonProperties(ctx);
    assertTrue(ctx.getConfig().isIpcSigningEnforced());
  }

  private void verifyCommonProperties(@NonNull BaseContext ctx) {
    assertNotNull(ctx);
    assertEquals(ResolverType.RESOLVER_BCAST, ctx.getConfig().getResolveOrder().get(0));
    assertEquals(-1 * 60, ctx.getConfig().getNetbiosCachePolicy());
    assertEquals(30000, ctx.getConfig().getResponseTimeout());
    assertEquals(5000, ctx.getConfig().getNetbiosRetryTimeout());
  }
}
