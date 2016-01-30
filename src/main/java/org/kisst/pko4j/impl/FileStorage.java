package org.kisst.pko4j.impl;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.kisst.item4j.json.JsonOutputter;
import org.kisst.item4j.json.JsonParser;
import org.kisst.item4j.seq.ArraySequence;
import org.kisst.item4j.seq.TypedSequence;
import org.kisst.item4j.struct.MultiStruct;
import org.kisst.item4j.struct.SingleItemStruct;
import org.kisst.item4j.struct.Struct;
import org.kisst.pko4j.PkoModel;
import org.kisst.pko4j.PkoObject;
import org.kisst.pko4j.PkoSchema;
import org.kisst.pko4j.StructStorage;
import org.kisst.pko4j.impl.GitStorage.Commit;
import org.kisst.props4j.Props;
import org.kisst.util.CallInfo;
import org.kisst.util.FileUtil;

public class FileStorage<T extends PkoObject> implements StructStorage<T> {
	public final PkoSchema<T> schema;

	private final File dir;
	private final String name;
	private final JsonParser parser=new JsonParser();
	private final JsonOutputter outputter = new JsonOutputter(null);
	private final Class<T> cls;
	private final GitStorage git;
	
	//private final Repository gitrepo;

	
	
	@SuppressWarnings("unchecked")
	public FileStorage(PkoSchema<T> schema, GitStorage git, File maindir) {
		this.schema=schema;
		this.cls=(Class<T>) schema.getJavaClass();
		this.name=cls.getSimpleName();
		dir=new File(maindir,name);
		if (! dir.exists())
			dir.mkdirs();
		this.git=git;
	}
	public FileStorage(PkoSchema<T> schema, Props props, GitStorage git) {
		this(schema, git, new File(props.getString("datadir", "data"))); 
	}
	
	@Override public Class<T> getRecordClass() { return cls; }

	@Override public void create(T value) {
		String key = value.getKey();
		if (key==null)
			key=createUniqueKey();
		createCommit("create", value)
			.newFile(getFile(value), outputter.createString(value))
			.enqueue();
	}
	
	private static AtomicInteger number=new AtomicInteger(new Random().nextInt(13));
	private String createUniqueKey() {
		int i=number.incrementAndGet();
		return Long.toHexString(System.currentTimeMillis())+Integer.toHexString(i);
	}
	
	private Struct createStruct(File f) {
		Struct result = new MultiStruct(
				parser.parse(f),
				new SingleItemStruct("savedModificationDate", Instant.ofEpochMilli(f.lastModified()))
				);
		return result;
	}
	
	
	@Override public void update(T oldValue, T newValue) {
		// The newValue may contain an id, but that is ignored
		createCommit("update "+name,oldValue)
			.changeFile(getFile(oldValue), outputter.createString(newValue))
			.enqueue();
	}
	@Override public void delete(T oldValue)  {
		checkForConcurrentModification(oldValue);
		createCommit("delete ",oldValue)
			.deleteFile(getFile(oldValue))
			.enqueue();
	}
	private void checkForConcurrentModification(T obj) {
		// TODO Auto-generated method stub

	}
	private File getFile(String key, String path) { return new File(dir, key+".dir/"+path); }
	private File getFile(T value) { return getFile(value.getKey(), "record.dat"); }

	@Override public TypedSequence<T> findAll(PkoModel model) {
		ArrayList<T> list=new ArrayList<>();
		long start= System.currentTimeMillis();
		//System.out.println("loading all records from "+name);
		int count=0;
		for (File f:dir.listFiles()) {
			try {
				String key=f.getName();
				if (! (key.endsWith(".dir") && f.isDirectory()))
					continue;
				if (new File(dir,"deleted").exists())
					continue;
				File newest=null;
				for (File f3 : f.listFiles()) {
					if (f3.getName().equals("record.dat") || f3.getName().endsWith(".todo")) {
						if (newest==null)
							newest=f3;
						else if (f3.getName().compareTo(newest.getName())>0)
							newest=f3;
					}
				}
				count++;
				key=key.substring(0,key.length()-4);
				Struct doc=createStruct(newest);
				list.add(schema.createObject(model, doc));
			}
			catch (Exception e) { e.printStackTrace();}// TODO: return dummy placeholder
		}
		System.out.println("DONE loading "+count+" records from "+name+" in "+(System.currentTimeMillis()-start)+" milliseconds");
		return new ArraySequence<T>(cls,list);
	}

	private Commit createCommit (String action, T value) {
		try {
			String data = value.getName();
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

	public String readBlob(String key, String path) {
		File f = getFile(key, path);
		return FileUtil.loadString(f);
	}
	public void writeBlob(String key, String path, String blob) {
		File f = getFile(key, path);
		FileUtil.saveString(f, blob);
	}
	public void appendBlob(String key, String path, String blob) {
		File f = getFile(key, path);
		FileUtil.appendString(f, blob);
	}
}
