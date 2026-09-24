package dev.havoc.havoctab.shared.features.chat;

import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.TabConstants;
import dev.havoc.havoctab.shared.platform.TabPlayer;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Chat filter: rate limiting, repeat detection, caps, blocked words and advertising.
 * <p>
 * Every check is configurable and every one can be turned off independently. Players with
 * the bypass permission skip all of it.
 * <p>
 * Two kinds of outcome exist. Some checks <b>block</b> the message outright (spam,
 * advertising, a blocked word when the action is BLOCK); others <b>rewrite</b> it (caps
 * become lowercase, long character runs collapse, a blocked word is censored). Rewriting
 * is preferred where it makes sense, because silently fixing a shouted message annoys
 * players far less than refusing it.
 */
@RequiredArgsConstructor
public class ChatFilter {

    /** Feature configuration */
    @NotNull
    private final ChatConfiguration configuration;

    /** Matches an IPv4 address with an optional port */
    private static final Pattern IP_PATTERN =
            Pattern.compile("\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}(?::\\d{1,5})?\\b");

    /** Matches a Discord invite in its common forms */
    private static final Pattern DISCORD_PATTERN =
            Pattern.compile("discord(?:\\.gg|(?:app)?\\.com/invite)/[a-z0-9-]+");

    /** Collapses runs of three or more identical characters down to one */
    private static final Pattern LONG_RUN = Pattern.compile("(.)\\1{2,}");

    /** Strips spacing and separators used to break up a domain, e.g. "play . example . com" */
    private static final Pattern SPACED_DOT = Pattern.compile("\\s*(?:\\.|\\(dot\\)|\\[dot\\]|\\sdot\\s)\\s*");

    /**
     * Outcome of filtering one message.
     */
    public static class Result {

        /** Whether the message should be cancelled entirely */
        public final boolean blocked;

        /** The message to send, possibly rewritten. Meaningless when blocked. */
        @NotNull public final String message;

        /** Text to send the sender explaining the block, {@code null} when allowed */
        @Nullable public final String notice;

        private Result(boolean blocked, @NotNull String message, @Nullable String notice) {
            this.blocked = blocked;
            this.message = message;
            this.notice = notice;
        }

        static Result allow(@NotNull String message) {
            return new Result(false, message, null);
        }

        static Result block(@NotNull String notice) {
            return new Result(true, "", notice);
        }
    }

    /**
     * Runs every enabled check against a message.
     *
     * @param   sender
     *          Player who sent the message
     * @param   message
     *          Raw message text
     * @return  Filter outcome
     */
    @NotNull
    public Result check(@NotNull TabPlayer sender, @NotNull String message) {
        ChatConfiguration.FilterSettings filter = configuration.getFilter();
        if (!filter.isEnabled()) return Result.allow(message);
        if (sender.hasPermission(filter.getBypassPermission())) return Result.allow(message);
        if (sender.hasPermission(TabConstants.Permission.COMMAND_ALL)) return Result.allow(message);

        Result spam = checkSpam(sender, message);
        if (spam.blocked) return spam;
        String working = spam.message;

        Result ads = checkAdvertising(working, filter);
        if (ads.blocked) return ads;
        working = ads.message;

        Result words = checkBlockedWords(working, filter);
        if (words.blocked) return words;
        working = words.message;

        // Remember what was actually said, so repeat detection compares like with like
        rememberMessage(sender, working, filter);
        return Result.allow(working);
    }

    // ------------------
    // Spam
    // ------------------

    @NotNull
    private Result checkSpam(@NotNull TabPlayer sender, @NotNull String message) {
        ChatConfiguration.SpamSettings spam = configuration.getFilter().getSpam();
        if (!spam.isEnabled()) return Result.allow(message);

        long now = System.currentTimeMillis();
        if (spam.getCooldownMilliseconds() > 0) {
            long since = now - sender.chatData.lastMessageTime;
            if (since < spam.getCooldownMilliseconds()) {
                return Result.block(HavocTab.getInstance().getConfiguration().getMessages()
                        .getFilterCooldown((int) Math.max(1, (spam.getCooldownMilliseconds() - since + 999) / 1000)));
            }
        }

        if (spam.isBlockRepeats()) {
            String normalized = normalize(message);
            synchronized (sender.chatData.recentMessages) {
                for (String previous : sender.chatData.recentMessages) {
                    if (previous.equals(normalized)) {
                        return Result.block(HavocTab.getInstance().getConfiguration().getMessages().getFilterRepeat());
                    }
                }
            }
        }

        String working = message;

        // Collapse "heeeeeelp" down to "help" rather than refusing the message
        if (spam.getMaxRepeatedCharacters() > 0) {
            working = collapseRuns(working, spam.getMaxRepeatedCharacters());
        }

        // Shouting gets lowercased, not blocked
        if (spam.getMaxCapsPercent() > 0 && working.length() >= spam.getCapsMinLength()) {
            int letters = 0;
            int caps = 0;
            for (int i = 0; i < working.length(); i++) {
                char c = working.charAt(i);
                if (Character.isLetter(c)) {
                    letters++;
                    if (Character.isUpperCase(c)) caps++;
                }
            }
            if (letters > 0 && (caps * 100 / letters) > spam.getMaxCapsPercent()) {
                working = working.toLowerCase(Locale.US);
            }
        }

        sender.chatData.lastMessageTime = now;
        return Result.allow(working);
    }

    private void rememberMessage(@NotNull TabPlayer sender, @NotNull String message,
                                 @NotNull ChatConfiguration.FilterSettings filter) {
        if (!filter.getSpam().isEnabled() || !filter.getSpam().isBlockRepeats()) return;
        synchronized (sender.chatData.recentMessages) {
            sender.chatData.recentMessages.addFirst(normalize(message));
            while (sender.chatData.recentMessages.size() > Math.max(1, filter.getSpam().getRepeatHistory())) {
                sender.chatData.recentMessages.removeLast();
            }
        }
    }

