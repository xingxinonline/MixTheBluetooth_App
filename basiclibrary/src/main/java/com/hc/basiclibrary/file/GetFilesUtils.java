package com.hc.basiclibrary.file;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 用于获取手机的文件夹及文件的工具类，如果权限允许，可以获取手机上任意路径的文件列表
 * GetFilesUtils使用的是懒汉式单例模式，线程安全
 */
public class GetFilesUtils {

    public static final String FILE_TYPE_FOLDER="wFl2d";

    public static final String FILE_INFO_NAME="fName";
    public static final String FILE_INFO_ISFOLDER="fIsDir";
    public static final String FILE_INFO_TYPE="fFileType";
    public static final String FILE_INFO_NUM_SONDIRS="fSonDirs";
    public static final String FILE_INFO_NUM_SONFILES="fSonFiles";
    public static final String FILE_INFO_PATH="fPath";

    private static GetFilesUtils gfu;

    private GetFilesUtils(){

    }

    /**
     * 获取GetFilesUtils实例
     * @return GetFilesUtils
     **/
    public static synchronized GetFilesUtils getInstance(){
        if(gfu==null){
            gfu=new GetFilesUtils();
        }
        return gfu;
    }

    /**
     * 获取文件path文件夹下的文件列表
     * @see #getSonNode(String)
     * @param path 手机上的文件夹
     * @return path文件夹下的文件列表的信息，信息存储在Map中，Map的key的列表如下：<br />
     *     FILE_INFO_NAME : String 文件名称 <br />
     *     FILE_INFO_ISFOLDER: boolean 是否为文件夹  <br />
     *     FILE_INFO_TYPE: string 文件的后缀 <br />
     *     FILE_INFO_NUM_SONDIRS : int 子文件夹个数  <br />
     *     FILE_INFO_NUM_SONFILES: int 子文件个数  <br />
     *     FILE_INFO_PATH : String 文件的绝对路径 <br />
     **/
    public List<Map<String, Object>> getSonNode(File path){
        if(path.isDirectory()){
            List<Map<String, Object>> list= new ArrayList<>();
            File[] files=path.listFiles();
            if(files!=null){
                for (File file : files) {
                    if (file.getName().startsWith(".")){
                        continue;
                    }
                    Map<String, Object> fileInfo = new HashMap<>();
                    fileInfo.put(FILE_INFO_NAME, file.getName());
                    if (file.isDirectory()) {
                        fileInfo.put(FILE_INFO_ISFOLDER, true);
                        File[] bFiles = file.listFiles();
                        if (bFiles == null) {
                            fileInfo.put(FILE_INFO_NUM_SONDIRS, 0);
                            fileInfo.put(FILE_INFO_NUM_SONFILES, 0);
                        } else {
                            int getNumOfDir = 0;
                            int getNumOfFile = 0;
                            int hideNumOfDir = 0;
                            int hideNumOfFile = 0;
                            for (File bFile : bFiles) {
                                if (bFile.isDirectory()) {
                                    if (!bFile.getName().startsWith(".")) {
                                        getNumOfDir++;
                                    } else {
                                        hideNumOfDir++;
                                    }
                                } else {
                                    if (!bFile.getName().startsWith(".")) {
                                        getNumOfFile++;
                                    } else {
                                        hideNumOfFile++;
                                    }
                                }
                            }
                            fileInfo.put(FILE_INFO_NUM_SONDIRS, getNumOfDir);
                            fileInfo.put(FILE_INFO_NUM_SONFILES, getNumOfFile);
                        }
                        fileInfo.put(FILE_INFO_TYPE, FILE_TYPE_FOLDER);
                    } else {
                        fileInfo.put(FILE_INFO_ISFOLDER, false);
                        fileInfo.put(FILE_INFO_NUM_SONDIRS, 0);
                        fileInfo.put(FILE_INFO_NUM_SONFILES, 0);
                        fileInfo.put(FILE_INFO_TYPE, getFileType(file.getName()));
                    }
                    fileInfo.put(FILE_INFO_PATH, file.getAbsoluteFile());
                    list.add(fileInfo);
                }
                return list;
            }else{
                return null;
            }
        }else{
            return null;
        }
    }
    /**
     * 获取文件pathStr文件夹下的文件列表
     * @see #getSonNode(File)
     * @param pathStr 手机上的文件夹的绝对路径
     * @return pathStr文件夹下的文件列表的信息，信息存储在Map中，Map的key的列表如下：<br />
     *     FILE_INFO_NAME : String 文件名称 <br />
     *     FILE_INFO_ISFOLDER: boolean 是否为文件夹  <br />
     *     FILE_INFO_TYPE: string 文件的后缀 <br />
     *     FILE_INFO_NUM_SONDIRS : int 子文件夹个数  <br />
     *     FILE_INFO_NUM_SONFILES: int 子文件个数  <br />
     *     FILE_INFO_PATH : String 文件的绝对路径 <br />
     **/
    public List<Map<String, Object>> getSonNode(String pathStr){
        File path=new File(pathStr);
        return sort(getSonNode(path));
    }



