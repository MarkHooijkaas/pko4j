package org.kisst.pko4j;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

import org.kisst.item4j.json.JsonOutputter;
//import org.kisst.item4j.json.JsonParser;
import org.kisst.item4j.struct.Struct;
import org.kisst.util.FileUtil;

public class PkoQueue<T extends Struct> {
	private final ConcurrentLinkedDeque<Entry<T>> queue=new ConcurrentLinkedDeque<>();
	private final File dir;
	private AtomicLong counter = new AtomicLong(System.currentTimeMillis());
	//private final JsonParser parser=new JsonParser();
	private final JsonOutputter outputter = new JsonOutputter(null);


	private final class Entry<TT> {
		public final File file;
		public final TT obj;
		public Entry(File f, TT obj) { this.file=f; this.obj=obj; }
	}
	
	public PkoQueue(File dir) {
		this.dir=dir;
		File[] files = dir.listFiles();
		Arrays.sort(files);
		for (File f: files) {
			if (f.getName().endsWith(",msg"))
				queue.add(new Entry<T>(f,fromString(FileUtil.loadString(f))));
		}
	} 

	
	public void put(T obj) {
		File f=new File(dir, counter.getAndIncrement()+".msg");
		FileUtil.saveString(f, toString(obj));
		queue.add(new Entry<>(f,obj));
	}
	public T get() {
		Entry<T> entry=queue.poll();
		if (entry.file.exists())
			entry.file.delete();
		return entry.obj;
	}
	
	
	
	public String toString(T obj) { return outputter.createString(obj); }
	public T fromString(String s) { return null; }//parser.parse(s); } // TODO
	
	
}
