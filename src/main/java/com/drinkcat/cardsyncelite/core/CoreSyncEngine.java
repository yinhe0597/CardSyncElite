package com.drinkcat.cardsyncelite.core;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class CoreSyncEngine {
    /*
      所需参数
      1. 搜索目录
      2. 目标目录
      3. 文件类型 / 拓展名
      选项控制
      1. 是否需要检验
      2. 是否维持原目录架构
      3. 是否更改名称
      4. 线程数量
      */

    private final boolean needCheck;
    private final boolean maintainStructure;
    private final boolean needRename;
    private final int maxThreads;
    private final List<String> extensions;

    public CoreSyncEngine() {
        this(true, true, false, 5, List.of(".pdf"));
    }

    public CoreSyncEngine(boolean needCheck, boolean maintainStructure, boolean needRename, int maxThreads, List<String> extensions) {
        this.needCheck = needCheck;
        this.maintainStructure = maintainStructure;
        this.needRename = needRename;
        this.maxThreads = maxThreads;
        this.extensions = extensions;
    }

    public void syncFile(Path source, Path target) throws Exception {
        // 搜索
        List<Path> searchFiles = null;
        try {
            searchFiles = CoreSearchEngine.searchTask(source, extensions);
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
        if(searchFiles == null) {
            System.out.println("共搜索到 0 个文件");
            return;
        }
        else System.out.println("共搜索到 " + searchFiles.size() + " 个文件: ");
        searchFiles.stream().limit(5).forEach(System.out::println);
        if(searchFiles.size() > 5) System.out.print("等 " + searchFiles.size() + " 个文件, ");

        // 测试异常情况 searchFiles.add(Paths.get("/asdw"));

        // Copy and Check
        copyAndCheck(searchFiles, target);
    }

    private void copyAndCheck(List<Path> files, Path target) throws Exception {
        // 复制
        Map<File, File> successFiles = null;
        try {
            successFiles = CoreCopyEngine.copyTask(files, target, this.maxThreads, this.maintainStructure, this.needRename);
        } catch (InterruptedException e) {
            throw new Exception("复制进程被异常中断");
        }

        // 校验
        if(!needCheck) return;
        var crcErrFiles = CoreCheckEngine.checkTask(successFiles, this.maxThreads);
        if(crcErrFiles.isEmpty()) System.out.println("文件校验通过!");
        else {
            System.out.println("有 " + crcErrFiles.size() + " 个文件校验失败!");
            crcErrFiles.stream().limit(5).forEach(System.out::println);
            if(crcErrFiles.size() > 5) System.out.print("等 " + crcErrFiles.size() + " 个文件, ");

            copyAndCheck(crcErrFiles, target);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        CoreSyncEngine syncEngine = new CoreSyncEngine();
        Path source = Paths.get("/Users/drinkcat/Desktop/Java学习资料/");
        Path target = Paths.get("/Users/drinkcat/Desktop/test2/test");

        Thread sync = new Thread(() -> {
            try {
                syncEngine.syncFile(source, target);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        });
        sync.start();

        System.out.println("开始同步操作...");
        sync.join();
        System.out.println("同步完成!");
    }
}