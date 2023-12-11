package org.joshihut.koboget;

import java.util.Map;

public class ProjectCache {
    private static ProjectCache instance;
    private Map<String, String> projectsMap;
    private ProjectCache() {

    }

    public static ProjectCache getInstance() {
        if (instance == null) {
            instance = new ProjectCache();
        }

        return instance;
    }

    public void cacheProjectsMap(Map<String, String> projects) {
        projectsMap = projects;
    }

    public Map<String, String> getProjectsMap() {
        return projectsMap;
    }
}
