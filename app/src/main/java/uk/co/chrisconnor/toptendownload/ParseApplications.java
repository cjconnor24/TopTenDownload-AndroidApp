package uk.co.chrisconnor.toptendownload;

import java.util.ArrayList;

public class ParseApplications {

    private static final String TAG = "ParseApplications";

    private ArrayList<FeedEntry> applications;

    public ParseApplications() {
        this.applications = new ArrayList<>();
    }

    public ArrayList<FeedEntry> getApplications() {
        return applications;
    }
}
