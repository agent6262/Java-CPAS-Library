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
package net.cpas;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.cpas.model.BanHistoryModel;
import net.cpas.model.BanInfoModel;
import net.cpas.model.InfoModel;
import net.cpas.model.SuccessResponseModel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.*;

/**
 * A library for communicating with the remote CPAS API
 * <p>
 * Note: All API calls are spun off in background threads. Using {@link #configure(String, String, String, String)} to
 * configure the parameters this class uses when making API calls is thread safe. API calls that have already begun
 * using the existing configuration will continue with that configuration and new API calls will start using the new
 * configuration once it has been completely updated
 *
 * @author agent6262, oey192
 * @version 1.2.1
 */
public final class Cpas {

    /**
     * Holder for the {@link Cpas} singleton instance.
     */
    private static class CpasHolder {

        /**
         * {@link Cpas} singleton instance.
         */
        private static final Cpas INSTANCE = new Cpas();
    }

    public interface ProcessResponse<Model> {

        /**
         * Called when a CPAS API call has completed.
         *
         * @param response     the contents of the response parsed into a POJO model. Guaranteed to be nonnull if the
         *                     {@code errorMessage} parameter is null
         * @param errorMessage a string containing the error message if something went wrong while retreiving/parsing
         *                     the response. If no error occurred, this parameter will be null and the {@code response}
         *                     parameter is guaranteed to be nonnull
         */
        void process(Model response, String errorMessage);

        /**
         * @return the class of the model for the response
         */
        Class<Model> getModelClass();
    }

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * The base url used in any-given api call.<br>
     * Example: https://www.example.cc/cpas/api
     */
    private String apiUrl;

    /**
     * The api key used to access the CPAS API.<br>
     * Example: sFZQOFKF69162tU7J9gU13LJlsYinrzv
     */
    private String apiKey;

    /**
     * The ip address the server is running on or bound to if there are more than one.<br>
     * Example: 176.198.3.255
     */
    private String serverIp;

    /**
     * The port the server is bound to.<br>
     * Example: 25578
     */
    private String serverPort;

    /**
     * The executor service used to run network calls. Is a hybrid of {@link Executors#newFixedThreadPool(int)} and
     * {@link Executors#newCachedThreadPool()}. It has a limit of 5 threads and any given thread will stop running if
     * left idle for more than 60 seconds
     */
    private final ExecutorService executorService =
            new ThreadPoolExecutor(0, 5, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());

    /**
     * Lock object for the reloading the {@link Cpas} information.
     */
    private final Object mutex = new Object();

    /**
     * Gets the CPAS singleton.
     *
     * @return The CPAS singleton.
     */
    public static Cpas getInstance() {
        return CpasHolder.INSTANCE;
    }

    /**
     * Configures or reconfigures the {@link Cpas} object. This function will hold a thread if any
     * api calls are in the process of creation.
     *
     * @param apiUrl     The base url used in any given api call.
     * @param apiKey     The api key used to access the CPAS API.
     * @param serverIp   The ip address the server is running on or bound to if there are more than one.
     * @param serverPort The port the server is bound to.
     */
    public void configure(String apiUrl, String apiKey, String serverIp, String serverPort) {
        synchronized(mutex) {
            this.apiUrl = Objects.requireNonNull(apiUrl, "apiUrl");
            this.apiKey = Objects.requireNonNull(apiKey, "apiKey");
            this.serverIp = Objects.requireNonNull(serverIp, "serverIp");
            this.serverPort = Objects.requireNonNull(serverPort, "serverPort");
        }
    }

