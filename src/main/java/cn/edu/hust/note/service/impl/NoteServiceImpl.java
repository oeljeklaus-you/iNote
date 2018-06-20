package cn.edu.hust.note.service.impl;

import cn.edu.hust.note.bean.Note;
import cn.edu.hust.note.bean.NoteBook;
import cn.edu.hust.note.dao.NoteDao;
import cn.edu.hust.note.service.NoteService;
import cn.edu.hust.utils.constants.Constants;
import cn.edu.hust.note.service.jedis.RedisUtils;
import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class NoteServiceImpl implements NoteService
{
    @Autowired(required = false)
    private RedisUtils redisUtils;

    @Autowired(required = false)
    private NoteDao noteDao;

    /**
     * 1.先从redis中查询，如果有直接返回，如果没有进行步骤2
     * 2.使用HBase进行查询,将查询结果放入redis,然后将结果返回
     * @param userName
     * @return
     */
    @Override
    public List<NoteBook> getAllNoteBook(String userName) {


        List<NoteBook> noteBookList=getNoteBookFromRedis(userName);
        if(noteBookList==null)
        {
            noteBookList=this.noteDao.getAllNoteBook(userName);
            //添加到redis中
            for (NoteBook noteBook:noteBookList)
            {
                String noteBookString=JSON.toJSONString(noteBook);
                redisUtils.lSet(userName,noteBookString);
            }

        }
        return noteBookList;
    }


    private List<NoteBook> getNoteBookFromRedis(String userName)
    {
        Long size=this.redisUtils.lGetListSize(userName);
        List<String> noteBookListString;
        List<NoteBook> noteBookList=new ArrayList<>();
        if(size!=0)
        {
            List<Object> objects=this.redisUtils.lGet(userName,0,size);
            for(int i=0;i<objects.size();i++)
            {
                String s=(String)objects.get(i);
                NoteBook noteBook =JSON.parseObject(s,NoteBook.class);
                noteBookList.add(noteBook);
            }
            return noteBookList;
        }
        else return null;
    }
    /**
     * 1.先将数据保存在redis中，在将数据写入到Hbase中
     * 2。一成功都成功，一失败都失败
     * @param noteBookName
     * @param userName
     * @param s
     * @param i
     * @return
     */
    @Override
    public boolean addNoteBook(String noteBookName, String userName, String s, int i) {

        try{
            boolean flag=addNoteBook2Redis(noteBookName,userName,s,i);
            addNoteBook2HBase(noteBookName,userName,s,i);

        }
        catch (Exception e)
        {
            return false;
        }

        return true;
    }

    private boolean addNoteBook2Redis(String noteBookName, String userName, String s, int i)
    {
        NoteBook noteBook=new NoteBook();
        noteBook.setRowKey(userName+Constants.ROWKEY_SEPARATOR+s);
        noteBook.setName(noteBookName);
        noteBook.setCreateTime(s);
        noteBook.setStatus(i+"");
        String noteBookString=JSON.toJSONString(noteBook);
        redisUtils.lSet(userName,noteBookString);
        return true;
    }

    private boolean addNoteBook2HBase(String noteBookName, String userName, String s, int i)
    {
        String rowKey=userName.trim()+Constants.ROWKEY_SEPARATOR+s.trim();
        List<Note> noteList=new ArrayList<Note>();
        String noteListString= JSON.toJSONString(noteList);
        String[][] data=new String[4][3];
        data[0][0]=Constants.NOTEBOOK_FAMLIY_NOTEBOOKINFO;
        data[0][1]=Constants.NOTEBOOK_NOTEBOOKINFO_CLU_NOTEBOOKNAME;
        data[0][2]=noteBookName;

        data[1][0]=Constants.NOTEBOOK_FAMLIY_NOTEBOOKINFO;
        data[1][1]=Constants.NOTEBOOK_NOTEBOOKINFO_CLU_CREATETIME;
        data[1][2]=s;

        data[2][0]=Constants.NOTEBOOK_FAMLIY_NOTEBOOKINFO;
        data[2][1]=Constants.NOTEBOOK_NOTEBOOKINFO_CLU_STATUS;
        data[2][2]=String.valueOf(i);

        data[3][0]= Constants.NOTEBOOK_FAMLIY_NOTEBOOKINFO;
        data[3][1]= Constants.NOTEBOOK_NOTEBOOKINFO_CLU_NOTELIST;
        data[3][2]= noteListString;

        return this.noteDao.addNoteBook(Constants.NOTEBOOK_TABLE_NAME,rowKey,data);
    }

    /**
     *
     * @param rowkey
     * @return
     */
    @Override
    public List<Note> getNoteListByNotebook(String rowkey) {
        //从redis中查询
        List<Note> noteList=getNoteListFromRedis(rowkey);
        //redis中没有
        if(noteList.size()==0)
        {
            noteList=this.noteDao.getNoteListByNotebook(rowkey);
            //添加到redis中
            for (Note note:noteList)
            {
                String noteString=JSON.toJSONString(note);
                redisUtils.lSet(rowkey,noteString);
            }
        }
        return noteList;
    }

    private List<Note> getNoteListFromRedis(String rowkey) {
        List<Note> noteList=new ArrayList();
        Long size=this.redisUtils.lGetListSize(rowkey);
        if(size!=0)
        {
            List<Object> objects=this.redisUtils.lGet(rowkey,0,size);
            for(Object object:objects)
            {
                Note note=JSON.parseObject((String)object,Note.class);
                noteList.add(note);
            }

        }
        return noteList;
    }

    /**
     * 1.删除redis中的笔记本
     * 2。删除HBase中的笔记本
     * 3.事务：一成功都成功
     * @param noteBookName
     * @param s
     * @param s1
     * @param i
     * @return
     */
    @Override
    public boolean deleteNoteBook(String noteBookName, String s, String s1, int i) {

        try{
            deleteNoteBookFromRedis(noteBookName,s,s1,i);
            deleteNoteBookFromHBase(s+Constants.ROWKEY_SEPARATOR+s1);
        }
        catch(Exception e)
        {
            return false;
        }
        return true;
    }

    private void deleteNoteBookFromHBase(String s) {
        this.noteDao.deleteData(s,Constants.NOTEBOOK_TABLE_NAME);
    }

    private boolean deleteNoteBookFromRedis(String noteBookName,String userName,String CreateTime,int status)
    {
        //从redis中删除笔记本
        redisUtils.remove(userName+Constants.ROWKEY_SEPARATOR+CreateTime);
        //从笔记本的list中删除笔记本呢
       NoteBook noteBook=new NoteBook();
       noteBook.setStatus(status+"");
       noteBook.setName(noteBookName);
       noteBook.setCreateTime(CreateTime);
       noteBook.setRowKey(userName+Constants.ROWKEY_SEPARATOR+CreateTime);
       String value=JSON.toJSONString(noteBook);
        redisUtils.lRemove(userName,1,value);
       return true;
    }

    /**
     * 更新笔记本名称
     * 1。在redis中更新存储笔记的名字和笔记本的名字
     * 2。在HBase中更新笔记本
     * 3。事务：一成功都成功
     * @param newNoteBookName
     * @param oldNoteBookName
     * @param s
     * @param s1
     * @param i
     * @return
     */
    @Override
    public boolean updateNoteBook(String newNoteBookName, String oldNoteBookName, String s, String s1, int i) {
        String rowKey=s+Constants.ROWKEY_SEPARATOR+s1;
        try
        {
            //首先更新HBase数据
            this.noteDao.addNote2NotBook(Constants.NOTEBOOK_TABLE_NAME,rowKey,Constants.NOTEBOOK_FAMLIY_NOTEBOOKINFO,Constants.NOTEBOOK_TABLE_NAME,newNoteBookName);
            //更新redis数据
            updateNoteBookfromRedis(newNoteBookName,oldNoteBookName,s,s1,i);
        }
        catch(Exception e)
        {
            updateNoteBookfromRedis(oldNoteBookName,newNoteBookName,s,s1,i);
            this.noteDao.addNote2NotBook(Constants.NOTEBOOK_TABLE_NAME,rowKey,Constants.NOTEBOOK_FAMLIY_NOTEBOOKINFO,Constants.NOTEBOOK_TABLE_NAME,oldNoteBookName);
            return false;
        }
        finally {
            return true;
        }

    }

    private void updateNoteBookfromRedis(String newNoteBookName, String oldNoteBookName, String s, String s1, int i)
    {
        NoteBook noteBook=new NoteBook();
        noteBook.setRowKey(s+Constants.ROWKEY_SEPARATOR+s1);
        noteBook.setCreateTime(s1);
        noteBook.setName(oldNoteBookName);
        noteBook.setStatus(i+"");
        //从笔记列表中删除
        redisUtils.lRemove(s,1,JSON.toJSONString(noteBook));
        noteBook.setName(newNoteBookName);;
        redisUtils.lSet(s,JSON.toJSONString(noteBook));
    }
    /**
     * 添加笔记本
     * 1。先添加到HBase中的NOteBook中
     * 2。添加到到HBase中的Note中
     * 3.添加笔记到笔记本中
     * @param s
     * @param noteName
     * @param s1
     * @param s2
     * @param noteBookRowkey
     * @return
     */
    @Override
    public boolean addNote(String s, String noteName, String s1, String s2, String noteBookRowkey) {

        addNote2NotebookAndRedis(s,noteName,s1,s2,noteBookRowkey);
        addNote2HBase(s,noteName,s1,s2,"");
        return true;
    }

    private boolean addNote2NotebookAndRedis(String s, String noteName, String s1, String s2, String noteBookRowkey)
    {
        List<Note> noteList=this.noteDao.getNoteListByNotebook(noteBookRowkey);
        Note note=new Note();
        note.setRowKey(s);
        note.setName(noteName);
        note.setCreateTime(s1);
        note.setStatus(s2);
        note.setContent("");
        noteList.add(note);
        String noteString=JSON.toJSONString(note);
        String noteListString=JSON.toJSONString(noteList);
        redisUtils.lSet(noteBookRowkey,noteString);
        this.noteDao.addNote2NotBook(Constants.NOTEBOOK_TABLE_NAME,noteBookRowkey,Constants.NOTEBOOK_FAMLIY_NOTEBOOKINFO,Constants.NOTEBOOK_NOTEBOOKINFO_CLU_NOTELIST,noteListString);;
        return true;
    }

    private boolean addNote2HBase(String s,String noteName,String createTime,String status,String content)
    {
        String[][] data=new String[4][3];
        data[0][0]=Constants.NOTE_FAMLIY_NOTEINFO;
        data[0][1]=Constants.NOTE_NOTEINFO_CLU_NOTENAME;
        data[0][2]=noteName;

        data[1][0]=Constants.NOTE_FAMLIY_NOTEINFO;
        data[1][1]=Constants.NOTE_NOTEINFO_CLU_CREATETIME;
        data[1][2]=createTime;

        data[2][0]=Constants.NOTE_FAMLIY_NOTEINFO;
        data[2][1]=Constants.NOTE_NOTEINFO_CLU_STATUS;
        data[2][2]=status;

        data[3][0]= Constants.NOTE_FAMLIY_CONTENTINFO;
        data[3][1]= Constants.NOTE_CONTENTINFO_CLU_CONTENT;
        data[3][2]=content;
        return this.noteDao.addNote(Constants.NOTE_TABLE_NAME,s,data);

    }


    /**
     * 查询笔记详情
     * 1.根据笔记的NoteRowKey在HBase中查询
     * @param noteRowkey
     * @return
     */
    @Override
    public Note getNoteByRowKey(String noteRowkey) {
        Note note=this.noteDao.getNoteByRowKey(noteRowkey);
        return note;
    }

    /**
     * 删除笔记
     * 1.删除Notebook表中的JSON串
     * 2.删除HBase中的Note
     * 3.删除redis中的内容
     * @param noteRowKey
     * @param s
     * @param s1
     * @param oldNoteName
     * @param noteBookRowkey
     * @return
     */
    @Override
    public boolean deleteNote(String noteRowKey, String s, String s1, String oldNoteName, String noteBookRowkey) {

        return false;
    }

    private boolean deleteNotefromRedis()
    {
        return true;
    }

    /**
     * 更新笔记内容
     * 1.从NoteBook中移除旧的笔记
     * 2.添加新的笔记的到笔记本中
     * 3.更新HBase中的数据
     * 4.更新Redis中的数据
     * @param noteRowKey
     * @param noteName
     * @param s
     * @param content
     * @param s1
     * @param oldNoteName
     * @param noteBookRowkey
     * @return
     */
    @Override
    public boolean updateNote(String noteRowKey, String noteName, String s, String content, String s1, String oldNoteName, String noteBookRowkey) {
        //this.noteDao.addNote2NotBook();
        List<Note> noteList=this.noteDao.getNoteListByNotebook(noteBookRowkey);
        redisUtils.remove(noteBookRowkey);
        for(Note note:noteList)
        {
            if(note.getRowKey().equals(noteRowKey))
            {

                note.setContent(content);
                note.setName(noteName);

            }
            redisUtils.lSet(noteBookRowkey,JSON.toJSONString(note));
        }

        //更新笔记名
        this.noteDao.addNote2NotBook(Constants.NOTE_TABLE_NAME,noteRowKey,Constants.NOTE_FAMLIY_NOTEINFO,Constants.NOTE_NOTEINFO_CLU_NOTENAME,noteName);
        //更新内容
        this.noteDao.addNote2NotBook(Constants.NOTE_TABLE_NAME,noteRowKey,Constants.NOTE_FAMLIY_CONTENTINFO,Constants.NOTE_CONTENTINFO_CLU_CONTENT,content);
        //更新笔记内容
        this.noteDao.addNote2NotBook(Constants.NOTEBOOK_TABLE_NAME,noteBookRowkey,Constants.NOTEBOOK_FAMLIY_NOTEBOOKINFO,Constants.NOTEBOOK_NOTEBOOKINFO_CLU_NOTELIST,JSON.toJSONString(noteList));
        return true;
    }

    /**
     * 笔记迁移
     * 1。将笔记从一个笔记本下面迁移到另一个笔记本下面
     * 2。更新HBase数据库
     * 3.更新redis缓存
     * @param noteRowKey
     * @param oldNoteBookRowkey
     * @param newNoteBookRowkey
     * @param noteName
     * @return
     */
    @Override
    public boolean moveAndDeleteNote(String noteRowKey, String oldNoteBookRowkey, String newNoteBookRowkey, String noteName) {
        List<Note> noteList=this.noteDao.getNoteListByNotebook(oldNoteBookRowkey);
        Note noteDeleted=null;
        redisUtils.remove(oldNoteBookRowkey);
        for(Note note:noteList)
        {
            if(note.getRowKey().equals(noteRowKey))
            {
              noteDeleted=note;

            }
            else redisUtils.lSet(oldNoteBookRowkey,JSON.toJSONString(note));
        }
        noteList.remove(noteDeleted);

        //更新笔记内容
        this.noteDao.addNote2NotBook(Constants.NOTEBOOK_TABLE_NAME,oldNoteBookRowkey,Constants.NOTEBOOK_FAMLIY_NOTEBOOKINFO,Constants.NOTEBOOK_NOTEBOOKINFO_CLU_NOTELIST,JSON.toJSONString(noteList));

        List<Note> noteListNEW=this.noteDao.getNoteListByNotebook(newNoteBookRowkey);
        noteListNEW.add(noteDeleted);
        redisUtils.lSet(newNoteBookRowkey,JSON.toJSONString(noteDeleted));
        //更新笔记内容
        this.noteDao.addNote2NotBook(Constants.NOTEBOOK_TABLE_NAME,newNoteBookRowkey,Constants.NOTEBOOK_FAMLIY_NOTEBOOKINFO,Constants.NOTEBOOK_NOTEBOOKINFO_CLU_NOTELIST,JSON.toJSONString(noteListNEW));
        return true;

    }

    @Override
    public boolean activeMyNote(String noteRowKey, String newNoteBookRowkey) {
        return false;
    }

    @Override
    public boolean starOtherNote(String noteRowKey, String starBtRowKey) {
        return false;
    }

    @Override
    public boolean shareNote(String rowKey) {
        return false;
    }
}