    /**
     * Shortens any run of the same character longer than the limit.
     */
    @NotNull
    private String collapseRuns(@NotNull String text, int limit) {
        StringBuilder out = new StringBuilder(text.length());
        int run = 0;
        char last = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == last) {
                run++;
                if (run >= limit) continue;
            } else {
                run = 0;
                last = c;
            }
            out.append(c);
        }
        return out.toString();
    }

    // ------------------
    // Blocked words
    // ------------------

    @NotNull
    private Result checkBlockedWords(@NotNull String message, @NotNull ChatConfiguration.FilterSettings filter) {
        ChatConfiguration.WordSettings words = filter.getBlockedWords();
        if (!words.isEnabled() || words.getWords().isEmpty()) return Result.allow(message);

        String normalized = normalize(message);
        for (String allowed : words.getAllowedPhrases()) {
            // Remove known-good phrases first, so "Scunthorpe" cannot trip a substring rule
            normalized = normalized.replace(normalize(allowed), "");
        }

        List<String> hits = new ArrayList<>();
        for (String word : words.getWords()) {
            String needle = normalize(word);
            if (!needle.isEmpty() && normalized.contains(needle)) hits.add(word);
        }
        if (hits.isEmpty()) return Result.allow(message);

        if (words.getAction().equalsIgnoreCase("CENSOR")) {
            String censored = message;
            for (String word : hits) {
                censored = censorLoosely(censored, word, words.getCensorReplacement());
            }
            return Result.allow(censored);
        }
        return Result.block(HavocTab.getInstance().getConfiguration().getMessages().getFilterBlockedWord());
    }

    /**
     * Replaces a word in the original text, ignoring case.
     * <p>
     * This only catches the plain spelling. A word disguised with symbols is still
     * detected by the normalized check above, but cannot be located precisely enough in
     * the original text to censor it - which is why BLOCK is the default action.
     */
    @NotNull
    private String censorLoosely(@NotNull String text, @NotNull String word, @NotNull String replacement) {
        return Pattern.compile(Pattern.quote(word), Pattern.CASE_INSENSITIVE)
                .matcher(text).replaceAll(Matcher.quoteReplacement(replacement));
    }

    // ------------------
    // Advertising
    // ------------------

    @NotNull
    private Result checkAdvertising(@NotNull String message, @NotNull ChatConfiguration.FilterSettings filter) {
        ChatConfiguration.AdvertisingSettings ads = filter.getAdvertising();
        if (!ads.isEnabled()) return Result.allow(message);

        // Rejoin domains split up to dodge the filter: "play . example . com"
        String joined = SPACED_DOT.matcher(message.toLowerCase(Locale.US)).replaceAll(".");

        boolean hit = false;
        if (ads.isBlockDiscordInvites() && matches(DISCORD_PATTERN, joined, ads)) hit = true;
        if (!hit && ads.isBlockIps() && matches(IP_PATTERN, joined, ads)) hit = true;
        if (!hit && ads.isBlockDomains() && !ads.getDomainPattern().isEmpty()) {
            try {
                if (matches(Pattern.compile(ads.getDomainPattern()), joined, ads)) hit = true;
            } catch (Exception e) {
                // Invalid regex in config, treat as no match rather than breaking chat
                HavocTab.getInstance().getConfigHelper().startup().startupWarn(
                        "chat.filter.advertising.domain-pattern is not a valid regular expression, skipping it.");
            }
        }
        if (!hit) return Result.allow(message);

        if (ads.getAction().equalsIgnoreCase("CENSOR")) {
            return Result.allow(message); // Nothing sensible to censor, let it through unchanged
        }
        return Result.block(HavocTab.getInstance().getConfiguration().getMessages().getFilterAdvertising());
    }

    /**
     * Returns whether the pattern matches anything not covered by the allowed list.
     */
    private boolean matches(@NotNull Pattern pattern, @NotNull String text,
                            @NotNull ChatConfiguration.AdvertisingSettings ads) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String found = matcher.group();
            boolean allowed = false;
            for (String entry : ads.getAllowed()) {
                if (found.contains(entry.toLowerCase(Locale.US))) {
                    allowed = true;
                    break;
                }
            }
            if (!allowed) return true;
        }
        return false;
    }

    // ------------------
    // Normalization
    // ------------------

    /**
     * Reduces text to a comparable form: lowercase, common letter swaps undone, separators
     * removed and long character runs collapsed.
     * <p>
     * This is what makes {@code "5 l  u  r"} and {@code "s1ur"} match the same entry.
     * It is deliberately lossy, which is why {@code allowed-phrases} exists - removing
     * separators can create matches inside innocent words.
     *
     * @param   text
     *          Text to normalize
     * @return  Normalized text
     */
    @NotNull
    public static String normalize(@NotNull String text) {
        String lower = LONG_RUN.matcher(text.toLowerCase(Locale.US)).replaceAll("$1");
        StringBuilder out = new StringBuilder(lower.length());
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            switch (c) {
                case '4': case '@': out.append('a'); break;
                case '3': out.append('e'); break;
                case '1': case '!': case '|': out.append('i'); break;
                case '0': out.append('o'); break;
                case '5': case '$': out.append('s'); break;
                case '7': case '+': out.append('t'); break;
                case '8': out.append('b'); break;
                case '9': out.append('g'); break;
                default:
                    if (Character.isLetterOrDigit(c)) out.append(c);
                    break;
            }
        }
        return out.toString();
    }
}
