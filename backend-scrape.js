const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');
(async () => {
  const resultPath = path.join(process.cwd(), 'vocab_single.json');
  console.log('[scrape] Launching...');
  const browser = await chromium.launch({ headless: false, args: ['--no-sandbox'] });
  const context = await browser.newContext();
  const page = await context.newPage();
  page.setDefaultTimeout(15000);
  try {
    await page.goto('https://preply.com/en/learn/english/test-your-vocab', { timeout: 30000 });
    await page.waitForTimeout(5000);
    console.log('[scrape] URL:', page.url());
    let allWords = [], allKnown = [];
    for (let pg = 0; pg < 3; pg++) {
      const pd = await page.evaluate(() => {
        const cbs = document.querySelectorAll('input[type=checkbox]');
        if (cbs.length === 0) return { words: [], hasContinue: false };
        const ws = [];
        for (const cb of cbs) {
          let label = '';
          const lbl = document.querySelector('label[for=' + CSS.escape(cb.id) + ']');
          if (lbl) label = lbl.textContent.trim();
          else if (cb.parentElement) label = cb.parentElement.textContent.trim();
          const w = label.split(/\s+/)[0].toLowerCase().replace(/[^a-z]/g, '');
          if (w && w.length >= 2) ws.push({ word: w, id: cb.id, checked: cb.checked });
        }
        let hasCont = false;
        for (const btn of document.querySelectorAll('button'))
          if (btn.textContent.toLowerCase().includes('continue')) hasCont = true;
        return { words: ws, hasContinue: hasCont };
      });
      if (pd.words.length === 0) break;
      console.log('[scrape] Page', pg+1+':', pd.words.length, 'words');
      let checked = 0;
      for (const w of pd.words) {
        if (allWords.includes(w.word)) continue;
        allWords.push(w.word);
        const prob = Math.min(0.7, 0.15 + (1 / Math.max(w.word.length, 3)) * 2.0);
        if (Math.random() < prob) {
          allKnown.push(w.word);
          if (w.id && !w.checked) {
            try {
              await page.evaluate((id) => {
                const cb = document.getElementById(id);
                if (cb) {
                  cb.checked = true;
                  cb.dispatchEvent(new Event('change', { bubbles: true }));
                  cb.dispatchEvent(new Event('input', { bubbles: true }));
                }
              }, w.id);
              checked++;
            } catch(e) {}
          }
        }
      }
      console.log('[scrape] Checked', checked, 'words on page', pg+1);
      if (pd.hasContinue) {
        console.log('[scrape] Clicking Continue...');
        try { await page.locator('button').filter({ hasText: 'Continue' }).first().click({ timeout: 8000 }); } catch(e) { console.log('[scrape] Continue not found'); }
        await page.waitForTimeout(5000);
      } else break;
    }
    await page.waitForTimeout(3000);
    console.log('[scrape] Final URL:', page.url());
    const estimate = await page.evaluate(() => {
      const lines = document.body.innerText.split('\n').map(l => l.trim()).filter(l => l.length > 0);
      for (let i = 1; i < lines.length; i++) {
        if (lines[i].toLowerCase() === 'words' || lines[i].toLowerCase() === 'word') {
          const m = lines[i-1].match(/^(\d{3,5})$/);
          if (m) { const n = parseInt(m[1]); if (n >= 500 && n <= 50000) return n; }
        }
      }
      for (let i = 0; i < lines.length; i++) {
        if (lines[i].toLowerCase().includes('awesome')) {
          for (let j = i+1; j <= Math.min(lines.length-1, i+10); j++) {
            const m = lines[j].match(/^(\d{3,5})$/);
            if (m) { const n = parseInt(m[1]); if (n >= 500 && n <= 50000) return n; }
          }
        }
      }
      return 0;
    });
    console.log('[scrape] Estimate:', estimate);
    const unique = [...new Set(allWords)];
    allKnown = [...new Set(allKnown)];
    const unknown = unique.filter(w => !allKnown.includes(w));
    console.log('[scrape] Words:', unique.length, 'Known:', allKnown.length, 'Unknown:', unknown.length);
    fs.writeFileSync(resultPath, JSON.stringify({ knownWords: allKnown, unknownWords: unknown, standardEstimate: estimate }));
    console.log('[scrape] Saved');
  } catch(e) {
    console.log('[scrape] Error:', e.message);
    fs.writeFileSync(resultPath, JSON.stringify({ knownWords: [], unknownWords: [], standardEstimate: 0 }));
  }
  await browser.close();
  console.log('[scrape] Done');
})();
