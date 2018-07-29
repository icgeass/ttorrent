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


    public final static String HARD_LINK_TO_SEED_STRUCTURE_FOLDER = ".hardlink.uTorrent";

    private final static ThreadLocal<Long> SIZE_OF_PROC_ALL = new ThreadLocal<Long>();

    private final static ThreadLocal<Long> NUM_OF_PROC_ALL = new ThreadLocal<Long>();


    public static String[] createHardLink(String rootPath) throws Exception {
        System.out.println("==========================生成Seed目录开始==========================");

        SIZE_OF_PROC_ALL.set(0L);
        NUM_OF_PROC_ALL.set(0L);

        String rootFileCanonicalPath = DirectoryValidator.validateRootPath(rootPath);

        File seedFolderCanonicalPath = new File(rootFileCanonicalPath + HARD_LINK_TO_SEED_STRUCTURE_FOLDER);
        if (!seedFolderCanonicalPath.exists()) {
            seedFolderCanonicalPath.mkdir();
        }
        Runtime.getRuntime().exec("attrib +R +S +H \"" + seedFolderCanonicalPath.getCanonicalPath() + "\"");

        list(rootFileCanonicalPath, new File(rootFileCanonicalPath), new AtomicInteger(0), 2);

        //
        long sizeOfBakFolder = FileUtils.sizeOfDirectory(seedFolderCanonicalPath);
        int numOfBakFolder = FileUtils.listFiles(seedFolderCanonicalPath, null, true).size();
        if (sizeOfBakFolder != SIZE_OF_PROC_ALL.get()) {
            throw new RuntimeException("备份文件夹大小与备份处理文件大小不一致，sizeOfBakFolder=" + sizeOfBakFolder + "，procSize=" + SIZE_OF_PROC_ALL.get());
        }
        if (numOfBakFolder != NUM_OF_PROC_ALL.get()) {
            throw new RuntimeException("备份文件夹文件数量与备份处理文件数量不一致，numOfBakFolder=" + numOfBakFolder + "，procFileNum=" + NUM_OF_PROC_ALL.get());
        }
        // 如果有空目录则异常
        HardlinkToBakDirectory.checkIfContainsEmptyFolder(seedFolderCanonicalPath);
        System.out.println("文件总计：" + NUM_OF_PROC_ALL.get() + "，文件大小：" + SIZE_OF_PROC_ALL.get());
        System.out.println("==========================生成Seed目录结束==========================");
        return new String[]{NUM_OF_PROC_ALL.get() + "", SIZE_OF_PROC_ALL.get() + ""};

    }

    /**
     * 将该levelTo的目录保持文件夹结构硬链接到指定目录（HARD_LINK_TO_SEED_STRUCTURE_FOLDER）
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
                    String targetLinkPath = rootFileCanonicalPath + HARD_LINK_TO_SEED_STRUCTURE_FOLDER + File.separator + toHardLinkFile.getCanonicalPath().replace(file.getParentFile().getCanonicalPath() + File.separator, "");
                    File targetLinkFile = new File(targetLinkPath);
                    SIZE_OF_PROC_ALL.set(SIZE_OF_PROC_ALL.get() + toHardLinkFile.length());
                    NUM_OF_PROC_ALL.set(NUM_OF_PROC_ALL.get() + 1);
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
