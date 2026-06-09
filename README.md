# Information Retrieval Laboratory Exercises

This repository contains laboratory exercises for the Information Retrieval course at the Faculty of Electronic Engineering.

The projects demonstrate the core workflow of an information retrieval system: preparing documents, building indexes, executing queries, ranking results, extracting content from different document formats, and exposing search through a small web application.

## Laboratory Projects

### Lab 1 - Lucene Indexing and Search

The first project uses Apache Lucene to index four plain text books and 400 generated document parts. It compares index size and creation time for original documents versus split documents, then executes Boolean queries and `TermRangeQuery`.

Project folder:

```text
_19190_Lazar_Krstic_lab1
```

### Lab 2 - Similarity Models and Ranking

The second project compares Lucene ranking behavior with `BM25Similarity` and `ClassicSimilarity`. It searches the same document collection using the same Boolean query, prints result scores, explains score calculation with Lucene `Explanation`, and demonstrates query boosting.

Project folder:

```text
_19190_Lazar_Krstic_lab2
```

### Lab 3 - Lucene and Apache Tika

The third project compares indexing plain `.txt` files directly with indexing documents in different formats through Apache Tika. Tika extracts textual content from HTML, RTF, and PDF documents, and Lucene indexes the extracted text.

Project folder:

```text
_19190_Lazar_Krstic_lab3
```

### Lab 4 - Web Search Application

The fourth project is a small search application built with a .NET Web API backend, React + TypeScript frontend, and Elasticsearch. It indexes 400 split text documents, supports field-based search, paginates results, displays scores and metadata, and provides document download links.

Project folder:

```text
_19190_Lazar_Krstic_lab4
```

## Technologies

- Java and Apache Lucene
- Apache Tika
- .NET Web API
- React and TypeScript
- Elasticsearch
- Docker

## Notes

The laboratory material folders contain assignment text, defense notes, and generated outputs. The project folders contain the runnable implementations.
