package org.kisst.pko4j.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.kisst.item4j.struct.ReflectStruct;
import org.kisst.util.FileUtil;

public class GitStorage implements Runnable {
	private final int dirLength;
	private final Git git;
	private final ArrayBlockingQueue<Commit> queue=new ArrayBlockingQueue<>(10);
	
	private AtomicLong counter = new AtomicLong(System.currentTimeMillis());

	
	public final class Commit extends ReflectStruct {
		private class FileChange {
			public final File file;
			protected FileChange(File f) { this.file=f; }
			public FileChange(File f, String content) { 
				this.file=f;
				File file= todoFile();
				File dir = file.getParentFile();
				if (! dir.exists())
					dir.mkdirs();
				FileUtil.saveString(file , content);
			}
			protected File todoFile() { return new File(file.getAbsolutePath()+"."+id+".todo"); }
			public void prepareCommit() { 
				if (file.exists())
					file.delete();
				todoFile().renameTo(file);
			}
		}
		private class AppendToFile extends FileChange {
			public final String content;
			public AppendToFile(File f, String content) { super(f); this.content=content; }
			public void prepareCommit() { 
				FileUtil.appendString(file, content);
			}
		}
		private class DeleteFile extends FileChange {
			public DeleteFile(File f) { 
				super(f);
				//File f = ,"deleted");
				//FileUtil.saveString(f, "deleted");
			}
			public void prepareCommit() { 
				file.delete();
			}
		}
		
		public final String id;
		public final String user;
		public final String mail;		
		public final String comment;
		public final ArrayList<FileChange> changes=new ArrayList<>();;
		public Commit(String user, String mail,String comment) {
			this.id=""+counter.getAndIncrement();
			this.user=user;
			this.mail=mail;
			this.comment=comment;
		}
		public Commit newFile(File filename, String content) { changes.add(new FileChange(filename,content)); return this; } // TODO: check file does not yet exist
		public Commit changeFile(File filename, String content) { changes.add(new FileChange(filename,content)); return this; }
		public Commit appendToFile(File filename, String content) { changes.add(new AppendToFile(filename,content)); return this; }
		public Commit deleteFile(File filename) { changes.add(new DeleteFile(filename)); return this; }
		
		
		public void enqueue() { queue.add(this); }
		private void commit() {
			synchronized(git) {				
				try {
					for (FileChange c: changes) {
						c.prepareCommit();
						git.add().addFilepattern(c.file.getAbsolutePath().substring(dirLength)).call();
					}
					//git.add().addFilepattern(".").call();
					git.commit().setAuthor(user,mail).setMessage(comment).call();
				}
				catch (GitAPIException e) { throw new RuntimeException(e); }
			}
		}
	}

	public void run() {
		while (true) {
			Commit c;
			try {
				c = queue.take();
			}
			catch (InterruptedException e) {  break; }
			c.commit();
		}
	}
	public GitStorage(File dir) {
		this.dirLength=dir.getAbsolutePath().length()+1;
		try {
			git = Git.open(dir);
			new Thread(this).start();
		}
		catch (IOException e) { throw new RuntimeException(e);}
	}

	public Commit createCommit(String user, String ip,String comment) { return new Commit(user,ip,comment); }
}
