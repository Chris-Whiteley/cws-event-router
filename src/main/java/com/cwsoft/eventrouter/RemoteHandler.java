package com.cwsoft.eventrouter;

public interface  RemoteHandler extends Handler {
    String getRemoteService();
    String getRemoteServicesSite();

    /**
     * @param siteA - fully qualified site Name
     * @param siteB - fully qualified site Name
     * @return true if siteA and SiteB are in the same branch false otherwise
     */
    static boolean sitesInSameBranch(String siteA, String siteB) {
        if (siteA == null || siteA.isBlank()) return false;
        if (siteB == null || siteB.isBlank()) return false;
        return siteA.startsWith(siteB) || siteB.startsWith(siteA);
    }
}
