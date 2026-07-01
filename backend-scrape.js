const { chromium } = require("playwright");
const fs = require("fs");
const path = require("path");
(async () => {
  const resultPath = path.join(process.cwd(), "vocab_single.json");
  console.log("[scrape] Launching...");
  const browser = await chromium.launch({
    headless: false,
    args: ["--no-sandbox", "--disable-blink-features=AutomationControlled", "--start-maximized"]
  });
  const context = await browser.newContext({
    userAgent: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
    viewport: { width: 1280, height: 800 },
    locale: "en-US"
  });
  const page = await context.newPage();
  page.setDefaultTimeout(90000);
  try {
    console.log("Navigating...");
    await page.goto("https://preply.com/en/learn/english/test-your-vocab", { timeout: 90000, waitUntil: "networkidle" });
    await page.waitForTimeout(2000);
    console.log("URL:", page.url());
    const allKnown = [], allUnknown = [];
    for (let pg = 0; pg < 3; pg++) {
      await page.waitForTimeout(1500);
      const cs = page.locator("input[type=checkbox]:visible");
      const n = await cs.count();
      console.log("Page", pg+1, "checkboxes:", n);
      if (n === 0) break;
      let checked = 0;
      for (let i = 0; i < n; i++) {
        try {
          if (await cs.nth(i).isHidden().catch(() => true)) continue;
          let word = null;
          const id = await cs.nth(i).getAttribute("id").catch(() => null);
          if (id) {
            const lbl = page.locator("label[for=\"" + id + "\"]").first();
            if (await lbl.isVisible().catch(() => false))
              word = await lbl.textContent().catch(() => null);
          }
          if (!word) {
            word = await cs.nth(i).locator("xpath=..").textContent().catch(() => null);
          }
          if (!word) continue;
          const cw = word.trim().split(/[\s,]/)[0].toLowerCase().replace(/[^a-z]/g, "");
          if (!cw || cw.length < 2) continue;
          const prob = cw.length <= 3 ? 0.6 : (cw.length <= 5 ? 0.4 : 0.2);
          if (Math.random() < prob) {
            try { await cs.nth(i).check({ force: true, timeout: 3000 }); }
            catch(e) { try { await cs.nth(i).click({ force: true, timeout: 2000 }); } catch(e2) { continue; } }
            checked++;
            allKnown.push(cw);
          } else { allUnknown.push(cw); }
        } catch(e) {}
      }
      console.log("Checked", checked, "words");
      const cont = page.locator("button").filter({ hasText: /continue/i }).first();
      if (await cont.isVisible().catch(() => false)) {
        console.log("Clicking Continue...");
        await cont.scrollIntoViewIfNeeded();
        await page.waitForTimeout(300);
        await cont.click({ timeout: 10000 });
        await page.waitForTimeout(3000);
      } else { break; }
    }
    await page.waitForTimeout(2000);
    const est = await page.evaluate(() => {
      const l = document.body.innerText.split("\n").map(x => x.trim()).filter(x => x.length > 0);
      for (let i = 1; i < l.length; i++) {
        if (l[i].toLowerCase() === "words" || l[i].toLowerCase() === "word") {
          const m = l[i-1].match(/^(\d{1,5})$/);
          if (m) { const n = parseInt(m[1]); if (n >= 100 && n <= 100000) return n; }
        }
      }
      for (const kw of ["awesome","vocabulary","estimated","result"]) {
        for (let i = 0; i < l.length; i++) {
          if (l[i].toLowerCase().includes(kw)) {
            for (let j = Math.max(0,i-3); j <= Math.min(l.length-1,i+5); j++) {
              const m = l[j].match(/\b(\d{3,5})\b/);
              if (m) { const n = parseInt(m[1]); if (n >= 100 && n <= 100000) return n; }
            }
          }
        }
      }
      const h = document.querySelectorAll("h1,h2,h3,.stat-value");
      for (const x of h) {
        const m = x.textContent.trim().match(/\b(\d{3,5})\b/);
        if (m) { const n = parseInt(m[1]); if (n >= 100 && n <= 100000) return n; }
      }
      return 0;
    });
    console.log("Estimate:", est);
    const d = {
      knownWords: [...new Set(allKnown)],
      unknownWords: [...new Set(allUnknown)].filter(w => !allKnown.includes(w)),
      standardEstimate: est
    };
    console.log("Known:", d.knownWords.length, "Unknown:", d.unknownWords.length);
    fs.writeFileSync(resultPath, JSON.stringify(d));
    console.log("Saved");
  } catch(e) {
    console.log("Error:", e.message);
    fs.writeFileSync(resultPath, JSON.stringify({ knownWords: [], unknownWords: [], standardEstimate: 0 }));
  }
  await browser.close();
  console.log("Done");
})();
