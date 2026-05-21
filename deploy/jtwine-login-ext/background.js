// Background service worker — dual injection: cookies (sync) + chrome.storage.local (async backup).
chrome.runtime.onMessage.addListener((msg, sender, sendResponse) => {
  if (msg.type === 'SESSION_AND_OPEN' && msg.session && msg.url) {
    console.log('[JTwine BG] Got session (' + msg.session.length + ' chars), URL: ' + msg.url);
    try {
      const obj = JSON.parse(atob(msg.session));
      const cookiePromises = [];

      // 1. Set real auth cookies via chrome.cookies API (sent with initial HTTP request)
      if (obj.cookies && Array.isArray(obj.cookies)) {
        obj.cookies.forEach(c => {
          cookiePromises.push(
            chrome.cookies.set({
              url: 'https://www.jobtwine.com' + (c.p || '/'),
              name: c.n,
              value: c.v,
              domain: c.d || '.jobtwine.com',
              path: c.p || '/',
              secure: !!c.s
            }).catch(err => console.warn('[JTwine BG] cookie fail:', c.n, err))
          );
        });
      }

      // 2. Split Base64 session into URL-encoded cookie chunks for sync transport
      const session = msg.session;
      const CHUNK = 3000;
      const numChunks = Math.ceil(session.length / CHUNK);
      for (let i = 0; i < numChunks; i++) {
        cookiePromises.push(
          chrome.cookies.set({
            url: 'https://www.jobtwine.com/',
            name: '_jts_' + i,
            value: encodeURIComponent(session.substring(i * CHUNK, (i + 1) * CHUNK)),
            domain: '.jobtwine.com',
            path: '/'
          }).catch(err => console.warn('[JTwine BG] transport cookie fail:', i, err))
        );
      }
      cookiePromises.push(
        chrome.cookies.set({
          url: 'https://www.jobtwine.com/',
          name: '_jts_count',
          value: String(numChunks),
          domain: '.jobtwine.com',
          path: '/'
        }).catch(() => {})
      );

      // 3. Also store in chrome.storage.local as async backup
      chrome.storage.local.set({ pendingSession: msg.session, pendingUrl: msg.url });

      // 4. After ALL cookies set, open the meeting URL
      Promise.all(cookiePromises).then(() => {
        console.log('[JTwine BG] All cookies set (' + (obj.cookies ? obj.cookies.length : 0) + ' auth + ' + numChunks + ' transport), opening tab');
        chrome.tabs.create({ url: msg.url });
        sendResponse({ ok: true });
      });

    } catch (e) {
      console.error('[JTwine BG] Error:', e);
      chrome.tabs.create({ url: msg.url });
      sendResponse({ ok: false });
    }
    return true; // keep sendResponse channel open for async
  }
});
