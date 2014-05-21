package com.smartcodeltd.jenkinsci.plugins.buildmonitor.viewmodel;

import static hudson.model.Result.ABORTED;
import static hudson.model.Result.FAILURE;
import static hudson.model.Result.NOT_BUILT;
import static hudson.model.Result.SUCCESS;
import static hudson.model.Result.UNSTABLE;
import hudson.model.Job;
import hudson.model.Run;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.codehaus.jackson.annotate.JsonProperty;

import com.smartcodeltd.jenkinsci.plugins.buildmonitor.viewmodel.plugins.BuildAugmentor;

/**
 * @author Jan Molak
 */
public class JobView {
    private final Date systemTime;
    private final Job<?, ?> job;
    private final BuildAugmentor augmentor;

    public static JobView of(final Job<?, ?> job) {
        return new JobView(job, new BuildAugmentor(), new Date());
    }

    public static JobView of(final Job<?, ?> job, final BuildAugmentor augmentor) {
        return new JobView(job, augmentor, new Date());
    }

    public static JobView of(final Job<?, ?> job, final Date systemTime) {
        return new JobView(job, new BuildAugmentor(), systemTime);
    }

    @JsonProperty
    public String name() {
        return (null != job.getDisplayNameOrNull())
                ? job.getDisplayName()
                : job.getName();
    }

    @JsonProperty
    public String url() {
        return job.getUrl();
    }

    @JsonProperty
    public String status() {
        // todo: consider introducing a BuildResultJudge to keep this logic in one place
        String status = "unknown";

        if(lastCompletedBuild().result() == SUCCESS) {
            status = "successful";
        } else if(lastCompletedBuild().result() == FAILURE) {
            status = "failing";
        } else if(lastCompletedBuild().result() == NOT_BUILT) {
            status = "not_built";
        } else if(lastCompletedBuild().result() == ABORTED) {
            status = "aborted";
        } else if(lastCompletedBuild().result() == UNSTABLE) {
            status = "unstable";
        }

        if (lastBuild().isRunning()) {
            status += " running";
        }

        if (lastCompletedBuild().isClaimed()) {
            status += " claimed";
        }

        return status;
    }

    @JsonProperty
    public String lastBuildName() {
        return lastBuild().name();
    }

    @JsonProperty
    public String lastBuildUrl() {
        return lastBuild().url();
    }

    @JsonProperty
    public String lastBuildDuration() {
        if (lastBuild().isRunning()) {
            return formatted(lastBuild().elapsedTime());
        }

        return formatted(lastBuild().duration());
    }

    @JsonProperty
    public String estimatedDuration() {
        return formatted(lastBuild().estimatedDuration());
    }

    private String formatted(final Duration duration) {
        return null != duration
                ? duration.toString()
                : "";
    }

    @JsonProperty
    public int progress() {
        return lastBuild().progress();
    }

    @JsonProperty
    public Set<String> culprits() {
        final Set<String> culprits = new HashSet<String>();

        BuildViewModel build = lastBuild();
        // todo: consider introducing a BuildResultJudge to keep this logic in one place
        while (! SUCCESS.equals(build.result())) {
            culprits.addAll(build.culprits());

            if (! build.hasPreviousBuild()) {
                break;
            }

            build = build.previousBuild();
        };

        return culprits;
    }

    @JsonProperty
    public boolean isClaimed() {
        return lastCompletedBuild().isClaimed();
    }

    @JsonProperty
    public String claimAuthor() {
        return lastCompletedBuild().claimant();
    }

    @JsonProperty
    public String claimReason() {
        return lastCompletedBuild().reasonForClaim();
    }

    @JsonProperty
    public boolean hasKnownFailures() {
        return lastCompletedBuild().hasKnownFailures();
    }

    @JsonProperty
    public List<String> knownFailures() {
        return lastCompletedBuild().knownFailures();
    }

    @Override
    public String toString() {
        return name();
    }


    private JobView(final Job<?, ?> job, final BuildAugmentor augmentor, final Date systemTime) {
        this.job        = job;
        this.augmentor  = augmentor;
        this.systemTime = systemTime;
    }

    private BuildViewModel lastBuild() {
        return buildViewOf(job.getLastBuild());
    }

    private BuildViewModel lastCompletedBuild() {
        BuildViewModel previousBuild = lastBuild();
        while (previousBuild.isRunning() && previousBuild.hasPreviousBuild()) {
            previousBuild = previousBuild.previousBuild();
        }

        return previousBuild;
    }

    private BuildViewModel buildViewOf(final Run<?, ?> build) {
        if (null == build) {
            return new NullBuildView();
        }

        return BuildView.of(job.getLastBuild(), augmentor, systemTime);
    }
}