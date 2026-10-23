/**
 * NowShowing TV — ad overlay cleanup script
 *
 * Injected into the embed WebView via evaluateJavascript on every page load.
 * Guards prevent observer stacking across repeated onPageFinished calls.
 *
 * Target: Monetag/PropellerAds-style "Confirm you're not a robot — scan the
 * QR code" full-page overlays that appear on top of the video player.
 *
 * Design constraints (Android TV):
 *  - Must NOT steal D-pad input target (no element focal grabbing).
 *  - Must NOT block key events.
 *  - Idempotent: safe to call multiple times; guard via window.__nsAdCleanup.
 *  - Uses MutationObserver — no recurring timer polling.
 *  - Hides first (display:none !important), then removes after a tick for
 *    provider scripts that inspect their own node's style before detaching.
 *  - Matches on multiple signals: high-z-index overlay position, QR-related
 *    canvas/image, and keyword text — not on a single English string.
 *  - Never hides elements without an elevated z-index to avoid breaking the
 *    provider's own UI controls.
 */
(function () {
    'use strict';

    // Idempotency guard — repeated onPageFinished calls from the same page
    // (e.g. after a hash navigation) must not stack observers.
    if (window.__nsAdCleanup) return;
    window.__nsAdCleanup = true;

    // --- Detection heuristics ------------------------------------------------

    var KEYWORDS = [
        'not a robot',
        'scan the qr',
        'scan qr',
        'confirm you',
        'verify you',
        'human verification',
        'qr code',
        'qrcode',
    ];

    // Minimum z-index that characterises a deliberate full-screen overlay.
    // Provider player controls typically sit at 1–100; legitimate modal dialogs
    // may reach 1000. We only target above 1000 to avoid false positives.
    var Z_THRESHOLD = 1000;

    function isHighZIndex(el) {
        try {
            var cs = window.getComputedStyle(el);
            var pos = cs.position;
            if (pos !== 'fixed' && pos !== 'absolute') return false;
            var z = parseInt(cs.zIndex, 10);
            return !isNaN(z) && z > Z_THRESHOLD;
        } catch (_) {
            return false;
        }
    }

    function hasKeywordText(el) {
        try {
            var text = (el.innerText || el.textContent || '').toLowerCase();
            return KEYWORDS.some(function (k) { return text.indexOf(k) !== -1; });
        } catch (_) {
            return false;
        }
    }

    function containsQrMedia(el) {
        // A QR overlay typically includes a <canvas> or <img> for the code.
        return el.querySelector('canvas') !== null ||
               el.querySelector('img[src*="qr"]') !== null ||
               el.querySelector('img[alt*="qr"]') !== null;
    }

    function looksLikeAdOverlay(el) {
        if (!isHighZIndex(el)) return false;
        return hasKeywordText(el) || containsQrMedia(el);
    }

    // --- Remediation ---------------------------------------------------------

    function hide(el) {
        el.style.setProperty('display', 'none', 'important');
        el.style.setProperty('visibility', 'hidden', 'important');
        el.setAttribute('aria-hidden', 'true');
        // Remove after one tick; some scripts re-append if they see their node
        // detached immediately, but don't re-check style.
        setTimeout(function () {
            try { if (el.parentNode) el.parentNode.removeChild(el); } catch (_) {}
        }, 0);
    }

    // Sweep the given root (always document for the top frame).
    function sweep(root) {
        try {
            var candidates = root.querySelectorAll(
                'div, section, aside, dialog, [role="dialog"], [role="alertdialog"]'
            );
            for (var i = 0; i < candidates.length; i++) {
                var el = candidates[i];
                if (looksLikeAdOverlay(el)) {
                    hide(el);
                }
            }
        } catch (_) {}
    }

    // --- Bootstrap -----------------------------------------------------------

    sweep(document);

    var observer = new MutationObserver(function () { sweep(document); });
    observer.observe(document.documentElement, { childList: true, subtree: true });

    // Clean up when the page navigates away (belt-and-suspenders for SPA navigations).
    window.addEventListener('beforeunload', function () {
        try { observer.disconnect(); } catch (_) {}
    }, { once: true });
})();
