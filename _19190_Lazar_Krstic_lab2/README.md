# Lab 2 - Similarity Models and Ranking

This project compares Lucene result ranking with two similarity models: `BM25Similarity` and `ClassicSimilarity`.

The same document collection is indexed twice, once with BM25 and once with Classic similarity. The same Boolean query is then executed over both indexes so that score calculation and ranking differences can be observed.

## Requirements

- Eclipse IDE
- Java 21 or newer
- Lucene JAR files in the `jar` folder

## Import in Eclipse

1. Open Eclipse.
2. Select `File -> Import`.
3. Select `General -> Existing Projects into Workspace`.
4. Use `Lab vezbe` as the root directory.
5. Select `_19190_Lazar_Krstic_lab2`.
6. Click `Finish`.

## Run

Run the `Main` class:

```text
Right click Main.java -> Run As -> Java Application
```

The full workflow is executed automatically:

1. Create `IndeksBM25` using `BM25Similarity`.
2. Create `IndeksClassic` using `ClassicSimilarity`.
3. Execute the Boolean query `yellow AND wallpaper`.
4. Search both the `naslov` and `sadrzaj` fields.
5. Print result scores.
6. Print Lucene `Explanation` output.
7. Compare scores for the same document.
8. Apply `BoostQuery` to the lower-scoring query.

## Expected Result

The console should show:

- index statistics for both similarity models
- results for title and content search
- score values for matching documents
- detailed `Explanation` output for score calculation
- calculated boost factor and boosted query result

The most important defense point is that BM25 and Classic scores are not on the same absolute scale. They are useful for ranking inside the same similarity model, not for direct cross-model numeric comparison.

## Notes

The indexes are recreated on every run because the project uses `OpenMode.CREATE`.
