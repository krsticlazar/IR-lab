namespace Lab4.Api.Models;

public sealed record IndexSummary(
    int IndexedDocuments,
    string IndexName,
    string DocumentsDirectory,
    DateTime IndexedAt);

public sealed record IndexStatus(
    bool Exists,
    long DocumentCount,
    string IndexName,
    string DocumentsDirectory);
