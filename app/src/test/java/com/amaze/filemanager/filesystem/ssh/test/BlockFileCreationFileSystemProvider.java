package com.amaze.filemanager.filesystem.ssh.test;

import org.apache.sshd.common.file.root.RootedFileSystem;
import org.apache.sshd.common.file.root.RootedFileSystemProvider;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystemException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.Set;

public class BlockFileCreationFileSystemProvider extends RootedFileSystemProvider
{
    /**
     * Read only.
     * @param path
     * @param options
     * @param attrs
     * @return
     * @throws IOException
     */
    @Override
    public FileChannel newFileChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        Path r = unroot(path);
        FileSystemProvider p = provider(r);
        return p.newFileChannel(r, Collections.singleton(StandardOpenOption.READ), attrs);
    }

    @Override
    public OutputStream newOutputStream(Path path, OpenOption... options) throws IOException {
        throw new FileSystemException("Unsupported operation");
    }
}
