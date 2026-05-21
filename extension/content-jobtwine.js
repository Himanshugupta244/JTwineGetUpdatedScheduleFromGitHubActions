// Runs on jobtwine.com at document_start.
(function() {
  console.log('[JTwine] content-jobtwine.js running on: ' + location.href);

  // Show visible banner so user knows extension is active
  function showBanner(text, color) {
    var existing = document.getElementById('jtwine-ext-banner');
    if (existing) existing.remove();
    function add() {
      var d = document.createElement('div');
      d.id = 'jtwine-ext-banner';
      d.textContent = text;
      d.style.cssText = 'position:fixed;top:0;left:0;right:0;z-index:999999;background:' + color + ';color:#fff;font-size:14px;font-weight:bold;padding:8px;text-align:center;';
      (document.body || document.documentElement).appendChild(d);
    }
    if (document.body) add();
    else document.addEventListener('DOMContentLoaded', add);
  }

  chrome.storage.local.get(['pendingSession', 'pendingUrl'], function(data) {
    if (!data || !data.pendingSession) {
      console.log('[JTwine] No pending session — normal visit');
      return;
    }

    var session = data.pendingSession;
    var meetingUrl = data.pendingUrl || '';
    console.log('[JTwine] Found pending session (' + session.length + ' chars), meeting: ' + meetingUrl);
    showBanner('[EXT] Setting cookies... please wait', '#059669');

    // Clear pending session (one-time use, prevent loops)
    chrome.storage.local.remove(['pendingSession', 'pendingUrl']);

    // Inject localStorage and sessionStorage
    try {
      var obj = JSON.parse(atob(session));

      if (obj.ls) {
        var lsKeys = Object.keys(obj.ls);
        for (var i = 0; i < lsKeys.length; i++) {
          try { localStorage.setItem(lsKeys[i], obj.ls[lsKeys[i]]); } catch(e) {}
        }
        console.log('[JTwine] Set ' + lsKeys.length + ' localStorage keys');
      }

      if (obj.ss) {
        var ssKeys = Object.keys(obj.ss);
        for (var i = 0; i < ssKeys.length; i++) {
          try { sessionStorage.setItem(ssKeys[i], obj.ss[ssKeys[i]]); } catch(e) {}
        }
        console.log('[JTwine] Set ' + ssKeys.length + ' sessionStorage keys');
      }

      if (obj.cookies && Array.isArray(obj.cookies)) {
        obj.cookies.forEach(function(c) {
          try {
            document.cookie = c.n + '=' + c.v + ';domain=' + (c.d || '.jobtwine.com') + ';path=' + (c.p || '/') + (c.s ? ';secure' : '');
          } catch(e) {}
        });
        console.log('[JTwine] Set ' + obj.cookies.length + ' cookies via document.cookie');
      }
    } catch (e) {
      console.error('[JTwine] Error injecting session:', e);
    }

    // Send to background for chrome.cookies API (can set HttpOnly + SameSite cookies)
    chrome.runtime.sendMessage({
      type: 'SET_COOKIES_ONLY',
      session: session
    }, function(response) {
      if (chrome.runtime.lastError) {
        console.warn('[JTwine] Background error:', chrome.runtime.lastError.message);
        showBanner('[EXT] Background error: ' + chrome.runtime.lastError.message, '#dc2626');
      } else {
        console.log('[JTwine] Background set cookies:', JSON.stringify(response));
      }

      // Small delay to ensure cookies are flushed to browser cookie store
      setTimeout(function() {
        // IMPORTANT: check pathname only, NOT full href.
        // The signin URL query string contains /meeting/ (returnUrl param) which
        // would trick a href check into thinking we're already on the meeting page.
        if (meetingUrl && location.pathname.indexOf('/meeting/') === -1) {
          console.log('[JTwine] Redirecting to meeting: ' + meetingUrl);
          showBanner('[EXT] Cookies set! Redirecting to meeting...', '#1d4ed8');
          // Use location.replace to avoid caching the /signin redirect
          setTimeout(function() { window.location.replace(meetingUrl); }, 300);
        } else {
          console.log('[JTwine] Already on meeting page, reloading...');
          location.reload();
        }
      }, 500);
    });
  });
})();
