package com.turn.ttorrent;

import org.apache.commons.io.FileUtils;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Iterator;

/**
 * Created by yuuki asuna on 2017/7/8.
 */
public class HardlinkBakTest {

    final String sourceDirectory = "E:\\0";

    final String targetDirectory = "E:\\.hardlink.bak";

    @Test
    public void hardlinkBak() throws Exception {
        String sourcePath = new File(sourceDirectory).getCanonicalPath();
        String targetPath =  new File(targetDirectory).getCanonicalPath();
        Collection<File> files = FileUtils.listFiles(new File(sourcePath), null, true);
        Iterator<File> iterator = files.iterator();
        while (iterator.hasNext()) {
            File f = iterator.next();
            String targetLinkPath = targetPath + File.separator + f.getCanonicalPath().replace(":", "");
            File targetLinkFile = new File(targetLinkPath);
            //
            FileUtils.forceMkdirParent(targetLinkFile);
            Files.createLink(targetLinkFile.toPath(), f.toPath());
        }
    }


}


