const { chromium } = require("playwright");
const fs = require("fs");
const path = require("path");
(async () => {
  const resultPath = path.join(process.cwd(), "vocab_single.json");
  console.log("[scrape] Launching...");
  const browser = await chromium.launch({ headless: false, args: ["--no-sandbox"] });
  const context = await browser.newContext();
  const page = await context.newPage();
  page.setDefaultTimeout(20000);
  try {
    await page.goto("https://preply.com/en/learn/english/test-your-vocab", { timeout: 30000, waitUntil: "networkidle" });
    await page.waitForTimeout(3000);
    console.log("[scrape] URL:", page.url());
    
    // Use Playwright locator to find checkboxes (more reliable than evaluate)
    let checkboxes = page.locator("input[type=checkbox]");
    let count = await checkboxes.count();
    console.log("[scrape] Found", count, "checkboxes with locator");
    
    let allWords = [], allKnown = [];
    let pg = 0;
    
    while (count > 0 && pg < 3) {
      pg++;
      // Get word labels for each checkbox
      const words = await page.evaluate(() => {
        const cbs = document.querySelectorAll("input[type=checkbox]");
        return Array.from(cbs).map(cb => {
          let label = "";
          const lbl = document.querySelector("label[for=" + CSS.escape(cb.id) + "]");
          if (lbl) label = lbl.textContent.trim();
          else if (cb.parentElement) label = cb.parentElement.textContent.trim();
          const w = label.split(/\s+/)[0].toLowerCase().replace(/[^a-z]/g, "");
          return { word: w, id: cb.id, checked: cb.checked };
        }).filter(w => w.word && w.word.length >= 2);
      });
      console.log("[scrape] Page", pg+":", words.length, "words");
      
      let checked = 0;
      for (const w of words) {
        if (allWords.includes(w.word)) continue;
        allWords.push(w.word);
        const prob = Math.min(0.7, 0.15 + (1 / Math.max(w.word.length, 3)) * 2.0);
        if (Math.random() < prob) {
          allKnown.push(w.word);
          if (w.id && !w.checked) {
            try {
              // Use Playwright check() - simulates real mouse click
              const cb = page.locator("#" + CSS.escape(w.id));
              if (await cb.isVisible()) {
                await cb.check({ force: true });
                checked++;
              } else {
                console.log("[scrape] Checkbox not visible:", w.word);
              }
            } catch(e) {
              console.log("[scrape] check() failed for", w.word, ":", e.message.substring(0, 50));
              try {
                await page.evaluate((id) => {
                  const cb = document.getElementById(id);
                  if (cb) { cb.checked = true; cb.dispatchEvent(new Event("change", { bubbles: true })); }
                }, w.id);
                checked++;
              } catch(e2) {}
            }
          }
        }
      }
      console.log("[scrape] Checked", checked, "words");
      
      // Look for Continue button
      const contBtn = page.locator("button").filter({ hasText: "Continue" });
      if (await contBtn.count() > 0) {
        console.log("[scrape] Clicking Continue...");
        try { await contBtn.first().click({ timeout: 5000 }); await page.waitForTimeout(4000); } catch(e) {}
      } else break;
      
      checkboxes = page.locator("input[type=checkbox]");
      count = await checkboxes.count();
    }
    
    await page.waitForTimeout(3000);
    console.log("[scrape] Final URL:", page.url());
    
    const estimate = await page.evaluate(() => {
      const lines = document.body.innerText.split("\n").map(l => l.trim()).filter(l => l.length > 0);
      for (let i = 1; i < lines.length; i++) {
        if (lines[i].toLowerCase() === "words" || lines[i].toLowerCase() === "word") {
          const m = lines[i-1].match(/^(\d{3,5})$/);
          if (m) { const n = parseInt(m[1]); if (n >= 500 && n <= 50000) return n; }
        }
      }
      for (let i = 0; i < lines.length; i++) {
        if (lines[i].toLowerCase().includes("awesome")) {
          for (let j = i+1; j <= Math.min(lines.length-1, i+10); j++) {
            const m = lines[j].match(/^(\d{3,5})$/);
            if (m) { const n = parseInt(m[1]); if (n >= 500 && n <= 50000) return n; }
          }
        }
      }
      return 0;
    });
    console.log("[scrape] Estimate:", estimate);
    
    const unique = [...new Set(allWords)];
    allKnown = [...new Set(allKnown)];
    const unknown = unique.filter(w => !allKnown.includes(w));
    console.log("[scrape] Words:", unique.length, "Known:", allKnown.length, "Unknown:", unknown.length);
    fs.writeFileSync(resultPath, JSON.stringify({ knownWords: allKnown, unknownWords: unknown, standardEstimate: estimate }));
    console.log("[scrape] Saved");
  } catch(e) {
    console.log("[scrape] Error:", e.message);
    console.log("[scrape] Stack:", (e.stack || "").substring(0, 300));
    fs.writeFileSync(resultPath, JSON.stringify({ knownWords: [], unknownWords: [], standardEstimate: 0 }));
  }
  await browser.close();
  console.log("[scrape] Done");
})();
