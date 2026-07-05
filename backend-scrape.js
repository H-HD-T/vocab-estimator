const { chromium } = require("playwright");
const fs = require("fs");
const path = require("path");
const http = require("http");

const DIFFICULTY_PROB = { "K": 0.80, "P": 0.50, "F": 0.25, "C": 0.10, "UNKNOWN": 0.05 };
const DEFAULT_PROB = 0.30;
const DIFF_RANK = { "K": 0, "P": 1, "F": 2, "C": 3, "UNKNOWN": 4 };
const MAX_PROB = { "K": 0.99, "P": 0.95, "F": 0.80, "C": 0.60, "UNKNOWN": 0.10 };

function lookupDifficulties(words) {
  return new Promise((resolve) => {
    const data = JSON.stringify(words);
    const opts = { hostname: "localhost", port: 8088, path: "/api/validation/lookup-words", method: "POST",
      headers: { "Content-Type": "application/json", "Content-Length": Buffer.byteLength(data) } };
    const req = http.request(opts, (res) => {
      let body = "";
      res.on("data", chunk => body += chunk);
      res.on("end", () => { try { const r = JSON.parse(body); resolve(r.code === 200 ? (r.data || {}) : {}); } catch(e) { resolve({}); } });
    });
    req.on("error", () => resolve(null));
    req.write(data); req.end();
  });
}

async function extractWord(page, cb) {
  try {
    if (await cb.isHidden().catch(() => true)) return null;
    let word = null;
    const id = await cb.getAttribute("id").catch(() => null);
    if (id) { const lbl = page.locator('label[for="' + id + '"]').first();
      if (await lbl.isVisible().catch(() => false)) word = await lbl.textContent().catch(() => null); }
    if (!word) word = await cb.locator("xpath=..").textContent().catch(() => null);
    if (!word) return null;
    const cw = word.trim().split(/[\s,]/)[0].toLowerCase().replace(/[^a-z]/g, "");
    if (!cw || cw.length < 2) return null;
    return cw;
  } catch(e) { return null; }
}

function generateProbabilities(mastery) {
  const probs = {};
  for (const [diff, rank] of Object.entries(DIFF_RANK)) {
    probs[diff] = Math.min(MAX_PROB[diff] || 0.10, Math.max(0.01, Math.pow(mastery, rank)));
  }
  return probs;
}

async function extractCi(page) {
  let attempts = 0;
  while (attempts < 10) {
    await page.waitForTimeout(2000);
    const ci = await page.evaluate(() => {
      const lines = document.body.innerText.split("\n").map(x => x.trim()).filter(x => x.length > 0);
      const nums = [];
      for (const line of lines) {
        const m = line.match(/\b(\d{3,5})\b/g);
        if (m) for (const nStr of m) { const n = parseInt(nStr); if (n >= 500 && n <= 40000) nums.push(n); }
      }
      if (nums.length > 0) { nums.sort((a,b) => b-a); return nums[0]; }
      const big = [];
      for (const line of lines) {
        const m = line.match(/\b(\d{2,6})\b/g);
        if (m) for (const nStr of m) { const n = parseInt(nStr); if (n >= 100 && n <= 100000) big.push(n); }
      }
      if (big.length > 0) { big.sort((a,b) => b-a); return big[0]; }
      return 0;
    });
    attempts++;
    if (ci >= 500) return ci;
  }
  return 0;
}