    /**
     * Gets information about a game ID and associated CPAS user if applicable. If the game ID specified doesn't exist
     * in the system, CPAS creates a new entry for the game ID, sets default values and returns "blank slate" info for
     * that player. This function should be called for every player every time they connect to any server, and any local
     * data that is cached for that player should be updated. (Cached data should only be used in the event that the call
     * fails)
     * <p>
     * The provided callback will be given a response containing the following info unless an error occurs. In such a
     * case, look for "error" and "internalError" keys in the response
     * <ul>
     * <li>gameid - The Game ID that was passed as part of the request. This is provided for plugins that use
     * asynchronous requests and have no idea what Game ID a response is for when they receive it.</li>
     * <li>userid - The (forum) User ID for the player. This can be used with other APIs to refer to the given
     * player. Will be 0 for players with no associated forum user (e.g. pubs, unverified members).</li>
     * <li>name - The current forum name of the user.</li>
     * <li>groups - An array of dictionaries containing ranks and group names.</li>
     * <li>primaryGroup - A string with the name of the user's primary group, or an empty string if the Game ID has
     * no associated user or if the user has no groups.</li>
     * <li>primaryRank - The rank associated with the primaryGroup.</li>
     * <li>verification - True or false. Used to show a message to the user that they just verified. This will
     * always be false for Game ID types that use web verification.</li>
     * <li>verificationExpired - True or false. Used to tell the user whether or not it has been too long since they
     * entered their Game ID into CPAS.</li>
     * <li>state - A byte containing binary 00000000 through 11111111 (returned as an int) - Note that if verbose is
     * specified in the parameters, a list will be returned instead of a bit (e.g. ["active", "no bat"] or
     * ["not active", "bat", "no aat"], etc).</li>
     * <li>division - The identifier for the division the player is in. This will be an empty string for players
     * with no associated forum user (e.g. pubs, unverified members).</li>
     * </ul>
     *
     * @param gameId          The unique game ID of the user. For source games, this is the player's 64-bit Steam ID. For
     *                        Minecraft, this is the player's Minecraft UUID, etc. <i>Sanitized (converted to url-safe base 64)
     *                        by this function</i>.
     * @param playerIpAddress (opt) The IP address of the user. This is used to keep track of the most recent IP used
     *                        by a player and to perform verification. It should always be specified if it is being
     *                        used to fetch info for a player who just joined the server, but this is the only case in
     *                        which it should be specified. An empty string may be used if you wish to omit this
     *                        parameter and specify the verbose option.
     * @param verbose         (opt) If specified, the state to be returned as a list instead of an integer.
     * @param callback        A {@link ProcessResponse} to be executed when the response comes back from the server. Note that
     *                        this gets executed on a thread other than the one it was called from. If you need to process the
     *                        response on a specific thread you will have to take care of passing the data given to you in the
     *                        callback into another {@link Runnable}, {@link Callable}, or similar type of object and have that
     *                        task executed on the appropriate thread.
     * @return the future object to be executed.
     */
    public Future<?> getInfo(String gameId, String playerIpAddress, boolean verbose, ProcessResponse<InfoModel> callback) {
        final StringJoiner joiner = new StringJoiner("/");
        return makeApiCall(joiner.add("info")
                        .add(base64EncodeSafe(gameId))
                        .add(playerIpAddress != null ? playerIpAddress : "")
                        .add(verbose ? "verbose" : "")
                        .toString(),
                callback);
    }

    /**
     * Gets information about a game ID and associated CPAS user if applicable. If the game ID specified doesn't exist
     * in the system, CPAS creates a new entry for the game ID, sets default values and returns "blank slate" info for
     * that player. This function should be called for every player every time they connect to any server, and any local
     * data that is cached for that player should be updated. (Cached data should only be used in the event that the call fails)
     * <p>
     * For full documentation see {@link #getInfo(String, String, boolean, ProcessResponse)}
     *
     * @see #getInfo(String, String, boolean, ProcessResponse)
     */
    public Future<?> getInfo(String gameId, boolean verbose, ProcessResponse<InfoModel> callback) {
        return getInfo(gameId, "", verbose, callback);
    }

    /**
     * Attempts to add a ban for the given user in CPAS. The callback will receive the response which will be
     * {"success": true} on success or it will be an error. Look for "error" and "internalError" keys in the response if
     * the "success" key is not present
     *
     * @param gameId    The game ID to ban. Any game IDs associated with this ID will also be banned in
     *                  the system. <i>Sanitized (converted to url-safe base 64) by this function</i>.
     * @param handle    The in-game name of the banned player at the time of the ban. If the Game ID
     *                  being banned is the same as the in-game name, it should simply be listed twice.
     *                  <i>Sanitized (converted to url-safe base 64) by this function</i>.
     * @param bannerId  The game ID of the banning admin. If the game ID doesn't exist or if it doesn't have an
     *                  associated user, the ban will fail (and an error will be returned). If the "banning admin" is
     *                  the console or some other inanimate entity, an empty string may be passed and the banning admin
     *                  will be set to the default "super" admin from the CPAS config.
     *                  <i>Sanitized (converted to url-safe base 64) by this function</i>.
     * @param adminList An array of the gameIDs of the top 10 admins by rank online at the time of the ban. If the
     *                  banning admin gameid is not in the list, it will be assumed that they were also online at the
     *                  time of the ban. An empty array is allowed if no other admins were online at the time of the ban.
     *                  Note: If any of the admins specified do not have an associated user, they will be omitted from
     *                  the list and an error will be logged in CPAS. No error will be returned in this case.
     *                  The array is converted to a url-safe base 64 encoded string for use in the API call
     * @param time      (opt) The time to ban the player for in minutes. If the time is 0 or if the parameter is left out,
     *                  the ban is permanent. Times should be specified in minutes and will be converted to integers. No
     *                  banning people for 283.43 minutes! If a string is sent as the time, the ban will not succeed.
     * @param reason    (opt) The reason for the ban. While a reason should always be specified according
     *                  to eGO admining policies, the command will succeed without this parameter. If no reason is
     *                  specified, "Banned" will be set as the reason. <i>Sanitized (converted to url-safe base 64) by
     *                  this function</i>.
     * @param callback  A {@link ProcessResponse} to be executed when the response comes back from the server. Note that
     *                  this gets executed on a thread other than the one it was called from. If you need to process the
     *                  response on a specific thread you will have to take care of passing the data given to you in the
     *                  callback into another {@link Runnable}, {@link Callable}, or similar type of object and have that
     *                  task executed on the appropriate thread.
     * @return the future object to be executed.
     */
    public Future<?> banUser(String gameId, String handle, String bannerId, String[] adminList, int time, String reason, ProcessResponse<SuccessResponseModel> callback) {
        final StringJoiner sanitizedAdminList = new StringJoiner(",");
        for (String admin : adminList) {
            sanitizedAdminList.add(base64EncodeSafe(admin));
        }
        final StringJoiner joiner = new StringJoiner("/");
        return makeApiCall(joiner.add("ban")
                        .add(base64EncodeSafe(gameId))
                        .add(base64EncodeSafe(handle))
                        .add(base64EncodeSafe(bannerId))
                        .add(sanitizedAdminList.toString())
                        .add(String.valueOf(time))
                        .add(reason != null ? base64EncodeSafe(reason) : "")
                        .toString(),
                callback);
    }