    /**
     * 获取文件path文件或文件夹的兄弟节点文件列表
     * @see #getBrotherNode(String)
     * @param path 手机上的文件夹
     * @return path文件夹下的文件列表的信息，信息存储在Map中，Map的key的列表如下：<br />
     *     FILE_INFO_NAME : String 文件名称 <br />
     *     FILE_INFO_ISFOLDER: boolean 是否为文件夹  <br />
     *     FILE_INFO_TYPE: string 文件的后缀 <br />
     *     FILE_INFO_NUM_SONDIRS : int 子文件夹个数  <br />
     *     FILE_INFO_NUM_SONFILES: int 子文件个数  <br />
     *     FILE_INFO_PATH : String 文件的绝对路径 <br />
     **/
    public List<Map<String, Object>> getBrotherNode(File path){
        if(path.getParentFile()!=null){
            return getSonNode(path.getParentFile());
        }
        return null;
    }
//    /**
//     * 获取文件path文件或文件夹的兄弟节点文件列表
//     * @see #getBrotherNode(File)
//     * @param path 手机上的文件夹
//     * @return path文件夹下的文件列表的信息，信息存储在Map中，Map的key的列表如下：<br />
//     *     FILE_INFO_NAME : String 文件名称 <br />
//     *     FILE_INFO_ISFOLDER: boolean 是否为文件夹  <br />
//     *     FILE_INFO_TYPE: string 文件的后缀 <br />
//     *     FILE_INFO_NUM_SONDIRS : int 子文件夹个数  <br />
//     *     FILE_INFO_NUM_SONFILES: int 子文件个数  <br />
//     *     FILE_INFO_PATH : String 文件的绝对路径 <br />
//     **/
    public List<Map<String, Object>> getBrotherNode(String pathStr){
        File path=new File(pathStr);
        return getBrotherNode(path);
    }

//    /**
//     * 获取文件或文件夹的父路径
//     * @param File path文件或者文件夹
//     * @return String path的父路径
//     **/
    public String getParentPath(File path){
        if(path.getParentFile()==null){
            return null;
        }else{
            return path.getParent();
        }
    }
//    /**
//     * 获取文件或文件的父路径
//     * @param String pathStr文件或者文件夹路径
//     * @return String pathStr的父路径
//     **/
    public String getParentPath(String pathStr){
        File path=new File(pathStr);
        if(path.getParentFile()==null){
            return null;
        }else{
            return path.getParent();
        }
    }

    /**
     * 获取sd卡的绝对路径
     * @return String 如果sd卡存在，返回sd卡的绝对路径，否则返回null
     **/
    public String getSDPath(){
        String sdcard=Environment.getExternalStorageState();
        if(sdcard.equals(Environment.MEDIA_MOUNTED)){
            return Environment.getExternalStorageDirectory().getAbsolutePath();
        }else{
            return null;
        }
    }

    /**
     * 获取一个基本的路径，一般应用创建存放应用数据可以用到
     * @return String 如果SD卡存在，返回SD卡的绝对路径，如果SD卡不存在，返回Android数据目录的绝对路径
     **/
    public String getBasePath(){
        String basePath=getSDPath();
        if(basePath==null){
//            Log.d("FolderActivity", "getBasePath: 11111111111111111111111");
            return Environment.getDataDirectory().getAbsolutePath();
        }else{
//            Log.d("FolderActivity", "getBasePath: 22222222222222");

            return basePath;
        }
    }

    /**
     * 获取文件path的大小
     * @return String path的大小
     **/
    public String getFileSize(File path) throws IOException{
        if(path.exists()){
            DecimalFormat df = new DecimalFormat("#.00");
            String sizeStr;
            FileInputStream fis=new FileInputStream(path);
            long size=fis.available();
            fis.close();
            if(size<1024){
                sizeStr=size+"B";
            }else if(size<1048576){
                sizeStr=df.format(size/(double)1024)+"KB";
            }else if(size<1073741824){
                sizeStr=df.format(size/(double)1048576)+"MB";
            }else{
                sizeStr=df.format(size/(double)1073741824)+"GB";
            }
            return sizeStr;
        }else{
            return null;
        }
    }

