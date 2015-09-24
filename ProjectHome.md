Vimoir is a server and a library for building [Netbeans](http://vimhelp.appspot.com/netbeans.txt.html#netbeans.txt) applications interfaced with [Vim](http://www.vim.org/) and that includes an example allowing to hear (_oir_ in spanish) Vim with [phonemic](http://sourceforge.net/apps/trac/phonemic/wiki).

Use the vimoir library to interface a java or a python application with Vim through the Netbeans protocol. The client application implements the `NetbeansEventHandler` interface which maps Vim Netbeans events to callback methods. The vimoir library takes care of the details of the protocol such as registering buffers with a `bufID`, managing `seqno` command sequence numbers, instantiating the client class on each new connection request from a Vim instance and managing the corresponding concurrent Netbeans socket connections.

More information is available on the [Wiki](http://code.google.com/p/vimoir/w/list). The library is documented with javadoc and the corresponding html documentation is packaged with the tarball that may be downloaded from the [Downloads](http://code.google.com/p/vimoir/downloads/list) tab.

<br>

<h4>Process</h4>
Another example is the <code>Process</code> client that allows to start tasks from Vim in the background, and to get the results of the process execution in a Vim balloon when the process terminates. The example is not very useful as the balloon vanishes with cursor and mouse movements, but it demonstrates how to use the library. There is a python <a href='http://code.google.com/p/vimoir/source/browse/src/examples/process.py'>Process</a> class and a java <code>Process</code> class, see below the java class. Please note that this example does not work very well on Windows as the balloon does not always show up, so instead run the Phonemic example that works even when phonemic is not installed.<br>
<br>
To run the java <code>Process</code> class, one must edit the <code>vimoir.netbeans.java.client</code> property in the <code>conf/vimoir.properties</code> file and set its value to <code>vimoir.examples.Process</code>.  To start the server, add the directory containing the properties file to the java classpath and run the <code>vimoir.netbeans.Netbeans</code> class located in <code>netbeans.jar</code> (add also <code>netbeans-examples.jar</code> to the classpath as it contains the <code>Process</code> class).<br>
<br>
Then <i>(a)</i> run the <code>:nbstart</code> command in Vim to connect to the server, <i>(b)</i> <b>edit</b> a file if necessary since <code>:nbkey</code> commands are postponed when the current buffer is the <code>[No Name]</code> buffer and <i>(c)</i> run the <code>:nbkey run proc_name args...</code> command to start <code>proc_name</code>.<br>
<br>
<br>

<pre><code><br>
/**<br>
* Run a process from a Vim :nbkey run command in a worker thread.<br>
*<br>
* The process is run in a thread so as not to block I/O on the Netbeans<br>
* socket. On the process termination, the process stdout and stderr output is<br>
* displayed in a Vim balloon.<br>
*<br>
* &lt;p&gt;For example to run a python command that sleeps 2 seconds and prints<br>
* 'Ok', run in Vim:<br>
*<br>
*      :nbkey run python -c "import time; time.sleep(2); print \"Ok\""<br>
*<br>
* &lt;p&gt;This class extends {@link vimoir.netbeans.NetbeansClient} so as to<br>
* implement only the methods it is interested in.<br>
*/<br>
public class Process extends NetbeansClient implements NetbeansEventHandler {<br>
<br>
/**<br>
* Constructor.<br>
*<br>
* @param nbsock the Netbeans socket<br>
*/<br>
public Process(NetbeansSocket nbsock) {<br>
super(nbsock);<br>
}<br>
<br>
/**<br>
* Run a process in a worker thread.<br>
*<br>
* @param buf   the buffer instance (not used here)<br>
* @param args  the remaining string of the :nbkey run command<br>
*/<br>
public void cmd_run(NetbeansBuffer buf, final String args) {<br>
final String[] command = this.nbsock.split_quoted_string(args);<br>
Thread thread = new Thread(new Runnable() {<br>
public void run() {<br>
String output = "";<br>
try {<br>
java.lang.Process p = new ProcessBuilder(Arrays.asList(command))<br>
.redirectErrorStream(true).start();<br>
p.waitFor();<br>
BufferedReader br = new BufferedReader(<br>
new InputStreamReader(p.getInputStream()));<br>
String line = null;<br>
while ((line = br.readLine()) != null)<br>
output += line + "\n";<br>
} catch (IOException e) {<br>
nbsock.send_cmd(null, "showBalloon", nbsock.quote(e.toString()));<br>
return;<br>
} catch (InterruptedException e) {<br>
nbsock.send_cmd(null, "showBalloon", nbsock.quote(e.toString()));<br>
return;<br>
}<br>
String msg = "Result of process '" + args + "':\n" + output;<br>
nbsock.send_cmd(null, "showBalloon", nbsock.quote(msg));<br>
}<br>
});<br>
thread.start();<br>
}<br>
}<br>
</code></pre>