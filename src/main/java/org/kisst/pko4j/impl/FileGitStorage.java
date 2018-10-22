package org.kisst.pko4j.impl;

import org.kisst.item4j.ImmutableSequence;

import org.kisst.pko4j.PkoObject;
import org.kisst.pko4j.PkoSchema;
import org.kisst.pko4j.impl.GitStorage.Commit;
import org.kisst.props4j.Props;
import org.kisst.util.CallInfo;


import java.io.File;

public class FileGitStorage<T extends PkoObject> extends FileStorage<T>  {
	private final GitStorage git;


	@SuppressWarnings("unchecked")
	public FileGitStorage(PkoSchema<T> schema, GitStorage git, File maindir) {
		super(schema, maindir);
		this.git=git;
	}
	public FileGitStorage(PkoSchema<T> schema, Props props, GitStorage git) {
		this(schema, git, new File(props.getString("datadir", "data"))); 
	}

	@Override public void create(T value) {
		String key = value.getKey();
		if (key==null)
			key=createUniqueKey();
		createCommit("create", value)
			.newFile(getFile(value), createSaveString(value))
			.enqueue();
	}
	

	@Override public void update(T oldValue, T newValue) {
		// The newValue may contain an id, but that is ignored
		createCommit("update "+name,oldValue)
			.changeFile(getFile(oldValue), createSaveString(newValue))
			.enqueue();
	}
	@Override public void delete(T oldValue)  {
		checkForConcurrentModification(oldValue);
		createCommit("delete ",oldValue)
			.deleteFile(getFile(oldValue))
			.enqueue();
	}

	private Commit createCommit (String action, T value) {
		try {
			String data = "all "+this.name;
			if (value!=null)
				data=value.getName();
			CallInfo callinfo = CallInfo.instance.get();
			if (callinfo.action!=null)
				action=callinfo.action;
			if (callinfo.data!=null)
				data=callinfo.data;
			String comment = action+" on "+data;
			return git.createCommit(callinfo.user, callinfo.ip,comment);
		}
		catch (Exception e) { throw new RuntimeException(e); }
	}

	@Override public ImmutableSequence<HistoryItem> getHistory(String key, String path) {
		return git.getHistory(getFile(key,path), new File(dir, key+".rec"));
	}
	@Override public ImmutableSequence<HistoryItem> getHistory(String key) {
		return git.getHistory(getFile(key,"record.dat"), new File(dir, key+".rec"));
	}

	public void saveAll(Iterable<T> records) {
		Commit comm = createCommit("save", null);
		for (T rec: records)
			comm.changeFile(getFile(rec), createSaveString(rec));
		comm.enqueue();
	}

}
