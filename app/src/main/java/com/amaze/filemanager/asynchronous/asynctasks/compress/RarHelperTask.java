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

package com.amaze.filemanager.asynchronous.asynctasks.compress;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.compress.archivers.ArchiveException;

import com.amaze.filemanager.adapters.data.CompressedObjectParcelable;
import com.amaze.filemanager.asynchronous.asynctasks.AsyncTaskResult;
import com.amaze.filemanager.filesystem.compressed.CompressedHelper;
import com.amaze.filemanager.filesystem.compressed.showcontents.helpers.RarDecompressor;
import com.amaze.filemanager.utils.OnAsyncTaskFinished;
import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.exception.UnsupportedRarV5Exception;
import com.github.junrar.rarfile.FileHeader;
import com.github.junrar.rarfile.MainHeader;

import androidx.annotation.NonNull;

public class RarHelperTask extends CompressedHelperTask {

  private String fileLocation;
  private String relativeDirectory;

  /**
   * AsyncTask to load RAR file items.
   *
   * @param realFileDirectory the location of the zip file
   * @param dir relativeDirectory to access inside the zip file
   */
  public RarHelperTask(
      String realFileDirectory,
      String dir,
      boolean goBack,
      OnAsyncTaskFinished<AsyncTaskResult<ArrayList<CompressedObjectParcelable>>> l) {
    super(goBack, l);
    fileLocation = realFileDirectory;
    relativeDirectory = dir;
  }

  @Override
  void addElements(@NonNull ArrayList<CompressedObjectParcelable> elements)
      throws ArchiveException {
    try {
      Archive zipfile = getArchive(fileLocation);
      String relativeDirDiffSeparator = relativeDirectory.replace(CompressedHelper.SEPARATOR, "\\");

      for (FileHeader rarArchive : zipfile.getFileHeaders()) {
        String name = rarArchive.getFileNameString(); // This uses \ as separator, not /
        if (!CompressedHelper.isEntryPathValid(name)) {
          continue;
        }
        boolean isInBaseDir =
            (relativeDirDiffSeparator == null || relativeDirDiffSeparator.equals(""))
                && !name.contains("\\");
        boolean isInRelativeDir =
            relativeDirDiffSeparator != null
                && name.contains("\\")
                && name.substring(0, name.lastIndexOf("\\")).equals(relativeDirDiffSeparator);

        if (isInBaseDir || isInRelativeDir) {
          elements.add(
              new CompressedObjectParcelable(
                  RarDecompressor.convertName(rarArchive),
                  0,
                  rarArchive.getFullUnpackSize(),
                  rarArchive.isDirectory()));
        }
      }
    } catch (UnsupportedRarV5Exception e) {
      throw new ArchiveException("RAR v5 archives are not supported", e);
    } catch (FileNotFoundException e) {
      throw new ArchiveException("First part of multipart archive not found", e);
    } catch (RarException | IOException e) {
      throw new ArchiveException(String.format("RAR archive %s is corrupt", fileLocation));
    }
  }

  public static Archive getArchive(String fileLocation) throws RarException, IOException {
    Archive zipfile = new Archive(new File(fileLocation));
    MainHeader rarHeader = zipfile.getMainHeader();
    if (rarHeader.isMultiVolume() && !rarHeader.isFirstVolume()) {
      File firstPartOfArchive = new File(guessFirstPartOfRar(fileLocation));
      if (firstPartOfArchive.exists()) {
        zipfile = new Archive(firstPartOfArchive);
      } else {
        throw new FileNotFoundException(
            String.format("First part of archive [%s] not found", firstPartOfArchive.getName()));
      }
    }
    return zipfile;
  }

  /**
   * Try to guess the first part of a multipart rar.
   *
   * <ul>
   *   <li>For filenames like file.part5.rar, it will return file.part1.rar
   *   <li>For filenames like file.part003.rar, it will return file.part001.rar
   * </ul>
   *
   * Best effort only, won't be smart enough to detect if part number is zero padded.
   *
   * @param archivePath file path to check
   * @return file path with filename = first part of rar guessed
   */
  public static String guessFirstPartOfRar(@NonNull String archivePath) {
    if (archivePath.lastIndexOf(".part") < 1) {
      return archivePath;
    } else {
      StringBuilder sb = new StringBuilder(archivePath);
      int start = sb.lastIndexOf("part");
      int end = sb.lastIndexOf(".rar");
      if (start > 0 && end > start) {
        for (int i = start + 4; i < end - 1; i++) sb.setCharAt(i, '0');

        sb.setCharAt(end - 1, '1');
      }
      return sb.toString();
    }
  }
}
