package cn.edu.hust.note.dao;

import cn.edu.hust.note.bean.Note;
import cn.edu.hust.note.bean.NoteBook;

import java.util.List;

public interface NoteDao {
    List<NoteBook> getAllNoteBook(String userName);


    boolean addNoteBook(String noteName,String rowKey, String[][] data);

    List<Note> getNoteListByNotebook(String rowkey);

    boolean addNote( String tableName,String rowKey, String[][] data);

    void addNote2NotBook(String notebookTableName, String rowKey,String notebookFamliyNotebookinfo, String notebookNotebookinfoCluNotelist, String noteListString);

    void deleteData(String s,String tableName);

    Note getNoteByRowKey(String noteRowkey);

    //void updateData(String notebookTableName, String rowKey, String notebookFamliyNotebookinfo, String newNoteBookName);
}
