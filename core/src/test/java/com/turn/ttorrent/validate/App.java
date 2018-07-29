package com.turn.ttorrent.validate;

/**
 * Created by yuuki asuna on 2018/3/19.
 */
public class App {


    private final static String rootPath = "P:";

    public static void main(String[] args) throws Exception {
        String[] numAndSizeInfo0 = DirectoryValidator.validate(rootPath);
        String[] numAndSizeInfo1 = HardlinkToBakDirectory.hardlinkBak(rootPath);
        if (!numAndSizeInfo0[0].equals(numAndSizeInfo1[0]) || !numAndSizeInfo0[1].equals(numAndSizeInfo1[1])) {
            throw new RuntimeException("备份文件夹与原始文件夹文件数量大小不一致, " + HardlinkToBakDirectory.HARD_LINK_TO_BAK_FOLDER);
        }
        String[] numAndSizeInfo2 = HardLinkToSeedStructure.createHardLink(rootPath);
        if (!numAndSizeInfo0[0].equals(numAndSizeInfo2[0]) || !numAndSizeInfo0[1].equals(numAndSizeInfo2[1])) {
            throw new RuntimeException("备份文件夹与原始文件夹文件数量大小不一致, " + HardLinkToSeedStructure.HARD_LINK_TO_SEED_STRUCTURE_FOLDER);
        }
    }
}
