package org.rafaelia.rafffft;

/** Zero Trust decision gate, independent of Android and of any credentials. */
public final class PolicyGate {
    public enum Operation {
        SYNTHETIC_TEST, PUBLIC_GITHUB_READ, DRIVE_TREE_PICKER,
        PRIVATE_GITHUB_READ, DRIVE_CONTENT_READ, CLIPBOARD_RECEIPT
    }

    public static final class Decision {
        public final boolean allowed;
        public final String code;
        private Decision(boolean allowed, String code) {
            this.allowed = allowed;
            this.code = code;
        }
    }

    private PolicyGate() {}

    public static Decision decide(Operation op, boolean userGesture) {
        if (op == null) return new Decision(false, "DENY_UNKNOWN_OPERATION");
        if (!userGesture) return new Decision(false, "DENY_NO_USER_GESTURE");
        switch (op) {
            case SYNTHETIC_TEST:
                return new Decision(true, "ALLOW_OFFLINE_FIXTURE");
            case PUBLIC_GITHUB_READ:
                return new Decision(true, "ALLOW_PUBLIC_GITHUB_METADATA");
            case DRIVE_TREE_PICKER:
                return new Decision(true, "ALLOW_SAF_PICKER_ONLY");
            case CLIPBOARD_RECEIPT:
                return new Decision(true, "ALLOW_SANITIZED_RECEIPT");
            case PRIVATE_GITHUB_READ:
                return new Decision(false, "DENY_PRIVATE_AUTH_NOT_VERIFIED");
            case DRIVE_CONTENT_READ:
                return new Decision(false, "DENY_DRIVE_ACCOUNT_AND_SCOPE_UNVERIFIED");
            default:
                return new Decision(false, "DENY_UNKNOWN_OPERATION");
        }
    }
}
