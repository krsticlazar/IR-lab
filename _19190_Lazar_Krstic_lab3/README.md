# Lab 3 - Lucene and Apache Tika

This project demonstrates indexing documents in different formats with Apache Lucene and Apache Tika.

Plain text documents are indexed directly with Lucene. Documents such as HTML, RTF, and PDF are first processed with Apache Tika, which extracts their textual content. Lucene then indexes that extracted text.

## Requirements

- Eclipse IDE
- Java 21 or newer
- Lucene JAR files in the `jar` folder
- `tika-app-3.3.1.jar` in the `jar` folder

## Import in Eclipse

1. Open Eclipse.
2. Select `File -> Import`.
3. Select `General -> Existing Projects into Workspace`.
4. Use `Lab vezbe` as the root directory.
5. Select `_19190_Lazar_Krstic_lab3`.
6. Click `Finish`.
7. Run `Project -> Clean` if Eclipse needs to refresh build settings.

## Run

Run the `Main` class:

```text
Right click Main.java -> Run As -> Java Application
```

The full workflow is executed automatically:

1. Create `IndeksTxt` over plain `.txt` files.
2. Create `IndeksTika` over HTML, RTF, and PDF files.
3. Print index statistics for both indexes.
4. Execute `BooleanQuery` through the object model.
5. Execute the same Boolean query through `QueryParser`.
6. Execute `TermRangeQuery` through the object model.
7. Execute the same range query through `QueryParser`.

## Expected Result

The console should show:

- 4 indexed plain text documents in `IndeksTxt`
- 3 indexed mixed-format documents in `IndeksTika`
- matching results for the Boolean query
- matching results for the `TermRangeQuery`
- score, format, file size, and file path for each result

Apache Tika is used because Lucene indexes text, while Tika extracts text from non-text document formats.

## Notes

The indexes are recreated on every run because the project uses `OpenMode.CREATE`.
