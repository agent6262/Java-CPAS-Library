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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Objects;

/**
 * Model for Dedicated Supporter information about players
 *
 * @author oey192
 */
public class DedicatedSupporterInfoModel {

    /**
     * Indicates if the user is currently a Dedicated Supporter or not. All configurations in this file should be
     * ignored if this value is false
     */
    public final boolean isDedicatedSupporter;

    /**
     * If users are Dedicated Supporters, a tag is displayed next to their name in chat. For users who have specified
     * they don't want this distinction, this value will be false
     */
    public final boolean displayChatTag;

    /**
     * All non-Dedicated Supporters may see ads in chat. Dedicated Supporters are given the option to disable these. If
     * this value is true then the given user should not see ads in chat.
     */
    public final boolean displayChatAds;

    /**
     * All non-Dedicated Supporters may see ads when they join a server (as part of a "message of the day"). Dedicated
     * Supporters are given the option to disable these ads. If this value is true then the given user should not see
     * ads in MOTDs.
     */
    public final boolean displayMotdAds;

    /**
     * Some servers will "spotlight" users that are Dedicated Supports to show the community how awesome they are. Some
     * users don't like this extra attention and are given the ability to opt-out of being spotlit. If this is false,
     * the given user should not be highlighted via any spotlight features
     */
    public final boolean displayInSpotlight;

    /**
     * Users are given the option to pick a color for their name. This is stored as a hex string representing RGB values
     */
    public final String nameColor;

    /**
     * When Dedicated Supporters join a server they are given the option to display a custom message. If not an empty
     * string, this will contain the message they wish to be displayed
     */
    public final String joinMessage;

    /**
     * Create an (immutable) instance of {@link DedicatedSupporterInfoModel}. Intended for use with the
     * {@link ObjectMapper} but may be used by anyone
     *
     * @param isDedicatedSupporter Whether the user is currently a Dedicated Support or not
     * @param displayChatTag       Whether the user wants a chat tag displayed or not
     * @param displayChatAds       Whether the user wants to see ads in chat or not
     * @param displayMotdAds       Whether the user wants to see MOTD ads or not
     * @param displayInSpotlight   Whether the user wants to be included in the spotlight or not
     * @param nameColor            A hex string for the color the user wants their name to be displayed in
     * @param joinMessage          A message the user wants displayed whenever they join the server
     */
    @JsonCreator
    public DedicatedSupporterInfoModel(
            @JsonProperty("ds") boolean isDedicatedSupporter,
            @JsonProperty("chatTag") boolean displayChatTag,
            @JsonProperty("chatAds") boolean displayChatAds,
            @JsonProperty("motdAds") boolean displayMotdAds,
            @JsonProperty("spotlight") boolean displayInSpotlight,
            @JsonProperty("nameColor") String nameColor,
            @JsonProperty("joinMessage") String joinMessage)
    {
        this.isDedicatedSupporter = isDedicatedSupporter;
        this.displayChatTag = displayChatTag;
        this.displayChatAds = displayChatAds;
        this.displayMotdAds = displayMotdAds;
        this.displayInSpotlight = displayInSpotlight;
        this.nameColor = Objects.requireNonNull(nameColor, "nameColor");
        this.joinMessage = Objects.requireNonNull(joinMessage, "joinMessage");
    }
}
