package com.qingping.app;

/** Executable JVM regression tests, no Android runtime or third-party library needed. */
public final class PolicyChecks {
    private static int passed;
    public static void main(String[] args) {
        for (String value : new String[]{"跳过", "跳过 5s", "跳过（5秒）", "Skip", "SKIP (3)", "5秒 跳过", "  跳过  ",
                "跳过广告", "跳过广告 5秒", "Skip ad", "SKIP AD (3)", "5秒 跳过广告",
                "跳过｜5", "跳过 | 1", "跳过广告｜3秒", "Skip | 5s", "5｜跳过", "3s | Skip"}) {
            check(SkipPolicy.matches(value, "", "", "", "", true), "accept exact skip: " + value);
        }
        for (String value : new String[]{"跳过教程", "不要跳过", "点击跳过并下载", "立即体验", "关闭", "跳过广告并下载", "Skip tutorial", "", "跳过999秒", "Skip ad and install",
                "跳过｜下载", "跳过｜5｜安装", "跳过||5", "跳过｜", "5｜跳过并下载"}) {
            check(!SkipPolicy.matches(value, "", "", "", "", true), "reject ambiguous text: " + value);
        }
        check(!SkipPolicy.matches("跳过", "", "", "", "", false), "default requires upper right");
        check(SkipPolicy.skipRegion(.9f, .1f), "upper right skip region");
        check(SkipPolicy.skipRegion(.92f, .92f), "native lower-right skip button");
        check(SkipPolicy.skipRegion(.1f, .9f), "lower-left countdown skip button");
        check(!SkipPolicy.skipRegion(.5f, .9f), "bottom-center content is not an automatic target");
        check(!SkipPolicy.skipRegion(.1f, .1f), "upper-left navigation excluded");
        check(!SkipPolicy.skipRegion(.9f, .5f), "mid-page actions excluded");
        check(!SkipPolicy.skipRegion(1.1f, .9f), "off-window region rejected");
        check(!SkipPolicy.skipRegion(Float.NaN, .9f), "invalid geometry rejected");
        check(SkipPolicy.skipLabel("跳过 3", ""), "countdown label can be learned");
        check(!SkipPolicy.skipLabel("支付", ""), "reused ID with payment label is not trusted");
        check(SkipPolicy.resourceId("com.realtech.xiaocan", "com.realtech.xiaocan:id/tv_skip"), "real resource ID can be learned");
        check(!SkipPolicy.resourceId("sample.app", "foreign.app:id/skip"), "foreign resource ID never learned");
        check(!SkipPolicy.resourceId("sample.app", "sample.app:id/"), "empty resource name rejected");
        check(!SkipPolicy.resourceId("sample.app", null), "missing resource ID leaves text recognition available");
        check(SkipPolicy.matches(null, "跳过", null, "", "", true), "content description supported");
        check(!SkipPolicy.matches(null, null, null, null, null, true), "null nodes safe");
        check(SkipPolicy.matches("跳过广告", "", "", "跳过广告", "", false), "exact custom text");
        check(!SkipPolicy.matches("跳过", "", "", "跳过广告", "", true), "custom overrides generic");
        check(SkipPolicy.matches("", "", "sample:id/skip", "", "sample:id/skip", false), "exact ID");
        check(!SkipPolicy.matches("跳过", "", "sample:id/other", "", "sample:id/skip", true), "ID mismatch never falls back");
        check(!SkipPolicy.matches("别点", "", "sample:id/skip", "跳过广告", "sample:id/skip", true), "custom constraints conjunctive");
        check(SkipPolicy.matches("跳过广告", "", "sample:id/skip", "跳过广告", "sample:id/skip", false), "both custom constraints");

        SkipPolicy.Session s = new SkipPolicy.Session();
        check(!s.canScan("sample.app", 0), "idle cannot scan");
        s.enter("sample.app", 100);
        check(s.isOpen(101), "missing root may retry within original window");
        check(!s.isOpen(12100), "missing root retries stop at deadline");
        check(s.canScan("sample.app", 100), "opens on foreground");
        check(!s.canScan("other.app", 100), "cannot scan foreign package");
        check(!s.canScan("sample.app", 99), "clock regression rejected");
        check(s.canScan("sample.app", 12099), "before deadline");
        check(!s.canScan("sample.app", 12100), "exact deadline expired");
        s.enter("sample.app", 13000);
        check(!s.canScan("sample.app", 13000), "same package navigation does not extend deadline");
        s.enter("other.app", 14000);
        check(s.canScan("other.app", 14000), "new package opens session");
        s.attempted();
        check(!s.canScan("other.app", 14001), "one attempt per session");
        s.enter("other.app", 14002);
        check(!s.canScan("other.app", 14003), "same package event cannot retry");
        s.reset();
        check(!s.canScan("other.app", 14004), "reset cancels pending action");
        s.enter("other.app", 15000);
        check(s.canScan("other.app", 15001), "new session after reset");

        SkipPolicy.Session delayed = new SkipPolicy.Session();
        check(!delayed.observe(null, 100), "missing transition root cannot open a session");
        check(delayed.observe("launcher", 200), "observe actual old foreground first");
        check(delayed.observe("target.app", 550), "later scan opens new foreground without another state event");
        check(!delayed.canScan("launcher", 551), "old foreground cannot receive click");
        check(!delayed.observe("", 600), "empty transient root ignored");
        check(delayed.observe("target.app", 12549), "content scans retain original window");
        check(!delayed.observe("target.app", 12550), "repeated content cannot extend deadline");
        check(!delayed.observe(null, 13000), "missing root does not reset expired session");
        check(!delayed.observe("target.app", 13001), "root recovery does not reopen expired same app");
        delayed.observe("launcher", 14000);
        check(delayed.observe("target.app", 14500), "foreground return opens a new window");
        delayed.attempted();
        check(!delayed.observe("target.app", 14501), "content after click cannot retry");
        check(!delayed.observe("target.app", 15000), "same-app transitions cannot duplicate click");
        SkipPolicy.Session chained = new SkipPolicy.Session();
        chained.enter("ad.app", 100);
        check(chained.canAttempt("ad.app:id/splash", 100), "first verified button allowed");
        chained.attempted("ad.app:id/splash", true, 100);
        check(!chained.canAttempt("ad.app:id/popup", 1099), "second ad respects cooldown");
        check(!chained.canAttempt("ad.app:id/splash", 1100), "same button cannot be retried");
        check(!chained.canAttempt("", 1100), "second button needs verified resource ID");
        check(!chained.canAttempt(null, 1100), "null ID cannot bypass second-button check");
        check(chained.canAttempt("ad.app:id/popup", 1100), "distinct second startup ad allowed");
        chained.attempted("ad.app:id/popup", true, 1100);
        check(!chained.canAttempt("ad.app:id/third", 2200), "two-attempt hard limit");
        chained.enter("next.app", 3000);
        check(chained.canAttempt("", 3000), "new app resets chain");
        chained.attempted("next.app:id/skip", false, 3000);
        check(!chained.canAttempt("next.app:id/other", 4100), "rejected action stops chain");
        chained.enter("missing.id", 5000);
        chained.attempted("", true, 5000);
        check(!chained.canAttempt("missing.id:id/skip", 6100), "missing first ID stops chain");
        chained.enter("late.app", 10000);
        chained.attempted("late.app:id/skip", true, 10000);
        check(!chained.canAttempt("late.app:id/popup", 22000), "second ad cannot extend launch window");
        System.out.println("PASS: " + passed + " policy assertions");
    }
    private static void check(boolean result, String message) {
        if (!result) throw new AssertionError(message);
        passed++;
    }
}
