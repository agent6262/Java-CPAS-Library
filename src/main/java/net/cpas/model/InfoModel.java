/*
 * Copyright (c) 2017, Tyler Bucher
 * Copyright (c) 2017, Orion Stanger
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.cpas.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Model for the response of the CAPS 'info' command
 *
 * @author oey192
 */
@JsonIgnoreProperties({"division", "state"})
public class InfoModel {
    /**
     * The Minecraft Game ID ({@link UUID}) of the player.
     */
    public final UUID gameId;

    /**
     * The (forum) User ID for the user. It will be 0 for players with no connected user (e.g. pubs, unverified members)
     */
    public final int userId;

    /**
     * The current forum name of the user
     */
    public final String forumName;

    /**
     * The primary group for the user
     */
    public final CpasGroupModel primaryGroup;

    /**
     * A list of all groups the user is a part of
     */
    public final List<CpasGroupModel> groups;

    /**
     * The tag of the division the user is apart of
     */
    public final String division;

    /**
     * The human readable name of the division the user is a part of
     */
    public final String divisionName;

    /**
     * Information about the user's Dedicated Supporter status and various related settings
     */
    public final DedicatedSupporterInfoModel dsInfo;

    /**
     * If true, display a message to the user upon joining the server that they just successfully verified. Do not use
     * this value if the user did not just join the server as it may no longer be accurate.
     */
    public final boolean verification;

    /**
     * If true, it has been too long since the user entered their game ID into CAPS. Display a message to the user upon
     * joining that they need to go re-save or correct their appropriate Game ID in CAPS to complete the verification
     * process. Do not use this value if this object was not recently created from a server response (i.e. if it's been
     * more than 5 seconds since this info was pulled from the server it's no longer guaranteed to be accurate)
     */
    public final boolean verificationExpired;

    /**
     * Create an (immutable) instance of {@link InfoModel}. Intended for use with the {@link ObjectMapper} but may be
     * used by anyone
     *
     * @param gameId              The user's game ID
     * @param userId              The user's ID if known, otherwise 0
     * @param forumName           The user's forum name
     * @param primaryGroup        The name of the user's primary group
     * @param primaryRank         The rank of the user's primary group
     * @param groups              All the groups the user is a part of
     * @param divisionName        The name of the user's division
     * @param dsInfo              The Dedicated Supporter info for the user
     * @param verification        Whether the user just verified or not
     * @param verificationExpired Whether the user needs to refresh their game ID in CAPS for verification to succeed
     */
    @JsonCreator
    public InfoModel(
            @JsonProperty("gameid") String gameId,
            @JsonProperty("userid") int userId,
            @JsonProperty("name") String forumName,
            @JsonProperty("primaryGroup") String primaryGroup,
            @JsonProperty("primaryRank") int primaryRank,
            @JsonProperty("groups") List<CpasGroupModel> groups,
            @JsonProperty("division") String division,
            @JsonProperty("divisionName") String divisionName,
            @JsonProperty("dsInfo") DedicatedSupporterInfoModel dsInfo,
            @JsonProperty("verification") boolean verification,
            @JsonProperty("verificationExpired") boolean verificationExpired)
    {
        this.gameId = UUID.fromString(Objects.requireNonNull(gameId, "gameId"));
        this.userId = userId;
        this.forumName = Objects.requireNonNull(forumName, "forumName");
        this.primaryGroup = new CpasGroupModel(Objects.requireNonNull(primaryGroup, "primaryGroup"), primaryRank);
        this.groups = Objects.requireNonNull(groups, "groups");
        this.division = Objects.requireNonNull(division, "division");
        this.divisionName = Objects.requireNonNull(divisionName, "divisionName");
        this.dsInfo = Objects.requireNonNull(dsInfo, "dsInfo");
        this.verification = verification;
        this.verificationExpired = verificationExpired;
    }
}
