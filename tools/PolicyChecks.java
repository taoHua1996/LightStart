package com.qingping.app;

/** Executable JVM regression tests, no Android runtime or third-party library needed. */
public final class PolicyChecks {
    private static int passed;
    public static void main(String[] args) {
        for (String value : new String[]{"跳过", "跳过 5s", "跳过（5秒）", "Skip", "SKIP (3)", "5秒 跳过", "  跳过  "}) {
            check(SkipPolicy.matches(value, "", "", "", "", true), "accept exact skip: " + value);
        }
        for (String value : new String[]{"跳过教程", "不要跳过", "点击跳过并下载", "立即体验", "关闭", "跳过广告", "Skip tutorial", "", "跳过999秒"}) {
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
        System.out.println("PASS: " + passed + " policy assertions");
    }
    private static void check(boolean result, String message) {
        if (!result) throw new AssertionError(message);
        passed++;
    }
}
