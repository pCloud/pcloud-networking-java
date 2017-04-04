package com.pcloud.example;

import com.pcloud.networking.ParameterValue;

import java.util.List;

public class DiffResultResponse extends ApiResponse {

    @ParameterValue
    private String from;

    @ParameterValue("diffid")
    private long diffId;

    @ParameterValue("entries")
    private List<DiffEntry> entries;

    protected DiffResultResponse() {
    }

    public DiffResultResponse(long result, String message) {
        super(result, message);
    }

    public String from() {
        return from;
    }

    public long diffId() {
        return diffId;
    }

    public List<DiffEntry> entries() {
        return entries;
    }

    public static class DiffEntry {
        @ParameterValue("event")
        private String event;

        @ParameterValue("diffid")
        private long diffId;

        @ParameterValue("time")
        private String time;

        @ParameterValue("metadata")
        private Metadata metadata;
    }

    public static class Metadata {

        @ParameterValue("name")
        private String name;

        @ParameterValue("created")
        private String created;

        @ParameterValue
        private boolean thumb;

        @ParameterValue("modified")
        private String modified;

        @ParameterValue
        private boolean canread;

        @ParameterValue("isfolder")
        private boolean isFolder;

        @ParameterValue
        private boolean canmanage;

        @ParameterValue
        private boolean ismine;

        @ParameterValue
        private long fileid;

        @ParameterValue
        private long userid;

        @ParameterValue
        private boolean isbusiness_shared;

        @ParameterValue
        private long hash;

        @ParameterValue
        private long comments;

        @ParameterValue
        private long category;

        @ParameterValue
        private boolean candelete;

        @ParameterValue("id")
        private String id;

        @ParameterValue
        private boolean isshared;

        @ParameterValue
        private boolean canmodify;

        @ParameterValue
        private long size;

        @ParameterValue("parentfolderid")
        private long parentFolderId;

        @ParameterValue
        private String contenttype;

        @ParameterValue
        private String icon;
    }
}
