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


    public final static String HARD_LINK_TO_BAK_FOLDER = ".hardlink.bak";


    /**
     * 利用硬链接备份盘符下所有文件到 X:\\.hardlink.bak 目录
     * 排除根目录下ignoreUnderRootList包含的文件夹
     *
     * @throws Exception
     */
    public static String[] hardlinkBak(String rootPath) throws Exception {
        System.out.println("==========================备份开始==========================");
        String rootFileCanonicalPath = DirectoryValidator.validateRootPath(rootPath);
        File rootFile = new File(rootFileCanonicalPath);
        // 备份文件夹创建，加入只读，系统，隐藏属性
        File bakFolder = new File(rootFileCanonicalPath + HARD_LINK_TO_BAK_FOLDER);
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
        Long procSize = 0L;
        Integer procFileNum = 0;
        lab:
        while (iterator.hasNext()) {
            File file = iterator.next();
            String fileCanonicalPath = file.getCanonicalPath();
            for (String item : ignoreUnderRootList) {
                if (fileCanonicalPath.startsWith(item)) {
                    continue lab;
                }
            }
            procFileNum++;
            procSize += file.length();
            String targetLinkPath = bakFolderCanonicalPath + File.separator + fileCanonicalPath.replace(rootFileCanonicalPath, "");
            File targetLinkFile = new File(targetLinkPath);
            if (!targetLinkFile.exists()) {
                FileUtils.forceMkdirParent(targetLinkFile);
                Files.createLink(targetLinkFile.toPath(), file.toPath());
            }
        }
        //
        long sizeOfBakFolder = FileUtils.sizeOfDirectory(bakFolder);
        int numOfBakFolder = FileUtils.listFiles(bakFolder, null, true).size();
        if (sizeOfBakFolder != procSize) {
            throw new RuntimeException("备份文件夹大小与备份处理文件大小不一致，sizeOfBakFolder=" + sizeOfBakFolder + "，procSize=" + procSize);
        }
        if (numOfBakFolder != procFileNum) {
            throw new RuntimeException("备份文件夹文件数量与备份处理文件数量不一致，numOfBakFolder=" + numOfBakFolder + "，procFileNum=" + procFileNum);
        }
        System.out.println("文件总计：" + procFileNum + "，文件大小：" + procSize);
        System.out.println("==========================备份结束==========================");
        return new String[]{procFileNum + "", procSize + ""};
    }


}


