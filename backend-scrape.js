const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');
(async () => {
  const resultPath = path.join(process.cwd(), 'vocab_single.json');
  console.log('[scrape] Launching...');
  const browser = await chromium.launch({ headless: false, args: ['--no-sandbox'] });
  const page = await browser.newPage();
  page.setDefaultTimeout(20000);
  try {
    await page.goto('https://preply.com/en/learn/english/test-your-vocab', { timeout: 30000, waitUntil: 'load' });
    await page.waitForTimeout(5000);
    console.log('[scrape] URL:', page.url());
    console.log('[scrape] Title:', await page.title());

    // Debug: take screenshot
    await page.screenshot({ path: path.join(process.cwd(), 'scrape_debug.png') });
    console.log('[scrape] Screenshot saved');

    // Find all checkboxes using Playwright locator
    let cbs = page.locator('input[type=checkbox]');
    let total = await cbs.count();
    console.log('[scrape] Found', total, 'checkboxes');

    if (total > 0) {
      // Try clicking ALL checkboxes (no probability filtering for debugging)
      let clicked = 0;
      for (let i = 0; i < total; i++) {
        try {
          await cbs.nth(i).click({ force: true });
          clicked++;
        } catch(e) {
          console.log('[scrape] Click failed for checkbox', i, ':', e.message.substring(0, 50));
        }
      }
      console.log('[scrape] Clicked', clicked, 'of', total, 'checkboxes');
      
      // Now try dispatchEvent on any remaining unchecked
      const evResult = await page.evaluate(() => {
        const cbs = document.querySelectorAll('input[type=checkbox]');
        let evCount = 0;
        for (const cb of cbs) {
          if (!cb.checked) {
            cb.checked = true;
            cb.dispatchEvent(new Event('change', { bubbles: true }));
            cb.dispatchEvent(new Event('input', { bubbles: true }));
            evCount++;
          }
        }
        const words = Array.from(cbs).map(cb => {
          let label = '';
          const lbl = document.querySelector('label[for=' + CSS.escape(cb.id) + ']');
          if (lbl) label = lbl.textContent.trim();
          else if (cb.parentElement) label = cb.parentElement.textContent.trim();
          return label.split(/\\s+/)[0].toLowerCase().replace(/[^a-z]/g, '');
        }).filter(w => w && w.length >= 2);
        const checkedCount = Array.from(cbs).filter(cb => cb.checked).length;
        return { evCount, words, checkedCount };
      });
      console.log('[scrape] After events:', evResult.checkedCount, 'checked,', evResult.evCount, 'via dispatchEvent');
      console.log('[scrape] Words found:', evResult.words.length);
      console.log('[scrape] First 5 words:', JSON.stringify(evResult.words.slice(0, 5)));
      
      // Click Continue
      const contBtn = page.locator('button').filter({ hasText: 'Continue' });
      if (await contBtn.count() > 0) {
        console.log('[scrape] Clicking Continue...');
        await contBtn.first().click();
        await page.waitForTimeout(5000);
        console.log('[scrape] After Continue URL:', page.url());
        
        // Handle additional pages
        let moreCbs = page.locator('input[type=checkbox]');
        let moreCount = await moreCbs.count();
        console.log('[scrape] Next page checkboxes:', moreCount);
        
        if (moreCount > 0) {
          for (let i = 0; i < moreCount; i++) {
            try {
              await moreCbs.nth(i).click({ force: true });
            } catch(e) {}
          }
          // dispatchEvent fallback
          await page.evaluate(() => {
            const cbs = document.querySelectorAll('input[type=checkbox]');
            for (const cb of cbs) {
              if (!cb.checked) {
                cb.checked = true;
                cb.dispatchEvent(new Event('change', { bubbles: true }));
              }
            }
          });
          // Click Continue again
          const cont2 = page.locator('button').filter({ hasText: 'Continue' });
          if (await cont2.count() > 0) {
            await cont2.first().click();
            await page.waitForTimeout(5000);
          }
        }
      }
    }

    await page.waitForTimeout(3000);
    console.log('[scrape] Final URL:', page.url());

    // Extract estimate
    const est = await page.evaluate(() => {
      const lines = document.body.innerText.split('\\n').map(l => l.trim()).filter(l => l.length > 0);
      for (let i = 1; i < lines.length; i++) {
        if (lines[i].toLowerCase() === 'words' || lines[i].toLowerCase() === 'word') {
          const m = lines[i-1].match(/^(\\d{3,5})$/);
          if (m) { const n = parseInt(m[1]); if (n >= 500 && n <= 50000) return n; }
        }
      }
      for (let i = 0; i < lines.length; i++) {
        if (lines[i].toLowerCase().includes('awesome')) {
          for (let j = i+1; j <= Math.min(lines.length-1, i+10); j++) {
            const m = lines[j].match(/^(\\d{3,5})$/);
            if (m) { const n = parseInt(m[1]); if (n >= 500 && n <= 50000) return n; }
          }
        }
      }
      return 0;
    });
    console.log('[scrape] Estimate:', est);

    // Get known/unknown words from evaluate
    const finalData = await page.evaluate(() => {
      const cbs = document.querySelectorAll('input[type=checkbox]');
      const known = [], all = [];
      for (const cb of cbs) {
        let label = '';
        const lbl = document.querySelector('label[for=' + CSS.escape(cb.id) + ']');
        if (lbl) label = lbl.textContent.trim();
        else if (cb.parentElement) label = cb.parentElement.textContent.trim();
        const word = label.split(/\\s+/)[0].toLowerCase().replace(/[^a-z]/g, '');
        if (word && word.length >= 2) {
          all.push(word);
          if (cb.checked) known.push(word);
        }
      }
      const unknown = all.filter(w => !known.includes(w));
      return { knownWords: [...new Set(known)], unknownWords: [...new Set(unknown)] };
    });
    
    console.log('[scrape] Known:', finalData.knownWords.length, 'Unknown:', finalData.unknownWords.length);
    fs.writeFileSync(resultPath, JSON.stringify({ knownWords: finalData.knownWords, unknownWords: finalData.unknownWords, standardEstimate: est }));
    console.log('[scrape] Saved');
  } catch(e) {
    console.log('[scrape] Error:', e.message);
    console.log('[scrape] Stack:', (e.stack || '').substring(0, 300));
    fs.writeFileSync(resultPath, JSON.stringify({ knownWords: [], unknownWords: [], standardEstimate: 0 }));
  }
  await browser.close();
  console.log('[scrape] Done');
})();