(async () => {
  const resultPath = path.join(process.cwd(), "vocab_single.json");
  const debugPath = path.join(process.cwd(), "vocab_debug.json");
  const mastery = 0.15 + Math.random() * 0.70;
  const probs = generateProbabilities(mastery);
  console.log("[scrape] mastery=" + mastery.toFixed(3));

  const browser = await chromium.launch({ headless: false,
    args: ["--no-sandbox", "--disable-blink-features=AutomationControlled", "--start-maximized"] });
  const context = await browser.newContext({ userAgent: "Mozilla/5.0...", viewport: { width: 1280, height: 800 }, locale: "en-US" });
  const page = await context.newPage();
  page.setDefaultTimeout(120000);

  try {
    await page.goto("https://preply.com/en/learn/english/test-your-vocab", { timeout: 120000, waitUntil: "networkidle" });
    await page.waitForTimeout(2000);

    const allKnown = [], allUnknown = [];
    let allDifficulties = {};  // track difficulty distribution

    for (let pg = 0; pg < 6; pg++) {
      await page.waitForTimeout(1500);
      const cs = page.locator("input[type=checkbox]:visible");
      const n = await cs.count().catch(() => 0);
      const cont = page.locator("button").filter({ hasText: /continue|next/i }).first();
      const hasCont = await cont.isVisible().catch(() => false);
      if (n === 0 && !hasCont) break;

      if (n === 0) { await cont.click({ timeout: 10000 }); await page.waitForTimeout(3000); continue; }

      const pageWords = [];
      for (let i = 0; i < n; i++) {
        const wt = await extractWord(page, cs.nth(i));
        if (wt) pageWords.push({ index: i, text: wt, checkbox: cs.nth(i), known: false });
      }

      const texts = pageWords.map(w => w.text);
      const difficulties = await lookupDifficulties(texts);
      const useDefault = difficulties === null || Object.keys(difficulties).length === 0;

            for (const w of pageWords) {
        const diff = useDefault ? "UNKNOWN" : (difficulties[w.text] || "UNKNOWN");
        w.difficulty = diff;
        allDifficulties[w.text] = diff;
        const prob = useDefault ? DEFAULT_PROB : (probs[diff] || probs["UNKNOWN"]);
        // Decision: is the word known? Based on probability model, NOT checkbox interaction
        const wordKnown = Math.random() < prob;
        if (wordKnown) {
          // Try to check the box (visual feedback only - may or may not register on TVY)
          try {
            await w.checkbox.scrollIntoViewIfNeeded();
            await page.waitForTimeout(30);
            // Quick dispatchEvent attempt - if it works, great, if not, we still record the word as known
            await w.checkbox.evaluate(el => el.dispatchEvent(new MouseEvent("click", { bubbles: true, cancelable: true, view: window }))).catch(() => {});
          } catch(e) {}
          w.known = true;
          allKnown.push(w.text);
        } else {
          // Word is unknown - don't click anything
          allUnknown.push(w.text);
        }
      }console.log("[scrape] P" + (pg+1) + ": " + pageWords.length + " words");

      if (hasCont) { await cont.scrollIntoViewIfNeeded(); await page.waitForTimeout(300); await cont.click({ timeout: 15000 }); await page.waitForTimeout(3000); }
      else break;
    }

    const est = await extractCi(page);
    console.log("[scrape] Ci=" + est + " Known=" + allKnown.length + " Unknown=" + allUnknown.length);

    // Compute difficulty distribution for debugging
    const diffCounts = {};
    for (const w of allKnown) {
      const d = allDifficulties[w] || "UNKNOWN";
      diffCounts[d] = (diffCounts[d] || 0) + 1;
    }
    console.log("[scrape] Known difficulty distribution:", JSON.stringify(diffCounts));

    // Also save debug info
    const totalByDiff = {};
    for (const [word, diff] of Object.entries(allDifficulties)) {
      totalByDiff[diff] = (totalByDiff[diff] || 0) + 1;
    }
    const debug = {
      mastery: mastery,
      knownWords: allKnown,
      unknownWords: allUnknown,
      standardEstimate: est,
      difficultyDistribution: { known: diffCounts, total: totalByDiff }
    };
    fs.writeFileSync(debugPath, JSON.stringify(debug, null, 2));

    console.log("[scrape] allKnown=" + allKnown.length + " allUnknown=" + allUnknown.length);
    console.log("[scrape] Difficulty lookup unavailable for unknownWords check.");
    const d = { knownWords: allKnown.map(w => ({ word: w, difficulty: allDifficulties[w] || 'UNKNOWN' })), unknownWords: allUnknown.map(w => ({ word: w, difficulty: allDifficulties[w] || 'UNKNOWN' })), standardEstimate: est };
    fs.writeFileSync(resultPath, JSON.stringify(d));
    console.log("[scrape] Saved");

  } catch(e) {
    console.log("[scrape] Error:", e.message);
    fs.writeFileSync(resultPath, JSON.stringify({ knownWords: [], unknownWords: [], standardEstimate: 0 }));
  }
  await browser.close();
})();
