package com.turn.ttorrent.validate;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Created by yuuki asuna on 2017/7/8.
 */
public class HardlinkToBakDirectory {


    private final static String hardlinkToFolder = ".hardlink.bak";


    /**
     * 利用硬链接备份盘符下所有文件到 X:\\.hardlink.bak 目录
     * 排除根目录下ignoreUnderRootList包含的文件夹
     *
     * @throws Exception
     */
    public static void hardlinkBak(String rootPath) throws Exception {
        System.out.println("==========================备份开始==========================");
        String rootFileCanonicalPath = DirectoryValidator.validateRootPath(rootPath);
        File rootFile = new File(rootFileCanonicalPath);
        // 备份文件夹创建，加入只读，系统，隐藏属性
        File bakFolder = new File(rootFileCanonicalPath + hardlinkToFolder);
        if (!bakFolder.exists()) {
            bakFolder.mkdir();
        }
        String bakFolderCanonicalPath = bakFolder.getCanonicalPath();
        Runtime.getRuntime().exec("attrib +R +S +H \"" + bakFolderCanonicalPath + "\"");

        // 忽略的根目录下的文件夹
        List<String> ignoreUnderRootList = DirectoryValidator.genIgnorePathList(rootPath);
        // 遍历目标文件夹
        Collection<File> files = FileUtils.listFiles(rootFile, null, true);
        Iterator<File> iterator = files.iterator();
        Long srcSize = 0L;
        Integer srcFileNum = 0;
        lab:
        while (iterator.hasNext()) {
            File file = iterator.next();
            String fileCanonicalPath = file.getCanonicalPath();
            for (String item : ignoreUnderRootList) {
                if (fileCanonicalPath.startsWith(item)) {
                    continue lab;
                }
            }
            srcFileNum++;
            srcSize += file.length();
            String targetLinkPath = bakFolderCanonicalPath + File.separator + fileCanonicalPath.replace(rootFileCanonicalPath, "");
            File targetLinkFile = new File(targetLinkPath);
            if (!targetLinkFile.exists()) {
                FileUtils.forceMkdirParent(targetLinkFile);
                Files.createLink(targetLinkFile.toPath(), file.toPath());
            }
        }
        long sizeOfBakFolder = FileUtils.sizeOfDirectory(bakFolder);
        int numOfBakFolder = FileUtils.listFiles(bakFolder, null, true).size();
        if (sizeOfBakFolder != srcSize) {
            throw new RuntimeException("备份文件夹与源文件夹大小不一致，sizeOfBakFolder=" + sizeOfBakFolder + "，srcSize=" + srcSize);
        }
        if (numOfBakFolder != srcFileNum) {
            throw new RuntimeException("备份文件夹与源文件夹文件数量不一致，numOfBakFolder=" + numOfBakFolder + "，srcFileNum=" + srcFileNum);
        }
        System.out.println("文件总计：" + srcFileNum + "，文件大小：" + srcSize);
        System.out.println("==========================备份结束==========================");
    }


}


