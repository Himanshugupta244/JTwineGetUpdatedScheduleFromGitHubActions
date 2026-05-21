// Runs on GitHub Pages schedule report.
// Intercepts copyAndJoin() — sends session+URL to extension background.
(function() {
  // Show green badge confirming extension is active
  function addBadge() {
    var b = document.createElement('div');
    b.textContent = '\u2713 EXT';
    b.style.cssText = 'position:fixed;top:4px;right:4px;z-index:99999;background:#059669;color:#fff;font-size:10px;font-weight:900;padding:2px 8px;border-radius:6px;opacity:0.85;pointer-events:none;';
    document.body.appendChild(b);
  }
  if (document.body) addBadge(); else document.addEventListener('DOMContentLoaded', addBadge);

  // Listen for postMessage from injected page script
  window.addEventListener('message', function(event) {
    if (event.source !== window || !event.data || event.data.type !== 'JTWINE_SESSION') return;
    try {
      chrome.runtime.sendMessage({
        type: 'SESSION_AND_OPEN',
        session: event.data.session,
        url: event.data.url
      }, function() {
        if (chrome.runtime.lastError) {
          console.error('[JTwine Ext] sendMessage failed:', chrome.runtime.lastError.message);
          window.open(event.data.url, '_blank');
        }
      });
    } catch (e) {
      console.error('[JTwine Ext] Extension error, falling back:', e);
      window.open(event.data.url, '_blank');
    }
  });

  // Inject script into page context to REPLACE copyAndJoin
  var script = document.createElement('script');
  script.textContent = '(' + function() {
    window.copyAndJoin = function(who, url) {
      var id = who === 'him' ? 'sessionBlobHim' : 'sessionBlobSud';
      var el = document.getElementById(id);
      if (el && el.value) {
        window.postMessage({ type: 'JTWINE_SESSION', session: el.value, url: url }, '*');
      } else {
        window.open(url, '_blank');
      }
    };
  } + ')();';
  document.documentElement.appendChild(script);
  script.remove();
})();
