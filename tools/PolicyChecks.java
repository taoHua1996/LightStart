package com.qingping.app;

/** Executable JVM regression tests, no Android runtime or third-party library needed. */
public final class PolicyChecks {
    private static int passed;
    public static void main(String[] args) {
        for (String value : new String[]{"跳过", "跳过 5s", "跳过（5秒）", "Skip", "SKIP (3)", "5秒 跳过", "  跳过  ",
                "跳过广告", "跳过广告 5秒", "Skip ad", "SKIP AD (3)", "5秒 跳过广告"}) {
            check(SkipPolicy.matches(value, "", "", "", "", true), "accept exact skip: " + value);
        }
        for (String value : new String[]{"跳过教程", "不要跳过", "点击跳过并下载", "立即体验", "关闭", "跳过广告并下载", "Skip tutorial", "", "跳过999秒", "Skip ad and install"}) {
            check(!SkipPolicy.matches(value, "", "", "", "", true), "reject ambiguous text: " + value);
        }
        check(!SkipPolicy.matches("跳过", "", "", "", "", false), "default requires upper right");
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
        System.out.println("PASS: " + passed + " policy assertions");
    }
    private static void check(boolean result, String message) {
        if (!result) throw new AssertionError(message);
        passed++;
    }
}
