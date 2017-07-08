package com.turn.ttorrent;

import com.turn.ttorrent.common.Torrent;
import org.apache.commons.io.FileUtils;
import org.testng.annotations.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * Created by yuuki asuna on 2017/7/1.
 */

public class DirectoryValidatorTest {

    private final static List<String> list = new ArrayList<String>() {{
        // 修正
        add("[Love Hina][04][BDRIP][1440x1080][H264(vfr+ED_60fps)_FLAC][REV][AFA8371A].ass");
        add("[Love Hina][04][BDRIP][1440x1080][H264(vfr+ED_60fps)_FLAC][REV][AFA8371A].mkv");
        //
        add("[CASO][Suzumiya_Haruhi_no_Yuuutsu][25][V2][BDRIP][1920x1080][x264_FLAC_2][5E770389].TC.ass");
        add("[CASO][Suzumiya_Haruhi_no_Yuuutsu][25][V2][BDRIP][1920x1080][x264_FLAC_2][5E770389].SC.ass");
        add("[CASO][Suzumiya_Haruhi_no_Yuuutsu][25][V2][BDRIP][1920x1080][x264_FLAC_2][5E770389].mkv");
        //
        add("[Claymore][13][BDRIP][1080P][H264(vfr+ED60fps)_FLAC][REV].mkv");
        add("[Claymore][16][BDRIP][1080P][H264(vfr+ED60fps)_FLAC][REV].mkv");
    }};


    @Test
    public void directoryValidateTest() throws Exception {
        String root = "E:\\0";
        listAndValidate(new File(root), new AtomicInteger(0));

    }

