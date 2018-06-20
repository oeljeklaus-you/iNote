package cn.edu.hust.note.service;

import cn.edu.hust.note.bean.Note;
import cn.edu.hust.note.bean.NoteBook;

import java.util.List;

public interface NoteService
{
    List<NoteBook> getAllNoteBook(String userName);

    boolean addNoteBook(String noteBookName, String userName, String s, int i);

    List<Note> getNoteListByNotebook(String rowkey);

    boolean deleteNoteBook(String noteBookName, String s, String s1, int i);

    boolean updateNoteBook(String newNoteBookName, String oldNoteBookName, String s, String s1, int i);

    boolean addNote(String s, String noteName, String s1, String s2, String noteBookRowkey);

    Note getNoteByRowKey(String noteRowkey);

    boolean deleteNote(String noteRowKey, String s, String s1, String oldNoteName, String noteBookRowkey);

    boolean updateNote(String noteRowKey, String noteName, String s, String content, String s1, String oldNoteName, String noteBookRowkey);

    boolean moveAndDeleteNote(String noteRowKey, String oldNoteBookRowkey, String newNoteBookRowkey, String noteName);

    boolean activeMyNote(String noteRowKey, String newNoteBookRowkey);

    boolean starOtherNote(String noteRowKey, String starBtRowKey);

    boolean shareNote(String rowKey);
}
