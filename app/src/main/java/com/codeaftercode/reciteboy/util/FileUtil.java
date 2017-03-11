package com.codeaftercode.reciteboy.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.widget.Toast;

import com.codeaftercode.reciteboy.R;
import com.codeaftercode.reciteboy.bean.Question;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;


public class FileUtil {
    public static String getPath(Context context, Uri uri)
    {
        if ("content".equalsIgnoreCase(uri.getScheme()))
        {
            String[] projection = { "_data" };
            Cursor cursor = null;
            try
            {
                cursor = context.getContentResolver().query(uri, projection,null, null, null);
                if (cursor == null) {
                    return null;
                }
                int column_index = cursor.getColumnIndexOrThrow("_data");
                if (cursor.moveToFirst())
                {
                    return cursor.getString(column_index);
                }
            } catch (Exception e)
            {
                e.printStackTrace();
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        else if ("file".equalsIgnoreCase(uri.getScheme()))
        {
            return uri.getPath();
        }
        return null;
    }

    public static String getFileName(String pathandname)
    {
        int start=pathandname.lastIndexOf("/");
        int end=pathandname.lastIndexOf(".");
        if(start!=-1 && end!=-1)
        {
            return pathandname.substring(start+1,end);
        }
        else
        {
            return null;
        }
    }

    /**
     * 解析.xls类型题库文件,把每道题封装为1个Question类的实例
     * @param path  文件路径
     * @return  返回null表示异常
     */
    public static ArrayList<Question> readExcel(String path) {
        ArrayList<Question> questions = new ArrayList<>();
        Question q;
        Workbook workbook = null;
        try {
            //打开xls文件的第1个工作簿
            workbook = Workbook.getWorkbook(new File(path));
            Sheet sheet = workbook.getSheet(0);
            //获取总行数
            int rows = sheet.getRows();
            //逐行读取.(第0行是表头,从第1行开始读取)
            for (int i = 1; i < rows; i++) {
                q = new Question();
                //第0列为题目内容
                q.setTitle(sheet.getCell(0, i).getContents());
                //第1列为题型
                String typeString = sheet.getCell(1, i).getContents();
                for (int type = 0; type < Question.TYPES.length; type++) {
                    if (Question.TYPES[type].equals(typeString) || Integer.toString(type).equals(typeString)) {
                        q.setType(type);
                    }
                }
                //q.setType(Integer.parseInt(sheet.getCell(1, i).getContents()));
                //第2列为选项A
                //q.setOptionA(sheet.getCell(2, i).getContents());
                q.setOptionAtIndex(sheet.getCell(2, i).getContents(), 0);
                //第3列为选项B
                //q.setOptionB(sheet.getCell(3, i).getContents());
                q.setOptionAtIndex(sheet.getCell(3, i).getContents(), 1);
                //第4列为选项C
                //q.setOptionC(sheet.getCell(4, i).getContents());
                q.setOptionAtIndex(sheet.getCell(4, i).getContents(), 2);
                //第5列为选项D
                //q.setOptionD(sheet.getCell(5, i).getContents());
                q.setOptionAtIndex(sheet.getCell(5, i).getContents(), 3);
                //第6列为答案
                q.setAnswer(sheet.getCell(6, i).getContents());
                //Log.i("File", "readExcel: "+i+"行" + q.getTitle()+","+q.getType()+","+q.getOptionA()+","+q.getOptionB()+","+q.getOptionC()+","+q.getOptionD()+","+q.getAnswer());
                //添加到ArrayList中
                questions.add(q);
            }
        } catch (BiffException | IOException e) {
            e.printStackTrace();
        } finally {
            if (workbook != null) {
                workbook.close();
            }
        }

        return questions;
    }

    /**
     * 导出题库模板
     * @param context   上下文
     * @param desFile   目标文件
     * @return          操作结果
     */
    public static boolean exportQuestionbankDemo(Context context, File desFile) {
        boolean result = false;

        try {
            InputStream inputStream = context.getAssets().open(context.getResources().getString(R.string.fount_export_source_file));
            FileOutputStream fileOutputStream = new FileOutputStream(desFile);
            byte[] buffer = new byte[1024];
            int byteCount;
            while ((byteCount = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, byteCount);
            }
            fileOutputStream.flush();
            fileOutputStream.close();
            inputStream.close();
            result = true;
        } catch (IOException e) {
            //Toast.makeText(mContext, resources.getString(R.string.found_export_fail), Toast.LENGTH_SHORT).show();
            LogUtil.w("copy", e.getMessage());
        }

        return result;
    }
}
