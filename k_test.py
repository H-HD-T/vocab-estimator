k_words = [
    "a","about","after","again","all","also","am","an","and","any","are","around","as","ask","at","away",
    "baby","back","bad","bag","ball","be","bear","beautiful","bed","before","begin","behind","best","better"
]
# more words added from separate file
words = set(k_words)
with open("C:/Users/hjh/Downloads/1 初中-乱序.txt", encoding="utf-8", errors="replace") as f:
    for line in f:
        line=line.strip()
        if not line: continue
        w=line.split("\t")[0].strip().lower()
        if w: words.add(w)
print("Combined:", len(words))