    // Thumbs.db
    private void listAndValidate(File source, AtomicInteger level) throws Exception {
        if (null == source) {
            throw new RuntimeException("目录不能为空, " + source);
        }
        String infoPrefix = "level-" + level.get() + ": ";
        if (!source.isDirectory()) {
            if(level.get() == 0){
                throw new RuntimeException(infoPrefix + "根必须为目录, " + source.getName());
            }
            return;
        }
        level.addAndGet(1);
        Integer torrentCountLevel3 = 0;
        // if no sub files, throw ex
        File[] subFiles = source.listFiles();
        if(null == subFiles || subFiles.length == 0){
            throw new RuntimeException(infoPrefix + "空文件夹, " + source.getName());
        }
        for (File file : subFiles) {
            String fileName = file.getName();
            if (level.get() == 1) {
                if (!file.isDirectory()) {
                    throw new RuntimeException(infoPrefix + "必须为目录, " + fileName);
                }
                // "^.+?[(].+?[^\\s][／][a-zA-Z0-9 ;!&\\-.%]+[)]$"
                if (!Pattern.matches("^.+?[(].+?[^\\s][／].+?[)]$", fileName)) {
                    throw new RuntimeException(infoPrefix + "文件夹名称格式错误, " + fileName);
                }
            } else if (level.get() == 2) {
                if (!file.isDirectory()) {
                    throw new RuntimeException(infoPrefix + "必须为目录, " + fileName);
                }
            } else if (level.get() == 3) {
                if (file.isDirectory()) {
                    // ignore
                }
                // 校验文件名
                if (fileName.toLowerCase().endsWith(".torrent")) {
                    StringBuffer sb = new StringBuffer();
                    torrentCountLevel3++;
                    Torrent torrent = Torrent.load(file, false);
                    // 检验种子文件名是否为U2格式或种子标题，校验种子所在文件夹是否为种子标题
                    if (!fileName.equals(torrent.getName())) {
                        boolean u2 = true;
                        List<List<URI>> trackers = torrent.getAnnounceList();
                        for (List<URI> item : trackers) {
                            for (URI uri : item) {
                                if (null == uri.getHost()) { // uri中发布者写了非法字符导致乱码时，getHost()返回null
                                    continue;
                                }
                                if (!uri.getHost().contains("tracker.dmhy.org")) {
                                    u2 = false;
                                    break;
                                }
                            }
                        }
                        if (u2) {
                            if (!Pattern.matches("^\\[U2]\\.\\d{1,7}\\.torrent$", fileName)) {
                                throw new RuntimeException(infoPrefix + String.format("种子文件名%s不是U2格式", fileName));
                            }
                        } else {
                            if (!fileName.equals(torrent.getName() + ".torrent")) {
                                throw new RuntimeException(infoPrefix + String.format("种子文件名%s不是种子标题%s", fileName, torrent.getName()));
                            }
                        }
                        if (!torrent.getName().equals(file.getParentFile().getName())) {
                            throw new RuntimeException(infoPrefix + String.format("种子%s所在文件夹%s不是种子标题%s", fileName, file.getParentFile().getName(), torrent.getName()));
                        }
                    }
                    // 校验torrent中文件是否都存在，大小是否一致
                    Field field = Torrent.class.getDeclaredField("files");
                    field.setAccessible(true);
                    List<Torrent.TorrentFile> torrentFileList = (List<Torrent.TorrentFile>) field.get(torrent);
                    Map<String, File> diskFileMap = transferIdFileMap(FileUtils.listFiles(file.getParentFile(), null, true));
                    Map<String, File> torrentFileMap = new HashMap<String, File>();
                    for (Torrent.TorrentFile item : torrentFileList) {
                        File fileToValidate = new File(file.getParentFile().getParentFile().getCanonicalPath(), item.file.getPath());
                        if (!fileToValidate.exists()) {
                            throw new RuntimeException(infoPrefix + String.format("种子%s文件%s不存在", fileName, item.file.getPath()));
                        }
                        if (fileToValidate.length() != item.size) {
                            throw new RuntimeException(infoPrefix + String.format("种子%s中对应文件%s大小%s和种子中里面的大小%s不一致", fileName, item.file.getPath(), fileToValidate.length(), item.size));
                        }
                        String key = file.getParentFile().getParentFile().getCanonicalPath() + File.separator + item.file.getPath();
                        torrentFileMap.put(key, item.file);
                        diskFileMap.remove(key);
                    }
                    // 种子指明之外的文件只能是字幕文件，且必须对应上视频文件，种子文件排除)
                    for (Map.Entry<String, File> diskEntry : diskFileMap.entrySet()) {
                        boolean matched = false;
                        String diskEntryFileName = diskEntry.getValue().getName();
                        if (list.contains(diskEntryFileName)) {
                            System.out.println("配置保留文件: " + diskEntryFileName);
                            continue;
                        }
                        // 排除本省的torrent文件
                        if (diskEntry.getKey().endsWith(".torrent")) {
                            continue;
                        }
                        if (!diskEntry.getKey().matches("^.+?\\.(ass|ssa|idx|sub)$")) {
                            throw new RuntimeException(String.format("除种子文件中指明的文件外必须以ass,ssa,idx,sub其中之一结尾, 文件路径%s", diskEntry.getKey()));
                        }
                        for (Map.Entry<String, File> torrentEntry : torrentFileMap.entrySet()) {
                            if (!torrentEntry.getKey().matches("^.+?\\.(mkv|mp4|avi|wmv|MKV|MP4|AVI|WAV)$")) {
                                continue;
                            }
                            String prefix = torrentEntry.getKey().replaceAll("\\.(mkv|mp4|avi|wmv|MKV|MP4|AVI|WMV)$", "");
                            if (diskEntry.getKey().startsWith(prefix)) {
                                matched = true;
                                break;
                            }
                        }
                        if (!matched) {
                            throw new RuntimeException(String.format("字幕文件未对应上视频文件, 文件路径%s", diskEntry.getKey()));
                        }
                    }

                }

            }
            listAndValidate(file, level);
        }
        if (level.get() == 3) {
            if (torrentCountLevel3 == 0) {
                throw new RuntimeException(infoPrefix + String.format("文件夹%s中不存在种子文件", source.getName()));
            }
            if (torrentCountLevel3 > 1) {
                throw new RuntimeException(infoPrefix + String.format("文件夹%s中存在种子文件个数大于1", source.getName()));
            }
        }
        level.addAndGet(-1);
        return;
    }


    private Map<String, File> transferIdFileMap(Collection<File> fileCollection) throws Exception {
        Iterator<File> iterator = fileCollection.iterator();
        Map<String, File> result = new HashMap<String, File>();
        while (iterator.hasNext()) {
            File file = iterator.next();
            result.put(file.getCanonicalPath(), file);
        }
        return result;
    }


}
