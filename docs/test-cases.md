# Test Cases

## 1. Online GUI Test

### Test 1.1: Complete Online Test
1. Navigate to Online Test page (login first from Home)
2. Click Start New Test
3. Mark first 20 words as Know, remaining as Dont Know
4. Click Submit Test
5. Expected: Result shows estimate, range, confidence

### Test 1.2: Repeat Test (3-5 times)
1. Complete Test 1.1, click Test Again 2 more times
2. Expected: History chart shows all test results with trend line

### Test 1.3: Partial Answer Prevention
1. Start test, leave some words unanswered
2. Click Submit Test
3. Expected: Warning to answer all words

## 2. Batch Processing

### Test 2.1: Text Input
Input:
`
apple, known
philosophy, unknown
love, known
`
1. Paste into Batch Process page
2. Click Batch Calculate
3. Expected: Table with estimates for each word

### Test 2.2: File Upload
1. Upload test-data/sample-word-list.txt
2. Click Batch Calculate
3. Expected: All 20 words processed

### Test 2.3: Export Excel
1. After Test 2.1 or 2.2, click Export Excel
2. Expected: batch_results.xlsx downloaded

### Test 2.4: Sampling Test
1. Set sample length=200, know ratio=50
2. Click Run 900 Samples
3. Expected: Mean and variance displayed

## 3. Corpus Analysis

### Test 3.1: Import and Analyze
Upload sample-corpus-k.txt, p.txt, f.txt, c.txt for types K/P/F/C
Click Analyze All Corpuses
Expected: Table shows vocabulary estimates

## 4. Algorithm Validation

### Test 4.1: Validate Against TestYourVocab
Upload test-data/sample-validation.json
Click Run Validation
Expected: Shows Mean Error, Bias, Correlation, comparison chart

## 5. Statistics

### Test 5.1: Correlation Stats
Register users with CET scores, complete tests
Navigate to Statistics page
Expected: Scatter chart shows score vs vocabulary

## 6. Algorithm Correctness

### Test 6.1: Frequency Weighting
Test A: Know 10 high-frequency words
Test B: Know 10 low-frequency C-level words
Expected: Test B yields higher estimate

### Test 6.2: Level Calibration
Test with all K-level words known and all C-level words known
Expected: High estimate reflecting C-level vocabulary size