    /**
     * Attempts to add a ban for the given user in CPAS.
     * <p>
     * For full documentation see {@link #banUser(String, String, String, String[], int, String, ProcessResponse)} )}
     *
     * @see #banUser(String, String, String, String[], int, String, ProcessResponse)
     */
    public Future<?> banUser(String gameId, String handle, String bannerId, String[] adminList, int time, ProcessResponse<SuccessResponseModel> callback) {
        return banUser(gameId, handle, bannerId, adminList, time, "", callback);
    }

    /**
     * Attempts to add a ban for the given user in CPAS.
     * <p>
     * For full documentation see {@link #banUser(String, String, String, String[], int, String, ProcessResponse)} )}
     *
     * @see #banUser(String, String, String, String[], int, String, ProcessResponse)
     */
    public Future<?> banUser(String gameId, String handle, String bannerId, String[] adminList, ProcessResponse<SuccessResponseModel> callback) {
        return banUser(gameId, handle, bannerId, adminList, 0, callback);
    }

    /**
     * Gets the current ban information for a {@code gameId}.
     *
     * @param gameId   The game ID for which you want ban info. <i>Sanitized (converted to url-safe base 64) by this function</i>.
     * @param callback A {@link ProcessResponse} to be executed when the response comes back from the server. Note that
     *                 this gets executed on a thread other than the one it was called from. If you need to process the
     *                 response on a specific thread you will have to take care of passing the data given to you in the
     *                 callback into another {@link Runnable}, {@link Callable}, or similar type of object and have that
     *                 task executed on the appropriate thread.
     * @return the future object to be executed.
     */
    public Future<?> getBanInfo(String gameId, ProcessResponse<BanInfoModel> callback) {
        return makeApiCall("banInfo/" + base64EncodeSafe(gameId), callback);
    }

    /**
     * Get a list of past bans for a player. The response received by the callback will contain a JSON dictionary
     * mapping 'bans' to an array of a dictionary of bans, up to the count specified in the request. Note that a given
     * user might have no past bans at all and thus this can return an empty array. Each dictionary has the following keys
     * <ul>
     * <li>date - The date / time of the ban as a UNIX timestamp.</li>
     * <li>duration - The minutes remaining for the ban.</li>
     * <li>length - The total length of the bans in minutes.</li>
     * <li>reason - The reason given for the ban.</li>
     * </ul>
     *
     * @param gameId   The game ID for which you want ban history. <i>Sanitized (converted to url-safe base 64) by this
     *                 function</i>.
     * @param count    The max number of history items to return (This should be positive and less than PHP_MAX_INT. If it
     *                 isn't, it will be set to 1)
     * @param callback A {@link ProcessResponse} to be executed when the response comes back from the server. Note that
     *                 this gets executed on a thread other than the one it was called from. If you need to process the
     *                 response on a specific thread you will have to take care of passing the data given to you in the
     *                 callback into another {@link Runnable}, {@link Callable}, or similar type of object and have that
     *                 task executed on the appropriate thread.
     * @return the future object to be executed.
     */
    public Future<?> getBanHistory(String gameId, int count, ProcessResponse<BanHistoryModel> callback) {
        return makeApiCall("banHistory/" + base64EncodeSafe(gameId) + "/" + count, callback);
    }

