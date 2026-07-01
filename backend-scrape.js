const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');
(async () => {
  const resultPath = path.join(process.cwd(), 'vocab_single.json');
  console.log('[scrape] Launching...');
  const browser = await chromium.launch({ headless: false, args: ['--no-sandbox'] });
  const context = await browser.newContext();
  const page = await context.newPage();
  page.setDefaultTimeout(20000);
  try {
    await page.goto('https://preply.com/en/learn/english/test-your-vocab', { timeout: 30000, waitUntil: 'load' });
    await page.waitForTimeout(5000);
    console.log('[scrape] URL:', page.url());

    // Do everything in browser context - more reliable
    const result = await page.evaluate(() => {
      const log = [];
      let allWords = [];
      let allKnown = [];
      
      for (let pg = 0; pg < 3; pg++) {
        // Find ALL checkboxes on the current page
        const checkboxes = document.querySelectorAll('input[type=checkbox]');
        log.push('Found ' + checkboxes.length + ' checkboxes');
        
        if (checkboxes.length === 0) break;
        
        for (const cb of checkboxes) {
          // Get the word label
          let label = '';
          const lbl = document.querySelector('label[for=' + CSS.escape(cb.id) + ']');
          if (lbl) label = lbl.textContent.trim();
          else if (cb.parentElement) label = cb.parentElement.textContent.trim();
          const word = label.split(/\s+/)[0].toLowerCase().replace(/[^a-z]/g, '');
          
          if (!word || word.length < 2) continue;
          if (allWords.includes(word)) continue;
          
          allWords.push(word);
          
          // Probability based on word length
          const prob = Math.min(0.7, 0.15 + (1 / Math.max(word.length, 3)) * 2.0);
          
          if (Math.random() < prob) {
            allKnown.push(word);
            // === TRIPLE approach to check the checkbox ===
            cb.focus();
            cb.click();
            cb.checked = true;
            cb.dispatchEvent(new Event('change', { bubbles: true }));
            cb.dispatchEvent(new Event('input', { bubbles: true }));
          }
        }
        
        // Check for Continue button
        const buttons = document.querySelectorAll('button');
        let foundContinue = false;
        for (const btn of buttons) {
          if (btn.textContent.toLowerCase().includes('continue')) {
            btn.click();
            foundContinue = true;
            break;
          }
        }
        
        if (!foundContinue) break;
        
        // Wait for next page (synchronous sleep in evaluate)
        const deadline = Date.now() + 5000;
        while (Date.now() < deadline) {}
      }
      
      // Extract estimate from result page
      const text = document.body.innerText;
      const lines = text.split('\n').map(l => l.trim()).filter(l => l.length > 0);
      let estimate = 0;
      for (let i = 1; i < lines.length; i++) {
        if (lines[i].toLowerCase() === 'words' || lines[i].toLowerCase() === 'word') {
          const m = lines[i-1].match(/^(\d{3,5})$/);
          if (m) { const n = parseInt(m[1]); if (n >= 500 && n <= 50000) { estimate = n; break; } }
        }
      }
      if (estimate === 0) {
        for (let i = 0; i < lines.length; i++) {
          if (lines[i].toLowerCase().includes('awesome')) {
            for (let j = i+1; j <= Math.min(lines.length-1, i+10); j++) {
              const m = lines[j].match(/^(\d{3,5})$/);
              if (m) { const n = parseInt(m[1]); if (n >= 500 && n <= 50000) { estimate = n; break; } }
            }
            if (estimate) break;
          }
        }
      }
      
      const unknown = allWords.filter(w => !allKnown.includes(w));
      log.push('Total words: ' + allWords.length + ', Known: ' + allKnown.length + ', Unknown: ' + unknown.length + ', Estimate: ' + estimate);
      
      return { knownWords: allKnown, unknownWords: unknown, standardEstimate: estimate, log: log };
    });
    
    console.log('[scrape] Log:');
    result.log.forEach(l => console.log('[scrape]', l));
    console.log('[scrape] Estimate:', result.standardEstimate);
    console.log('[scrape] Words:', result.knownWords.length + result.unknownWords.length);
    fs.writeFileSync(resultPath, JSON.stringify({ knownWords: result.knownWords, unknownWords: result.unknownWords, standardEstimate: result.standardEstimate }));
    console.log('[scrape] Saved');
  } catch(e) {
    console.log('[scrape] Error:', e.message);
    console.log('[scrape] Stack:', (e.stack || '').substring(0, 300));
    fs.writeFileSync(resultPath, JSON.stringify({ knownWords: [], unknownWords: [], standardEstimate: 0 }));
  }
  await browser.close();
  console.log('[scrape] Done');
})();
