// Background service worker — sets cookies via chrome.cookies API.
chrome.runtime.onMessage.addListener((msg, sender, sendResponse) => {
  if ((msg.type === 'SET_COOKIES_AND_OPEN' || msg.type === 'SET_COOKIES_ONLY') && msg.session) {
    const shouldOpen = msg.type === 'SET_COOKIES_AND_OPEN' && msg.url;
    console.log('[JTwine BG] ' + msg.type);
    try {
      const obj = JSON.parse(atob(msg.session));
      const cookiePromises = [];
      const expiry = Math.floor(Date.now() / 1000) + 86400; // 24 hours from now

      if (obj.cookies && Array.isArray(obj.cookies)) {
        obj.cookies.forEach(c => {
          // Build the url from the cookie domain
          let cookieDomain = c.d || '.jobtwine.com';
          let urlDomain = cookieDomain.startsWith('.') ? cookieDomain.substring(1) : cookieDomain;
          let cookieUrl = 'https://' + urlDomain + (c.p || '/');

          cookiePromises.push(
            chrome.cookies.set({
              url: cookieUrl,
              name: c.n,
              value: c.v,
              domain: cookieDomain,
              path: c.p || '/',
              secure: true,
              httpOnly: true,
              sameSite: 'no_restriction',
              expirationDate: expiry
            }).then(result => {
              console.log('[JTwine BG] cookie OK:', c.n);
              return result;
            }).catch(err => {
              console.warn('[JTwine BG] cookie FAIL:', c.n, String(err));
              // Retry without httpOnly (some cookies might not be httpOnly)
              return chrome.cookies.set({
                url: cookieUrl,
                name: c.n,
                value: c.v,
                domain: cookieDomain,
                path: c.p || '/',
                secure: true,
                sameSite: 'lax',
                expirationDate: expiry
              }).catch(err2 => console.warn('[JTwine BG] cookie FAIL retry:', c.n, String(err2)));
            })
          );
        });
      }

      Promise.all(cookiePromises).then(() => {
        console.log('[JTwine BG] All done: ' + cookiePromises.length + ' cookies processed');
        if (shouldOpen) chrome.tabs.create({ url: msg.url });
        sendResponse({ ok: true, count: cookiePromises.length });
      });

    } catch (e) {
      console.error('[JTwine BG] Error:', e);
      if (shouldOpen) chrome.tabs.create({ url: msg.url });
      sendResponse({ ok: false, error: String(e) });
    }
    return true;
  }
});
