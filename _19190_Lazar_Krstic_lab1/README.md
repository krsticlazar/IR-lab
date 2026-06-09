# Lab 1 - Lucene Indexing and Search

This project demonstrates basic Apache Lucene indexing and querying over plain text files.

The application indexes four original text documents, splits them into 400 smaller parts, creates two separate Lucene indexes, compares their size and indexing time, and runs the required search queries.

## Requirements

- Eclipse IDE
- Java 21 or newer
- Lucene JAR files in the `jar` folder

## Import in Eclipse

1. Open Eclipse.
2. Select `File -> Import`.
3. Select `General -> Existing Projects into Workspace`.
4. Use `Lab vezbe` as the root directory.
5. Select `_19190_Lazar_Krstic_lab1`.
6. Click `Finish`.

## Run

Run the `Main` class:

```text
Right click Main.java -> Run As -> Java Application
```

The full workflow is executed automatically:

1. Split the original text files into 400 parts.
2. Create `IndeksOriginalni` over the four original files.
3. Create `IndeksDelovi` over the 400 generated parts.
4. Compare index size and creation time.
5. Execute `BooleanQuery` and `TermRangeQuery`.

## Expected Result

The console should show:

- 4 original documents
- 400 generated document parts
- index statistics for `IndeksOriginalni`
- index statistics for `IndeksDelovi`
- comparison of index size and indexing time
- search results with score, file name, file size, and file path

The required query type for the student index `19190` is `TermRangeQuery`, because the index ends with `0`.

## Notes

The indexes are recreated on every run because the project uses `OpenMode.CREATE`. Manual index cleanup is not required.
