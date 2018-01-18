/*
 * Statvfs.java
 *
 * Copyright © 2018 Raymond Lai <airwave209gt at gmail.com>.
 *
 * This file is part of AmazeFileManager.
 *
 * AmazeFileManager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AmazeFileManager is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AmazeFileManager. If not, see <http ://www.gnu.org/licenses/>.
 */

package com.amaze.filemanager.filesystem.ssh;

import net.schmizz.sshj.common.Buffer;
import net.schmizz.sshj.sftp.PacketType;
import net.schmizz.sshj.sftp.Request;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.sftp.SFTPException;

import java.math.BigInteger;

/**
 * Wrapper for SSH <code>statvfs@openssh.com</code> request/response.
 *
 * This is not specified in official SSH protocol; it is implemented by OpenSSH (which is the most
 * used SSH server implementation around.
 */
public class Statvfs
{
    private static final String STATVFS_OPENSSH_COM = "statvfs@openssh.com";

    /**
     * Convenience method for creating <code>statvfs@openssh.com</code> {@link Request}s.
     *
     * @param sftpClient {@link SFTPClient} instance
     * @param path remote path
     * @return {@link Request}
     */
    public static final Request request(final SFTPClient sftpClient, final String path){
        return sftpClient.getSFTPEngine().newRequest(PacketType.EXTENDED)
                .putString(STATVFS_OPENSSH_COM)
                .putString(path,
                        sftpClient.getSFTPEngine().getSubsystem().getRemoteCharset());
    }

    /**
     * Wrapper object for {@link net.schmizz.sshj.sftp.Response}.
     *
     * PROTOCOL specification defines the packet structure as
     *
     * <code>
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
     * </code>
     *
     * whereas <code>f_flag</code> is defined as
     *
     * <code>
     * #define SSH_FXE_STATVFS_ST_RDONLY       0x1     // read-only
     * #define SSH_FXE_STATVFS_ST_NOSUID       0x2     // no setuid
     * </code>
     *
     * @see <a href="https://cvsweb.openbsd.org/cgi-bin/cvsweb/src/usr.bin/ssh/PROTOCOL?annotate=HEAD">PROTOCOL specification</a>
     */
    public static class Response
    {
        public final net.schmizz.sshj.sftp.Response mResponse;

        /*
            Regarding choice of number types:

            - fileSystemFlag is only 0x0, 0x1 and 0x2
            - filenameMaxLength should be well within Long.VALUE_MAX

            Others, can use BigInteger to prepare for the ever increasing disk size for years to come.
         */

        // f_bsize
        public final Long fileSystemBlockSize;

        // f_frsize
        public final Long fundermentalFileSystemBlockSize;

        // f_blocks
        public final Long fileSystemBlocks;

        // f_bfree
        public final Long freeFileSystemBlocks;

        // f_bavail
        public final Long availableFileSystemBlocks;

        // f_files
        public final Long totalFileInodes;

        // f_ffree
        public final Long freeFileInodes;

        // f_favail
        public final Long availableFileInodes;

        // f_fsid
        public final Long fileSystemId;

        // f_flag
        public final Long fileSystemFlag;

        // f_namemax
        public final Long filenameMaxLength;

        public Response(net.schmizz.sshj.sftp.Response response) throws SFTPException, Buffer.BufferException
        {
            response.ensurePacketTypeIs(PacketType.EXTENDED_REPLY);

            if(!response.readStatusCode().equals(net.schmizz.sshj.sftp.Response.StatusCode.OK)) {
                throw new SFTPException("Bad response code: " + response.readStatusCode());
            }

            mResponse = response;

            fileSystemBlockSize = mResponse.readUInt32();
            fundermentalFileSystemBlockSize = mResponse.readUInt64();
            fileSystemBlocks = mResponse.readUInt64();
            freeFileSystemBlocks = mResponse.readUInt64();
            availableFileSystemBlocks = mResponse.readUInt64();
            totalFileInodes = mResponse.readUInt64();
            freeFileInodes = mResponse.readUInt64();
            availableFileInodes = mResponse.readUInt64();
            fileSystemId = mResponse.readUInt64();
            fileSystemFlag = mResponse.readUInt64();
            filenameMaxLength = mResponse.readUInt64();
        }

        /**
         * Return disk size (filesystem block size * total filesystem blocks).
         *
         * Depending on the target SSH server implementation, the result may be the disk size of the
         * physical disk where the queried path resides at.
         *
         * @return disk size in bytes
         */
        public Long diskSize()
        {
            return fileSystemBlocks * fileSystemBlockSize;
        }

        /**
         * Return disk free space (filesystem block size * available filesystem blocks).
         *
         * Depending on the target SSH server implementation, the result may be the disk free space
         * of the physical disk where the queried path resides at.
         *
         * @return disk free space in bytes
         */
        public Long diskFreeSpace()
        {
            return availableFileSystemBlocks * fileSystemBlockSize;
        }
    }
}
