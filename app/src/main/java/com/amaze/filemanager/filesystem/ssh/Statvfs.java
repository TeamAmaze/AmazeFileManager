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

package com.amaze.filemanager.filesystem.ssh;

import java.math.BigInteger;

import net.schmizz.sshj.common.Buffer;
import net.schmizz.sshj.sftp.PacketType;
import net.schmizz.sshj.sftp.Request;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.sftp.SFTPException;

/**
 * Wrapper for SSH <code>statvfs@openssh.com</code> request/response.
 *
 * <p>This is not specified in official SSH protocol; it is implemented by OpenSSH (which is the
 * most used SSH server implementation around.
 */
public class Statvfs {
  private static final String STATVFS_OPENSSH_COM = "statvfs@openssh.com";

  /**
   * Convenience method for creating <code>statvfs@openssh.com</code> {@link Request}s.
   *
   * @param sftpClient {@link SFTPClient} instance
   * @param path remote path
   * @return {@link Request}
   */
  public static final Request request(final SFTPClient sftpClient, final String path) {
    return sftpClient
        .getSFTPEngine()
        .newRequest(PacketType.EXTENDED)
        .putString(STATVFS_OPENSSH_COM)
        .putString(path, sftpClient.getSFTPEngine().getSubsystem().getRemoteCharset());
  }

  /**
   * Wrapper object for {@link net.schmizz.sshj.sftp.Response}.
   *
   * <p>PROTOCOL specification defines the packet structure as <code>
   * uint64          f_bsize         // file system block size
   * uint64          f_frsize        // fundamental fs block size
   * uint64          f_blocks        // number of blocks (unit f_frsize)
   * uint64          f_bfree         // free blocks in file system
   * uint64          f_bavail        // free blocks for non-root
   * uint64          f_files         // total file inodes
   * uint64          f_ffree         // free file inodes
   * uint64          f_favail        // free file inodes for to non-root
   * uint64          f_fsid          // file system id
   * uint64          f_flag          // bit mask of f_flag values
   * uint64          f_namemax       // maximum filename length
   * </code> whereas <code>f_flag</code> is defined as <code>
   * #define SSH_FXE_STATVFS_ST_RDONLY       0x1     // read-only
   * #define SSH_FXE_STATVFS_ST_NOSUID       0x2     // no setuid
   * </code>
   *
   * @see <a
   *     href="https://cvsweb.openbsd.org/cgi-bin/cvsweb/src/usr.bin/ssh/PROTOCOL?annotate=HEAD">PROTOCOL
   *     specification</a>
   */
  public static class Response {
    public final String remotePath;

    public final net.schmizz.sshj.sftp.Response response;

    // f_bsize
    public final int fileSystemBlockSize;

    // f_frsize
    public final int fundamentalFileSystemBlockSize;

    // f_blocks
    public final long fileSystemBlocks;

    // f_bfree
    public final long freeFileSystemBlocks;

    // f_bavail
    public final long availableFileSystemBlocks;

    // f_files
    public final long totalFileInodes;

    // f_ffree
    public final long freeFileInodes;

    // f_favail
    public final long availableFileInodes;

    // f_fsid
    private final long fileSystemId;

    // f_flag
    public final int fileSystemFlag;

    // f_namemax
    public final int filenameMaxLength;

    public Response(String remotePath, net.schmizz.sshj.sftp.Response response)
        throws SFTPException, Buffer.BufferException {
      response.ensurePacketTypeIs(PacketType.EXTENDED_REPLY);

      if (!response.readStatusCode().equals(net.schmizz.sshj.sftp.Response.StatusCode.OK)) {
        throw new SFTPException("Bad response code: " + response.readStatusCode());
      }

      this.remotePath = remotePath;
      this.response = response;

      fileSystemBlockSize = (int) this.response.readUInt32();
      fundamentalFileSystemBlockSize = (int) this.response.readUInt64();
      fileSystemBlocks = this.response.readUInt64();
      freeFileSystemBlocks = this.response.readUInt64();
      availableFileSystemBlocks = this.response.readUInt64();
      totalFileInodes = this.response.readUInt64();
      freeFileInodes = this.response.readUInt64();
      availableFileInodes = this.response.readUInt64();
      fileSystemId = readUInt64FromBuffer(this.response);
      fileSystemFlag = (int) this.response.readUInt64();
      filenameMaxLength = (int) this.response.readUInt64();
    }

    /**
     * Return disk size (filesystem block size * total filesystem blocks).
     *
     * <p>Depending on the target SSH server implementation, the result may be the disk size of the
     * physical disk where the queried path resides at.
     *
     * @return disk size in bytes
     */
    public long diskSize() {
      return fileSystemBlocks * fileSystemBlockSize;
    }

    /**
     * Return disk free space (filesystem block size * available filesystem blocks).
     *
     * <p>Depending on the target SSH server implementation, the result may be the disk free space
     * of the physical disk where the queried path resides at.
     *
     * @return disk free space in bytes
     */
    public long diskFreeSpace() {
      return availableFileSystemBlocks * fileSystemBlockSize;
    }

    /**
     * Returns fileSystemId wrapped in {@link BigInteger}.
     *
     * @return {@link BigInteger} version of fileSystemId
     */
    public BigInteger getFileSystemId() {
      return BigInteger.valueOf(fileSystemId);
    }

    /**
     * Returns fileSystemId as is.
     *
     * @return fileSystemId
     */
    public long getRawFileSystemId() {
      return fileSystemId;
    }

    /**
     * Read an uint64 from the buffer. This workarounds sshj's original method to prevent exception
     * thrown which calls for {@link BigInteger} to store negative {@link Long}s.
     *
     * <p>Many thanks to Alexander--@github for the comments.
     *
     * @see Buffer#readUInt64()
     */
    private long readUInt64FromBuffer(Buffer buffer) throws Buffer.BufferException {
      long uint64 = (buffer.readUInt32() << 32) + (buffer.readUInt32() & 0xffffffffL);
      return uint64;
    }

    @Override
    public String toString() {
      return new StringBuilder()
          .append("Response statvfs@openssh.com query for [")
          .append(remotePath)
          .append("], ")
          .append("fileSystemBlockSize=")
          .append(fileSystemBlockSize)
          .append(',')
          .append("fundamentalFileSystemBlockSize=")
          .append(fundamentalFileSystemBlockSize)
          .append(',')
          .append("fileSystemBlocks=")
          .append(fileSystemBlocks)
          .append(',')
          .append("freeFileSystemBlocks=")
          .append(freeFileSystemBlocks)
          .append(',')
          .append("availableFileSystemBlocks=")
          .append(availableFileSystemBlocks)
          .append(',')
          .append("totalFileInodes=")
          .append(totalFileInodes)
          .append(',')
          .append("freeFileInodes=")
          .append(freeFileInodes)
          .append(',')
          .append("availableFileInodes=")
          .append(availableFileInodes)
          .append(',')
          .append("fileSystemId=")
          .append(getFileSystemId())
          .append(',')
          .append("fileSystemFlag=")
          .append(fileSystemFlag)
          .append(',')
          .append("filenameMaxLength=")
          .append(filenameMaxLength)
          .toString();
    }
  }
}
