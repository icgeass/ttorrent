package com.turn.ttorrent.validate;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by yuuki asuna on 2017/7/4.
 */
public class HardLinkToSeedStructure {


    private final static String hardlinkToFolder = ".hardlink.uTorrent";

    private final static ThreadLocal<Long> sizeOfAll = new ThreadLocal<Long>();

    private final static ThreadLocal<Long> numOfAll = new ThreadLocal<Long>();


    public static void createHardLink(String rootPath) throws Exception {
        System.out.println("==========================生成Seed目录开始==========================");
        String rootFileCanonicalPath = DirectoryValidator.validateRootPath(rootPath);
        File rootFile = new File(rootFileCanonicalPath);
        if (!rootFile.exists()) {
            rootFile.mkdir();
        }
        Runtime.getRuntime().exec("attrib +R +S +H \"" + rootFileCanonicalPath + hardlinkToFolder + "\"");

        list(rootFileCanonicalPath, new File(rootPath), new AtomicInteger(0), 2);
        System.out.println("文件总计：" + numOfAll.get() + "，文件大小：" + sizeOfAll.get());
        System.out.println("==========================生成Seed目录结束==========================");

    }

    /**
     * 将该levelTo的目录保持文件夹结构硬链接到指定目录（hardlinkToFolder）
     * 0（含）-levelTo（含）必须全是目录
     *
     * @param rootFileCanonicalPath
     * @param source
     * @param level
     * @param levelTo
     * @throws Exception
     */
    private static void list(String rootFileCanonicalPath, File source, AtomicInteger level, int levelTo) throws Exception {
        if (null == source) {
            throw new RuntimeException("目录不能为空, " + source);
        }
        level.addAndGet(1);
        for (File file : source.listFiles()) {
            if (!file.isDirectory()) {
                throw new RuntimeException("必须为目录, " + level);
            }
            List<String> ignoreUnderRootList = DirectoryValidator.genIgnorePathList(rootFileCanonicalPath);
            if (level.get() == 1) {
                if (ignoreUnderRootList.contains(rootFileCanonicalPath + file.getName())) {
                    continue;
                }
            }
            if (level.get() < levelTo) {
                list(rootFileCanonicalPath, file, level, levelTo);
            } else if (level.get() == levelTo) {
                Collection<File> toHardLinkFiles = FileUtils.listFiles(file, null, true);
                Iterator<File> iterator = toHardLinkFiles.iterator();
                while (iterator.hasNext()) {
                    File toHardLinkFile = iterator.next();
                    String targetLinkPath = rootFileCanonicalPath + hardlinkToFolder + File.separator + toHardLinkFile.getCanonicalPath().replace(file.getParentFile().getCanonicalPath() + File.separator, "");
                    File targetLinkFile = new File(targetLinkPath);
                    if (null == sizeOfAll.get()) {
                        sizeOfAll.set(0L);
                    }
                    if (null == numOfAll.get()) {
                        numOfAll.set(0L);
                    }
                    sizeOfAll.set(sizeOfAll.get() + toHardLinkFile.length());
                    numOfAll.set(numOfAll.get() + 1);
                    if (!targetLinkFile.exists()) {
                        FileUtils.forceMkdirParent(targetLinkFile);
                        Files.createLink(targetLinkFile.toPath(), toHardLinkFile.toPath());
                    }
                }
            } else {
                break;
            }
        }
        level.addAndGet(-1);
    }


}
