// ==UserScript==
// @name         JTwine Auto-Login
// @namespace    https://cloud.codifixsolutions.com/
// @version      2.0
// @description  Auto-login to JTwine when JOIN link has #jt=him or #jt=sud hash
// @match        https://www.jobtwine.com/*
// @grant        none
// @run-at       document-idle
// ==/UserScript==

(function () {
    'use strict';

    // ============================================================
    //  CREDENTIALS — edit these once, stored locally in your browser
    // ============================================================
    var CREDS = {
        him: { user: 'HIMANSHU_USERNAME_HERE', pass: 'HIMANSHU_PASSWORD_HERE' },
        sud: { user: 'SUDHANSHU_USERNAME_HERE', pass: 'SUDHANSHU_PASSWORD_HERE' }
    };
    // ============================================================

    var hash = window.location.hash;      // e.g. #jt=him
    var path = window.location.pathname;  // e.g. /meeting/copilot or /signin
    // declared once here to avoid duplicate-var linter errors
    var who = null, meetingNext = null, meetingUrl = null;

    // --- STEP 1: On ANY page with #jt=him or #jt=sud, save auto-login info ---
    var hashMatch = hash.match(/^#jt=(him|sud)$/);
    if (hashMatch) {
        who = hashMatch[1];
        // Save meeting URL (without hash) + account to sessionStorage
        meetingUrl = window.location.href.replace(/#.*$/, '');
        sessionStorage.setItem('jt_auto', JSON.stringify({ who: who, next: meetingUrl }));
        console.log('[JTwine Auto-Login] Saved auto-login for: ' + who + ', meeting: ' + meetingUrl);

        // If we're already on /signin, proceed to login immediately
        if (path === '/signin') {
            doAutoLogin(who, meetingUrl);
            return;
        }
        // Otherwise the page will load normally if user is already logged in.
        // If NOT logged in, JTwine will redirect to /signin — Step 2 handles that.
        return;
    }

    // --- STEP 2: On /signin, extract who+meetingUrl from returnUrl query param ---
    // JTwine encodes the meeting URL (including our #jt= hash) into ?returnUrl=...
    // e.g. returnUrl=https://...meeting/copilot?id=...%23jt%3Dhim
    if (path === '/signin') {
        who = null; meetingNext = null;

        // Method A: sessionStorage (may be blocked by Edge Tracking Prevention)
        try {
            var saved = sessionStorage.getItem('jt_auto');
            if (saved) {
                var data = JSON.parse(saved);
                who = data.who; meetingNext = data.next;
            }
        } catch (e) {}

        // Method B (fallback): decode returnUrl param and look for #jt= inside it
        if (!who) {
            try {
                var params = new URLSearchParams(window.location.search);
                var returnUrl = params.get('returnUrl');
                if (returnUrl) {
                    var decoded = decodeURIComponent(returnUrl);
                    var jtMatch = decoded.match(/#jt=(him|sud)/);
                    if (jtMatch) {
                        who = jtMatch[1];
                        meetingNext = decoded.replace(/#.*$/, '');
                        console.log('[JTwine Auto-Login] Extracted from returnUrl: ' + who + ' → ' + meetingNext);
                    }
                }
            } catch (e) {
                console.log('[JTwine Auto-Login] returnUrl parse error: ' + e.message);
            }
        }

        if (who && CREDS[who]) {
            try { sessionStorage.removeItem('jt_auto'); } catch(e) {}
            console.log('[JTwine Auto-Login] Starting auto-login for: ' + who);
            doAutoLogin(who, meetingNext);
        } else {
            console.log('[JTwine Auto-Login] /signin — no jt_auto data found');
        }
        return;
    }

    // ============================================================
    //  AUTO-LOGIN FUNCTION
    // ============================================================
    function doAutoLogin(who, nextUrl) {
        var cred = CREDS[who];
        if (!cred) return;
        // Clear saved data so it doesn't trigger again
        sessionStorage.removeItem('jt_auto');

        console.log('[JTwine Auto-Login] Starting login for: ' + who);

        // Wait for username field, fill it, click Next
        waitFor('input[formcontrolname="userName"]', function (userInput) {
            setAngularValue(userInput, cred.user);
            console.log('[JTwine Auto-Login] Username filled');

            waitFor('button', function (nextBtn) {
                nextBtn.click();
                console.log('[JTwine Auto-Login] Next clicked');

                // Wait for password field, fill it, click Sign In
                waitFor('input[formcontrolname="password"]', function (passInput) {
                    setAngularValue(passInput, cred.pass);
                    console.log('[JTwine Auto-Login] Password filled');

                    waitFor('button', function (signInBtn) {
                        signInBtn.click();
                        console.log('[JTwine Auto-Login] Sign In clicked');

                        // After login, redirect to meeting URL
                        if (nextUrl) {
                            waitForLogin(function () {
                                console.log('[JTwine Auto-Login] Login done, redirecting to: ' + nextUrl);
                                window.location.href = nextUrl;
                            });
                        }
                    }, 'Sign In');
                });
            }, 'Next');
        });
    }

    // ============================================================
    //  HELPER: Wait for element to appear in DOM
    // ============================================================
    function waitFor(selector, callback, buttonText, attempts) {
        attempts = attempts || 0;
        if (attempts > 100) {
            console.log('[JTwine Auto-Login] Timeout waiting for: ' + selector + (buttonText ? ' (' + buttonText + ')' : ''));
            return;
        }
        var els = document.querySelectorAll(selector);
        if (buttonText) {
            for (var i = 0; i < els.length; i++) {
                if (els[i].textContent.trim().indexOf(buttonText) !== -1) {
                    callback(els[i]);
                    return;
                }
            }
        } else if (els.length > 0) {
            callback(els[0]);
            return;
        }
        setTimeout(function () { waitFor(selector, callback, buttonText, attempts + 1); }, 200);
    }

    // ============================================================
    //  HELPER: Set value on Angular reactive form input
    // ============================================================
    function setAngularValue(el, value) {
        var nativeInputValueSetter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;
        nativeInputValueSetter.call(el, value);
        el.dispatchEvent(new Event('input', { bubbles: true }));
        el.dispatchEvent(new Event('change', { bubbles: true }));
    }

    // ============================================================
    //  HELPER: Wait until URL changes away from /signin
    // ============================================================
    function waitForLogin(callback, attempts) {
        attempts = attempts || 0;
        if (attempts > 150) { // 30 seconds max
            console.log('[JTwine Auto-Login] Timeout waiting for login redirect');
            return;
        }
        if (window.location.pathname !== '/signin') {
            callback();
        } else {
            setTimeout(function () { waitForLogin(callback, attempts + 1); }, 200);
        }
    }

})();
