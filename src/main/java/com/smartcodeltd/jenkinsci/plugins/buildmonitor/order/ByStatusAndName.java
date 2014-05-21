package com.smartcodeltd.jenkinsci.plugins.buildmonitor.order;

import hudson.model.Result;
import hudson.model.AbstractProject;
import hudson.model.Run;

import java.util.Comparator;

public class ByStatusAndName implements Comparator<AbstractProject> {

    @Override
    public int compare(final AbstractProject a, final AbstractProject b) {
        final Run lastBuildA = lastCompletedBuild(a);
        final Run lastBuildB = lastCompletedBuild(b);
        if (lastBuildA == null && lastBuildB != null)
            return 1;
        if (lastBuildA != null && lastBuildB == null)
            return -1;
        if (lastBuildA != null && lastBuildB != null) {
            final int resultCompare =
                    compare(lastBuildA.getResult(), lastBuildB.getResult());
            if (resultCompare != 0)
                return resultCompare;
        }
        return a.getName().compareToIgnoreCase(b.getName());
    }

    private int compare(final Result resultA, final Result resultB) {
        if (resultA.isWorseThan(resultB))
            return -1;
        if (resultA.isBetterThan(resultB))
            return 1;
        return 0;
    }

    private Run lastCompletedBuild(final AbstractProject a) {
        final Run lastBuild = a.getLastBuild();
        if (lastBuild == null)
            return null;
        if (!lastBuild.isBuilding()) {
            return lastBuild;
        }
        return lastCompletedBuild(lastBuild);
    }

    private Run lastCompletedBuild(final Run build) {
        final Run previousBuild = build.getPreviousBuild();
        if (previousBuild == null)
            return null;
        if (!previousBuild.isBuilding())
            return previousBuild;
        return lastCompletedBuild(previousBuild);
    }

}
