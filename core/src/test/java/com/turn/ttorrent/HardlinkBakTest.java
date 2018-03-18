package com.turn.ttorrent;

import org.apache.commons.io.FileUtils;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by yuuki asuna on 2017/7/8.
 */
public class HardlinkBakTest {

    final String rootPath = null;

    //2882534950074   2882534950074  2882534950074    12946  12946   12946
    @Test
    public void hardlinkBak() throws Exception {

        if(null == rootPath || rootPath.trim().length() == 0){
            throw new RuntimeException("rootPath不能为空");
        }
        File rootFile = new File(rootPath.toUpperCase());
        // 只允许windows
        String os = System.getProperty("os.name", null);
        if (null == os) {
            throw new RuntimeException("无法获取系统属性，'os.name'");
        }
        if (os.toLowerCase().indexOf("windows") == -1) {
            throw new RuntimeException("只支持windows操作系统");
        }
        // 必须为根目录
        if (!rootFile.exists()) {
            throw new RuntimeException("目录不存在，" + rootPath);
        }
        if (rootFile.getParentFile() != null) {
            throw new RuntimeException("该目录不是根目录，" + rootPath);
        }
        // 验证根目录规范化路径是否正确
        final String rootFileCanonicalPath = rootFile.getCanonicalPath();
        String reg = "[A-Z]:\\\\";
        if (!Pattern.matches(reg, rootFileCanonicalPath)) {
            throw new RuntimeException("根目录规范化路径应满足正则，'" + reg + "'");
        }
        // 备份文件夹创建，加入只读，系统，隐藏属性
        File bakFolder = new File(rootFileCanonicalPath + ".hardlink.bak");
        if (!bakFolder.exists()) {
            bakFolder.mkdir();
        }
        String bakFolderCanonicalPath = bakFolder.getCanonicalPath();
        Runtime.getRuntime().exec("attrib +R +S +H \"" + bakFolderCanonicalPath + "\"");

        // 忽略的根目录下的文件夹
        List<String> ignoreUnderRootList = new ArrayList<String>() {{
            add(rootFileCanonicalPath + "$RECYCLE.BIN");
            add(rootFileCanonicalPath + "System Volume Information");
            add(rootFileCanonicalPath + "Recovery");
            add(bakFolderCanonicalPath);
        }};
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
            if (targetLinkFile.exists()) {
                continue;
            }
            FileUtils.forceMkdirParent(targetLinkFile);
            Files.createLink(targetLinkFile.toPath(), file.toPath());
        }
        long sizeOfBakFolder = FileUtils.sizeOfDirectory(bakFolder);
        Integer numOfBakFolder = FileUtils.listFiles(bakFolder, null, true).size();
        if (sizeOfBakFolder != srcSize) {
            throw new RuntimeException("备份文件夹与源文件夹大小不一致，sizeOfBakFolder=" + sizeOfBakFolder + "，srcSize=" + srcSize);
        }
        if (numOfBakFolder != srcFileNum) {
            throw new RuntimeException("备份文件夹与源文件夹文件数量不一致，numOfBakFolder=" + numOfBakFolder + "，srcFileNum=" + srcFileNum);
        }
        System.out.println("文件总计：" + srcFileNum + "，文件大小：" + srcSize);
    }


}


