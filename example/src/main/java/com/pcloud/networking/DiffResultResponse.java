/*
 * Copyright (c) 2017 pCloud AG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pcloud.networking;

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

    public enum EventType {
        @ParameterValue("reset") RESET,
        @ParameterValue("createfolder") CREATE_FOLDER,
        @ParameterValue("deletefolder") DELETE_FOLDER,
        @ParameterValue("modifyfolder") MODIFY_FOLDER,
        @ParameterValue("createfile") CREATE_FILE,
        @ParameterValue("modifyfile") MODIFY_FILE,
        @ParameterValue("deletefile") DELETE_FILE,
        @ParameterValue("modifyuserinfo") MODIFY_USERINFO,
        @ParameterValue("requestsharein")INCOMMING_SHARE,
        @ParameterValue("acceptedsharein") ACCEPTED_INCOMMING_SHARE,
        @ParameterValue("declinedsharein") DECLINED_INCOMMING_SHARE,
        @ParameterValue("cancelledsharein") CANCELLED_INCOMMING_SHARE,
        @ParameterValue("modifiedsharein") MODIFYED_INCOMMING_SHARE,
        @ParameterValue("removedsharein") REMOVED_INCOMMING_SHARE,
        @ParameterValue("requestshareout")OUTGOING_SHARE,
        @ParameterValue("acceptedshareout") ACCEPTED_OUTGOING_SHARE,
        @ParameterValue("declinedshareout") DECLINED_OUTGOING_SHARE,
        @ParameterValue("cancelledshareout") CANCELLED_OUTGOING_SHARE,
        @ParameterValue("modifiedshareout") MODIFYED_OUTGOING_SHARE,
        @ParameterValue("removedshareout") REMOVE_OUTGOING_SHARE,
        @ParameterValue("establishbsharein") INCOMMING_BUSINESS_SHARE,
        @ParameterValue("modifybsharein") MODIFIED_INCOMMING_BUSINESS_SHARE,
        @ParameterValue("removebsharein") REMOVED_INCOMMING_BUSINESS_SHARE,
        @ParameterValue("establishbshareout") OUTGOING_BUSINESS_SHARE,
        @ParameterValue("modifybshareout") MODIFIED_OUTGOING_BUSINESS_SHARE,
        @ParameterValue("removebshareout") REMOVED_OUTGOING_BUSINESS_SHARE
    }

    public static class DiffEntry {
        @ParameterValue("event")
        private EventType event;

        @ParameterValue("diffid")
        private long diffId;

        @ParameterValue("time")
        private long time;

        @ParameterValue("metadata")
        private Metadata metadata;
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
