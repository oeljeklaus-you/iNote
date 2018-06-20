package cn.edu.hust.note.dao.impl;

import cn.edu.hust.note.bean.Note;
import cn.edu.hust.note.bean.NoteBook;
import cn.edu.hust.note.dao.NoteDao;
import cn.edu.hust.utils.constants.Constants;
import com.alibaba.fastjson.JSONArray;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.BinaryPrefixComparator;
import org.apache.hadoop.hbase.filter.CompareFilter;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.RowFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.hadoop.hbase.HbaseTemplate;
import org.springframework.data.hadoop.hbase.TableCallback;
import org.springframework.stereotype.Repository;

import javax.ws.rs.GET;
import java.util.ArrayList;
import java.util.List;

@Repository
public class NoteDaoImpl implements NoteDao{
    @Autowired
    private HbaseTemplate hbaseTemplate;
    @Override
    public List<NoteBook> getAllNoteBook(final String userName) {

        final List<NoteBook> noteBookList=new ArrayList<>();
        this.hbaseTemplate.execute(Constants.NOTEBOOK_TABLE_NAME, new TableCallback<Object>() {
            @Override
            public Object doInTable(HTableInterface hTableInterface) throws Throwable {
                Filter filter=new RowFilter(CompareFilter.CompareOp.EQUAL,new BinaryPrefixComparator((userName+Constants.ROWKEY_SEPARATOR).getBytes()));
                Scan s = new Scan();
                s.setFilter(filter);
                ResultScanner rs = hTableInterface.getScanner(s);
                for(Result result:rs){
                    NoteBook noteBook=new NoteBook();
                    noteBook.setRowKey(new String(result.getRow()));
                    noteBook.setCreateTime(new String(result.getValue(Constants.NOTEBOOK_FAMLIY_NOTEBOOKINFO.getBytes(),Constants.NOTEBOOK_NOTEBOOKINFO_CLU_CREATETIME.getBytes())));
                    noteBook.setName(new String(result.getValue(Constants.NOTEBOOK_FAMLIY_NOTEBOOKINFO.getBytes(),Constants.NOTEBOOK_NOTEBOOKINFO_CLU_NOTEBOOKNAME.getBytes())));
                    noteBook.setStatus(new String(result.getValue(Constants.NOTEBOOK_FAMLIY_NOTEBOOKINFO.getBytes(),Constants.NOTEBOOK_NOTEBOOKINFO_CLU_STATUS.getBytes())));
                    noteBookList.add(noteBook);
                }
                return null;
            }
        });
        return noteBookList;
    }

    @Override
    public boolean addNoteBook(String tableName,final String rowKey,final String[][] data) {
        return insertData(tableName,rowKey,data);
    }

    private boolean insertData(final String tableName,final String rowKey,final String[][] data)
    {
        this.hbaseTemplate.execute(tableName,new TableCallback<Object>(){
            @Override
            public Object doInTable(HTableInterface hTableInterface) throws Throwable {
                Put put = new Put(rowKey.getBytes());
                for(int i=0;i<data.length;i++)
                {
                    put.addColumn(data[i][0].getBytes(),data[i][1].getBytes(),data[i][2].getBytes());
                }
                hTableInterface.put(put);
                return null;
            }
        });
        return true;
    }


    @Override
    public List<Note> getNoteListByNotebook(final String rowkey) {
         final List<Note> noteList=new ArrayList<>();
        this.hbaseTemplate.execute(Constants.NOTEBOOK_TABLE_NAME, new TableCallback<Object>() {
            @Override
            public Object doInTable(HTableInterface hTableInterface) throws Throwable {
                Get get=new Get(rowkey.getBytes());
                //get.addColumn();
                Result rs=hTableInterface.get(get);
                String noteListString=new String(rs.getValue(Constants.NOTEBOOK_FAMLIY_NOTEBOOKINFO.getBytes(),Constants.NOTEBOOK_NOTEBOOKINFO_CLU_NOTELIST.getBytes()));
                List<Note> noteList1=JSONArray.parseArray(noteListString,Note.class);
                noteList.addAll(noteList1);
                return null;
            }
        });
        return noteList;
    }

    @Override
    public boolean addNote(String tableName, String rowKey, String[][] data) {
        return insertData(tableName,rowKey,data);
    }

    @Override
    public void addNote2NotBook(String notebookTableName, final String rowKey,final String notebookFamliyNotebookinfo, final String notebookNotebookinfoCluNotelist, final String noteListString) {
        this.hbaseTemplate.execute(notebookTableName, new TableCallback<Object>() {
            @Override
            public Object doInTable(HTableInterface hTableInterface) throws Throwable {
                Put put=new Put(rowKey.getBytes());
                put.addColumn(notebookFamliyNotebookinfo.getBytes(),notebookNotebookinfoCluNotelist.getBytes(),noteListString.getBytes());
                hTableInterface.put(put);
                return null;
            }
        });
    }

    @Override
    public void deleteData(final String s,String tableName) {
        this.hbaseTemplate.execute(tableName,new TableCallback<Object>(){
            @Override
            public Object doInTable(HTableInterface hTableInterface) throws Throwable {
                Delete delete=new Delete(s.getBytes());
                hTableInterface.delete(delete);
                return null;
            }
        });
    }

    @Override
    public Note getNoteByRowKey(final String noteRowkey) {
        final Note note=new Note();
        this.hbaseTemplate.execute(Constants.NOTE_TABLE_NAME, new TableCallback<Object>() {
            @Override
            public Object doInTable(HTableInterface hTableInterface) throws Throwable {
                Get get=new Get(noteRowkey.getBytes());
                Result rs=hTableInterface.get(get);
                //Note note=new Note();
                note.setRowKey(noteRowkey);
                note.setName(new String(rs.getValue(Constants.NOTE_FAMLIY_NOTEINFO.getBytes(),Constants.NOTE_NOTEINFO_CLU_NOTENAME.getBytes())));
                note.setCreateTime(new String(rs.getValue(Constants.NOTE_FAMLIY_NOTEINFO.getBytes(),Constants.NOTE_NOTEINFO_CLU_CREATETIME.getBytes())));
                note.setStatus(new String(rs.getValue(Constants.NOTE_FAMLIY_NOTEINFO.getBytes(),Constants.NOTE_NOTEINFO_CLU_STATUS.getBytes())));
                note.setContent(new String(rs.getValue(Constants.NOTE_FAMLIY_CONTENTINFO.getBytes(),Constants.NOTE_CONTENTINFO_CLU_CONTENT.getBytes())));
                return null;
            }
        });
        return note;
    }


}
