// Runs on schedule page (codifixsolutions.com or file://).
// Stores session directly in chrome.storage.local, then opens meeting URL.
(function() {
  // Show green badge confirming extension is active
  function addBadge() {
    var b = document.createElement('div');
    b.textContent = '\u2713 EXT';
    b.style.cssText = 'position:fixed;top:4px;right:4px;z-index:99999;background:#059669;color:#fff;font-size:10px;font-weight:900;padding:2px 8px;border-radius:6px;opacity:0.85;pointer-events:none;';
    document.body.appendChild(b);
  }
  if (document.body) addBadge(); else document.addEventListener('DOMContentLoaded', addBadge);

  // Inject script into page context to REPLACE copyAndJoin with extension-aware version
  var script = document.createElement('script');
  script.textContent = '(' + function() {
    window.copyAndJoin = function(who, url) {
      var id = who === 'him' ? 'sessionBlobHim' : 'sessionBlobSud';
      var el = document.getElementById(id);
      if (el && el.value) {
        // Send session+url to content script via postMessage
        window.postMessage({ type: 'JTWINE_SESSION', session: el.value, url: url }, '*');
      } else {
        window.open(url, '_blank');
      }
    };
  } + ')();';
  document.documentElement.appendChild(script);
  script.remove();

  // Listen for postMessage from the injected page script
  window.addEventListener('message', function(event) {
    if (event.source !== window || !event.data || event.data.type !== 'JTWINE_SESSION') return;
    var session = event.data.session;
    var url = event.data.url;
    console.log('[JTwine Ext] Got session (' + session.length + ' chars), URL: ' + url);

    // Store session in chrome.storage.local, then open URL.
    // On file:// pages, background is unreachable — that's OK.
    // content-jobtwine.js will pick up the session on the jobtwine.com side
    // and message the background FROM THERE (https:// pages can message background).
    chrome.storage.local.set({ pendingSession: session, pendingUrl: url }, function() {
      console.log('[JTwine Ext] Session stored in chrome.storage.local, opening URL...');
      window.open(url, '_blank');
    });
  });
})();
