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
 * Represents a Ban in CAPS. Used in {@link BanHistoryModel}
 *
 * @author oey192
 */
public class CpasBanModel {

    /**
     * The UNIX timestamp for when the ban was created
     */
    public final long banDateSeconds;

    /**
     * The number of minutes remaining in the ban
     */
    public final int duration;

    /**
     * The total length of the ban in minutes
     */
    public final int length;

    /**
     * The reason for the ban
     */
    public final String reason;

    /**
     * Create an (immutable) instance of {@link CpasBanModel}. Intended for use with the {@link ObjectMapper} but may be used
     * by anyone
     *
     * @param banDateSeconds the UNIX timestamp of the ban
     * @param duration       the remaining duration of the ban in minutes
     * @param length         the length of the ban in minutes
     * @param reason         the reason for the ban
     */
    @JsonCreator
    public CpasBanModel(
            @JsonProperty("date") long banDateSeconds,
            @JsonProperty("duration") int duration,
            @JsonProperty("length") int length,
            @JsonProperty("reason") String reason)
    {
        this.banDateSeconds = banDateSeconds;
        this.duration = duration;
        this.length = length;
        this.reason = Objects.requireNonNull(reason, "reason");
    }
}
