package com.turn.ttorrent.validate;

/**
 * Created by yuuki asuna on 2018/3/19.
 */
public class App {


    private final static String rootPath = "O:";

    public static void main(String[] args) throws Exception{
        DirectoryValidator.validate(rootPath);
        HardlinkToBakDirectory.hardlinkBak(rootPath);
        HardLinkToSeedStructure.createHardLink(rootPath);
    }
}
