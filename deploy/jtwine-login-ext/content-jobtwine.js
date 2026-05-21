// Runs on jobtwine.com at document_start.
// DUAL APPROACH: (1) sync cookie transport, (2) async chrome.storage.local backup.
// Sets localStorage/sessionStorage BEFORE Angular boots.
(function() {
  console.log('[JTwine] content-jobtwine.js running on: ' + location.href);
  var injected = false;

  // ====== APPROACH 1: Synchronous cookie transport ======
  var cookies = document.cookie;
  var countMatch = cookies.match(/(^|;\s*)_jts_count=(\d+)/);
  if (countMatch) {
    var count = parseInt(countMatch[2]);
    if (count > 0) {
      var session = '';
      for (var i = 0; i < count; i++) {
        var re = new RegExp('(^|;\\s*)_jts_' + i + '=([^;]+)');
        var m = cookies.match(re);
        if (m) session += decodeURIComponent(m[2]);
      }
      if (session) {
        console.log('[JTwine] SYNC: Found session from cookies (' + session.length + ' chars)');
        injected = injectSession(session);
        // Clean up transport cookies
        for (var j = 0; j < count; j++) {
          document.cookie = '_jts_' + j + '=;domain=.jobtwine.com;path=/;max-age=0';
        }
        document.cookie = '_jts_count=;domain=.jobtwine.com;path=/;max-age=0';
      }
    }
  }

  // ====== APPROACH 2: Async chrome.storage.local backup ======
  if (!injected) {
    console.log('[JTwine] SYNC cookies not found, trying chrome.storage.local...');
    chrome.storage.local.get(['pendingSession', 'pendingUrl'], function(data) {
      if (!data || !data.pendingSession) {
        console.log('[JTwine] No pending session in storage either — normal visit');
        return;
      }
      console.log('[JTwine] ASYNC: Found session in storage.local');
      injectSession(data.pendingSession);
      var meetingUrl = data.pendingUrl || '';
      chrome.storage.local.remove(['pendingSession', 'pendingUrl']);
      // localStorage is now set. Reload so Angular picks it up.
      // If we ended up on login page, go to meeting URL instead.
      if (meetingUrl && location.href.indexOf('/login') !== -1) {
        console.log('[JTwine] On login page — redirecting to: ' + meetingUrl);
        location.href = meetingUrl;
      } else {
        console.log('[JTwine] Reloading to apply session...');
        location.reload();
      }
    });
  }

  function injectSession(sessionBase64) {
    try {
      var obj = JSON.parse(atob(sessionBase64));

      // localStorage (SYNCHRONOUS — available before Angular boots)
      if (obj.ls) {
        var lsKeys = Object.keys(obj.ls);
        for (var i = 0; i < lsKeys.length; i++) {
          localStorage.setItem(lsKeys[i], obj.ls[lsKeys[i]]);
        }
        console.log('[JTwine] localStorage: ' + lsKeys.length + ' keys set');
      }

      // sessionStorage (SYNCHRONOUS)
      if (obj.ss) {
        var ssKeys = Object.keys(obj.ss);
        for (var i = 0; i < ssKeys.length; i++) {
          sessionStorage.setItem(ssKeys[i], obj.ss[ssKeys[i]]);
        }
        console.log('[JTwine] sessionStorage: ' + ssKeys.length + ' keys set');
      }

      // Cookies via document.cookie (supplements chrome.cookies from background)
      if (obj.cookies && Array.isArray(obj.cookies)) {
        obj.cookies.forEach(function(c) {
          document.cookie = c.n + '=' + c.v + ';domain=' + (c.d || '.jobtwine.com') + ';path=' + (c.p || '/') + (c.s ? ';secure' : '');
        });
        console.log('[JTwine] document.cookie: ' + obj.cookies.length + ' cookies set');
      }

      return true;
    } catch (e) {
      console.error('[JTwine] injectSession error:', e);
      return false;
    }
  }
})();
