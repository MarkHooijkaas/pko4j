When executing a command, the following steps will be taken:
1. save the command to a commit queue
2. apply the command change to the memory model
3. return control to the caller

If the application starts it will 
1. read all memory objects from the storage.
2. for all (uncommitted) commands from the commit queue
  * read the command
  * apply the command change to the memory model
3. start the command handler
4. start the rest of the application (especially receiving new commands)


The command handler will, in order of the queue 
1. write a flag that the command is being handled
2. write the target object to storage
3. write possible command-specific other files in the store (e.g. appending notifications)
4. send possible command-specific messages (e.g. for sending emails)
5. git commit
6. remove the command from the commit queue

Note. Steps 1, 2, 5 and 6 are idempotent. 

Step 4 and 5 will be empty for many commands, but some may not be safe to retry.
Some mechanism is needed to handle that. 
Step 1 does not help much there yet, but might be useful for detecting this

interface PkoCommand {
	void applyMemoryChanges();
	void otherActions(boolean restart);
}