    /**
     * Opens up a connection to the CPAS API url on one of this.{@link #executorService}'s threads and retrieves the JSON
     * information as a {@code String} and then parses it to an {@link ObjectMapper}. The {@link ProcessResponse}
     * callback is invoked when the process completes and is provided the resulting parsed response or an error string
     *
     * @param apiInfo  The API call information specific to the particular call being made.
     * @param callback A {@link ProcessResponse} to be executed when the response comes back from the server. Note that
     *                 this gets executed on a thread other than the one it was called from. If you need to process the
     *                 response on a specific thread you will have to take care of passing the data given to you in the
     *                 callback into another {@link Runnable}, {@link Callable}, or similar type of object and have that
     *                 task executed on the appropriate thread.
     * @return the future object to be executed.
     */
    private <T> Future<?> makeApiCall(String apiInfo, ProcessResponse<T> callback) {
        Objects.requireNonNull(callback, "callback");
        final Runnable runnable;
        synchronized(mutex) {
            runnable = new GetFromNetwork<>(apiUrl, apiKey, serverIp, serverPort, apiInfo, callback);
        }
        return executorService.submit(runnable);
    }

    /**
     * The CPAS API requires a base 64 encoding of all information passed to it, however a standard base 64 encode
     * contains illegal characters for url address so the function helps to alleviate that by replacing certain
     * characters.
     *
     * @param string The information or {@code String} to be encoded
     * @return The converted string.
     */
    private String base64EncodeSafe(String string) {
        return new String(Base64.getEncoder().encode(string.getBytes()), Charset.forName("UTF-8")).replace('/', '_').replace('+', '-');
    }

    /**
     * Runnable to be used by the {@link #executorService} for async tasks.
     */
    private static class GetFromNetwork<Model> implements Runnable {

        /**
         * For full documentation see {@link Cpas#apiUrl}
         */
        private final String apiUrl;

        /**
         * For full documentation see {@link Cpas#apiKey}
         */
        private final String apiKey;

        /**
         * For full documentation see {@link Cpas#serverIp}
         */
        private final String serverIp;

        /**
         * For full documentation see {@link Cpas#serverPort}
         */
        private final String serverPort;

        /**
         * Api info for CPAS web api call.
         */
        private final String apiInfo;

        /**
         * Callback to be executed when CPAS web api call finishes.
         */
        private final ProcessResponse<Model> callback;

        /**
         * Creates a new {@link GetFromNetwork} object. Should be used in conjunction with the
         * {@link Cpas#executorService}.
         *
         * @param apiUrl     The base url used in any given api call.
         * @param apiKey     The api key used to access the CPAS API.
         * @param serverIp   The ip address the server is running on or bound to if there are more than one.
         * @param serverPort The port the server is bound to.
         * @param apiInfo    The api info for CPAS web api call.
         * @param callback   The callback to be executed when CPAS web api call finishes.
         */
        GetFromNetwork(String apiUrl, String apiKey, String serverIp, String serverPort, String apiInfo, ProcessResponse<Model> callback) {
            this.apiUrl = apiUrl;
            this.apiKey = apiKey;
            this.serverIp = serverIp;
            this.serverPort = serverPort;
            this.apiInfo = apiInfo;
            this.callback = callback;
        }

        @Override
        public void run() {
            final ExecutorService executor = Executors.newSingleThreadExecutor();
            final Future<String> future = executor.submit(()->{
                final StringJoiner joiner = new StringJoiner("/");
                final String urlString = joiner.add(apiUrl).add(apiKey).add(serverIp).add(serverPort).add(apiInfo).toString();

                try (InputStream inputStream = new URL(urlString).openStream()) {
                    final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    // Using StringBuilder for concatenation in a loop is better then using String see http://stackoverflow.com/a/1532483/2949095
                    final StringBuilder response = new StringBuilder();
                    int val;
                    while ((val = reader.read()) != -1) {
                        response.append((char) val);
                    }
                    return response.toString();
                } catch (IOException e) {
                    return e.toString();
                }
            });
            // Prevents the executor from receiving additional tasks; does not cancel previously submitted tasks
            executor.shutdown();

            try {
                final String rawResponse = Objects.requireNonNull(future.get(5, TimeUnit.SECONDS), "Network response");
                final Model parsedResponse = OBJECT_MAPPER.readValue(rawResponse, callback.getModelClass());
                callback.process(parsedResponse, null);
            } catch (Exception e) {
                callback.process(null, e.toString());
            }
        }
    }
}