    /**
     * 获取文件fpath的大小
     * @return String path的大小
     **/
    public String getFileSize(String fpath){
        File path=new File(fpath);
        if(path.exists()){
            DecimalFormat df = new DecimalFormat("#.00");
            String sizeStr;
            long size;
            try {
                FileInputStream fis = new FileInputStream(path);
                size=fis.available();
                fis.close();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return "未知大小";
            }
            double typeB = 1024;
            double typeK = 1024*1024;
            double typeM = 1024*typeK;
            if(size<typeB){
                sizeStr=size+"B";
            }else if(size<typeK){
                sizeStr=df.format(size/typeB)+"KB";
            }else if(size<typeM){
                sizeStr=df.format(size/typeK)+"MB";
            }else{
                sizeStr=df.format(size/typeM)+"GB";
            }
            return sizeStr;
        }else{
            return "未知大小";
        }
    }


    /**
     * 获取文件fpath的大小
     * @return String path的大小
     **/
    public String getFileSizeByte(File path){
        if(path.exists()){
            long size;
            try {
                FileInputStream fis = new FileInputStream(path);
                size=fis.available();
                fis.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return "未知大小";
            }
            return String.valueOf(size);
        }else{
            return "未知大小";
        }
    }

    /**
     * 根据后缀获取文件fileName的类型
     * @return String 文件的类型
     **/
    public String getFileType(String fileName){
        if(!fileName.equals("") &&fileName.length()>3){
            int dot=fileName.lastIndexOf(".");//e.mp3
            if(dot>0){                        //01234
                return fileName.substring(dot+1);
            }else{
                return "";
            }
        }
        return "";
    }

    private boolean isLetter(String str) {
        String regex = "^[a-zA-Z]+$";//其他需要，直接修改正则表达式就好
        //String regex = "^[\u4e00-\u9fa5a-z0-9A-Z]+$";
        return !str.matches(regex);
    }

    //自定义文件排序，汉字，_，字母依次
    private int compareAB(String left2, String right2){
        String A = left2.substring(0,1);
        String B = right2.substring(0,1);
        if(isLetter(A) && isLetter(B)){
            return A.compareTo(B);
        }else if(isLetter(A)){
            return -1;
        }else if(isLetter(B)){
            return 1;
        }else {
                String a = A.toLowerCase();
                String b = B.toLowerCase();
                if(a.compareTo(b) == 0){
                    return A.compareTo(B);
                }else {
                    return a.compareTo(b);
                }
        }

    }

    public Comparator<Map<String, Object>> defaultOrder() {

        final String orderBy0=FILE_INFO_ISFOLDER;
        final String orderBy1=FILE_INFO_TYPE;
        final String orderBy2=FILE_INFO_NAME;

        return (lhs, rhs) -> {
            // TODO Auto-generated method stub
            int left0=lhs.get(orderBy0).equals(true)?0:1;
            int right0=rhs.get(orderBy0).equals(true)?0:1;
            if(left0==right0){
                String left1=lhs.get(orderBy1).toString();
                String right1=rhs.get(orderBy1).toString();
                if(left1.compareTo(right1)==0){
                    String left2=lhs.get(orderBy2).toString();
                    String right2=rhs.get(orderBy2).toString();
                    return compareAB(left2,right2);
                }else{
                    return left1.compareTo(right1);
                }
            }else{
                return left0-right0;
            }
        };
    }


    private List<Map<String, Object>> sort(List<Map<String, Object>> list) {

        final String orderBy0=FILE_INFO_ISFOLDER;
        final String orderBy1=FILE_INFO_TYPE;
        final String orderBy2=FILE_INFO_NAME;

        list.sort((lhs, rhs) -> {
            int left0 = lhs.get(orderBy0).equals(true) ? 0 : 1;
            int right0 = rhs.get(orderBy0).equals(true) ? 0 : 1;
            if (left0 == right0) {
                String left1 = lhs.get(orderBy1).toString();
                String right1 = rhs.get(orderBy1).toString();
                if (left1.compareTo(right1) == 0) {
                    String left2 = lhs.get(orderBy2).toString();
                    String right2 = rhs.get(orderBy2).toString();
                    return compareAB(left2, right2);
                } else {
                    return left1.compareTo(right1);
                }
            } else {
                return left0 - right0;
            }
        });
        return list;
    }

}