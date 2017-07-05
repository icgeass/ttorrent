package com.turn.ttorrent;

import org.apache.commons.io.FileUtils;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by yuuki asuna on 2017/7/4.
 */
public class HardLinkTest {

    final String root = "E:\\0";


    final String suffix = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
    final String hardlinkRoot = "E:\\.hardlink" + "." + suffix;
    @Test
    public void createHardLink() throws Exception {
        File hardlinkRootFile = new File(hardlinkRoot);
        if (!hardlinkRootFile.getName().matches("\\.hardlink\\.\\d{14}")) {
            throw new RuntimeException("must be .hardlink");
        }
        if(hardlinkRootFile.exists()){
            throw new RuntimeException("hardlinkRoot already exists, " + hardlinkRootFile.getName());
        }
        if(!hardlinkRootFile.mkdir()){
            throw new RuntimeException("hardlinkRoot can not be created");
        }
        list(new File(root), new AtomicInteger(0));

    }

    private void list(File source, AtomicInteger level) throws Exception {
        if (null == source) {
            throw new RuntimeException("目录不能为空, " + source);
        }
        level.addAndGet(1);
        for (File file : source.listFiles()) {
            if (!file.isDirectory()) {
                throw new RuntimeException("must be directory, " + level);
            }
            if (level.get() == 1) {
                list(file, level);
            } else if (level.get() == 2) {
                Collection<File> toHardLinkFiles = FileUtils.listFiles(file, null, true);
                Iterator<File> iterator = toHardLinkFiles.iterator();
                while (iterator.hasNext()) {
                    File toHardLinkFile = iterator.next();
                    String targetLinkPath = hardlinkRoot + File.separator + toHardLinkFile.getCanonicalPath().replace(file.getParentFile().getCanonicalPath() + File.separator, "");
                    File targetLinkFile = new File(targetLinkPath);
                    FileUtils.forceMkdirParent(targetLinkFile);
                    Files.createLink(targetLinkFile.toPath(), toHardLinkFile.toPath());
                }
            } else {
                break;
            }
        }
        level.addAndGet(-1);
    }


}
