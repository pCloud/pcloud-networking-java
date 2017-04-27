package com.pcloud.networking;

import java.util.List;

public class FolderResponse extends ApiResponse {

    @ParameterValue("metadata")
    private Metadata metadata;

    public FolderResponse(long result, String message) {
        super(result, message);
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public static class Metadata {

        @ParameterValue("name")
        private String name;

        @ParameterValue("created")
        private long created;

        @ParameterValue
        private boolean thumb;

        @ParameterValue("modified")
        private long modified;

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
        private long folderid;

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

        @ParameterValue("contents")
        private List<Metadata> contents;

        public String getName() {
            return name;
        }

        public long getCreated() {
            return created;
        }

        public boolean isThumb() {
            return thumb;
        }

        public long getModified() {
            return modified;
        }

        public boolean isCanread() {
            return canread;
        }

        public boolean isFolder() {
            return isFolder;
        }

        public boolean isCanmanage() {
            return canmanage;
        }

        public boolean isIsmine() {
            return ismine;
        }

        public long getFileid() {
            return fileid;
        }

        public long getFolderid() {
            return folderid;
        }

        public long getUserid() {
            return userid;
        }

        public boolean isIsbusiness_shared() {
            return isbusiness_shared;
        }

        public long getHash() {
            return hash;
        }

        public long getComments() {
            return comments;
        }

        public long getCategory() {
            return category;
        }

        public boolean isCandelete() {
            return candelete;
        }

        public String getId() {
            return id;
        }

        public boolean isIsshared() {
            return isshared;
        }

        public boolean isCanmodify() {
            return canmodify;
        }

        public long getSize() {
            return size;
        }

        public long getParentFolderId() {
            return parentFolderId;
        }

        public String getContenttype() {
            return contenttype;
        }

        public String getIcon() {
            return icon;
        }
    }
}
