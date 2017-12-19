package org.processmining.plugins.etm.tests;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.annotations.UIImportPlugin;
import org.processmining.framework.abstractplugins.AbstractImportPlugin;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginLevel;
import org.processmining.processtree.ProcessTree;
import org.processmining.processtree.impl.ProcessTreeImpl;
import org.processmining.processtree.ptml.Ptml;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

@Plugin(name = "Import Process tree from PTML file", level = PluginLevel.PeerReviewed, parameterLabels = { "Filename" }, returnLabels = { "Process Tree" }, returnTypes = { ProcessTree.class })
@UIImportPlugin(description = "PTML Process tree files", extensions = { "ptml" })
public class ImportPtmlTest extends AbstractImportPlugin {
	private String fileName;
     public ImportPtmlTest(String filename) {
	this.fileName=filename;
	}
	protected FileFilter getFileFilter() {
		return new FileNameExtensionFilter("PTML files", "ptml");
	}

	protected Object importFromStream(PluginContext context, InputStream input, String filename, long fileSizeInBytes)
			throws Exception {
		Ptml ptml = importPtmlFromStream(context, input, filename, fileSizeInBytes);
		if (ptml == null) {
			/*
			 * No PTML found in file. Fail.
			 */
			JOptionPane.showMessageDialog(null, "No PTML-formatted process tree was found in file \"" + filename + "\".");
			return null;
		}
		/*
		 * PTML file has been imported. Now we need to convert the contents to a
		 * regular Process tree.
		 */
		ProcessTree tree = new ProcessTreeImpl(ptml.getId(), ptml.getName());

		tree = connectTree(context, ptml, tree);
		
		if (context != null) {
			if(tree.getName() == null){
				tree.setName("No Name");
			}
			if(!tree.getName().equalsIgnoreCase("")) {
				context.getFutureResult(0).setLabel(tree.getName());
			}
			else{
				context.getFutureResult(0).setLabel("Imported Tree@" + System.currentTimeMillis());
			}
		}
		
		return tree; 
	}

	public Ptml importPtmlFromStream(PluginContext context, InputStream input, String filename, long fileSizeInBytes)
			throws Exception {
		/*
		 * Get an XML pull parser.
		 */
		XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
		factory.setNamespaceAware(true);
		XmlPullParser xpp = factory.newPullParser();
		/*
		 * Initialize the parser on the provided input.
		 */
		xpp.setInput(input, null);
		/*
		 * Get the first event type.
		 */
		int eventType = xpp.getEventType();
		/*
		 * Create a fresh PNML object.
		 */
		Ptml ptml = new Ptml();

		/*
		 * Skip whatever we find until we've found a start tag.
		 */
		while (eventType != XmlPullParser.START_TAG) {
			eventType = xpp.next();
		}
		/*
		 * Check whether start tag corresponds to PNML start tag.
		 */
		if (xpp.getName().equals(Ptml.TAG)) {
			/*
			 * Yes it does. Import the PNML element.
			 */
			ptml.importElement(context, xpp, ptml);
		} else {
			/*
			 * No it does not. Return null to signal failure.
			 */
			ptml.log(Ptml.TAG, xpp.getLineNumber(), "Expected ptml");
		}
		
		ptml.log("Acyclicity check");
		ptml.checkAcyclicity();
		
		if (ptml.hasErrors()) {
			context.getProvidedObjectManager().createProvidedObject("Log of PTML import", ptml.getLog(), XLog.class,
					context);
			return null;
		}
		return ptml;
	}

	private ProcessTree connectTree(PluginContext context, Ptml ptml, ProcessTree tree) {
		ptml.unmarshall(tree);
//		System.out.println("Tree contains " + tree.getNodes().size() + " nodes and " + tree.getEdges().size() + "  edges.");
		return tree;
	}
	
	public ProcessTree importPtmlFie(){
	
		PluginContext context = null;
		InputStream input = null;
		try {
			input = new FileInputStream(fileName);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		long fileSizeInBytes = 0;
		String filename = " ";
		ProcessTree tree = null;
		try {
			tree = (ProcessTree) importFromStream(context, input, filename, fileSizeInBytes);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    //System.out.println(tree.toString());
		return tree;
	}
}
